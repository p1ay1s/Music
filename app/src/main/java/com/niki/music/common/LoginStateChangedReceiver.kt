package com.niki.music.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.niki.common.values.BroadCastMsg
import com.p1ay1s.dev.log.logE

class LoginStateChangedReceiver(private val block: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BroadCastMsg.LOGIN_STATE_CHANGED)
            block()
        logE("LoginStateChangedReceiver", "received")
    }
}