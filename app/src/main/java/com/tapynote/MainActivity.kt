package com.tapynote

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

/** Lists all notes. Tap to edit, long-press to delete, + to add. */
class MainActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        list = findViewById(R.id.list)
        findViewById<TextView>(R.id.add).setOnClickListener { open(NoteStore.create(this).id) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        // Drop notes left empty (e.g. opened then abandoned) unless a widget shows them.
        NoteStore.all(this).filter { it.text.isBlank() && NoteStore.widgetsFor(this, it.id).isEmpty() }
            .forEach { NoteStore.delete(this, it.id) }

        list.removeAllViews()
        val notes = NoteStore.all(this)
        findViewById<TextView>(R.id.empty).visibility =
            if (notes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        val d = resources.displayMetrics.density
        notes.forEach { n ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_note, list, false) as TextView
            row.text = n.text.ifBlank { "Empty note" }
            row.setTextColor(if (n.text.isBlank()) NoteColors.hint(n.color) else NoteColors.text(n.color))
            row.background = NoteColors.rounded(n.color, 16 * d)
            row.setOnClickListener { open(n.id) }
            row.setOnLongClickListener {
                AlertDialog.Builder(this).setMessage("Delete this note?")
                    .setPositiveButton("Delete") { _, _ -> NoteStore.delete(this, n.id); refresh() }
                    .setNegativeButton("Cancel", null).show()
                true
            }
            list.addView(row)
        }
    }

    private fun open(id: Long) =
        startActivity(Intent(this, EditNoteActivity::class.java).putExtra(NoteWidget.EXTRA_NOTE_ID, id))
}
