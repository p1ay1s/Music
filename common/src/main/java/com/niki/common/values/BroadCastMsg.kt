package com.niki.common.values

object BroadCastMsg {

    private const val BASE = "com.niki.music."

    /**
     * player
     */
    const val NEW_STATE = BASE + "NEW_STATUS"

    const val NEW_PROGRESS = BASE + "NEW_PROGRESS" // 进度条?

    const val NEXT = BASE + "NEXT_ONE"
    const val PREVIOUS = BASE + "PREVIOUS_ONE"

    const val PLAY = BASE + "PLAY"
    const val PAUSE = BASE + "PAUSE"

}