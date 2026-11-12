package com.tapynote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NoteWidget : AppWidgetProvider() {

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == ACTION_PINNED) {
            // Widget pinned from inside the app: point it at the chosen note.
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
            if (widgetId != -1 && noteId != -1L) {
                val auto = NoteStore.noteForWidget(ctx, widgetId)
                if (auto != noteId && NoteStore.get(ctx, auto)?.text?.isBlank() == true) {
                    NoteStore.delete(ctx, auto)
                }
                NoteStore.bind(ctx, widgetId, noteId)
                update(ctx, AppWidgetManager.getInstance(ctx), widgetId)
            }
            return
        }
        super.onReceive(ctx, intent)
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        ids.forEach { NoteStore.unbind(ctx, it) } // note stays in the app
    }

    companion object {
        const val ACTION_PINNED = "com.tapynote.PINNED"
        const val EXTRA_NOTE_ID = "note_id"

        fun update(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
            var note = NoteStore.get(ctx, NoteStore.noteForWidget(ctx, widgetId))
            if (note == null) {
                // Freshly dropped from the widget picker: give it a new note.
                note = NoteStore.create(ctx)
                NoteStore.bind(ctx, widgetId, note.id)
            }
            val views = RemoteViews(ctx.packageName, R.layout.widget_note)
            val blank = note.text.isBlank()
            views.setTextViewText(R.id.note_text, if (blank) "Tap to write a note…" else note.text)
            views.setTextColor(R.id.note_text,
                if (blank) NoteColors.hint(note.color) else NoteColors.text(note.color))
            views.setInt(R.id.widget_root, "setBackgroundResource",
                NoteColors.widgetBg[note.color.coerceIn(NoteColors.widgetBg.indices)])

            val intent = Intent(ctx, EditNoteActivity::class.java)
                .putExtra(EXTRA_NOTE_ID, note.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val pi = PendingIntent.getActivity(ctx, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            mgr.updateAppWidget(widgetId, views)
        }

        fun updateForNote(ctx: Context, noteId: Long) {
            val mgr = AppWidgetManager.getInstance(ctx)
            NoteStore.widgetsFor(ctx, noteId).forEach { update(ctx, mgr, it) }
        }

        fun pin(ctx: Context, noteId: Long): Boolean {
            val mgr = AppWidgetManager.getInstance(ctx)
            if (!mgr.isRequestPinAppWidgetSupported) return false
            val cb = PendingIntent.getBroadcast(ctx, noteId.toInt(),
                Intent(ctx, NoteWidget::class.java).setAction(ACTION_PINNED)
                    .putExtra(EXTRA_NOTE_ID, noteId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            return mgr.requestPinAppWidget(ComponentName(ctx, NoteWidget::class.java), null, cb)
        }
    }
}
