package com.lumi.android.voice

import android.content.Context
import android.provider.ContactsContract

data class LumiContact(val name: String, val phone: String)

class LumiContactResolver(private val context: Context) {
    fun findBestMatch(query: String): LumiContact? {
        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?"
            val args = arrayOf("%" + query + "%")
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                args,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) LumiContact(cursor.getString(0), cursor.getString(1)) else null
            }
        } catch (_: SecurityException) {
            null
        }
    }
}
