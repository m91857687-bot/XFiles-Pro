package com.xfiles.pro.domain

import com.xfiles.pro.domain.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `arabic and persian have RTL layout direction`() {
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertTrue(AppLanguage.PERSIAN.isRtl)
        assertEquals("ar", AppLanguage.ARABIC.code)
        assertEquals("fa", AppLanguage.PERSIAN.code)
    }

    @Test
    fun `english and spanish have LTR layout direction`() {
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertFalse(AppLanguage.SPANISH.isRtl)
        assertEquals("en", AppLanguage.ENGLISH.code)
        assertEquals("es", AppLanguage.SPANISH.code)
    }

    @Test
    fun `fromCode returns exact language for valid codes`() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromCode("ar"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("es"))
        assertEquals(AppLanguage.PERSIAN, AppLanguage.fromCode("fa"))
    }

    @Test
    fun `fromCode returns fallback for unknown or null codes`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("fr"))
    }

    @Test
    fun `native names match exact specification`() {
        assertEquals("العربية", AppLanguage.ARABIC.nativeName)
        assertEquals("English", AppLanguage.ENGLISH.nativeName)
        assertEquals("Español", AppLanguage.SPANISH.nativeName)
        assertEquals("فارسی", AppLanguage.PERSIAN.nativeName)
    }
}
