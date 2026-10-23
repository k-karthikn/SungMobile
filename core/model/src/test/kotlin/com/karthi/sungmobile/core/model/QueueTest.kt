package com.karthi.sungmobile.core.model

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class QueueTest {
    private fun entry(id: String, song: String = id) = QueueEntry(id, CatalogItem(song, CatalogKind.SONG, song, "Artist", "https://www.youtube.com/watch?v=$song"))
    private val entries = listOf(entry("a"), entry("b"), entry("c"), entry("d"))
    private val queue = QueueState(entries, "b")

    @Test fun duplicateSongsRemainIndependentOccurrences() {
        val updated = queue.insert(2, listOf(entry("duplicate", "b")))
        assertEquals(listOf("a", "b", "duplicate", "c", "d"), updated.entries.map { it.id })
        val removed = updated.remove(setOf("b"))
        assertEquals("duplicate", removed.currentId)
        assertEquals("b", removed.current?.track?.id)
    }
    @Test fun removeCurrentChoosesSuccessorAndRemoveUpcomingLeavesCurrentAlone() {
        assertEquals("c", queue.remove(setOf("b")).currentId)
        assertEquals("b", queue.remove(setOf("c", "d")).currentId)
        assertEquals("a", queue.remove(setOf("b", "c", "d")).currentId)
        assertNull(queue.remove(entries.map { it.id }.toSet()).currentId)
    }
    @Test fun movingCurrentKeepsItsIdentity() {
        val moved = queue.move(1, 2, 3)
        assertEquals("b", moved.currentId)
        assertEquals(3, moved.currentIndex)
        assertEquals(listOf("a", "c", "d", "b"), moved.originalOrder)
    }
    @Test fun shuffleKeepsCurrentAndHistoryThenRestoresOriginalOrder() {
        val shuffled = queue.shuffle(true, Random(1))
        assertEquals(entries.take(2), shuffled.entries.take(2))
        assertEquals(entries.toSet(), shuffled.entries.toSet())
        assertEquals(queue, shuffled.shuffle(false))
    }
    @Test fun smartShuffleSeparatesDuplicatesWithoutLosingOccurrences() {
        val candidates = listOf(entry("first", "song1"), entry("copy", "song1"),
            entry("other", "song2"), entry("third", "song3"))
        val source = QueueState(candidates, "first")
        val result = source.smartShuffle(Random(2))
        assertEquals("first", result.currentId)
        assertEquals(candidates.map { it.id }.toSet(), result.entries.map { it.id }.toSet())
        assertNotEquals("copy", result.entries[1].id)
        assertEquals(source, result.shuffle(false))
        assertEquals(result.entries, source.smartShuffle(Random(2)).entries)
    }
    @Test fun additionsAndRemovalsWhileShuffledRemainRestorable() {
        val edited = queue.shuffle(true, Random(3)).insert(2, listOf(entry("x"))).remove(setOf("d"))
        val restored = edited.shuffle(false)
        assertEquals(setOf("a", "b", "c", "x"), restored.entries.map { it.id }.toSet())
        assertEquals("b", restored.currentId)
        assertEquals(4, restored.originalOrder.size)
    }
    @Test fun repeatOneOnlyAffectsAutomaticCompletion() {
        val repeating = queue.copy(repeat = RepeatMode.ONE)
        assertEquals("b", repeating.nextId(true))
        assertEquals("c", repeating.nextId(false))
    }
    @Test fun endStopsWithoutRepeatAndWrapsWithRepeatAll() {
        val last = queue.copy(currentId = "d")
        assertNull(last.nextId(true))
        assertEquals("a", last.copy(repeat = RepeatMode.ALL).nextId(true))
    }
    @Test(expected = IllegalArgumentException::class) fun duplicateEntryIdsAreRejected() { QueueState(listOf(entries[0], entries[0]), "a") }
    @Test(expected = IllegalArgumentException::class) fun missingCurrentEntryIsRejected() { QueueState(entries, "missing") }
}
