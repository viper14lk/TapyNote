package com.tapynote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NoteWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        ids.forEach { NoteStore.delete(ctx, it) }
    }

    companion object {
        fun update(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val note = NoteStore.get(ctx, id)
            val views = RemoteViews(ctx.packageName, R.layout.widget_note)
            views.setTextViewText(R.id.note_text, note.ifBlank { "Tap to write a note…" })
            views.setTextColor(
                R.id.note_text,
                if (note.isBlank()) 0x99000000.toInt() else 0xFF222222.toInt()
            )

            val intent = Intent(ctx, EditNoteActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pi = PendingIntent.getActivity(
                ctx, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(id, views)
        }
    }
}
