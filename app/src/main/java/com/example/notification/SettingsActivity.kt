package com.example.notification

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val serverUrlEditText = findViewById<EditText>(R.id.serverUrlEditText)
        val photoPathEditText = findViewById<EditText>(R.id.photoPathEditText)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        
        serverUrlEditText.setText(sharedPreferences.getString("server_url", "https://cloud.tofa.uk"))
        photoPathEditText.setText(sharedPreferences.getString("photo_path", "Pictures/Camera Roll"))
        usernameEditText.setText(sharedPreferences.getString("username", ""))
        // Password is not re-displayed for security

        saveButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putString("server_url", serverUrlEditText.text.toString().trimEnd('/'))
            editor.putString("photo_path", photoPathEditText.text.toString().trim())
            editor.putString("username", usernameEditText.text.toString())
            editor.putString("password", passwordEditText.text.toString())
            editor.apply()

            Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
