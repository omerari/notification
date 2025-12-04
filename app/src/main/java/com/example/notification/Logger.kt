package com.example.notification

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {

    private fun getLogFile(context: Context): File {
        val directory = context.getExternalFilesDir(null) // App-specific storage
        return File(directory, "debug_log.txt")
    }

    fun log(context: Context, message: String) {
        try {
            val logFile = getLogFile(context)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            logFile.appendText("$timestamp: $message\n")
        } catch (e: Exception) {
            // Logging should not crash the app
            e.printStackTrace()
        }
    }

    fun readLogs(context: Context): String {
        return try {
            val logFile = getLogFile(context)
            if (logFile.exists()) logFile.readText() else "Günlük dosyası henüz oluşturulmadı."
        } catch (e: Exception) {
            "Günlük okunurken hata oluştu: ${e.message}"
        }
    }
    
    fun clearLogs(context: Context) {
        try {
            val logFile = getLogFile(context)
            if (logFile.exists()) {
                logFile.writeText("")
                log(context, "Günlük temizlendi.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
