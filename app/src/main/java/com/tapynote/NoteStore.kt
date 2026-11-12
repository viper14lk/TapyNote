package com.tapynote

import android.content.Context

/** Each widget keeps its own note, keyed by its widget id. */
object NoteStore {
    private const val PREFS = "notes"

    fun get(ctx: Context, id: Int): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("note_$id", "") ?: ""

    fun set(ctx: Context, id: Int, text: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("note_$id", text).apply()
    }

    fun delete(ctx: Context, id: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("note_$id").apply()
    }
}
