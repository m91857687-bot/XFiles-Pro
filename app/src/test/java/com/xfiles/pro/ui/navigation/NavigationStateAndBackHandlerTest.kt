package com.xfiles.pro.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateAndBackHandlerTest {

    private class TestNavigationManager(
        initial: NavDestination = NavDestination.HOME
    ) {
        var currentDestination: NavDestination = initial
            private set

        var isGridView: Boolean = false

        fun navigateTo(destination: NavDestination) {
            currentDestination = destination
        }

        fun toggleViewMode() {
            isGridView = !isGridView
        }

        fun handleBackPress(): Boolean {
            return if (currentDestination != NavDestination.HOME) {
                currentDestination = NavDestination.HOME
                true // Back handled
            } else {
                false // At home, exit activity
            }
        }
    }

    @Test
    fun `initial destination is HOME`() {
        val manager = TestNavigationManager()
        assertEquals(NavDestination.HOME, manager.currentDestination)
    }

    @Test
    fun `navigation to all sub destinations works correctly`() {
        val manager = TestNavigationManager()

        manager.navigateTo(NavDestination.FILES)
        assertEquals(NavDestination.FILES, manager.currentDestination)

        manager.navigateTo(NavDestination.MEDIA)
        assertEquals(NavDestination.MEDIA, manager.currentDestination)

        manager.navigateTo(NavDestination.TOOLS)
        assertEquals(NavDestination.TOOLS, manager.currentDestination)

        manager.navigateTo(NavDestination.SETTINGS)
        assertEquals(NavDestination.SETTINGS, manager.currentDestination)
    }

    @Test
    fun `back press from sub screen returns to HOME`() {
        val manager = TestNavigationManager()

        manager.navigateTo(NavDestination.SETTINGS)
        assertEquals(NavDestination.SETTINGS, manager.currentDestination)

        val handled = manager.handleBackPress()
        assertTrue("Back press must be handled when on sub screen", handled)
        assertEquals(NavDestination.HOME, manager.currentDestination)
    }

    @Test
    fun `back press when already on HOME is not consumed`() {
        val manager = TestNavigationManager()
        assertEquals(NavDestination.HOME, manager.currentDestination)

        val handled = manager.handleBackPress()
        assertFalse("Back press on HOME should not be consumed internally", handled)
        assertEquals(NavDestination.HOME, manager.currentDestination)
    }

    @Test
    fun `view mode toggle correctly alternates between list and grid`() {
        val manager = TestNavigationManager()
        assertFalse(manager.isGridView) // Default list

        manager.toggleViewMode()
        assertTrue(manager.isGridView)

        manager.toggleViewMode()
        assertFalse(manager.isGridView)
    }
}
