package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.log(context, "AlarmReceiver tetiklendi.")
        val pendingResult = goAsync()
        val photoRepository = PhotoRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val photoUrls = photoRepository.checkPhotos { logMessage ->
                    Logger.log(context, "[PhotoRepository] $logMessage")
                }
                
                if (!photoUrls.isNullOrEmpty()) {
                    Logger.log(context, "${photoUrls.size} fotoğraf bulundu, bildirim gönderiliyor.")
                    sendNotification(context, photoUrls)
                } else {
                    Logger.log(context, "Fotoğraf bulunamadı veya kurulum eksik, bildirim gönderilmedi.")
                }
            } finally {
                Logger.log(context, "Alarm görevi tamamlandı, bir sonraki alarm kuruluyor.")
                MainActivity.setRecurringAlarm(context)
                pendingResult.finish()
            }
        }
    }

    private fun sendNotification(context: Context, photoUrls: List<String>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("photos_channel", "Photos", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open StoryActivity
        val storyIntent = Intent(context, StoryActivity::class.java).apply {
            putStringArrayListExtra("IMAGE_URLS", ArrayList(photoUrls))
        }

        val pendingIntent = PendingIntent.getActivity(context, 0, storyIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, "photos_channel")
            .setContentTitle("Geçmişten Bir Anı")
            .setContentText("Geçmiş yıllarda bugün çekilmiş ${photoUrls.size} fotoğrafınız var.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(true) // Will be cancelled when the user clicks it
            .build()

        notificationManager.notify(1, notification)
    }
}
