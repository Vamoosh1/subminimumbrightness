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
import android.util.Log
import androidx.core.graphics.ColorUtils
import com.subminimumbrightness.OverlayAccessibilityService.Companion.ACTION_STOP_SERVICE
import android.os.Handler
import android.os.Looper





class OverlayAccessibilityService : AccessibilityService() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private val updateAlphaReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_ALPHA) {
                val alpha = intent.getFloatExtra(EXTRA_ALPHA, 0.5f)
                overlayView?.alpha = alpha
            }
        }
    }
    private fun getColorWithAlphaAndTemperature(alpha: Float, colorTemperature: Float): Int {
        val temperatureColor = sliderToRgb((colorTemperature * 100).toInt())
        return ColorUtils.setAlphaComponent(temperatureColor, (alpha * 255).toInt())
    }

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
            val savedalpha = sharedPreferences.getFloat(OVERLAY_ALPHA, 0.5f)
            this.alpha = savedalpha
            val savedColorTemperature = sharedPreferences.getFloat(COLOR_TEMPERATURE, 0f)
            Log.d("OverlayAccessibilityService", "Retrieved color temperature: $savedColorTemperature")
            updateColorTemperature(savedColorTemperature)
        }

        windowManager?.addView(overlayView, params)
    }
    fun sliderToRgb(sliderValue: Int): Int {
        val maxValue = 100.0
        val ratio = sliderValue / maxValue
        val red: Int
        val green: Int
        val blue: Int

        red = (130 * ratio * 2).toInt().coerceIn(0, 130)
        green = (25 * ratio).toInt().coerceIn(0, 25)
        blue = 0

        return Color.rgb(red, green, blue)
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
    private fun updateColorTemperature(value: Float) {
        val colorTemperature = (value * 100).toInt()
        overlayView?.setBackgroundColor(sliderToRgb(colorTemperature))
    }


    companion object {
        const val OVERLAY_ALPHA = "overlay_alpha"
        const val ACTION_UPDATE_ALPHA = "com.subminimumbrightness.action.UPDATE_ALPHA"
        const val EXTRA_ALPHA = "com.subminimumbrightness.extra.ALPHA"
        const val ACTION_STOP_SERVICE = "com.subminimumbrightness.ACTION_STOP_SERVICE"
        const val COLOR_TEMPERATURE = "color_temperature"
        const val ACTION_UPDATE_COLOR_TEMPERATURE = "com.subminimumbrightness.action.UPDATE_COLOR_TEMPERATURE"
        const val EXTRA_COLOR_TEMPERATURE = "com.subminimumbrightness.extra.COLOR_TEMPERATURE"
        const val DEFAULT_COLOR_TEMPERATURE = 0f
    }

    private val stopServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_STOP_SERVICE) {
                removeBrightnessOverlay()
                stopSelf()
            }
        }
    }
    private val updateColorTemperatureReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_COLOR_TEMPERATURE) {
                val colorTemperature = intent.getFloatExtra(EXTRA_COLOR_TEMPERATURE, 0f)
                updateColorTemperature(colorTemperature)
            }
        }
    }
    private fun applySavedColorTemperature() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedColorTemperature = sharedPreferences.getFloat(COLOR_TEMPERATURE, 0f)
        updateColorTemperature(savedColorTemperature)
    }
    private fun broadcastUpdateColorTemperature() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedColorTemperature = sharedPreferences.getFloat(COLOR_TEMPERATURE, 0f)
        val intent = Intent(ACTION_UPDATE_COLOR_TEMPERATURE).apply {
            putExtra(EXTRA_COLOR_TEMPERATURE, savedColorTemperature)
        }
        sendBroadcast(intent)
        Log.d("OverlayAccessibilityService", "Emulated broadcast sent successfully.")
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_UPDATE_ALPHA)
        registerReceiver(updateAlphaReceiver, filter)
        val colorTemperatureFilter = IntentFilter(ACTION_UPDATE_COLOR_TEMPERATURE)
        registerReceiver(updateColorTemperatureReceiver, colorTemperatureFilter)
        val stopFilter = IntentFilter(ACTION_STOP_SERVICE)
        registerReceiver(stopServiceReceiver, stopFilter)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        Handler(Looper.getMainLooper()).postDelayed({
            broadcastUpdateColorTemperature()
        }, 50)
    }


  override fun onUnbind(intent: Intent?): Boolean {
      removeBrightnessOverlay()
      unregisterReceiver(stopServiceReceiver)
      return super.onUnbind(intent)
  }


}
