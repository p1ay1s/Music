package com.niki.common.values

object BroadCastMsg {

    private const val BASE = "com.niki.music."

    /**
     * player
     */
    const val NEW_STATE = BASE + "NEW_STATUS"
    const val NEW_PROGRESS = BASE + "NEW_PROGRESS"
    const val NEXT_ONE = BASE + "NEXT_ONE"
    const val PREVIOUS_ONE = BASE + "PREVIOUS_ONE"

    /**
     * login
     */
    const val LOGIN_STATE_CHANGED = BASE + "LOGIN_STATE_CHANGED"
}