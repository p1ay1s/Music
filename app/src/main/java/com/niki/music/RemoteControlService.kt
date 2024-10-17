package com.niki.music


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.niki.common.repository.dataclasses.song.Song
import com.niki.common.values.BroadCastMsg


class RemoteControlService : Service() {
    private lateinit var remoteViews: RemoteViews
    private val binder = RemoteControlBinder()

    private var isPlaying = false

    companion object {
        const val CHANNEL_ID = "p1ay1s.music"
        const val NOTIFICATION_ID = 666

        const val ACTION_SWITCH = "switch"
        const val ACTION_NEXT = "next"
        const val ACTION_PREVIOUS = "previous"
    }

    override fun onCreate() {
        super.onCreate()

        remoteViews = RemoteViews(packageName, R.layout.remote_control)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        }
    }

    private fun createNotification(): Notification {
        val iSwitch = Intent(this, RemoteControlService::class.java)
        iSwitch.action = ACTION_SWITCH
        val iPrevious = Intent(this, RemoteControlService::class.java)
        iPrevious.action = ACTION_PREVIOUS
        val iNext = Intent(this, RemoteControlService::class.java)
        iNext.action = ACTION_NEXT

        val pSwitch =
            PendingIntent.getService(this, 0, iSwitch, PendingIntent.FLAG_MUTABLE)
        val pPrevious =
            PendingIntent.getService(this, 1, iPrevious, PendingIntent.FLAG_MUTABLE)
        val pNext =
            PendingIntent.getService(this, 2, iNext, PendingIntent.FLAG_MUTABLE)

        remoteViews.setOnClickPendingIntent(R.id.ivSwitch, pSwitch)
        remoteViews.setOnClickPendingIntent(R.id.ivPrevious, pPrevious)
        remoteViews.setOnClickPendingIntent(R.id.ivNext, pNext)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "music control channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(
            this,
            CHANNEL_ID
        ).setSmallIcon(R.drawable.ni)
            .setCustomContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        return notification
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        when (intent.action) {
            ACTION_SWITCH -> {
                if (isPlaying)
                    sendBroadcast(BroadCastMsg.PAUSE)
                else
                    sendBroadcast(BroadCastMsg.PLAY)
            }

            ACTION_PREVIOUS -> {
                sendBroadcast(BroadCastMsg.PREVIOUS)
            }

            ACTION_NEXT -> {
                sendBroadcast(BroadCastMsg.NEXT)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun setSong(song: Song, callback: () -> Unit) {
        val builder = StringBuilder()
        builder.apply {
            for (artist in song.ar) {
                if (artist.name.isNotBlank()) {
                    append(artist.name)
                } else {
                    continue
                }
                val index = song.ar.indexOf(artist)
                when (index) { // 效果: a, b, c & d
                    song.ar.size - 1 -> {} // the last
                    song.ar.size - 2 -> append(" & ")
                    else -> append(", ")
                }
            }
        }
        remoteViews.setTextViewText(R.id.tvSongName, song.name)
        remoteViews.setTextViewText(R.id.tvSinger, builder.toString())
        setCover(song.al.picUrl, callback)
    }

    private fun setCover(
        imageUrl: String,
        callback: () -> Unit
    ) {
        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .override(256)
            .transform(RoundedCorners(35))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    remoteViews.setImageViewBitmap(R.id.ivCover, resource)
                    callback()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    callback()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    callback()
                }
            })
    }

    private fun changeSong(song: Song) {
        setSong(song) {
            refreshRemoteView()
        }
    }

    private fun setPlayingStatus(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        if (isPlaying)
            remoteViews.setImageViewResource(R.id.ivSwitch, R.drawable.ic_pause)
        else
            remoteViews.setImageViewResource(R.id.ivSwitch, R.drawable.ic_play)
        refreshRemoteView()
    }

    private fun refreshRemoteView() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun sendBroadcast(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    inner class RemoteControlBinder : Binder() {
        fun changeSong(song: Song) = this@RemoteControlService.changeSong(song)
        fun setPlayingStatus(isPlaying: Boolean) =
            this@RemoteControlService.setPlayingStatus(isPlaying)
    }
}
