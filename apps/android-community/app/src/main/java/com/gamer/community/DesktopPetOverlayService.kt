package com.gamer.community

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.gamer.community.generation.FantasyPetPreviewDownloader
import com.gamer.community.generation.PetPreviewDownloadResult
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class DesktopPetOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var lastPreviewUrl: String = ""

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESET_POSITION -> {
                resetOverlayPosition()
                if (overlayView == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }
                return START_STICKY
            }
        }
        startAsForegroundService()
        showOverlayIfAllowed()
        return START_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        setOverlayRunning(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showOverlayIfAllowed() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        (overlayView as? DesktopPetOverlayView)?.let { existingView ->
            loadOverlayPreviewIfAvailable(existingView)
            return
        }

        val density = resources.displayMetrics.density
        val sizePx = (132f * density).roundToInt()
        val defaultPosition = defaultOverlayPosition(sizePx)
        val savedPosition = savedOverlayPosition(defaultPosition, sizePx)
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedPosition.x
            y = savedPosition.y
        }
        val view = DesktopPetOverlayView(this).apply {
            contentDescription = "gamer-system-desktop-pet"
            setOnTouchListener(
                OverlayDragTouchListener(
                    windowManager = windowManager,
                    params = params,
                    boundsProvider = { overlayBounds(sizePx) },
                    onPositionSettled = { x, y -> persistOverlayPosition(x, y, sizePx) }
                )
            )
        }
        windowManager.addView(view, params)
        overlayView = view
        overlayParams = params
        setOverlayRunning(true)
        loadOverlayPreviewIfAvailable(view)
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            windowManager.removeView(view)
        }
        overlayView = null
        overlayParams = null
        setOverlayRunning(false)
    }

    private fun setOverlayRunning(running: Boolean) {
        getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(DESKTOP_PET_OVERLAY_RUNNING_KEY, running)
            .apply()
    }

    private fun savedOverlayPosition(
        defaultPosition: OverlayPosition,
        sizePx: Int
    ): OverlayPosition {
        val prefs = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE)
        val savedX = if (prefs.contains(DESKTOP_PET_OVERLAY_X_KEY)) {
            prefs.getInt(DESKTOP_PET_OVERLAY_X_KEY, defaultPosition.x)
        } else {
            defaultPosition.x
        }
        val savedY = if (prefs.contains(DESKTOP_PET_OVERLAY_Y_KEY)) {
            prefs.getInt(DESKTOP_PET_OVERLAY_Y_KEY, defaultPosition.y)
        } else {
            defaultPosition.y
        }
        return overlayBounds(sizePx).clamp(savedX, savedY)
    }

    private fun resetOverlayPosition() {
        clearSavedPosition(this)
        val view = overlayView ?: return
        val params = overlayParams ?: return
        val sizePx = params.width
        val position = defaultOverlayPosition(sizePx)
        params.x = position.x
        params.y = position.y
        windowManager.updateViewLayout(view, params)
    }

    private fun defaultOverlayPosition(sizePx: Int): OverlayPosition {
        val density = resources.displayMetrics.density
        val defaultX = (20f * density).roundToInt()
        val defaultY = (118f * density).roundToInt()
        return overlayBounds(sizePx).clamp(defaultX, defaultY)
    }

    private fun persistOverlayPosition(x: Int, y: Int, sizePx: Int) {
        val position = overlayBounds(sizePx).clamp(x, y)
        getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putInt(DESKTOP_PET_OVERLAY_X_KEY, position.x)
            .putInt(DESKTOP_PET_OVERLAY_Y_KEY, position.y)
            .apply()
    }

    private fun overlayBounds(sizePx: Int): OverlayBounds {
        val metrics = resources.displayMetrics
        val minX = (-sizePx * 0.25f).roundToInt()
        val maxX = (metrics.widthPixels - (sizePx * 0.75f)).roundToInt().coerceAtLeast(minX)
        val minY = 0
        val maxY = (metrics.heightPixels - sizePx).coerceAtLeast(minY)
        return OverlayBounds(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY
        )
    }

    private fun loadOverlayPreviewIfAvailable(view: DesktopPetOverlayView) {
        val previewUrl = getSharedPreferences(UI_PREFS_NAME, MODE_PRIVATE)
            .getString(DESKTOP_PET_OVERLAY_PREVIEW_URL_KEY, "")
            .orEmpty()
            .trim()

        if (previewUrl.isBlank()) {
            lastPreviewUrl = ""
            view.setPreviewBitmap(null)
            return
        }

        if (previewUrl == lastPreviewUrl) {
            return
        }

        lastPreviewUrl = previewUrl
        val requestedPreviewUrl = previewUrl
        Thread {
            val bitmap = when (val result = FantasyPetPreviewDownloader().downloadBlocking(requestedPreviewUrl)) {
                is PetPreviewDownloadResult.Success -> {
                    BitmapFactory.decodeByteArray(result.bytes, 0, result.bytes.size)
                        ?.firstSpritesheetFrame()
                }
                is PetPreviewDownloadResult.Failure -> null
            }
            mainHandler.post {
                if (overlayView === view && lastPreviewUrl == requestedPreviewUrl) {
                    view.setPreviewBitmap(bitmap)
                }
            }
        }.start()
    }

    private fun startAsForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Desktop pet",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            fullAppIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_desktop_pet_notification)
            .setContentTitle("Gamer desktop pet")
            .setContentText("Desktop pet is showing over your screen.")
            .setContentIntent(openAppIntent)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_desktop_pet_notification),
                    "Stop",
                    stopIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    private class OverlayDragTouchListener(
        private val windowManager: WindowManager,
        private val params: WindowManager.LayoutParams,
        private val boundsProvider: () -> OverlayBounds,
        private val onPositionSettled: (Int, Int) -> Unit
    ) : View.OnTouchListener {
        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var moved = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    moved = moved || abs(dx) > DRAG_SLOP || abs(dy) > DRAG_SLOP
                    val bounds = boundsProvider()
                    params.x = (initialX + dx.roundToInt()).coerceIn(bounds.minX, bounds.maxX)
                    params.y = (initialY + dy.roundToInt()).coerceIn(bounds.minY, bounds.maxY)
                    windowManager.updateViewLayout(view, params)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) {
                        onPositionSettled(params.x, params.y)
                    } else {
                        view.performClick()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (moved) {
                        onPositionSettled(params.x, params.y)
                    }
                    return true
                }
            }
            return false
        }
    }

    private data class OverlayBounds(
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int
    ) {
        fun clamp(x: Int, y: Int): OverlayPosition =
            OverlayPosition(
                x = x.coerceIn(minX, maxX),
                y = y.coerceIn(minY, maxY)
            )
    }

    private data class OverlayPosition(
        val x: Int,
        val y: Int
    )

    private class DesktopPetOverlayView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
        }
        private val previewBounds = RectF()
        private val path = Path()
        private val startTime = SystemClock.uptimeMillis()
        private var previewBitmap: Bitmap? = null

        fun setPreviewBitmap(bitmap: Bitmap?) {
            previewBitmap = bitmap
            invalidate()
        }

        override fun performClick(): Boolean {
            super.performClick()
            context.startActivity(DesktopPetOverlayService.fullAppIntent(context))
            return true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val width = width.toFloat()
            val height = height.toFloat()
            val unit = width.coerceAtMost(height)
            val elapsed = (SystemClock.uptimeMillis() - startTime) / 1000f
            val bob = sin(elapsed * 2.6f) * unit * 0.035f
            val centerX = width * 0.5f
            val centerY = height * 0.54f + bob

            paint.style = Paint.Style.FILL
            paint.color = Color.argb(46, 16, 24, 40)
            canvas.drawOval(
                centerX - unit * 0.34f,
                height * 0.78f,
                centerX + unit * 0.34f,
                height * 0.91f,
                paint
            )

            val bitmap = previewBitmap
            if (bitmap != null) {
                paint.alpha = 255
                previewBounds.set(
                    centerX - unit * 0.38f,
                    centerY - unit * 0.39f,
                    centerX + unit * 0.38f,
                    centerY + unit * 0.37f
                )
                canvas.drawBitmap(bitmap, null, previewBounds, paint)
                postInvalidateOnAnimation()
                return
            }

            drawEar(canvas, centerX - unit * 0.22f, centerY - unit * 0.25f, true, unit)
            drawEar(canvas, centerX + unit * 0.22f, centerY - unit * 0.25f, false, unit)

            paint.color = Color.rgb(15, 118, 110)
            canvas.drawOval(
                centerX - unit * 0.34f,
                centerY - unit * 0.29f,
                centerX + unit * 0.34f,
                centerY + unit * 0.33f,
                paint
            )
            paint.color = Color.rgb(172, 228, 217)
            canvas.drawOval(
                centerX - unit * 0.19f,
                centerY + unit * 0.02f,
                centerX + unit * 0.19f,
                centerY + unit * 0.29f,
                paint
            )
            paint.color = Color.WHITE
            canvas.drawCircle(centerX - unit * 0.13f, centerY - unit * 0.07f, unit * 0.045f, paint)
            canvas.drawCircle(centerX + unit * 0.13f, centerY - unit * 0.07f, unit * 0.045f, paint)
            paint.color = Color.rgb(16, 24, 40)
            canvas.drawCircle(centerX - unit * 0.13f, centerY - unit * 0.07f, unit * 0.022f, paint)
            canvas.drawCircle(centerX + unit * 0.13f, centerY - unit * 0.07f, unit * 0.022f, paint)

            strokePaint.color = Color.rgb(255, 184, 107)
            strokePaint.strokeWidth = unit * 0.026f
            path.reset()
            path.moveTo(centerX - unit * 0.08f, centerY + unit * 0.08f)
            path.quadTo(centerX, centerY + unit * 0.14f, centerX + unit * 0.08f, centerY + unit * 0.08f)
            canvas.drawPath(path, strokePaint)

            postInvalidateOnAnimation()
        }

        private fun drawEar(
            canvas: Canvas,
            x: Float,
            y: Float,
            left: Boolean,
            unit: Float
        ) {
            val direction = if (left) -1f else 1f
            path.reset()
            path.moveTo(x, y + unit * 0.12f)
            path.lineTo(x + direction * unit * 0.05f, y - unit * 0.19f)
            path.lineTo(x + direction * unit * 0.24f, y + unit * 0.08f)
            path.close()
            paint.color = Color.rgb(13, 52, 48)
            paint.style = Paint.Style.FILL
            canvas.drawPath(path, paint)
        }
    }

    companion object {
        private const val ACTION_STOP = "com.gamer.community.desktop_pet_overlay.STOP"
        private const val ACTION_RESET_POSITION =
            "com.gamer.community.desktop_pet_overlay.RESET_POSITION"
        private const val CHANNEL_ID = "desktop_pet_overlay"
        private const val NOTIFICATION_ID = 7001
        private const val DRAG_SLOP = 4f
        private const val UI_PREFS_NAME = "pet-shell-ui"
        private const val DESKTOP_PET_OVERLAY_RUNNING_KEY = "desktopPetOverlayRunning"
        private const val DESKTOP_PET_OVERLAY_PREVIEW_URL_KEY = "desktopPetOverlayPreviewUrl"
        private const val DESKTOP_PET_OVERLAY_X_KEY = "desktopPetOverlayX"
        private const val DESKTOP_PET_OVERLAY_Y_KEY = "desktopPetOverlayY"

        fun startIntent(context: Context): Intent =
            Intent(context, DesktopPetOverlayService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, DesktopPetOverlayService::class.java).setAction(ACTION_STOP)

        fun resetPositionIntent(context: Context): Intent =
            Intent(context, DesktopPetOverlayService::class.java).setAction(ACTION_RESET_POSITION)

        fun clearSavedPosition(context: Context) {
            context.getSharedPreferences(UI_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(DESKTOP_PET_OVERLAY_X_KEY)
                .remove(DESKTOP_PET_OVERLAY_Y_KEY)
                .apply()
        }

        fun fullAppIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_FULL_APP, true)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
    }
}
