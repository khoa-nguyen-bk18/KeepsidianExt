package com.devindie.keepsidianext

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.os.FileObserver
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

class ScreenshotAccessibilityService : AccessibilityService() {

    private var screenshotObserver: FileObserver? = null
    private val screenshotFolderPath = "/storage/emulated/0/Pictures/Screenshot"

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Start observing screenshots folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.e(
                "ScreenshotService",
                "Service connected watching for screenshots in: $screenshotFolderPath"
            )
            // Android 10+ uses different path structure (e.g., /storage/emulated/0)
            startObservingScreenshots(screenshotFolderPath)
        } else {
            // Legacy support
            val screenshotsPath = "/sdcard/Pictures/Screenshots"
            Log.e(
                "ScreenshotService",
                "Service connected watching for screenshots in: $screenshotsPath"
            )
            startObservingScreenshots(screenshotsPath)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Optional fallback: detect screenshot notifications
        Log.e(
            "ScreenshotService",
            "Accessibility event received: ${event?.eventType} - ${AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED} - ${AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED}"
        )
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val text = event.text.joinToString()
            Log.d("ScreenshotService", "AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED text: $text")
            takeScreenshotOnce()
        }
    }

    private var lastScreenshotTime = 0L

    private fun takeScreenshotOnce() {
        val now = System.currentTimeMillis()
        if (now - lastScreenshotTime < 3000) return // throttle
        lastScreenshotTime = now

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                ScreenshotCallback()
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    inner class ScreenshotCallback : TakeScreenshotCallback {
        override fun onSuccess(result: ScreenshotResult) {
            Log.e("onSuccess", "${Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val bitmap = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
                Log.d("ScreenshotService", "Captured screen bitmap: ${bitmap != null}")
                // Optionally show notification or save bitmap
            }
        }

        override fun onFailure(errorCode: Int) {
            Log.e("ScreenshotService", "Screenshot failed with code: $errorCode")
        }
    }


    override fun onInterrupt() {
        // No special handling
    }

    override fun onDestroy() {
        super.onDestroy()
//        stopObservingScreenshots()
    }

    private fun startObservingScreenshots(path: String) {
        screenshotObserver = object : FileObserver(path, CREATE) {
            override fun onEvent(event: Int, fileName: String?) {
                if (event == CREATE && fileName != null) {
                    Log.d("ScreenshotService", "Screenshot file created: $fileName")
                    // Optional: trigger action or show notification here
                }
            }
        }.apply { startWatching() }
    }

    private fun stopObservingScreenshots() {
        screenshotObserver?.stopWatching()
        screenshotObserver = null
    }
}
