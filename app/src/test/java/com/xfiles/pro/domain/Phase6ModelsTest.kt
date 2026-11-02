package com.xfiles.pro.domain

import com.xfiles.pro.domain.model.DateFilterRange
import com.xfiles.pro.domain.model.FileSortOption
import com.xfiles.pro.domain.model.FileTab
import com.xfiles.pro.domain.model.FileType
import com.xfiles.pro.domain.model.PaneId
import com.xfiles.pro.domain.model.SearchFilterCriteria
import com.xfiles.pro.domain.model.SearchTarget
import com.xfiles.pro.domain.model.SizeFilterRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class Phase6ModelsTest {

    @Test
    fun `file tab retains properties and generates unique ids`() {
        val tab1 = FileTab(title = "Downloads", path = "/storage/0/Download")
        val tab2 = FileTab(title = "Documents", path = "/storage/0/Documents")

        assertNotEquals(tab1.id, tab2.id)
        assertEquals("Downloads", tab1.title)
        assertEquals("/storage/0/Download", tab1.path)
        assertFalse(tab1.isGridView)
        assertEquals(FileSortOption.NAME_ASC, tab1.sortOption)
    }

    @Test
    fun `size filter range matches boundary sizes correctly`() {
        assertTrue(SizeFilterRange.ALL.matches(0L))
        assertTrue(SizeFilterRange.ALL.matches(10_000_000_000L))

        // Tiny < 1MB
        assertTrue(SizeFilterRange.TINY_UNDER_1MB.matches(500 * 1024L))
        assertFalse(SizeFilterRange.TINY_UNDER_1MB.matches(2 * 1024 * 1024L))

        // Medium 1-50MB
        assertTrue(SizeFilterRange.MEDIUM_1_50MB.matches(5 * 1024 * 1024L))
        assertFalse(SizeFilterRange.MEDIUM_1_50MB.matches(500 * 1024L))
        assertFalse(SizeFilterRange.MEDIUM_1_50MB.matches(60 * 1024 * 1024L))

        // Large 50-500MB
        assertTrue(SizeFilterRange.LARGE_50_500MB.matches(100 * 1024 * 1024L))
        assertFalse(SizeFilterRange.LARGE_50_500MB.matches(10 * 1024 * 1024L))

        // Huge > 500MB
        assertTrue(SizeFilterRange.HUGE_OVER_500MB.matches(1024 * 1024 * 1024L))
        assertFalse(SizeFilterRange.HUGE_OVER_500MB.matches(100 * 1024 * 1024L))
    }

    @Test
    fun `date filter range calculates cutoff timestamp correctly`() {
        val now = 1_700_000_000_000L

        val todayCutoff = DateFilterRange.TODAY.getCutoffTimestamp(now)
        assertEquals(now - TimeUnit.DAYS.toMillis(1), todayCutoff)

        val weekCutoff = DateFilterRange.THIS_WEEK.getCutoffTimestamp(now)
        assertEquals(now - TimeUnit.DAYS.toMillis(7), weekCutoff)

        val monthCutoff = DateFilterRange.THIS_MONTH.getCutoffTimestamp(now)
        assertEquals(now - TimeUnit.DAYS.toMillis(30), monthCutoff)

        val yearCutoff = DateFilterRange.THIS_YEAR.getCutoffTimestamp(now)
        assertEquals(now - TimeUnit.DAYS.toMillis(365), yearCutoff)

        // Matching
        assertTrue(DateFilterRange.TODAY.matches(now - 1000L, now))
        assertFalse(DateFilterRange.TODAY.matches(now - TimeUnit.DAYS.toMillis(2), now))
    }

    @Test
    fun `search target matches files and folders properly`() {
        assertTrue(SearchTarget.BOTH.matches(isDirectory = true))
        assertTrue(SearchTarget.BOTH.matches(isDirectory = false))

        assertTrue(SearchTarget.FILES_ONLY.matches(isDirectory = false))
        assertFalse(SearchTarget.FILES_ONLY.matches(isDirectory = true))

        assertTrue(SearchTarget.FOLDERS_ONLY.matches(isDirectory = true))
        assertFalse(SearchTarget.FOLDERS_ONLY.matches(isDirectory = false))
    }

    @Test
    fun `search filter criteria active indicator reflects filters`() {
        val defaultCriteria = SearchFilterCriteria()
        assertFalse(defaultCriteria.isFilteringActive)

        val withCategory = defaultCriteria.copy(fileType = FileType.DOCUMENT)
        assertTrue(withCategory.isFilteringActive)

        val withSize = defaultCriteria.copy(sizeRange = SizeFilterRange.LARGE_50_500MB)
        assertTrue(withSize.isFilteringActive)

        val withDate = defaultCriteria.copy(dateRange = DateFilterRange.THIS_WEEK)
        assertTrue(withDate.isFilteringActive)

        val withTarget = defaultCriteria.copy(searchTarget = SearchTarget.FILES_ONLY)
        assertTrue(withTarget.isFilteringActive)

        val withScope = defaultCriteria.copy(scopePath = "/storage/0/DCIM")
        assertTrue(withScope.isFilteringActive)
    }

    @Test
    fun `pane id enum covers primary and secondary panes`() {
        assertEquals(2, PaneId.values().size)
        assertEquals(PaneId.PRIMARY, PaneId.valueOf("PRIMARY"))
        assertEquals(PaneId.SECONDARY, PaneId.valueOf("SECONDARY"))
    }
}
