package com.example.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    companion object {
        var instance: JarvisAccessibilityService? = null
            private set

        fun isServiceRunning(): Boolean = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("JarvisAccessibility", "Accessibility service connected successfully.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event listener for active window state changes if needed
    }

    override fun onInterrupt() {
        Log.w("JarvisAccessibility", "Accessibility service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Reads visible text content from current screen (e.g. WhatsApp conversation view)
     */
    fun readScreenText(): String {
        val rootNode = rootInActiveWindow ?: return "Could not read screen content. Please open WhatsApp."
        val textList = mutableListOf<String>()
        collectNodeText(rootNode, textList)
        return if (textList.isNotEmpty()) {
            "Screen content read successfully:\n" + textList.takeLast(10).joinToString("\n")
        } else {
            "No readable text found on screen."
        }
    }

    private fun collectNodeText(node: AccessibilityNodeInfo?, results: MutableList<String>) {
        node ?: return
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrBlank()) {
            results.add(text)
        }
        for (i in 0 until node.childCount) {
            collectNodeText(node.getChild(i), results)
        }
    }

    /**
     * Types message into WhatsApp message field and performs click on Send button
     */
    fun sendWhatsAppMessage(message: String): Boolean {
        val rootNode = rootInActiveWindow ?: run {
            Log.e("JarvisAccessibility", "Root in active window is null")
            return false
        }

        // Find WhatsApp message input edit text
        val inputNode = findInputNode(rootNode)
        if (inputNode != null) {
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
            }
            val textSet = inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d("JarvisAccessibility", "Set text into WhatsApp input: $textSet")

            // Small delay or direct click send
            val sendNode = findSendButton(rootNode)
            if (sendNode != null) {
                val clicked = sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("JarvisAccessibility", "Clicked Send button: $clicked")
                return clicked
            } else {
                Log.w("JarvisAccessibility", "Send button not found directly, text was typed.")
                return textSet
            }
        } else {
            Log.e("JarvisAccessibility", "Could not find WhatsApp text input field.")
            return false
        }
    }

    private fun findInputNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null
        if (node.className == "android.widget.EditText" || node.isEditable) {
            return node
        }
        for (i in 0 until node.childCount) {
            val res = findInputNode(node.getChild(i))
            if (res != null) return res
        }
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        if (desc.contains("send") || text.contains("send")) {
            if (node.isClickable) return node
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) return parent
                parent = parent.parent
            }
        }
        for (i in 0 until node.childCount) {
            val res = findSendButton(node.getChild(i))
            if (res != null) return res
        }
        return null
    }
}
