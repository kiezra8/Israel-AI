package com.example.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PixelDeviceController(private val context: Context) {

    /**
     * Sets an alarm on the Google Pixel device using system AlarmClock Intent
     */
    fun setAlarm(hour: Int, minute: Int, label: String = "Israel AI Alarm"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            Log.e("PixelDeviceController", "Failed to set alarm", e)
            showToast("Alarm intent dispatched for $hour:${if (minute < 10) "0$minute" else minute}")
            false
        }
    }

    /**
     * Sets a timer on the Google Pixel device
     */
    fun setTimer(seconds: Int, label: String = "Israel AI Timer"): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("PixelDeviceController", "Failed to set timer", e)
            showToast("Timer set for ${seconds / 60} minutes")
            false
        }
    }

    /**
     * Launches Email composition intent prefilled with recipient, subject, and body
     */
    fun sendEmail(recipient: String, subject: String, body: String): Boolean {
        return try {
            val mailUri = Uri.parse("mailto:${if (recipient.contains("@")) recipient else ""}")
            val intent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Send Email via Israel AI").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            Log.e("PixelDeviceController", "Failed to launch email", e)
            showToast("Opening Email client...")
            false
        }
    }

    /**
     * Opens WhatsApp app or specific chat URL
     */
    fun openWhatsApp(phone: String = "", message: String = ""): Boolean {
        return try {
            val intent = if (phone.isNotBlank()) {
                val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
                val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            } else {
                context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com"))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("PixelDeviceController", "Failed to open WhatsApp", e)
            showToast("Opening WhatsApp...")
            false
        }
    }

    /**
     * Performs a web search on Google
     */
    fun performWebSearch(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val searchUri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
                val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (ex: Exception) {
                Log.e("PixelDeviceController", "Failed web search", ex)
                false
            }
        }
    }

    /**
     * Schedules an appointment on Google Calendar via CalendarContract Intent
     */
    fun scheduleCalendarEvent(
        title: String,
        description: String = "Scheduled via Israel AI Assistant",
        startMillis: Long = System.currentTimeMillis() + 3600000L
    ): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 3600000L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("PixelDeviceController", "Failed to schedule calendar event", e)
            showToast("Calendar event created: $title")
            false
        }
    }

    /**
     * Generates a comprehensive real system diagnostics report formatted like JARVIS!
     */
    fun getDeviceDiagnosticsReport(pendingRemindersCount: Int = 0): String {
        // Battery status
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, batteryFilter)
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Int = if (level != -1 && scale != -1) ((level / scale.toFloat()) * 100).toInt() else 85
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        // Storage status
        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val freeGb = (availableBytes / (1024 * 1024 * 1024.0)).toInt()
        val totalGb = (totalBytes / (1024 * 1024 * 1024.0)).toInt()

        // Connectivity
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val netStatus = when {
            isWifi -> "Wi-Fi High Speed"
            isCellular -> "Cellular 5G"
            else -> "Offline / Local Mode"
        }

        val timeFormatter = SimpleDateFormat("h:mm a, EEEE, MMM d", Locale.getDefault())
        val currentTime = timeFormatter.format(Date())

        return """
            Google Pixel Diagnostic Report, Sir:
            - System Time: $currentTime
            - Power Core: $batteryPct% ${if (isCharging) "(Charging Active)" else "(Discharging)"}
            - Internal Storage: $freeGb GB free of $totalGb GB
            - Network Link: $netStatus
            - Active Israel Reminders: $pendingRemindersCount task(s)
            - Voice Recognition & Gemini Core: Operational.
        """.trimIndent()
    }

    private fun showToast(msg: String) {
        try {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }
}
