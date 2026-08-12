package com.example.device

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisNotificationListenerService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PixelDeviceController(private val context: Context) {

    private val TAG = "PixelDeviceController"

    // =========================================================================
    // TOOL 1: Open Installed App by Name
    // =========================================================================
    fun openAppByName(appName: String): String {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            var targetPackage: String? = null
            var targetLabel: String? = null

            val cleanQuery = appName.lowercase().trim()

            for (app in packages) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label == cleanQuery || label.contains(cleanQuery)) {
                    targetPackage = app.packageName
                    targetLabel = pm.getApplicationLabel(app).toString()
                    break
                }
            }

            if (targetPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    "Opening $targetLabel, Sir."
                } else {
                    "Found $targetLabel, but could not open launch screen."
                }
            } else {
                "Application '$appName' was not found on your Pixel device, Sir."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app: $appName", e)
            "Unable to launch application '$appName'."
        }
    }

    // =========================================================================
    // TOOL 2: Set Alarms & Reminders
    // =========================================================================
    fun setAlarmAndReminder(hour: Int, minute: Int, label: String = "Israel Alarm"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Alarm scheduled for ${formatTime(hour, minute)} with label '$label', Sir."
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alarm", e)
            "Failed to schedule alarm."
        }
    }

    // =========================================================================
    // TOOL 3: Make Phone Call to Named Contact or Number
    // =========================================================================
    fun makePhoneCall(contactNameOrNumber: String): String {
        return try {
            val number = resolveContactNumber(contactNameOrNumber) ?: contactNameOrNumber
            val cleanNumber = number.replace(Regex("[^0-9+]"), "")

            if (cleanNumber.isBlank()) {
                return "Could not find a valid phone number for '$contactNameOrNumber', Sir."
            }

            val hasCallPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val intentAction = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(intentAction, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            if (hasCallPermission) {
                "Placing call to $contactNameOrNumber ($cleanNumber), Sir."
            } else {
                "Opening dialer for $contactNameOrNumber ($cleanNumber), Sir."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error making call", e)
            "Failed to place call to $contactNameOrNumber."
        }
    }

    // =========================================================================
    // TOOL 4: Send SMS Message
    // =========================================================================
    fun sendSms(contactNameOrNumber: String, message: String): String {
        return try {
            val number = resolveContactNumber(contactNameOrNumber) ?: contactNameOrNumber
            val cleanNumber = number.replace(Regex("[^0-9+]"), "")

            if (cleanNumber.isBlank()) {
                return "Could not find contact number for '$contactNameOrNumber'."
            }

            val hasSmsPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

            if (hasSmsPermission) {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(cleanNumber, null, message, null, null)
                "SMS sent successfully to $contactNameOrNumber, Sir."
            } else {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening SMS app to dispatch message to $contactNameOrNumber, Sir."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
            "Failed to send SMS message."
        }
    }

    // =========================================================================
    // TOOL 5: Read Incoming Notifications Aloud
    // =========================================================================
    fun readNotificationsAloud(): String {
        return JarvisNotificationListenerService.getRecentNotificationsSummary()
    }

    // =========================================================================
    // TOOL 6: Toggle System Settings (Flashlight, Volume, WiFi, Bluetooth, Brightness)
    // =========================================================================
    fun toggleSystemSettings(setting: String, enable: Boolean? = true): String {
        val s = setting.lowercase().trim()
        return when {
            s.contains("flashlight") || s.contains("torch") -> {
                try {
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                        val chars = cameraManager.getCameraCharacteristics(id)
                        chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                    if (cameraId != null) {
                        val turnOn = enable ?: true
                        cameraManager.setTorchMode(cameraId, turnOn)
                        "Flashlight turned ${if (turnOn) "ON" else "OFF"}, Sir."
                    } else {
                        "Flashlight hardware not detected."
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error toggling flashlight", e)
                    "Unable to toggle flashlight."
                }
            }
            s.contains("volume") || s.contains("sound") -> {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val target = if (enable == true) max else max / 2
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
                    "Media volume set to ${if (enable == true) "100%" else "50%"}, Sir."
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting volume", e)
                    "Unable to adjust volume."
                }
            }
            s.contains("wifi") || s.contains("wi-fi") -> {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening Wi-Fi settings, Sir."
            }
            s.contains("bluetooth") -> {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening Bluetooth settings, Sir."
            }
            s.contains("brightness") -> {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening display & brightness settings, Sir."
            }
            else -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening system settings, Sir."
            }
        }
    }

    // =========================================================================
    // TOOL 7: Read & Respond to WhatsApp Messages via Accessibility Service
    // =========================================================================
    fun readAndRespondWhatsApp(messageToReply: String? = null): String {
        val service = JarvisAccessibilityService.instance
        if (service == null) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return "Israel Accessibility Service is not active. Please enable it in Settings, Sir."
        }

        return if (!messageToReply.isNullOrBlank()) {
            val success = service.sendWhatsAppMessage(messageToReply)
            if (success) {
                "WhatsApp reply sent: '$messageToReply', Sir."
            } else {
                "Typed reply into WhatsApp. Please confirm send, Sir."
            }
        } else {
            service.readScreenText()
        }
    }

    // =========================================================================
    // TOOL 8: Check Date/Time, Battery Level & Connectivity
    // =========================================================================
    fun checkDeviceStatus(): String {
        // Battery status
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, batteryFilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Int = if (level != -1 && scale != -1) ((level / scale.toFloat()) * 100).toInt() else 85
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Connectivity
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val netStatus = when {
            isWifi -> "Wi-Fi Connected"
            isCellular -> "Cellular Active (5G/LTE)"
            else -> "Offline Mode"
        }

        val timeFormatter = SimpleDateFormat("h:mm a, EEEE, MMM d, yyyy", Locale.getDefault())
        val currentTime = timeFormatter.format(Date())

        return """
            Google Pixel Status Report, Sir:
            • Date & Time: $currentTime
            • Power Level: $batteryPct% ${if (isCharging) "(Charging)" else "(Discharging)"}
            • Network Status: $netStatus
            • Assistant Engine: Active & Listening
        """.trimIndent()
    }

    // =========================================================================
    // TOOL 9: Web Search
    // =========================================================================
    fun searchWebAndSummarize(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching the web for '$query', Sir."
        } catch (e: Exception) {
            val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Searching Google for '$query', Sir."
        }
    }

    // Helper: Contact Lookup by Name
    private fun resolveContactNumber(nameOrNumber: String): String? {
        if (nameOrNumber.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }) {
            return nameOrNumber
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameOrNumber%")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numIndex != -1) {
                    return cursor.getString(numIndex)
                }
            }
        }
        return null
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val minStr = if (minute < 10) "0$minute" else "$minute"
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$displayHour:$minStr $amPm"
    }
}
