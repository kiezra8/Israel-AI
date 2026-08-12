package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationItem(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class JarvisNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
        val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

        fun clearNotifications() {
            _notifications.value = emptyList()
        }

        fun getRecentNotificationsSummary(): String {
            val list = _notifications.value
            if (list.isEmpty()) {
                return "You have no unread notifications right now, Sir."
            }
            val sb = StringBuilder("You have ${list.size} recent notification(s):\n")
            list.take(5).forEachIndexed { index, item ->
                sb.append("${index + 1}. From ${item.appName} (${item.title}): ${item.text}\n")
            }
            return sb.toString().trim()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        // Skip ongoing notifications or empty notifications
        if (!sbn.isClearable) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val packageName = sbn.packageName ?: ""

        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }

        val item = NotificationItem(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime
        )

        Log.d("JarvisNotifService", "Captured notification from $appName: $title - $text")

        val currentList = _notifications.value.toMutableList()
        currentList.add(0, item)
        // Keep max 20 recent
        if (currentList.size > 20) {
            currentList.removeAt(currentList.size - 1)
        }
        _notifications.value = currentList
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
