package com.teamnak.quizhelper

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class QuizAccessibilityService : AccessibilityService() {

    companion object {
        var instance: QuizAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        FloatingButtonManager.show(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Manual trigger only — no auto reading
    }

    fun readAndAnswer() {
        val rootNode = rootInActiveWindow ?: return
        val text = extractText(rootNode)
        rootNode.recycle()

        if (text.isBlank()) {
            OverlayManager.show(this, "Nothing to read on screen")
            return
        }

        val prefs = getSharedPreferences("quiz_helper", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""

        // API key is hardcoded as fallback, so always proceed

        OverlayManager.show(this, "Reading screen...")

        ClaudeApiHelper.askQuestion(
            apiKey = apiKey,
            questionText = text.take(1000),
            onResult = { answer -> OverlayManager.show(this, answer) },
            onError = { error -> OverlayManager.show(this, error) }
        )
    }

    private fun extractText(node: AccessibilityNodeInfo): String {
        val parts = mutableListOf<String>()
        fun traverse(n: AccessibilityNodeInfo?) {
            n ?: return
            val t = n.text?.toString()?.trim()
            if (!t.isNullOrEmpty()) parts.add(t)
            for (i in 0 until n.childCount) traverse(n.getChild(i))
        }
        traverse(node)
        return parts.joinToString("\n")
    }

    override fun onInterrupt() {
        OverlayManager.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        FloatingButtonManager.dismiss()
    }
}
