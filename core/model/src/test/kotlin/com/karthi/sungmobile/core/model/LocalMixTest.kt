package com.karthi.sungmobile.core.model

import com.karthi.sungmobile.core.model.discovery.localMix
import org.junit.Assert.*
import org.junit.Test

class LocalMixTest {
    private val songs = (0..70).map { id -> CatalogItem(id.toString(), CatalogKind.SONG, "$id", "Artist", "https://youtu.be/${id.toString().padStart(11, '0')}") }
    @Test fun seedIsReproducibleAcrossInputOrderAndLimitsDistinctTracks() {
        val mix = localMix(songs + songs.first(), 42)
        assertEquals(mix, localMix(songs.reversed(), 42))
        assertEquals(50, mix.size); assertEquals(50, mix.map { it.id }.distinct().size)
        assertNotEquals(mix, localMix(songs, 43))
    }
    @Test fun emptyAndSmallLibrariesRemainHonest() {
        assertTrue(localMix(emptyList(), 0).isEmpty())
        assertEquals(listOf(songs.first()), localMix(listOf(songs.first(), songs.first()), 0))
    }
}
