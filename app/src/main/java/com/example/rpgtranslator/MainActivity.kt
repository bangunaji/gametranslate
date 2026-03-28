package com.example.rpgtranslator

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.rpgtranslator.service.OverlayService

class MainActivity : AppCompatActivity() {

    private val mediaProjectionManager by lazy {
        getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START
                putExtra(OverlayService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(OverlayService.EXTRA_RESULT_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "Overlay Service Started", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnRequestOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Overlay permission already granted", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startScreenCapturePermission()
        }
    }

    private fun startScreenCapturePermission() {
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}
