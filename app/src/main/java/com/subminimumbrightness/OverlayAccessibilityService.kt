package com.subminimumbrightness

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.preference.PreferenceManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.subminimumbrightness.SettingsActivity.Companion.ACTION_STOP_SERVICE


class OverlayAccessibilityService : AccessibilityService() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    private fun createBrightnessOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        overlayView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@OverlayAccessibilityService)
            val alpha = sharedPreferences.getFloat(OVERLAY_ALPHA, 0.5f)
            this.alpha = alpha
        }

        windowManager?.addView(overlayView, params)
    }

    private fun removeBrightnessOverlay() {
        overlayView?.let { windowManager?.removeView(it) }
        overlayView = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No implementation needed
    }

    override fun onInterrupt() {
        // No implementation needed
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        createBrightnessOverlay()
    }

    companion object {
        const val OVERLAY_ALPHA = "overlay_alpha"
        const val ACTION_UPDATE_ALPHA = "com.subminimumbrightness.action.UPDATE_ALPHA"
        const val EXTRA_ALPHA = "com.subminimumbrightness.extra.ALPHA"
    }
    private val updateAlphaReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_ALPHA) {
                val alpha = intent.getFloatExtra(EXTRA_ALPHA, 0.5f)
                overlayView?.alpha = alpha
            }
        }
    }
    private val stopServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_STOP_SERVICE) {
                removeBrightnessOverlay()
                stopSelf()
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_UPDATE_ALPHA)
        registerReceiver(updateAlphaReceiver, filter)
        val stopFilter = IntentFilter(ACTION_STOP_SERVICE)
        registerReceiver(stopServiceReceiver, stopFilter)
    }

  //  override fun onUnbind(intent: Intent?): Boolean {
    //    unregisterReceiver(updateAlphaReceiver)
      //  removeBrightnessOverlay()
        //return super.onUnbind(intent)
    //}
  override fun onUnbind(intent: Intent?): Boolean {
      removeBrightnessOverlay()
      unregisterReceiver(stopServiceReceiver)
      return super.onUnbind(intent)
  }


}
