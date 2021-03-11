package de.westnordost.streetcomplete.data.osmnotes

import android.util.Log
import de.westnordost.streetcomplete.data.NotesApi

import javax.inject.Inject

import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.notes.Note
import de.westnordost.streetcomplete.data.download.Downloader
import de.westnordost.streetcomplete.ktx.format
import java.lang.System.currentTimeMillis
import java.util.concurrent.atomic.AtomicBoolean

/** Takes care of downloading notes and referenced avatar pictures into persistent storage */
class NotesDownloader @Inject constructor(
    private val notesApi: NotesApi,
    private val noteController: NoteController
) : Downloader {

    override fun download(bbox: BoundingBox, cancelState: AtomicBoolean) {
        if (cancelState.get()) return

        val time = currentTimeMillis()

        val notes = notesApi
            .getAll(bbox, 10000, 0)
            // exclude invalid notes (#1338)
            .filter { it.comments.isNotEmpty() }

        val seconds = (currentTimeMillis() - time) / 1000.0
        Log.i(TAG, "Downloaded ${notes.size} notes in ${seconds.format(1)}s")

        noteController.putAllForBBox(bbox, notes)
    }

    companion object {
        private const val TAG = "NotesDownload"
    }
}

private fun NotesApi.getAll(bbox: BoundingBox, limit: Int, hideClosedNoteAfter: Int): List<Note> {
    val notes = ArrayList<Note>()
    getAll(bbox, notes::add, limit, hideClosedNoteAfter)
    return notes
}