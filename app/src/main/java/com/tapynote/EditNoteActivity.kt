package com.tapynote

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText

/** Pops up over the home screen; saves automatically when dismissed. */
class EditNoteActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var editor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        editor = findViewById(R.id.editor)
        editor.setText(NoteStore.get(this, widgetId))
        editor.setSelection(editor.text.length)
        editor.requestFocus()

        findViewById<android.view.View>(R.id.done).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.clear).setOnClickListener { editor.setText("") }
    }

    override fun onPause() {
        super.onPause()
        if (::editor.isInitialized) {
            NoteStore.set(this, widgetId, editor.text.toString())
            NoteWidget.update(this, AppWidgetManager.getInstance(this), widgetId)
        }
    }
}
