package com.lumi.android.wellbeing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LumiCheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "com.lumi.android.CHECK_IN") return
        val open = Intent(context, com.lumi.android.MainActivity::class.java).apply {
            action = "com.lumi.android.CHECK_IN"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(open)
    }
}
