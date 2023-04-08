package com.subminimumbrightness

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import android.graphics.PixelFormat
import androidx.preference.SeekBarPreference
import com.subminimumbrightness.OverlayAccessibilityService
import android.content.ComponentName
import android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
import android.view.MenuItem

class SettingsActivity : AppCompatActivity() {

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayAccessibilityService::class.java)
        stopService(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val stopIntent = Intent(ACTION_STOP_SERVICE)
                sendBroadcast(stopIntent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun startOverlayService() {
        val intent = Intent(this, OverlayAccessibilityService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestAccessibilityPermission() {
        val serviceName = ComponentName(this, OverlayAccessibilityService::class.java).flattenToShortString()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ACTION_ACCESSIBILITY_SETTINGS, serviceName)
        }
        startActivityForResult(intent, REQUEST_ACCESSIBILITY_PERMISSION_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        requestAccessibilityPermission()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION_CODE) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, start the overlay service
                startOverlayService()
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied, show a message or prompt again
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val dimmerPreference = findPreference<SeekBarPreference>("dimmer_slider")
            dimmerPreference?.setOnPreferenceChangeListener { _, newValue ->
                val alpha = (newValue as Int) / 100f
                val intent = Intent(OverlayAccessibilityService.ACTION_UPDATE_ALPHA).apply {
                    putExtra(OverlayAccessibilityService.EXTRA_ALPHA, alpha)
                }
                activity?.sendBroadcast(intent)
                true
            }

        }
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION_CODE = 1001
        private const val REQUEST_ACCESSIBILITY_PERMISSION_CODE = 1002
        const val ACTION_STOP_SERVICE = "com.subminimumbrightness.ACTION_STOP_SERVICE"

    }
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
}
