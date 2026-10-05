package dev.nick.stepcounter.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service.NOTIFICATION_SERVICE
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.nick.stepcounter.MainActivity
import dev.nick.stepcounter.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "StepTrackerChannel"
        const val NOTIFICATION_ID = 1
        private const val GOAL_NOTIFICATION_ID = 100
    }

    private val iconSize = 144
    private val progressBitmap = createBitmap(iconSize, iconSize)
    private val progressCanvas = Canvas(progressBitmap)
    private val padding = 12f
    private val rect = RectF(padding, padding, iconSize - padding, iconSize - padding)

    private val trackPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.progress_track)
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }

    private val progressPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.purple_500)
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.progress_track)
        textSize = 64f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }


    data class NotificationData(
        val currentSteps: Int,
        val currentMinutes: Int,
        val currentKcal: Double,
        val currentKm: Double,
        val stepsGoal: Int
    )

    private val notificationManager by lazy {
        context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setSound(null, null)
            enableVibration(false)
        }

        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("ResourceType")
    fun getNotification(data: NotificationData): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val progress =
            (data.currentSteps.toFloat() / data.stepsGoal.coerceAtLeast(1)).coerceIn(0f, 1f)

        val largeIconBitmap = createProgressIcon(progress)

        val title = context.getString(R.string.notification_title, data.currentSteps)
        val content = context.getString(
            R.string.notification_body,
            data.currentKcal,
            data.currentMinutes,
            data.currentKm
        )


        val customViews =
            RemoteViews(context.packageName, R.layout.notification_step_tracker).apply {
                setTextViewText(R.id.notification_title, title)
                setTextViewText(R.id.notification_content, content)
                setImageViewBitmap(R.id.notification_progress_icon, largeIconBitmap)
            }

        return NotificationCompat.Builder(context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_walking)
            .setCustomContentView(customViews)
            .setCustomBigContentView(customViews)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    fun updateNotification(data: NotificationData) {
        notificationManager.notify(NOTIFICATION_ID, getNotification(data))
    }

    fun showOneOffNotification(stepsGoal: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_walking)
            .setContentTitle(context.getString(R.string.goal_reached_title))
            .setContentText(context.getString(R.string.goal_reached_body, stepsGoal))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(GOAL_NOTIFICATION_ID, notification)
    }

    private fun createProgressIcon(progress: Float): Bitmap {
        // Clean the previous frame
        progressCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        // Draw track
        progressCanvas.drawArc(rect, 0f, 360f, false, trackPaint)

        // Draw progress
        progressCanvas.drawArc(rect, -90f, 360f * progress, false, progressPaint)

        // Draw text vertically and horizontally centered
        val textY = (iconSize / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        progressCanvas.drawText("\uD83C\uDFC6", iconSize / 2f, textY, textPaint)

        return progressBitmap
    }
}
