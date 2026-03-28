package com.example.rpgtranslator.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.rpgtranslator.capture.ScreenCaptureManager
import com.example.rpgtranslator.ocr.OcrProcessor
import com.example.rpgtranslator.translation.TranslationClient
import com.example.rpgtranslator.ui.OverlayViewManager
import kotlinx.coroutines.*

class OverlayService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "OverlayServiceChannel"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private lateinit var overlayViewManager: OverlayViewManager
    private lateinit var screenCaptureManager: ScreenCaptureManager
    private val ocrProcessor = OcrProcessor()
    private val translationClient = TranslationClient()

    private var autoTranslateJob: Job? = null
    private var isAutoTranslateEnabled = false
    private var lastTranslatedText = ""

    override fun onCreate() {
        super.onCreate()
        overlayViewManager = OverlayViewManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        overlayViewManager.onManualTranslateClicked = {
            performManualTranslation()
        }
        
        overlayViewManager.onToggleAutoMode = { enabled ->
            isAutoTranslateEnabled = enabled
            if (enabled) {
                startAutoTranslation()
            } else {
                stopAutoTranslation()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)
            
            if (resultData != null) {
                screenCaptureManager = ScreenCaptureManager(this, resultCode, resultData)
                screenCaptureManager.start()
                overlayViewManager.showOverlays()
            }
        }
        return START_NOT_STICKY
    }

    private fun performManualTranslation() {
        scope.launch {
            overlayViewManager.updateSubtitle("Translating...")
            val bitmap = screenCaptureManager.captureBitmap()
            if (bitmap != null) {
                val ocrResult = ocrProcessor.processBitmap(bitmap)
                bitmap.recycle() // free memory
                
                if (ocrResult.text.isNotEmpty()) {
                    val translated = translationClient.translate(ocrResult.text)
                    overlayViewManager.updateSubtitle(translated)
                    
                    delay(4000)
                    overlayViewManager.updateSubtitle(null)
                } else {
                    overlayViewManager.updateSubtitle("No text detected")
                    delay(2000)
                    overlayViewManager.updateSubtitle(null)
                }
            } else {
                overlayViewManager.updateSubtitle("Capture failed")
            }
        }
    }

    private fun startAutoTranslation() {
        autoTranslateJob?.cancel()
        autoTranslateJob = scope.launch {
            while (isActive && isAutoTranslateEnabled) {
                val bitmap = screenCaptureManager.captureBitmap()
                if (bitmap != null) {
                    val ocrResult = ocrProcessor.processBitmap(bitmap)
                    bitmap.recycle()

                    if (ocrResult.text.isNotEmpty() && ocrResult.isStable) {
                        if (ocrResult.text != lastTranslatedText) {
                            lastTranslatedText = ocrResult.text
                            val translated = translationClient.translate(ocrResult.text)
                            overlayViewManager.updateSubtitle(translated)
                        }
                    } else if (ocrResult.text.isEmpty()) {
                        overlayViewManager.updateSubtitle(null)
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopAutoTranslation() {
        autoTranslateJob?.cancel()
        autoTranslateJob = null
        overlayViewManager.updateSubtitle(null)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setContentTitle("RPG Translator")
        .setContentText("Overlay service running...")
        .build()

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        screenCaptureManager.stop()
        overlayViewManager.removeOverlays()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
