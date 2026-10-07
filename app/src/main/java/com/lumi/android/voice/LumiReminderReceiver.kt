package com.lumi.android.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lumi.android.MainActivity

class LumiReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Recordatorio de Lumi"
        val channelId = "lumi_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(channelId, "Recordatorios de Lumi", NotificationManager.IMPORTANCE_HIGH))
        val openActivity = Intent(context, MainActivity::class.java).apply {
            if (intent.getBooleanExtra("lumi_check_in", false)) action = "com.lumi.android.CHECK_IN"
        }
        val openIntent = PendingIntent.getActivity(context, 7001, openActivity, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Lumi")
            .setContentText(title)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}
