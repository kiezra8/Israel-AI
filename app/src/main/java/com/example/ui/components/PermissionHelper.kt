package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.JarvisAccessibilityService

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val actionIntent: Intent? = null
)

object PermissionHelper {

    fun getPermissionStatusList(context: Context): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = "RECORD_AUDIO",
                title = "Microphone (RECORD_AUDIO)",
                description = "Required for real-time speech recognition and voice commands.",
                isGranted = hasMic
            )
        )

        // 2. Notification Access
        val hasNotifAccess = isNotificationListenerGranted(context)
        list.add(
            PermissionItem(
                id = "NOTIFICATION_ACCESS",
                title = "Notification Access",
                description = "Allows Israel to read incoming notifications aloud on request.",
                isGranted = hasNotifAccess,
                actionIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        )

        // 3. Accessibility Service
        val hasAccessibility = JarvisAccessibilityService.isServiceRunning()
        list.add(
            PermissionItem(
                id = "ACCESSIBILITY_SERVICE",
                title = "Accessibility Service",
                description = "Allows reading screen content and performing WhatsApp voice reply automation.",
                isGranted = hasAccessibility,
                actionIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        )

        // 4. System Alert Window Overlay
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        list.add(
            PermissionItem(
                id = "SYSTEM_ALERT_WINDOW",
                title = "Overlay (System Alert Window)",
                description = "Allows displaying floating voice visualizers over other apps.",
                isGranted = hasOverlay,
                actionIntent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        )

        // 5. Phone / SMS / Contacts
        val hasPhone = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val hasSms = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val phoneSmsGranted = hasPhone && hasSms && hasContacts

        list.add(
            PermissionItem(
                id = "PHONE_SMS_CONTACTS",
                title = "Phone, SMS & Contacts",
                description = "Allows placing phone calls, sending SMS, and resolving contact names by voice.",
                isGranted = phoneSmsGranted
            )
        )

        // 6. Query All Packages
        list.add(
            PermissionItem(
                id = "QUERY_ALL_PACKAGES",
                title = "Query All Packages",
                description = "Allows listing and launching any installed app on your Pixel device by name.",
                isGranted = true // Declared in Manifest
            )
        )

        return list
    }

    private fun isNotificationListenerGranted(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(context.packageName)
    }
}
