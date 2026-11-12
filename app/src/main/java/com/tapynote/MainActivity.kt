package com.tapynote

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import android.widget.Button

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mgr = AppWidgetManager.getInstance(this)
        val add = findViewById<Button>(R.id.add_widget)
        if (mgr.isRequestPinAppWidgetSupported) {
            add.setOnClickListener {
                mgr.requestPinAppWidget(ComponentName(this, NoteWidget::class.java), null, null)
            }
        } else {
            add.isEnabled = false
        }
    }
}
