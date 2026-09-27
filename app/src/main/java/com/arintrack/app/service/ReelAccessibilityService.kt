package com.arintrack.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.arintrack.app.domain.repository.GoalRepository
import com.arintrack.app.domain.repository.ReelRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ReelAccessibilityService : AccessibilityService() {

    @Inject lateinit var reelRepository: ReelRepository
    @Inject lateinit var goalRepository: GoalRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var powerManager: PowerManager? = null
    private var lastRecordedReelTime: Long = 0L
    private var isVideoViewerActive: Boolean = false

    companion object {
        const val PKG_INSTAGRAM = "com.instagram.android"
        const val PKG_YOUTUBE = "com.google.android.youtube"
        private const val DEBOUNCE_MILLIS = 800L

        // Configurable Selectors for Instagram Reels
        val INSTAGRAM_REELS_CONTAINER_IDS = listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/reel_viewer_view_pager",
            "com.instagram.android:id/clips_video_container"
        )

        // Configurable Selectors for YouTube Shorts
        val YOUTUBE_SHORTS_CONTAINER_IDS = listOf(
            "com.google.android.youtube:id/reel_player_page_tree",
            "com.google.android.youtube:id/shorts_container",
            "com.google.android.youtube:id/reel_recycler"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            packageNames = arrayOf(PKG_INSTAGRAM, PKG_YOUTUBE)
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != PKG_INSTAGRAM && pkg != PKG_YOUTUBE) return
        if (powerManager?.isInteractive == false) return

        val root = rootInActiveWindow ?: return
        val currentlyInVideo = isViewingReelsOrShorts(pkg, root)

        // Dynamically toggle floating icon on top of video ONLY when viewer is open!
        if (currentlyInVideo && !isVideoViewerActive) {
            isVideoViewerActive = true
            showFloatingBadge()
        } else if (!currentlyInVideo && isVideoViewerActive) {
            isVideoViewerActive = false
            hideFloatingBadge()
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && isVideoViewerActive) {
            val now = System.currentTimeMillis()
            if (now - lastRecordedReelTime >= DEBOUNCE_MILLIS) {
                lastRecordedReelTime = now
                serviceScope.launch {
                    val count = reelRepository.recordReelEvent(pkg == PKG_YOUTUBE)
                    updateFloatingBadge(count)
                }
            }
        }
    }

    private fun isViewingReelsOrShorts(pkg: String, root: AccessibilityNodeInfo): Boolean {
        val targetIds = if (pkg == PKG_INSTAGRAM) INSTAGRAM_REELS_CONTAINER_IDS else YOUTUBE_SHORTS_CONTAINER_IDS
        for (id in targetIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) return true
        }
        return false
    }

    private fun showFloatingBadge() {
        val intent = Intent(this, FloatingReelBadgeService::class.java).apply {
            action = FloatingReelBadgeService.ACTION_SHOW
        }
        startService(intent)
    }

    private fun updateFloatingBadge(count: Int) {
        val intent = Intent(this, FloatingReelBadgeService::class.java).apply {
            action = FloatingReelBadgeService.ACTION_UPDATE
            putExtra(FloatingReelBadgeService.EXTRA_COUNT, count)
        }
        startService(intent)
    }

    private fun hideFloatingBadge() {
        val intent = Intent(this, FloatingReelBadgeService::class.java).apply {
            action = FloatingReelBadgeService.ACTION_HIDE
        }
        startService(intent)
    }

    override fun onInterrupt() {
        hideFloatingBadge()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingBadge()
        serviceScope.cancel()
    }
}