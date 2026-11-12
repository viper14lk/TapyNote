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
        widgetsFor(ctx, id).forEach { unbind(ctx, it) }
    }

    // ---- widget <-> note mapping ----
    fun noteForWidget(ctx: Context, widgetId: Int): Long =
        prefs(ctx).getLong("w_$widgetId", -1)

    fun bind(ctx: Context, widgetId: Int, noteId: Long) =
        prefs(ctx).edit().putLong("w_$widgetId", noteId).apply()

    fun unbind(ctx: Context, widgetId: Int) =
        prefs(ctx).edit().remove("w_$widgetId").apply()

    fun widgetsFor(ctx: Context, noteId: Long): List<Int> =
        prefs(ctx).all.filter { it.key.startsWith("w_") && it.value == noteId }
            .map { it.key.removePrefix("w_").toInt() }
}
