package com.xfiles.pro.domain

import com.xfiles.pro.R
import com.xfiles.pro.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `default theme is CLASSIC`() {
        assertEquals(ThemeMode.CLASSIC, ThemeMode.DEFAULT)
    }

    @Test
    fun `fromId correctly resolves all theme ids`() {
        assertEquals(ThemeMode.CLASSIC, ThemeMode.fromId("classic"))
        assertEquals(ThemeMode.MIDNIGHT, ThemeMode.fromId("midnight"))
        assertEquals(ThemeMode.ROYAL, ThemeMode.fromId("royal"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromId("system"))
    }

    @Test
    fun `fromId is case insensitive`() {
        assertEquals(ThemeMode.CLASSIC, ThemeMode.fromId("CLASSIC"))
        assertEquals(ThemeMode.MIDNIGHT, ThemeMode.fromId("Midnight"))
        assertEquals(ThemeMode.ROYAL, ThemeMode.fromId("RoYaL"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromId("SYSTEM"))
    }

    @Test
    fun `fromId falls back to DEFAULT on null or unknown id`() {
        assertEquals(ThemeMode.CLASSIC, ThemeMode.fromId(null))
        assertEquals(ThemeMode.CLASSIC, ThemeMode.fromId(""))
        assertEquals(ThemeMode.CLASSIC, ThemeMode.fromId("neon_matrix"))
    }

    @Test
    fun `each theme has valid non-zero title and description string resources`() {
        ThemeMode.entries.forEach { mode ->
            assert(mode.titleRes != 0) { "Theme ${mode.id} has invalid titleRes" }
            assert(mode.descriptionRes != 0) { "Theme ${mode.id} has invalid descriptionRes" }
        }
    }
}
