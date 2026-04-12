package com.teamnak.quizhelper

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView

object OverlayManager {

    private var windowManager: WindowManager? = null
    private var overlayView: android.view.View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var hideRunnable: Runnable? = null

    fun show(context: Context, answer: String) {
        handler.post {
            dismiss()

            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.overlay_answer, null)

            view.findViewById<TextView>(R.id.tvAnswer).text = answer
            view.findViewById<TextView>(R.id.tvClose).setOnClickListener { dismiss() }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 16
                y = 120
            }

            // Make overlay draggable
            var initialX = 0; var initialY = 0
            var touchX = 0f; var touchY = 0f

            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        touchX = event.rawX; touchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (touchX - event.rawX).toInt()
                        params.y = initialY + (event.rawY - touchY).toInt()
                        windowManager?.updateViewLayout(view, params)
                        true
                    }
                    else -> false
                }
            }

            overlayView = view
            windowManager?.addView(view, params)

            // Auto-dismiss after 15 seconds
            hideRunnable = Runnable { dismiss() }
            handler.postDelayed(hideRunnable!!, 30000)
        }
    }

    fun showLoading(context: Context) {
        show(context, "Analyzing question...")
    }

    fun dismiss() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        overlayView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
    }
}
