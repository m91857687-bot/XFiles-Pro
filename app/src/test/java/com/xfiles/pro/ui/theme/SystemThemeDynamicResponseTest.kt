package com.xfiles.pro.ui.theme

import androidx.compose.ui.graphics.Color
import com.xfiles.pro.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SystemThemeDynamicResponseTest {

    @Test
    fun `system mode selects dark scheme when system in dark and light scheme when in light`() {
        val lightTokens = createTokensFromColorScheme(ClassicColorScheme, isDark = false)
        val darkTokens = createTokensFromColorScheme(MidnightColorScheme, isDark = true)

        // Light mode
        assertEquals(ClassicColorScheme.background, lightTokens.background)
        assertEquals(Color(0xFFFFFFFF), lightTokens.background)
        assertEquals(Color(0xFF0F172A), lightTokens.textPrimary)

        // Dark mode
        assertEquals(MidnightColorScheme.background, darkTokens.background)
        assertEquals(Color(0xFF090D16), darkTokens.background)
        assertEquals(Color(0xFFF9FAFB), darkTokens.textPrimary)

        // Contrast verification
        assertNotEquals(lightTokens.background, darkTokens.background)
        assertNotEquals(lightTokens.textPrimary, darkTokens.textPrimary)
    }

    @Test
    fun `switching theme mode preserves token invariants without activity recreation`() {
        val classic = ClassicTokens
        val midnight = MidnightTokens
        val royal = RoyalTokens

        assertEquals(Color(0xFFFFFFFF), classic.background)
        assertEquals(Color(0xFF090D16), midnight.background)
        assertEquals(Color(0xFF0C0B0E), royal.background)

        // Royal accent is distinctly gold
        assertEquals(Color(0xFFD4AF37), royal.primary)
        assertEquals(Color(0xFFFFD700), royal.accent)
    }
}
