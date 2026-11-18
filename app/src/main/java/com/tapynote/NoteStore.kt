package com.tapynote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Note(val id: Long, var text: String, var color: Int, var updated: Long)

/** Notes live in the app; widgets just point at a note. */
object NoteStore {
    private const val PREFS = "tapynote"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun all(ctx: Context): List<Note> {
        val arr = JSONArray(prefs(ctx).getString("notes", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Note(o.getLong("id"), o.getString("text"), o.getInt("color"), o.getLong("updated"))
        }.sortedByDescending { it.updated }
    }

    private fun saveAll(ctx: Context, notes: List<Note>) {
        val arr = JSONArray()
        notes.forEach {
            arr.put(JSONObject().put("id", it.id).put("text", it.text)
                .put("color", it.color).put("updated", it.updated))
        }
        prefs(ctx).edit().putString("notes", arr.toString()).apply()
    }

    fun get(ctx: Context, id: Long) = all(ctx).find { it.id == id }

    fun create(ctx: Context): Note {
        val now = System.currentTimeMillis()
        val note = Note(now, "", 0, now)
        saveAll(ctx, all(ctx) + note)
        return note
    }

    fun save(ctx: Context, note: Note) {
        note.updated = System.currentTimeMillis()
        saveAll(ctx, all(ctx).filter { it.id != note.id } + note)
    }

    fun delete(ctx: Context, id: Long) {
        saveAll(ctx, all(ctx).filter { it.id != id })
        widgetsFor(ctx, id).forEach { w ->
            val t = tabs(ctx, w)
            val idx = t.ids.indexOf(id)
            t.ids.removeAt(idx)
            if (t.active >= idx && t.active > 0) t.active--
            setTabs(ctx, w, t)
        }
    }

    // ---- widget -> tabs (up to 6 notes per widget) ----
    const val MAX_TABS = 6

    class Tabs(val ids: MutableList<Long>, var active: Int) {
        val activeId get() = ids.getOrNull(active.coerceIn(0, (ids.size - 1).coerceAtLeast(0)))
    }

    fun tabs(ctx: Context, widgetId: Int): Tabs {
        val raw = prefs(ctx).all["w_$widgetId"] ?: return Tabs(mutableListOf(), 0)
        if (raw is Long) return Tabs(mutableListOf(raw), 0) // older single-note format
        val (ids, active) = (raw as String).split("|")
        val known = all(ctx).map { it.id }.toSet()
        val list = ids.split(",").mapNotNull { it.toLongOrNull() }.filter { it in known }.toMutableList()
        return Tabs(list, active.toIntOrNull() ?: 0)
    }

    fun setTabs(ctx: Context, widgetId: Int, t: Tabs) {
        t.active = t.active.coerceIn(0, (t.ids.size - 1).coerceAtLeast(0))
        prefs(ctx).edit().putString("w_$widgetId", t.ids.joinToString(",") + "|" + t.active).apply()
    }

    fun unbind(ctx: Context, widgetId: Int) =
        prefs(ctx).edit().remove("w_$widgetId").apply()

    private fun widgetIds(ctx: Context) =
        prefs(ctx).all.keys.filter { it.startsWith("w_") }.map { it.removePrefix("w_").toInt() }

    fun widgetsFor(ctx: Context, noteId: Long): List<Int> =
        widgetIds(ctx).filter { noteId in tabs(ctx, it).ids }
}
