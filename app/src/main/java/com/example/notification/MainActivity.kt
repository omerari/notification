package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var manualCheckButton: Button
    private lateinit var viewLogButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private val photoRepository by lazy { PhotoRepository(this) }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
            showSnackbar("Bildirim izni verilmediği için anılarınız size ulaştırılamayacak.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        manualCheckButton = findViewById(R.id.manualCheckButton)
        viewLogButton = findViewById(R.id.viewLogButton)
        sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)

        manualCheckButton.setOnClickListener {
            statusTextView.text = "Kontrol başlatılıyor..."
            lifecycleScope.launch {
                val photoUrls = photoRepository.checkPhotos { message ->
                    runOnUiThread { statusTextView.text = message }
                }
                if (!photoUrls.isNullOrEmpty()) {
                    // Start StoryActivity directly instead of sending a notification
                    val storyIntent = Intent(this@MainActivity, StoryActivity::class.java).apply {
                        putStringArrayListExtra("IMAGE_URLS", ArrayList(photoUrls))
                    }
                    startActivity(storyIntent)
                }
            }
        }
        
        viewLogButton.setOnClickListener {
            showLogDialog()
        }

        requestNotificationPermission()
    }

    private fun showLogDialog() {
        val logContent = Logger.readLogs(this)
        AlertDialog.Builder(this)
            .setTitle("Hata Ayıklama Günlüğü")
            .setMessage(logContent)
            .setPositiveButton("Kapat", null)
            .setNeutralButton("Temizle") { _, _ -> 
                Logger.clearLogs(this)
                showLogDialog()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        checkSetupAndPermissions()
    }

    private fun checkSetupAndPermissions() {
        val isSetupComplete = !sharedPreferences.getString("photo_path", null).isNullOrBlank()
        manualCheckButton.isEnabled = isSetupComplete

        if (!isSetupComplete) {
            statusTextView.text = "Kurulum gerekli. Lütfen menüden Ayarlar\'a gidin."
            return
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            statusTextView.text = "Otomatik kontrol için İZİN GEREKLİ."
            showSnackbar("Otomatik kontrol için özel izin gerekiyor.", "Ayarlar") {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                    startActivity(it)
                }
            }
        } else {
            val selectedFolder = sharedPreferences.getString("photo_path", "")
            statusTextView.text = "Kontrol edilecek klasör: $selectedFolder\nHer şey yolunda!"
            setRecurringAlarm(this)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun showSnackbar(message: String, actionText: String? = null, action: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        snackbar.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun setRecurringAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                 if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                 } 
            } else {
                 alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }
    }
}