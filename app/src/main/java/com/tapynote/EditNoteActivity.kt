package com.tapynote

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast

/** Editor shared by the widget and the app. Saves automatically when closed. */
class EditNoteActivity : Activity() {
    private var note: Note? = null
    private var widgetId = -1
    private var removed = false
    private lateinit var editor: EditText
    private lateinit var card: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        widgetId = intent.getIntExtra(NoteWidget.EXTRA_WIDGET_ID, -1)
        val n = if (intent.getBooleanExtra(NoteWidget.EXTRA_NEW_TAB, false) && widgetId != -1) {
            // "+" tapped on the widget: add a new tab and make it active.
            val t = NoteStore.tabs(this, widgetId)
            if (t.ids.size >= NoteStore.MAX_TABS) { finish(); return }
            val created = NoteStore.create(this)
            created.color = NoteStore.get(this, t.activeId ?: -1)?.color ?: 0
            NoteStore.save(this, created)
            t.ids.add(created.id); t.active = t.ids.size - 1
            NoteStore.setTabs(this, widgetId, t)
            created
        } else NoteStore.get(this, intent.getLongExtra(NoteWidget.EXTRA_NOTE_ID, -1))
            ?: run { finish(); return }
        note = n

        card = findViewById(R.id.card)
        editor = findViewById(R.id.editor)
        editor.setText(n.text)
        editor.setSelection(editor.text.length)
        editor.requestFocus()

        val swatches = findViewById<LinearLayout>(R.id.swatches)
        val d = resources.displayMetrics.density
        NoteColors.fills.indices.forEach { i ->
            swatches.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams((28 * d).toInt(), (28 * d).toInt())
                    .apply { marginEnd = (10 * d).toInt() }
                background = NoteColors.rounded(i, 14 * d).apply { setStroke((1 * d).toInt(), 0x33000000) }
                setOnClickListener { n.color = i; applyColor() }
            })
        }
        applyColor()

        // Opened from a widget with several tabs: allow removing this tab (note stays in the app).
        val remove = findViewById<View>(R.id.remove_tab)
        if (widgetId != -1 && NoteStore.tabs(this, widgetId).ids.size > 1) {
            remove.visibility = View.VISIBLE
            remove.setOnClickListener {
                save()
                val t = NoteStore.tabs(this, widgetId)
                val idx = t.ids.indexOf(n.id)
                if (idx >= 0) {
                    t.ids.removeAt(idx)
                    if (t.active >= idx && t.active > 0) t.active--
                    NoteStore.setTabs(this, widgetId, t)
                }
                NoteWidget.updateWidget(this, widgetId)
                removed = true
                finish()
            }
        }

        findViewById<View>(R.id.done).setOnClickListener { finish() }
        findViewById<View>(R.id.pin).setOnClickListener {
            save()
            if (!NoteWidget.pin(this, n.id))
                Toast.makeText(this, "Add it from your home screen's widget list", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyColor() {
        val n = note ?: return
        card.background = NoteColors.rounded(n.color, 20 * resources.displayMetrics.density)
        editor.setTextColor(NoteColors.text(n.color))
        editor.setHintTextColor(NoteColors.hint(n.color))
    }

    private fun save() {
        val n = note ?: return
        n.text = editor.text.toString()
        NoteStore.save(this, n)
        NoteWidget.updateForNote(this, n.id)
        if (widgetId != -1) NoteWidget.updateWidget(this, widgetId)
    }

    override fun onPause() {
        super.onPause()
        if (!removed) save()
    }
}
