package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.NetworkTelemetry
import com.example.model.SpeedUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class NetworkSpeedOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.example.netspeed.ACTION_START"
        const val ACTION_STOP = "com.example.netspeed.ACTION_STOP"
        const val ACTION_UPDATE_OVERLAY_VISIBILITY = "com.example.netspeed.ACTION_UPDATE_OVERLAY_VISIBILITY"
        const val ACTION_UPDATE_OVERLAY_POSITION = "com.example.netspeed.ACTION_UPDATE_OVERLAY_POSITION"

        private const val NOTIFICATION_CHANNEL_ID = "netspeed_status_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var updatesJob: Job? = null

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    private var tvDownload: TextView? = null
    private var tvUpload: TextView? = null
    private var tvSignal: TextView? = null
    private var ivSignalIcon: ImageView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                SpeedMonitorManager.setMonitoringActive(this, false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_OVERLAY_VISIBILITY -> {
                syncOverlayState()
                return START_STICKY
            }
            ACTION_UPDATE_OVERLAY_POSITION -> {
                updateOverlayPositionPreset()
                return START_STICKY
            }
            else -> {
                // ACTION_START or default
                startForegroundNotification()
                syncOverlayState()
                startObservingMetrics()
                return START_STICKY
            }
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification(
            SpeedMonitorManager.telemetry.value,
            SpeedMonitorManager.selectedUnit.value
        )
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startObservingMetrics() {
        updatesJob?.cancel()
        updatesJob = serviceScope.launch {
            combine(
                SpeedMonitorManager.telemetry,
                SpeedMonitorManager.selectedUnit
            ) { telemetry, unit ->
                Pair(telemetry, unit)
            }.collect { (telemetry, unit) ->
                updateOverlayContent(telemetry, unit)
                val notification = buildNotification(telemetry, unit)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Индикатор скорости интернета",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Показывает скорость сети и сигнал в статусной строке"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(telemetry: NetworkTelemetry, unit: SpeedUnit): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, NetworkSpeedOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (rxVal, rxUnit) = unit.formatSpeedParts(telemetry.rxBytesPerSec)
        val (txVal, txUnit) = unit.formatSpeedParts(telemetry.txBytesPerSec)

        val title = "Скорость: ↓ $rxVal $rxUnit  ↑ $txVal $txUnit"
        val signalStr = if (telemetry.signalDbm != null) {
            "${telemetry.signalBars}/4 (${telemetry.signalDbm} dBm)"
        } else {
            "${telemetry.signalBars}/4"
        }
        val content = "${telemetry.networkName} • Сигнал: $signalStr"

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText("NetSpeed")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Выкл.", pendingStop)
            .build()
    }

    private fun syncOverlayState() {
        val shouldShow = SpeedMonitorManager.isOverlayEnabled.value &&
                SpeedMonitorManager.hasOverlayPermission(this) &&
                SpeedMonitorManager.isMonitoringActive.value

        if (shouldShow) {
            if (overlayView == null) {
                createOverlayView()
            }
        } else {
            removeOverlayView()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return

        val context = this
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = calculateInitialX()
            y = dpToPx(8)
        }
        overlayParams = params

        // Create sleek modern Pill UI container
        val pillLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))

            // Rounded dark translucent background with neon cyan subtle border
            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.parseColor("#E60A1322")) // Translucent deep navy
                setStroke(dpToPx(1), Color.parseColor("#3338BDF8")) // Subtle cyan border
            }
            background = backgroundDrawable
            elevation = dpToPx(8).toFloat()
        }

        // Signal icon / bars view
        val signalLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, dpToPx(6), 0)
        }

        val signalIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.stat_sys_data_bluetooth)
            setColorFilter(Color.parseColor("#38BDF8")) // Cyan
            val size = dpToPx(14)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(3)
            }
        }
        ivSignalIcon = signalIcon

        val signalText = TextView(context).apply {
            text = "4G"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        tvSignal = signalText

        signalLayout.addView(signalIcon)
        signalLayout.addView(signalText)
        pillLayout.addView(signalLayout)

        // Separator dot
        val dotView = View(context).apply {
            val dotSize = dpToPx(3)
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginStart = dpToPx(2)
                marginEnd = dpToPx(6)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#64748B"))
            }
        }
        pillLayout.addView(dotView)

        // Download Speed View (↓ 0.0 Мбит/с)
        val downloadText = TextView(context).apply {
            text = "↓ 0.0 Мбит/с"
            setTextColor(Color.parseColor("#38BDF8")) // Electric Cyan
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setPadding(0, 0, dpToPx(6), 0)
        }
        tvDownload = downloadText
        pillLayout.addView(downloadText)

        // Upload Speed View (↑ 0.0 Мбит/с)
        val uploadText = TextView(context).apply {
            text = "↑ 0.0"
            setTextColor(Color.parseColor("#34D399")) // Emerald green
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        tvUpload = uploadText
        pillLayout.addView(uploadText)

        // Drag & Touch Handling
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isDragging = false

        pillLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - touchStartX).toInt()
                    val deltaY = (event.rawY - touchStartY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isDragging = true
                    }
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager?.updateViewLayout(pillLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Clicked -> Open NetSpeed App
                        val launchIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(launchIntent)
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = pillLayout
        try {
            windowManager?.addView(pillLayout, params)
            updateOverlayContent(
                SpeedMonitorManager.telemetry.value,
                SpeedMonitorManager.selectedUnit.value
            )
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
        }
    }

    private fun updateOverlayPositionPreset() {
        val params = overlayParams ?: return
        val view = overlayView ?: return
        params.x = calculateInitialX()
        params.y = dpToPx(8)
        try {
            windowManager?.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateInitialX(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        return when (SpeedMonitorManager.overlayPositionPreset.value) {
            OverlayPositionPreset.TOP_RIGHT -> screenWidth - dpToPx(210)
            OverlayPositionPreset.TOP_CENTER -> (screenWidth / 2) - dpToPx(90)
            OverlayPositionPreset.TOP_LEFT -> dpToPx(16)
        }
    }

    private fun updateOverlayContent(telemetry: NetworkTelemetry, unit: SpeedUnit) {
        val (rxVal, rxUnit) = unit.formatSpeedParts(telemetry.rxBytesPerSec)
        val (txVal, _) = unit.formatSpeedParts(telemetry.txBytesPerSec)

        tvDownload?.text = "↓ $rxVal $rxUnit"
        tvUpload?.text = "↑ $txVal"

        val signalLabel = if (telemetry.signalDbm != null) {
            "${telemetry.signalDbm}dBm"
        } else {
            "${telemetry.signalBars}/4"
        }
        tvSignal?.text = signalLabel

        // Visual indicator of signal
        val signalColor = when {
            telemetry.signalBars >= 3 -> Color.parseColor("#34D399") // Green
            telemetry.signalBars == 2 -> Color.parseColor("#FBBF24") // Yellow/Amber
            telemetry.signalBars == 1 -> Color.parseColor("#F87171") // Red
            else -> Color.parseColor("#94A3B8") // Gray
        }
        ivSignalIcon?.setColorFilter(signalColor)
    }

    private fun removeOverlayView() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            overlayParams = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updatesJob?.cancel()
        serviceScope.cancel()
        removeOverlayView()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
