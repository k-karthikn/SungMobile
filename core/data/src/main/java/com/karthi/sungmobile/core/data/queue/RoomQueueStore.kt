package com.karthi.sungmobile.core.data.queue

import android.content.Context
import androidx.room.Room
import com.karthi.sungmobile.core.data.db.*
import com.karthi.sungmobile.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RoomQueueStore(private val database: SungDatabase) : QueueStore {
    constructor(context: Context) : this(DatabaseProvider.open(context))

    override suspend fun read(): QueueSnapshot = withContext(Dispatchers.IO) {
        val stored = database.queueDao().read()
        val header = stored.header ?: return@withContext QueueSnapshot()
        val entries = stored.entries.map { row ->
            QueueEntry(row.entryId, CatalogItem(row.trackId, CatalogKind.SONG, row.title,
                row.subtitle, row.canonicalUrl, row.artworkUrl, row.durationMs))
        }
        QueueSnapshot(QueueState(entries, header.currentId,
            RepeatMode.entries.find { it.name == header.repeatMode } ?: RepeatMode.OFF,
            header.shuffled, stored.entries.sortedBy { it.originalIndex }.map { it.entryId }),
            header.positionMs.coerceAtLeast(0), header.playbackPassId)
    }

    override suspend fun write(snapshot: QueueSnapshot) = withContext(Dispatchers.IO) {
        val queue = snapshot.queue
        val header = QueueHeaderEntity().apply {
            currentId = queue.currentId
            playbackPassId = snapshot.passId
            positionMs = snapshot.positionMs.coerceAtLeast(0)
            repeatMode = queue.repeat.name
            shuffled = queue.shuffled
        }
        val originalIndices = queue.originalOrder.withIndex().associate { it.value to it.index }
        val rows = queue.entries.mapIndexed { index, entry ->
            QueueEntryEntity().apply {
                entryId = entry.id
                playIndex = index
                originalIndex = originalIndices.getValue(entry.id)
                trackId = entry.track.id
                title = entry.track.title
                subtitle = entry.track.subtitle
                canonicalUrl = entry.track.url
                artworkUrl = entry.track.artworkUrl
                durationMs = entry.track.durationMs
            }
        }
        database.queueDao().replace(header, rows)
    }
}
