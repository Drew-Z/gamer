package com.gamer.community

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gamer.community.api.CommunityRepository
import com.gamer.community.api.HttpCommunityApiClient
import com.gamer.community.generation.FantasyPetGenerationService
import com.gamer.community.generation.HttpFantasyPetGenerationClient
import com.gamer.community.ui.PetShellApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyGamerSystemBars(window)
        val uiPrefs = getSharedPreferences("pet-shell-ui", MODE_PRIVATE)

        if (
            !intent.getBooleanExtra(EXTRA_OPEN_FULL_APP, false) &&
            uiPrefs.getBoolean("desktopPetOverlayAutoShowEnabled", false) &&
            canShowDesktopPetOverlay() &&
            canPostDesktopPetNotification()
        ) {
            startDesktopPetOverlay()
            moveTaskToBack(true)
            finish()
            return
        }

        val repository = CommunityRepository(
            client = HttpCommunityApiClient(BuildConfig.COMMUNITY_API_BASE_URL)
        )
        val generationService = FantasyPetGenerationService(
            client = HttpFantasyPetGenerationClient(BuildConfig.FANTASY_PET_API_BASE_URL),
            apiBaseUrl = BuildConfig.FANTASY_PET_API_BASE_URL
        )

        setContent {
            PetShellApp(
                repository = repository,
                generationService = generationService,
                canShowDesktopPetOverlay = ::canShowDesktopPetOverlay,
                canPostDesktopPetNotification = ::canPostDesktopPetNotification,
                onRequestDesktopPetOverlayPermission = ::requestDesktopPetOverlayPermission,
                onRequestDesktopPetNotificationPermission = ::requestDesktopPetNotificationPermission,
                onStartDesktopPetOverlay = ::startDesktopPetOverlay,
                onStopDesktopPetOverlay = ::stopDesktopPetOverlay
            )
        }

    }

    private fun canShowDesktopPetOverlay(): Boolean =
        Settings.canDrawOverlays(this)

    private fun canPostDesktopPetNotification(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestDesktopPetOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun requestDesktopPetNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !canPostDesktopPetNotification()
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS
            )
        }
    }

    private fun startDesktopPetOverlay() {
        if (!canShowDesktopPetOverlay()) {
            requestDesktopPetOverlayPermission()
            return
        }
        if (!canPostDesktopPetNotification()) {
            requestDesktopPetNotificationPermission()
            return
        }
        val intent = DesktopPetOverlayService.startIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopDesktopPetOverlay() {
        startService(DesktopPetOverlayService.stopIntent(this))
    }

    companion object {
        const val EXTRA_OPEN_FULL_APP = "com.gamer.community.extra.OPEN_FULL_APP"
        private const val REQUEST_POST_NOTIFICATIONS = 7301
    }
}
