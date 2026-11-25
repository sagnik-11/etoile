package com.etoile.app.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.etoile.app.MainActivity
import com.etoile.app.R

/** Showing, replacing and clearing Étoile notifications on the phone. */
object Notifications {
    const val CHANNEL_ID = "etoile"
    const val EXTRA_TYPE = "type"
    const val EXTRA_DATE_ID = "dateId"

    /** Same id for all; the tag ("love", "date-<id>") decides which notification gets replaced. */
    private const val ID = 0

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Étoile", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Dates, movies and love"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Shows (or replaces) the notification with this [tag].
     * [quiet] = update the text without sound or buzz (love sent again before it was seen).
     */
    fun show(context: Context, tag: String, title: String, body: String, data: Map<String, String>, quiet: Boolean) {
        if (!canPost(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data[EXTRA_TYPE]?.let { putExtra(EXTRA_TYPE, it) }
            data[EXTRA_DATE_ID]?.let { putExtra(EXTRA_DATE_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            context, tag.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_star)
            .setColor(ContextCompat.getColor(context, R.color.gold))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setSilent(quiet)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(tag, ID, notification)
        } catch (_: SecurityException) {
            // Permission was taken away in the meantime: nothing to show
        }
    }

    /** Removes the notification with this tag (when its screen is opened). */
    fun cancel(context: Context, tag: String) {
        NotificationManagerCompat.from(context).cancel(tag, ID)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
