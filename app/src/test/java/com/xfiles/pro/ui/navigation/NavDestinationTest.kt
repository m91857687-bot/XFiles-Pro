package com.xfiles.pro.ui.navigation

import com.xfiles.pro.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavDestinationTest {

    @Test
    fun `fromRoute maps all standard routes correctly`() {
        assertEquals(NavDestination.HOME, NavDestination.fromRoute("home"))
        assertEquals(NavDestination.FILES, NavDestination.fromRoute("files"))
        assertEquals(NavDestination.MEDIA, NavDestination.fromRoute("media"))
        assertEquals(NavDestination.TOOLS, NavDestination.fromRoute("tools"))
        assertEquals(NavDestination.SETTINGS, NavDestination.fromRoute("settings"))
    }

    @Test
    fun `fromRoute is case insensitive`() {
        assertEquals(NavDestination.HOME, NavDestination.fromRoute("HOME"))
        assertEquals(NavDestination.FILES, NavDestination.fromRoute("Files"))
        assertEquals(NavDestination.MEDIA, NavDestination.fromRoute("MeDia"))
        assertEquals(NavDestination.TOOLS, NavDestination.fromRoute("TOOLS"))
        assertEquals(NavDestination.SETTINGS, NavDestination.fromRoute("SeTTiNgS"))
    }

    @Test
    fun `fromRoute falls back to HOME for null or unknown route`() {
        assertEquals(NavDestination.HOME, NavDestination.fromRoute(null))
        assertEquals(NavDestination.HOME, NavDestination.fromRoute(""))
        assertEquals(NavDestination.HOME, NavDestination.fromRoute("unknown_route_123"))
    }

    @Test
    fun `each destination has valid label resource and icons`() {
        NavDestination.entries.forEach { destination ->
            assertNotEquals(0, destination.labelRes)
            assert(destination.selectedIcon != destination.unselectedIcon)
        }
    }

    @Test
    fun `all 5 primary destinations have unique routes`() {
        val routes = NavDestination.entries.map { it.route }
        assertEquals(5, routes.distinct().size)
    }
}
