package com.tapynote

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

class NoteWidget : AppWidgetProvider() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        when (intent.action) {
            ACTION_PINNED -> {
                // Pinned from inside the app: this widget starts with the chosen note.
                val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
                if (widgetId != -1 && noteId != -1L) {
                    NoteStore.tabs(ctx, widgetId).ids
                        .filter { it != noteId && NoteStore.get(ctx, it)?.text?.isBlank() == true }
                        .forEach { NoteStore.delete(ctx, it) }
                    NoteStore.setTabs(ctx, widgetId, NoteStore.Tabs(mutableListOf(noteId), 0))
                    update(ctx, AppWidgetManager.getInstance(ctx), widgetId)
                }
            }
            ACTION_SELECT -> if (widgetId != -1) {
                val t = NoteStore.tabs(ctx, widgetId)
                t.active = intent.getIntExtra(EXTRA_TAB, 0)
                NoteStore.setTabs(ctx, widgetId, t)
                update(ctx, AppWidgetManager.getInstance(ctx), widgetId)
            }
            else -> super.onReceive(ctx, intent)
        }
    }

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(ctx, mgr, it) }
    }

    override fun onDeleted(ctx: Context, ids: IntArray) {
        ids.forEach { NoteStore.unbind(ctx, it) } // notes stay in the app
    }

    companion object {
        const val ACTION_PINNED = "com.tapynote.PINNED"
        const val ACTION_SELECT = "com.tapynote.SELECT"
        const val EXTRA_NOTE_ID = "note_id"
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_NEW_TAB = "new_tab"
        const val EXTRA_TAB = "tab"

        private val TAB_IDS = intArrayOf(R.id.tab0, R.id.tab1, R.id.tab2, R.id.tab3, R.id.tab4, R.id.tab5)
        private val TAB_ON = intArrayOf(R.drawable.tab_on_0, R.drawable.tab_on_1, R.drawable.tab_on_2,
            R.drawable.tab_on_3, R.drawable.tab_on_4, R.drawable.tab_on_5, R.drawable.tab_on_6)
        private val TAB_OFF = intArrayOf(R.drawable.tab_off_0, R.drawable.tab_off_1, R.drawable.tab_off_2,
            R.drawable.tab_off_3, R.drawable.tab_off_4, R.drawable.tab_off_5, R.drawable.tab_off_6)

        private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        fun update(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
            val t = NoteStore.tabs(ctx, widgetId)
            if (t.ids.isEmpty()) {
                // Freshly dropped from the widget picker: start with one new note.
                t.ids.add(NoteStore.create(ctx).id)
                NoteStore.setTabs(ctx, widgetId, t)
            }
            val note = NoteStore.get(ctx, t.activeId!!)!!
            val c = note.color.coerceIn(NoteColors.widgetBg.indices)
            val views = RemoteViews(ctx.packageName, R.layout.widget_note)

            // Body
            val blank = note.text.isBlank()
            views.setTextViewText(R.id.note_text, if (blank) "Tap to write a note…" else note.text)
            views.setTextColor(R.id.note_text, if (blank) NoteColors.hint(c) else NoteColors.text(c))
            views.setInt(R.id.body, "setBackgroundResource", NoteColors.widgetBg[c])
            views.setOnClickPendingIntent(R.id.body, PendingIntent.getActivity(ctx, widgetId * 10,
                editIntent(ctx, widgetId).putExtra(EXTRA_NOTE_ID, note.id), FLAGS))

            // Tabs: one small coloured tab per note, the active one stands taller.
            TAB_IDS.forEachIndexed { i, viewId ->
                val id = t.ids.getOrNull(i)
                if (id == null) { views.setViewVisibility(viewId, View.GONE); return@forEachIndexed }
                val tc = (NoteStore.get(ctx, id)?.color ?: 0).coerceIn(TAB_ON.indices)
                views.setViewVisibility(viewId, View.VISIBLE)
                views.setInt(viewId, "setBackgroundResource",
                    if (i == t.active) TAB_ON[tc] else TAB_OFF[tc])
                val sel = Intent(ctx, NoteWidget::class.java).setAction(ACTION_SELECT)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId).putExtra(EXTRA_TAB, i)
                views.setOnClickPendingIntent(viewId,
                    PendingIntent.getBroadcast(ctx, widgetId * 10 + i + 1, sel, FLAGS))
            }

            // "+" to add a tab (hidden at the limit)
            if (t.ids.size < NoteStore.MAX_TABS) {
                views.setViewVisibility(R.id.tab_add, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.tab_add, PendingIntent.getActivity(ctx, widgetId * 10 + 8,
                    editIntent(ctx, widgetId).putExtra(EXTRA_NEW_TAB, true), FLAGS))
            } else views.setViewVisibility(R.id.tab_add, View.GONE)

            mgr.updateAppWidget(widgetId, views)
        }

        private fun editIntent(ctx: Context, widgetId: Int) =
            Intent(ctx, EditNoteActivity::class.java)
                .putExtra(EXTRA_WIDGET_ID, widgetId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        fun updateForNote(ctx: Context, noteId: Long) {
            val mgr = AppWidgetManager.getInstance(ctx)
            NoteStore.widgetsFor(ctx, noteId).forEach { update(ctx, mgr, it) }
        }

        fun updateWidget(ctx: Context, widgetId: Int) =
            update(ctx, AppWidgetManager.getInstance(ctx), widgetId)

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
