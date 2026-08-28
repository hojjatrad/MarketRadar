package com.arena.marketradar

import com.arena.marketradar.domain.util.Version
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {

    @Test
    fun newer_isDetected() {
        assertTrue(Version.isNewer("1.4", "1.3"))
        assertTrue(Version.isNewer("2.0", "1.9"))
        assertTrue(Version.isNewer("v1.5", "1.4"))
    }

    @Test
    fun notNewer_orEqual() {
        assertFalse(Version.isNewer("1.3", "1.3"))
        assertFalse(Version.isNewer("1.2", "1.3"))
        assertFalse(Version.isNewer("0.9", "1.0"))
    }

    @Test
    fun parse_cleansPrefix() {
        assertEquals(listOf(1, 4), Version.parse("v1.4"))
        assertEquals(listOf(1, 3), Version.parse("1.3"))
    }
}
