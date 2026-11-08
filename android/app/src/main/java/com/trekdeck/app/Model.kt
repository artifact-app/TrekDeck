package com.trekdeck.app

import org.json.JSONArray
import org.json.JSONObject

const val COLS = 5
const val ROWS = 3
const val SLOTS = COLS * ROWS

data class Btn(
    val label: String = "",
    val icon: String = "",
    val color: Int = 0,
    val script: String = "",
) {
    val isEmpty: Boolean get() = label.isBlank() && icon.isBlank() && script.isBlank()
}

data class Page(val name: String, val buttons: List<Btn>)

fun emptyPage(name: String) = Page(name, List(SLOTS) { Btn() })

fun parsePages(arr: JSONArray?): List<Page> {
    if (arr == null) return emptyList()
    val result = ArrayList<Page>()
    for (i in 0 until arr.length()) {
        val p = arr.optJSONObject(i) ?: continue
        val bs = ArrayList<Btn>()
        val ba = p.optJSONArray("buttons")
        if (ba != null) {
            for (k in 0 until minOf(ba.length(), SLOTS)) {
                val b = ba.optJSONObject(k) ?: JSONObject()
                bs.add(
                    Btn(
                        label = b.optString("label"),
                        icon = b.optString("icon"),
                        color = b.optInt("color"),
                        script = b.optString("script"),
                    )
                )
            }
        }
        while (bs.size < SLOTS) bs.add(Btn())
        result.add(Page(p.optString("name", "Страница"), bs))
    }
    return result
}

fun pagesToJson(pages: List<Page>): JSONArray {
    val arr = JSONArray()
    for (p in pages) {
        val ba = JSONArray()
        for (b in p.buttons) {
            ba.put(
                JSONObject()
                    .put("label", b.label)
                    .put("icon", b.icon)
                    .put("color", b.color)
                    .put("script", b.script)
            )
        }
        arr.put(JSONObject().put("name", p.name).put("buttons", ba))
    }
    return arr
}
