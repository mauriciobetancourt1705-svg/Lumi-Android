package com.lumi.android.voice

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

class LumiAgendaActionExecutor(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun prepare(request: LumiAgendaRequest): String = LumiAgendaEngine().describe(request)

    fun createReminder(request: LumiAgendaRequest): String {
        val intent = Intent(context, LumiReminderReceiver::class.java).apply {
            putExtra("title", request.title)
        }
        val id = request.triggerAtMillis.hashCode()
        val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, request.triggerAtMillis, pending)
        return "Listo. Te recordaré «${request.title}» cuando llegue el momento."
    }

    fun openCalendar(request: LumiAgendaRequest): String {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, request.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, request.triggerAtMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, request.triggerAtMillis + request.durationMinutes * 60_000L)
        }
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Abrí el calendario para que confirmes y guardes el evento «${request.title}»."
        } catch (_: RuntimeException) {
            "No pude abrir el calendario en este dispositivo."
        }
    }
}
