package com.karthi.sungmobile.core.model

import java.util.UUID
import kotlin.random.Random

/** Identity belongs to an occurrence, not the song: duplicate songs are valid. */
data class QueueEntry(val id: String = UUID.randomUUID().toString(), val track: CatalogItem)
enum class RepeatMode { OFF, ALL, ONE }

data class QueueState(
    val entries: List<QueueEntry> = emptyList(),
    val currentId: String? = null,
    val repeat: RepeatMode = RepeatMode.OFF,
    val shuffled: Boolean = false,
    val originalOrder: List<String> = entries.map { it.id },
) {
    val currentIndex: Int get() = entries.indexOfFirst { it.id == currentId }
    val current: QueueEntry? get() = entries.getOrNull(currentIndex)

    init {
        require(entries.map { it.id }.distinct().size == entries.size)
        require(entries.all { it.track.kind == CatalogKind.SONG })
        require(currentId == null && entries.isEmpty() || entries.any { it.id == currentId })
        require(originalOrder.size == entries.size && originalOrder.toSet() == entries.map { it.id }.toSet())
    }

    fun insert(index: Int, added: List<QueueEntry>): QueueState {
        if (added.isEmpty()) return this
        val at = index.coerceIn(0, entries.size)
        val updated = entries.toMutableList().apply { addAll(at, added) }
        val canonicalAt = entries.getOrNull(at)?.id?.let(originalOrder::indexOf) ?: originalOrder.size
        val canonical = originalOrder.toMutableList().apply { addAll(canonicalAt, added.map { it.id }) }
        return copy(entries = updated, currentId = currentId ?: added.first().id, originalOrder = canonical)
    }

    fun remove(ids: Set<String>): QueueState {
        val remaining = entries.filterNot { it.id in ids }
        val next = if (currentId !in ids) currentId else
            entries.drop(currentIndex + 1).firstOrNull { it.id !in ids }?.id ?: remaining.lastOrNull()?.id
        return copy(entries = remaining, currentId = next, originalOrder = originalOrder.filterNot { it in ids })
    }

    fun move(from: Int, until: Int, destination: Int): QueueState {
        if (from !in entries.indices || until !in (from + 1)..entries.size) return this
        val moved = entries.subList(from, until)
        val updated = entries.toMutableList().apply {
            subList(from, until).clear()
            addAll(destination.coerceIn(0, size), moved)
        }
        // A manual reorder defines a new baseline, including while shuffled.
        return copy(entries = updated, originalOrder = updated.map { it.id })
    }

    fun shuffle(enabled: Boolean, random: Random = Random.Default): QueueState {
        if (enabled == shuffled) return this
        val updated = if (enabled) entries.take(currentIndex + 1) + entries.drop(currentIndex + 1).shuffled(random)
        else originalOrder.map { id -> entries.first { it.id == id } }
        return copy(entries = updated, shuffled = enabled)
    }

    fun nextId(automatic: Boolean): String? = when {
        entries.isEmpty() -> null
        automatic && repeat == RepeatMode.ONE -> currentId
        currentIndex < entries.lastIndex -> entries[currentIndex + 1].id
        repeat == RepeatMode.ALL -> entries.first().id
        else -> null
    }
}

data class QueueSnapshot(val queue: QueueState = QueueState(), val positionMs: Long = 0)

interface QueueStore {
    suspend fun read(): QueueSnapshot
    suspend fun write(snapshot: QueueSnapshot)
}
