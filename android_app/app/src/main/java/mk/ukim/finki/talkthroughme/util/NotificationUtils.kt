package mk.ukim.finki.talkthroughme.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import mk.ukim.finki.talkthroughme.R

class NotificationUtils {

    companion object {

        private lateinit var notificationManager: NotificationManager
        private const val CHANNEL_ID = 1000
        private const val progressMax = 100

        fun showNotification(context: Context): NotificationCompat.Builder? {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager = getNotificationManager(context)

                val notification = getNotification(context)

                updateNotification(notification)
                return notification
            } else {
                Toast.makeText(
                    context,
                    context.resources.getText(R.string.notification_error_message).toString(),
                    Toast.LENGTH_LONG
                ).show()

                return null
            }
        }

        fun updateNotificationProgress(
            context: Context,
            notification: NotificationCompat.Builder,
            isFinished: Boolean
        ) {
            if (isFinished) {
                notification.setContentText(
                    context.resources.getText(R.string.notification_inference_finished).toString()
                )
                    .setProgress(0, 0, false)
            } else {
                notification.setContentText(
                    context.resources.getText(R.string.notification_content_text_progress)
                        .toString()
                )
                    .setProgress(progressMax, progressMax / 2, true)
            }

            updateNotification(notification)
        }

        fun cancelNotification() {
            notificationManager.cancel(CHANNEL_ID)
        }

        private fun updateNotification(
            notification: NotificationCompat.Builder
        ) {
            notificationManager.notify(CHANNEL_ID, notification.build())
        }

        private fun getNotification(
            context: Context
        ): NotificationCompat.Builder {

            return NotificationCompat.Builder(
                context,
                context.resources.getText(R.string.app_id).toString()
            )
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(context.resources.getText(R.string.app_name).toString())
                .setContentText(
                    context.resources.getText(R.string.notification_content_text).toString()
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .setProgress(0, 0, false)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun getNotificationManager(context: Context): NotificationManager {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel =
                NotificationChannel(
                    context.resources.getText(R.string.app_id).toString(),
                    context.resources.getText(R.string.notification_content_text).toString(),
                    NotificationManager.IMPORTANCE_HIGH
                )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.BLACK
            notificationChannel.enableVibration(true)

            notificationManager.createNotificationChannel(notificationChannel)
            return notificationManager
        }
    }
}