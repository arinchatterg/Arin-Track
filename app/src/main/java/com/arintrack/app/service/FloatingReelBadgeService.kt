package com.arintrack.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.arintrack.app.domain.repository.ReelRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FloatingReelBadgeService : Service() {

    @Inject lateinit var reelRepository: ReelRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var windowManager: WindowManager? = null
    private var badgeView: View? = null
    private var countTextView: TextView? = null

    companion object {
        const val ACTION_SHOW = "ACTION_SHOW"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val ACTION_HIDE = "ACTION_HIDE"
        const val EXTRA_COUNT = "EXTRA_COUNT"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                serviceScope.launch {
                    val count = reelRepository.getTodayReelCount()
                    createOrShowBadge(count)
                }
            }
            ACTION_UPDATE -> {
                val count = intent.getIntExtra(EXTRA_COUNT, -1)
                if (count != -1) {
                    countTextView?.text = "$count"
                }
            }
            ACTION_HIDE -> removeBadge()
        }
        return START_NOT_STICKY
    }

    private fun createOrShowBadge(initialCount: Int) {
        if (badgeView != null) {
            countTextView?.text = "$initialCount"
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = 0
                y = 48
            }

            // Minimalist center-top counter: bold number only, no writing
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                setPadding(28, 10, 28, 10)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#E60C0A09")) // 90% warm obsidian
                    cornerRadius = 999f
                    setStroke(2, Color.parseColor("#99FF6B00")) // classy orange outline
                }
            }

            countTextView = TextView(this).apply {
                text = "$initialCount"
                textSize = 15f
                setTextColor(Color.parseColor("#FF6B00"))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            container.addView(countTextView)

            windowManager?.addView(container, params)
            badgeView = container
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeBadge() {
        badgeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            badgeView = null
            countTextView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBadge()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}