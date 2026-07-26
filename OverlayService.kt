package com.example.dynamicisland

import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * Diğer uygulamaların/oyunların ÜZERİNDE gösterilen, iPhone Dynamic Island'a
 * benzeyen küçük bir "hap" (pill) overlay penceresi oluşturur.
 *
 * - Ekranın üst ortasında sabit durur, sürüklenebilir.
 * - Dokununca genişleyip daha fazla bilgi (FPS, bildirim vb.) gösterir.
 * - updateContent() ile dışarıdan (örn. bir arka plan ölçümünden) içerik güncellenebilir.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var islandView: LinearLayout
    private lateinit var dotView: View
    private lateinit var labelText: TextView
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false

    private val collapsedWidthDp = 64
    private val collapsedHeightDp = 28
    private val expandedWidthDp = 170
    private val expandedHeightDp = 40

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createIslandView()
    }

    private fun startForegroundWithNotification() {
        val channelId = "dynamic_island_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Dynamic Island",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Dynamic Island aktif")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun createIslandView() {
        islandView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(Color.BLACK)
            }
        }

        // iPhone'daki mikrofon kullanım göstergesi gibi sade turuncu nokta
        dotView = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF9500"))
            }
        }
        val dotSize = dp(12)
        val dotParams = LinearLayout.LayoutParams(dotSize, dotSize)
        islandView.addView(dotView, dotParams)

        labelText = TextView(this).apply {
            text = "Mikrofon"
            setTextColor(Color.WHITE)
            textSize = 12f
            setPadding(dp(8), 0, 0, 0)
            visibility = View.GONE
        }
        islandView.addView(labelText)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            dp(collapsedWidthDp),
            dp(collapsedHeightDp),
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(8)
        }

        var isDragging = false
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        islandView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params!!.x
                    initialY = params!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX)
                    val dy = (event.rawY - initialTouchY)
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) {
                        isDragging = true
                    }
                    params!!.x = initialX + dx.toInt()
                    params!!.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(islandView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleExpand()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(islandView, params)
    }

    /** Dışarıdan (örn. mikrofon/kamera kullanımı başladığında) etiketi güncellemek için. */
    fun updateContent(label: String) {
        labelText.text = label
    }

    private fun toggleExpand() {
        val fromWidth = if (expanded) dp(expandedWidthDp) else dp(collapsedWidthDp)
        val toWidth = if (expanded) dp(collapsedWidthDp) else dp(expandedWidthDp)
        val fromHeight = if (expanded) dp(expandedHeightDp) else dp(collapsedHeightDp)
        val toHeight = if (expanded) dp(collapsedHeightDp) else dp(expandedHeightDp)

        labelText.visibility = if (expanded) View.GONE else View.VISIBLE

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 220
        animator.addUpdateListener { anim ->
            val fraction = anim.animatedFraction
            params!!.width = (fromWidth + (toWidth - fromWidth) * fraction).toInt()
            params!!.height = (fromHeight + (toHeight - fromHeight) * fraction).toInt()
            windowManager.updateViewLayout(islandView, params)
        }
        animator.start()

        expanded = !expanded
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::islandView.isInitialized) {
            windowManager.removeView(islandView)
        }
    }
}
