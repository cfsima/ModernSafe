/*
 * Copyright (C) 2011 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.cfsima.modernsafe.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.cfsima.modernsafe.LogOffActivity
import io.github.cfsima.modernsafe.R

class ServiceNotification(context: Context) {
    private val mNotifyManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
    private var wrapBuilder: NotificationCompat.Builder? = null

    init {
        createChannel(context)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notif_channel_title)
            val description = context.getString(R.string.notif_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.enableLights(false)
            mChannel.enableVibration(false)
            mChannel.description = description
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(mChannel)
        }
    }

    @SuppressLint("NewApi", "MissingPermission")
    fun setNotification(context: Context) {
        val intent = Intent(context, LogOffActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            flags
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setContentTitle(context.getString(R.string.app_name))
        builder.setContentText(
            context.getString(R.string.notification_msg)
        )
        builder.setSmallIcon(R.drawable.passicon)
        builder.setOngoing(true)
        builder.setContentIntent(pi)
        builder.setCategory(Notification.CATEGORY_SERVICE)
        builder.setProgress(100, 0, false)

        wrapBuilder = builder
        mNotifyManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun clearNotification() {
        mNotifyManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Update the existing notification progress bar. This should start with
     * progress == max and progress decreasing over time to depict time running
     * out.
     *
     * @param max
     * @param progress
     */
    @SuppressLint("MissingPermission")
    fun updateProgress(max: Int, progress: Int) {
        wrapBuilder?.let {
            it.setProgress(max, progress, false)
            mNotifyManager.notify(NOTIFICATION_ID, it.build())
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "safe"
    }
}
