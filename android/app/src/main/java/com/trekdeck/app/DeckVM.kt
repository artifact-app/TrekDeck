package com.trekdeck.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

sealed interface ConnState {
    data object Idle : ConnState
    data object Connecting : ConnState
    data object Connected : ConnState
    data class Error(val msg: String) : ConnState
}

data class Found(val name: String, val host: String)

private const val TCP_PORT = 47600
private const val UDP_PORT = 47601

class DeckVM(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("trekdeck", Context.MODE_PRIVATE)

    var state by mutableStateOf<ConnState>(ConnState.Idle)
    var pages by mutableStateOf<List<Page>>(emptyList())
    var found by mutableStateOf<List<Found>>(emptyList())
    var searching by mutableStateOf(false)
    var host by mutableStateOf(prefs.getString("host", "") ?: "")
    var pin by mutableStateOf(prefs.getString("pin", "") ?: "")
    var pcName by mutableStateOf("")

    private var sock: Socket? = null
    private var outCh: Channel<String>? = null
    private var job: Job? = null
    private var accX = 0f
    private var accY = 0f

    init {
        if (host.isNotBlank() && pin.isNotBlank()) connect()
        discover()
    }

    // ---------- подключение ----------

    fun connect() {
        val h = host.trim()
        val p = pin.trim()
        if (h.isEmpty() || p.isEmpty()) {
            state = ConnState.Error("Введи адрес ПК и PIN")
            return
        }
        closeCurrent()
        state = ConnState.Connecting
        job = viewModelScope.launch(Dispatchers.IO) {
            val s = Socket()
            sock = s
            try {
                s.connect(InetSocketAddress(h, TCP_PORT), 4000)
                s.tcpNoDelay = true
                val output = s.getOutputStream()
                val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                output.write((JSONObject().put("t", "hello").put("pin", p).toString() + "\n").toByteArray(Charsets.UTF_8))
                output.flush()

                s.soTimeout = 6000
                val first = reader.readLine() ?: throw IOException("ПК закрыл соединение")
                val j = JSONObject(first)
                if (j.optString("t") != "ok") throw IOException(j.optString("msg", "Отказ в доступе"))

                s.soTimeout = 45000 // ПК отвечает на ping раз в 15 с
                pages = parsePages(j.optJSONArray("pages"))
                pcName = j.optString("name")
                prefs.edit().putString("host", h).putString("pin", p).apply()

                val ch = Channel<String>(Channel.UNLIMITED)
                outCh = ch
                state = ConnState.Connected

                val writer = launch {
                    try {
                        for (m in ch) {
                            output.write((m + "\n").toByteArray(Charsets.UTF_8))
                            output.flush()
                        }
                    } catch (_: Exception) {
                        runCatching { s.close() }
                    }
                }
                val pinger = launch {
                    while (true) {
                        delay(15000)
                        ch.trySend("{\"t\":\"ping\"}")
                    }
                }
                try {
                    while (true) {
                        reader.readLine() ?: break
                    }
                } finally {
                    writer.cancel()
                    pinger.cancel()
                    ch.close()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (sock === s) state = ConnState.Error(friendly(e))
            } finally {
                runCatching { s.close() }
                if (sock === s) {
                    sock = null
                    outCh = null
                    if (state is ConnState.Connected) state = ConnState.Error("Связь с ПК потеряна")
                }
            }
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is SocketTimeoutException, is ConnectException ->
            "Не удалось подключиться. Проверь адрес, что телефон и ПК в одной Wi‑Fi сети и что ТрекДек запущен на ПК."
        else -> e.message ?: "Ошибка соединения"
    }

    private fun closeCurrent() {
        val s = sock
        sock = null
        outCh = null
        job?.cancel()
        runCatching { s?.close() }
    }

    fun disconnect() {
        closeCurrent()
        state = ConnState.Idle
    }

    fun discover() {
        if (searching) return
        searching = true
        viewModelScope.launch(Dispatchers.IO) {
            val list = LinkedHashMap<String, Found>()
            try {
                DatagramSocket().use { ds ->
                    ds.broadcast = true
                    ds.soTimeout = 700
                    val msg = "TREKDECK?".toByteArray()
                    val target = InetAddress.getByName("255.255.255.255")
                    repeat(4) {
                        runCatching { ds.send(DatagramPacket(msg, msg.size, target, UDP_PORT)) }
                        val buf = ByteArray(256)
                        try {
                            while (true) {
                                val pk = DatagramPacket(buf, buf.size)
                                ds.receive(pk)
                                val text = String(pk.data, 0, pk.length)
                                if (text.startsWith("TREKDECK|")) {
                                    val ip = pk.address.hostAddress ?: continue
                                    list[ip] = Found(text.split("|").getOrElse(1) { "ПК" }, ip)
                                }
                            }
                        } catch (_: SocketTimeoutException) {
                        }
                    }
                }
            } catch (_: Exception) {
            }
            found = list.values.toList()
            searching = false
        }
    }

    // ---------- отправка команд ----------

    private fun send(o: JSONObject) {
        outCh?.trySend(o.toString())
    }

    fun run(script: String) {
        if (script.isNotBlank()) send(JSONObject().put("t", "run").put("script", script))
    }

    fun mouse(dx: Float, dy: Float) {
        accX += dx
        accY += dy
        val ix = accX.toInt()
        val iy = accY.toInt()
        if (ix != 0 || iy != 0) {
            accX -= ix
            accY -= iy
            send(JSONObject().put("t", "mouse").put("dx", ix).put("dy", iy))
        }
    }

    fun click(button: String, n: Int = 1) {
        send(JSONObject().put("t", "click").put("b", button).put("n", n))
    }

    fun scroll(wheelUnits: Int) {
        if (wheelUnits != 0) send(JSONObject().put("t", "scroll").put("dy", wheelUnits))
    }

    // ---------- редактирование раскладок ----------

    private fun commit(newPages: List<Page>) {
        pages = newPages
        send(JSONObject().put("t", "save").put("pages", pagesToJson(newPages)))
    }

    fun setBtn(page: Int, slot: Int, btn: Btn) {
        if (page !in pages.indices) return
        val p = pages[page]
        val bs = p.buttons.toMutableList()
        bs[slot] = btn
        commit(pages.toMutableList().also { it[page] = p.copy(buttons = bs) })
    }

    fun addPage(name: String) {
        commit(pages + emptyPage(name.ifBlank { "Страница ${pages.size + 1}" }))
    }

    fun renamePage(page: Int, name: String) {
        if (page !in pages.indices || name.isBlank()) return
        commit(pages.toMutableList().also { it[page] = it[page].copy(name = name.trim()) })
    }

    fun deletePage(page: Int) {
        if (pages.size <= 1 || page !in pages.indices) return
        commit(pages.toMutableList().also { it.removeAt(page) })
    }

    override fun onCleared() {
        closeCurrent()
    }
}
