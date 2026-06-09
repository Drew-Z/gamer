package com.gamer.community

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivitySystemBarTest {
    @Test
    fun defaultSystemBarsUseDarkIconsOnLightShellBackgrounds() {
        val style = defaultGamerSystemBarStyle()

        assertEquals(0xFFF1F5F9.toInt(), style.statusBarColor)
        assertEquals(0xFFFFFFFF.toInt(), style.navigationBarColor)
        assertTrue(style.useDarkStatusBarIcons)
        assertTrue(style.useDarkNavigationBarIcons)
    }
}
