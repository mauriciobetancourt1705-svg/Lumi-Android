package com.lumi.android.voice

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class LumiNotificationListenerService : NotificationListenerService() {
    private lateinit var store: LumiNotificationStore

    override fun onCreate() {
        super.onCreate()
        store = LumiNotificationStore(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val appName = try {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        store.add(
            LumiNotification(
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                postedAt = sbn.postTime
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) = Unit
}
