package com.gamer.community

import android.view.Window
import androidx.core.view.WindowCompat

internal data class GamerSystemBarStyle(
    val statusBarColor: Int,
    val navigationBarColor: Int,
    val useDarkStatusBarIcons: Boolean,
    val useDarkNavigationBarIcons: Boolean
)

internal fun defaultGamerSystemBarStyle(): GamerSystemBarStyle =
    GamerSystemBarStyle(
        statusBarColor = 0xFFF1F5F9.toInt(),
        navigationBarColor = 0xFFFFFFFF.toInt(),
        useDarkStatusBarIcons = true,
        useDarkNavigationBarIcons = true
    )

@Suppress("DEPRECATION")
internal fun applyGamerSystemBars(
    window: Window,
    style: GamerSystemBarStyle = defaultGamerSystemBarStyle()
) {
    window.statusBarColor = style.statusBarColor
    window.navigationBarColor = style.navigationBarColor

    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.isAppearanceLightStatusBars = style.useDarkStatusBarIcons
    controller.isAppearanceLightNavigationBars = style.useDarkNavigationBarIcons
}
