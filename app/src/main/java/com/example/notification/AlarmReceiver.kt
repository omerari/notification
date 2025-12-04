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

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Logger.log(context, "AlarmReceiver tetiklendi.")
        val pendingResult = goAsync()
        val photoRepository = PhotoRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val photoCount = photoRepository.checkPhotos { logMessage ->
                    Logger.log(context, "[PhotoRepository] $logMessage")
                }
                
                if (photoCount > 0) {
                    Logger.log(context, "$photoCount fotoğraf bulundu, bildirim gönderiliyor.")
                    sendNotification(context, photoCount)
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

    private fun sendNotification(context: Context, photoCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("photos_channel", "Photos", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, "photos_channel")
            .setContentTitle("Geçmişten Bir Anı")
            .setContentText("Geçmiş yıllarda bugün çekilmiş $photoCount fotoğrafınız var.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
