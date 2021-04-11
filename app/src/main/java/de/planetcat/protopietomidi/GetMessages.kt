package de.planetcat.protopietomidi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GetMessages : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getStringExtra("messageId")
        val value = intent.getStringExtra("value")
        println("Message from ProtoPie. messageId=$messageId value=$value")
        Log.e("Message received", messageId.toString())
    }
}