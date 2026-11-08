@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.trekdeck.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.awaitFirstDown

// ===================================================================
// Корень
// ===================================================================

@Composable
fun TrekApp(vm: DeckVM) {
    TrekTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
                .displayCutoutPadding()
        ) {
            when (vm.state) {
                is ConnState.Connected -> DeckRoot(vm)
                else -> ConnectScreen(vm)
            }
        }
    }
}

/** Касание пятью пальцами одновременно — переключение «панель ⇄ трекпад». События не съедаются. */
@Composable
fun Modifier.fiveFingers(onToggle: () -> Unit): Modifier {
    val cb by rememberUpdatedState(onToggle)
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            var fired = false
            while (true) {
                val e = awaitPointerEvent(PointerEventPass.Initial)
                val count = e.changes.count { it.pressed }
                if (count >= 5 && !fired) {
                    fired = true
                    cb()
                } else if (count == 0) {
                    fired = false
                }
            }
        }
    }
}

@Composable
fun DeckRoot(vm: DeckVM) {
    var trackpad by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().fiveFingers { trackpad = !trackpad }) {
        if (trackpad) {
            TrackpadScreen(vm, onBack = { trackpad = false })
        } else {
            DeckScreen(vm, onTrackpad = { trackpad = true })
        }
    }
}

// ===================================================================
// Подключение
// ===================================================================

@Composable
fun ConnectScreen(vm: DeckVM) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Accent, AccentDeep))),
                contentAlignment = Alignment.Center,
            ) {
                Text("▦", fontSize = 48.sp, color = Color.White)
            }
            Spacer(Modifier.height(14.dp))
            Text("ТрекДек", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = TextMain)
            Text(
                "телефон как стрим-панель и трекпад для ПК",
                color = TextDim, fontSize = 13.sp, textAlign = TextAlign.Center,
            )
        }

        Column(
            Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Подключение к ПК", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextMain)
            Spacer(Modifier.height(8.dp))

            if (vm.found.isNotEmpty()) {
                vm.found.forEach { f ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF13234F))
                            .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { vm.host = f.host }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🖥️", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(f.name, fontWeight = FontWeight.SemiBold, color = TextMain)
                            Text(f.host, fontSize = 12.sp, color = TextDim)
                        }
                    }
                }
            } else {
                Text(
                    if (vm.searching) "Ищу ПК в сети…" else "ПК автоматически не найден — введи адрес вручную",
                    color = TextDim, fontSize = 13.sp,
                )
            }
            TextButton(onClick = { vm.discover() }, enabled = !vm.searching) { Text("Искать снова") }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = vm.host,
                    onValueChange = { vm.host = it },
                    label = { Text("Адрес ПК") },
                    placeholder = { Text("192.168.1.10") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.weight(1.4f),
                )
                OutlinedTextField(
                    value = vm.pin,
                    onValueChange = { vm.pin = it.filter(Char::isDigit).take(6) },
                    label = { Text("PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))

            val st = vm.state
            Button(
                onClick = { vm.connect() },
                enabled = st !is ConnState.Connecting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentDeep, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(if (st is ConnState.Connecting) "Подключаюсь…" else "Подключиться", fontWeight = FontWeight.Bold)
            }
            if (st is ConnState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(st.msg, color = Color(0xFFFCA5A5), fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "На ПК запусти TrekDeck-PC.exe — в его окне будет PIN и адрес.",
                color = TextDim, fontSize = 12.sp,
            )
        }
    }
}

// ===================================================================
// Панель
// ===================================================================

@Composable
fun Pill(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    onLong: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(AccentDeep, Accent))
                else Brush.horizontalGradient(listOf(Color(0xFF13234F), Color(0xFF13234F)))
            )
            .border(1.dp, if (selected) Accent else Color(0xFF29427F), shape)
            .combinedClickable(onClick = onClick, onLongClick = onLong)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Color.White else TextDim,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun DeckScreen(vm: DeckVM, onTrackpad: () -> Unit) {
    var pageIdx by rememberSaveable { mutableIntStateOf(0) }
    var edit by rememberSaveable { mutableStateOf(false) }
    var editSlot by remember { mutableStateOf<Int?>(null) }
    var pageDialog by remember { mutableStateOf<Int?>(null) } // индекс страницы, -1 = новая
    val haptic = LocalHapticFeedback.current

    val pages = vm.pages
    if (pages.isEmpty()) return
    if (pageIdx !in pages.indices) pageIdx = 0
    val page = pages[pageIdx]

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ТрекДек", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Accent)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34D399)))
            Spacer(Modifier.width(14.dp))
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(pages) { i, p ->
                    Pill(
                        p.name, selected = i == pageIdx,
                        onClick = { pageIdx = i },
                        onLong = { pageDialog = i },
                    )
                }
                item { Pill("＋", onClick = { pageDialog = -1 }) }
            }
            Spacer(Modifier.width(10.dp))
            Pill(if (edit) "✔ Готово" else "✎ Правка", selected = edit, onClick = { edit = !edit })
            Spacer(Modifier.width(8.dp))
            Pill("🖱 Трекпад", onClick = onTrackpad)
        }
        Spacer(Modifier.height(10.dp))

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (r in 0 until ROWS) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (c in 0 until COLS) {
                        val slot = r * COLS + c
                        Tile(
                            btn = page.buttons[slot],
                            edit = edit,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (edit) editSlot = slot else vm.run(page.buttons[slot].script)
                            },
                            onLong = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editSlot = slot
                            },
                        )
                    }
                }
            }
        }
        if (edit) {
            Text(
                "Режим правки: нажми на плитку, чтобы изменить. Долгое нажатие на вкладку — переименовать или удалить страницу.",
                color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    editSlot?.let { slot ->
        BtnEditor(
            initial = page.buttons[slot],
            onSave = { vm.setBtn(pageIdx, slot, it); editSlot = null },
            onClear = { vm.setBtn(pageIdx, slot, Btn()); editSlot = null },
            onDismiss = { editSlot = null },
        )
    }
    pageDialog?.let { idx ->
        PageDialog(
            isNew = idx < 0,
            initialName = if (idx >= 0) pages[idx].name else "",
            canDelete = idx >= 0 && pages.size > 1,
            onSave = { name ->
                if (idx < 0) {
                    vm.addPage(name)
                    pageIdx = vm.pages.size - 1
                } else vm.renamePage(idx, name)
                pageDialog = null
            },
            onDelete = {
                vm.deletePage(idx)
                pageIdx = 0
                pageDialog = null
            },
            onDismiss = { pageDialog = null },
        )
    }
}

@Composable
fun Tile(
    btn: Btn,
    edit: Boolean,
    modifier: Modifier,
    onTap: () -> Unit,
    onLong: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, label = "tileScale")
    val shape = RoundedCornerShape(20.dp)
    val col = tileColor(btn.color)

    val bg = when {
        btn.isEmpty -> Brush.verticalGradient(listOf(Color(0xFF0B1636), Color(0xFF0B1636)))
        pressed -> Brush.verticalGradient(listOf(col.copy(alpha = 0.60f), col.copy(alpha = 0.25f)))
        else -> Brush.verticalGradient(listOf(col.copy(alpha = 0.22f), Color(0xFF0B1636)))
    }
    val border = when {
        btn.isEmpty -> Color(0xFF1B2F66)
        pressed -> col
        else -> col.copy(alpha = 0.55f)
    }

    Box(
        modifier
            .scale(scale)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(if (pressed) 2.dp else 1.dp, border), shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { if (!btn.isEmpty || edit) onTap() },
                onLongClick = onLong,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (btn.isEmpty) {
            if (edit) Text("＋", fontSize = 28.sp, color = TextDim)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(btn.icon.ifBlank { "◼" }, fontSize = 30.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    btn.label,
                    color = TextMain,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

// ===================================================================
// Диалоги редактирования
// ===================================================================

private val IconPresets = listOf(
    "⌨️", "🌐", "🎵", "📝", "📁", "📸", "🔇", "🎤", "🎬", "🎮", "⏯️", "⏭️",
    "⏮️", "🔊", "🔉", "🔒", "🧮", "🖥️", "📋", "↩️", "🔴", "💬", "⚙️", "⭐",
    "🚀", "💡", "📡", "📷", "🔎", "✂️", "▶️", "🕹️",
)

@Composable
fun BtnEditor(initial: Btn, onSave: (Btn) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    var label by remember(initial) { mutableStateOf(initial.label) }
    var icon by remember(initial) { mutableStateOf(initial.icon) }
    var color by remember(initial) { mutableIntStateOf(initial.color) }
    var script by remember(initial) { mutableStateOf(initial.script) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("Плитка", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = icon, onValueChange = { icon = it.take(4) },
                        label = { Text("Иконка") }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = label, onValueChange = { label = it.take(20) },
                        label = { Text("Название") }, singleLine = true, modifier = Modifier.weight(2f),
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(IconPresets) { _, ic ->
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (ic == icon) AccentDeep else Color(0xFF13234F))
                                .clickable { icon = ic },
                            contentAlignment = Alignment.Center,
                        ) { Text(ic, fontSize = 20.sp) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Цвет", color = TextDim, fontSize = 13.sp)
                    TilePalette.forEachIndexed { i, c ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (i == color) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { color = i }
                        )
                    }
                }
                OutlinedTextField(
                    value = script, onValueChange = { script = it },
                    label = { Text("Команды (по одной в строке)") },
                    minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "key ctrl+shift+m  — сочетание клавиш\n" +
                        "type текст  — напечатать\n" +
                        "open https://…  — открыть сайт, файл, папку\n" +
                        "start notepad.exe  — запустить программу\n" +
                        "media playpause|next|prev|volup|voldown|mute\n" +
                        "wait 500  — пауза, мс",
                    color = TextDim, fontSize = 11.sp, lineHeight = 15.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(Btn(label.trim(), icon.trim(), color, script.trim())) }) { Text("Сохранить") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Очистить", color = Color(0xFFFCA5A5)) }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

@Composable
fun PageDialog(
    isNew: Boolean,
    initialName: String,
    canDelete: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(if (isNew) "Новая страница" else "Страница", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(16) },
                label = { Text("Название") }, singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }) { Text(if (isNew) "Создать" else "Сохранить") } },
        dismissButton = {
            Row {
                if (canDelete) TextButton(onClick = onDelete) { Text("Удалить", color = Color(0xFFFCA5A5)) }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

// ===================================================================
// Трекпад
// ===================================================================

@Composable
fun TrackpadScreen(vm: DeckVM, onBack: () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val tapSlop = 14f * density
    val haptic = LocalHapticFeedback.current

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("← Панель", onClick = onBack)
            Spacer(Modifier.width(12.dp))
            Text(
                "Трекпад · 1 палец — курсор, тап — клик, 2 пальца — прокрутка / ПКМ · 5 пальцев — назад",
                color = TextDim, fontSize = 12.sp, maxLines = 2,
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0E2358), Color(0xFF081231))))
                .border(1.dp, Accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                .pointerInput(tapSlop) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var maxPointers = 1
                            var travelled = 0f
                            var lastUp = down.uptimeMillis
                            var lastEventTime = down.uptimeMillis
                            var scrollAcc = 0f
                            do {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                lastEventTime = event.changes.maxOf { it.uptimeMillis }
                                if (pressed.size > maxPointers) maxPointers = pressed.size
                                if (pressed.size == 1 && maxPointers == 1) {
                                    val d: Offset = pressed[0].positionChange()
                                    travelled += kotlin.math.abs(d.x) + kotlin.math.abs(d.y)
                                    if (d != Offset.Zero) vm.mouse(d.x * 1.6f, d.y * 1.6f)
                                    pressed[0].consume()
                                } else if (pressed.size == 2 && maxPointers == 2) {
                                    val dy = (pressed[0].positionChange().y + pressed[1].positionChange().y) / 2f
                                    travelled += kotlin.math.abs(dy)
                                    scrollAcc += -dy * 2.5f // тянешь вверх — страница едет вниз, как на трекпаде
                                    val units = scrollAcc.toInt()
                                    if (units != 0) {
                                        vm.scroll(units)
                                        scrollAcc -= units
                                    }
                                    pressed.forEach { it.consume() }
                                } else {
                                    event.changes.forEach { it.consume() }
                                }
                                if (pressed.isNotEmpty()) lastUp = lastEventTime
                            } while (event.changes.any { it.pressed })

                            val duration = lastEventTime - down.uptimeMillis
                            if (travelled < tapSlop && duration < 300) {
                                when (maxPointers) {
                                    1 -> vm.click("left")
                                    2 -> vm.click("right")
                                    3 -> vm.click("middle")
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("▦", fontSize = 72.sp, color = Accent.copy(alpha = 0.12f))
        }

        Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ClickBar("ЛКМ", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.click("left") }
            ClickBar("СКМ", Modifier.weight(0.5f)) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.click("middle") }
            ClickBar("ПКМ", Modifier.weight(1f)) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.click("right") }
        }
    }
}

@Composable
private fun ClickBar(text: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .fillMaxHeight()
            .clip(shape)
            .background(Color(0xFF13234F))
            .border(1.dp, Color(0xFF29427F), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TextMain, fontWeight = FontWeight.Bold)
    }
}
