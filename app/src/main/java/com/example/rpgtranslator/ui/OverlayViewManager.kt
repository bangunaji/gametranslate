package com.example.rpgtranslator.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.rpgtranslator.R

class OverlayViewManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private val floatingView: View = LayoutInflater.from(context).inflate(R.layout.overlay_view, null)
    private val subtitleView: View = LayoutInflater.from(context).inflate(R.layout.overlay_subtitle, null)
    
    private val tvSubtitle: TextView = subtitleView.findViewById(R.id.tvSubtitle)
    private val floatingButton: TextView = floatingView.findViewById(R.id.floatingButton)

    var onManualTranslateClicked: (() -> Unit)? = null
    var onToggleAutoMode: ((Boolean) -> Unit)? = null

    private var isAutoMode = false
    private var lastClickTime: Long = 0

    init {
        setupFloatingButton()
    }

    private fun setupFloatingButton() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        floatingButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dX = event.rawX - initialTouchX
                    val dY = event.rawY - initialTouchY
                    if (Math.abs(dX) > 10 || Math.abs(dY) > 10) isMoved = true
                    
                    params.x = initialX + dX.toInt()
                    params.y = initialY + dY.toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        val clickTime = System.currentTimeMillis()
                        if (clickTime - lastClickTime < 300) {
                            isAutoMode = !isAutoMode
                            floatingButton.text = if (isAutoMode) "AUTO" else "TR"
                            onToggleAutoMode?.invoke(isAutoMode)
                        } else {
                            if (!isAutoMode) {
                                onManualTranslateClicked?.invoke()
                            }
                        }
                        lastClickTime = clickTime
                    }
                    true
                }
                else -> false
            }
        }
    }

    fun showOverlays() {
        val floatingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        val subtitleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        try {
            windowManager.addView(floatingView, floatingParams)
            windowManager.addView(subtitleView, subtitleParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSubtitle(text: String?) {
        if (text.isNullOrEmpty()) {
            tvSubtitle.visibility = View.GONE
        } else {
            tvSubtitle.text = text
            tvSubtitle.visibility = View.VISIBLE
            ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f).apply {
                duration = 300
                start()
            }
        }
    }

    fun removeOverlays() {
        try {
            windowManager.removeView(floatingView)
            windowManager.removeView(subtitleView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
