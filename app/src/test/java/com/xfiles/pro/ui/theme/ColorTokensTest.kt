package com.xfiles.pro.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ColorTokensTest {

    @Test
    fun `ClassicTokens has pure white background and dark text`() {
        assertEquals(Color(0xFFFFFFFF), ClassicTokens.background)
        assertEquals(Color(0xFFFFFFFF), ClassicTokens.surface)
        assertEquals(Color(0xFF0F172A), ClassicTokens.primary)
        assertEquals(Color(0xFF0F172A), ClassicTokens.textPrimary)
        assertEquals(Color(0xFFFFFFFF), ClassicTokens.onPrimary)
    }

    @Test
    fun `MidnightTokens has deep dark background and light text for OLED`() {
        assertEquals(Color(0xFF090D16), MidnightTokens.background)
        assertEquals(Color(0xFF111827), MidnightTokens.surface)
        assertEquals(Color(0xFF60A5FA), MidnightTokens.primary)
        assertEquals(Color(0xFFF9FAFB), MidnightTokens.textPrimary)
        assertEquals(Color(0xFF090D16), MidnightTokens.onPrimary)
    }

    @Test
    fun `RoyalTokens has obsidian velvet background and imperial gold accents`() {
        assertEquals(Color(0xFF0C0B0E), RoyalTokens.background)
        assertEquals(Color(0xFF17161B), RoyalTokens.surface)
        assertEquals(Color(0xFFD4AF37), RoyalTokens.primary)
        assertEquals(Color(0xFFFFD700), RoyalTokens.accent)
        assertEquals(Color(0xFFFBF8F0), RoyalTokens.textPrimary)
        assertEquals(Color(0xFF121008), RoyalTokens.onPrimary)
    }

    @Test
    fun `all 13 required semantic tokens are populated and distinct from transparent`() {
        val allTokenSets = listOf(ClassicTokens, MidnightTokens, RoyalTokens)
        allTokenSets.forEach { tokens ->
            assertNotEquals(Color.Transparent, tokens.background)
            assertNotEquals(Color.Transparent, tokens.surface)
            assertNotEquals(Color.Transparent, tokens.surfaceVariant)
            assertNotEquals(Color.Transparent, tokens.primary)
            assertNotEquals(Color.Transparent, tokens.secondary)
            assertNotEquals(Color.Transparent, tokens.accent)
            assertNotEquals(Color.Transparent, tokens.textPrimary)
            assertNotEquals(Color.Transparent, tokens.textSecondary)
            assertNotEquals(Color.Transparent, tokens.border)
            assertNotEquals(Color.Transparent, tokens.divider)
            assertNotEquals(Color.Transparent, tokens.error)
            assertNotEquals(Color.Transparent, tokens.success)
            assertNotEquals(Color.Transparent, tokens.warning)
        }
    }

    @Test
    fun `createTokensFromColorScheme correctly derives tokens for dark and light modes`() {
        val lightTokens = createTokensFromColorScheme(ClassicColorScheme, isDark = false)
        assertEquals(ClassicColorScheme.background, lightTokens.background)
        assertEquals(ClassicColorScheme.surface, lightTokens.surface)
        assertEquals(ClassicColorScheme.primary, lightTokens.primary)
        assertEquals(ClassicColorScheme.onSurface, lightTokens.textPrimary)

        val darkTokens = createTokensFromColorScheme(MidnightColorScheme, isDark = true)
        assertEquals(MidnightColorScheme.background, darkTokens.background)
        assertEquals(MidnightColorScheme.surface, darkTokens.surface)
        assertEquals(MidnightColorScheme.primary, darkTokens.primary)
        assertEquals(MidnightColorScheme.onSurface, darkTokens.textPrimary)
    }
}
