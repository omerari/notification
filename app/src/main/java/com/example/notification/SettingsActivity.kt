package com.example.notification

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val photoRepository by lazy { PhotoRepository(this) }
    private var selectedFolderPath: String? = null

    // View references
    private lateinit var loginSection: LinearLayout
    private lateinit var folderSection: LinearLayout
    private lateinit var serverUrlEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var folderListView: ListView
    private lateinit var selectedFolderTextView: TextView
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        bindViews()
        loadSettings()

        connectButton.setOnClickListener { handleConnect() }
        saveButton.setOnClickListener { handleSave() }
    }

    private fun bindViews() {
        loginSection = findViewById(R.id.loginSection)
        folderSection = findViewById(R.id.folderSection)
        serverUrlEditText = findViewById(R.id.serverUrlEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        connectButton = findViewById(R.id.connectButton)
        progressBar = findViewById(R.id.progressBar)
        folderListView = findViewById(R.id.folderListView)
        selectedFolderTextView = findViewById(R.id.selectedFolderTextView)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadSettings() {
        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        serverUrlEditText.setText(sharedPreferences.getString("server_url", "https://cloud.tofa.uk"))
        usernameEditText.setText(sharedPreferences.getString("username", ""))
        // Password is not re-displayed for security
        selectedFolderPath = sharedPreferences.getString("photo_path", null)
        if (selectedFolderPath != null) {
            selectedFolderTextView.text = "Mevcut Seçim: $selectedFolderPath"
        }
    }

    private fun handleConnect() {
        val serverUrl = serverUrlEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Lütfen tüm giriş alanlarını doldurun.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingState(true)

        lifecycleScope.launch {
            val folders = photoRepository.listFolders(serverUrl, username, password) { error ->
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, error, Toast.LENGTH_LONG).show()
                    setLoadingState(false)
                }
            }

            if (folders != null) {
                displayFolderList(folders)
            }
        }
    }

    private fun displayFolderList(folders: List<String>) {
        setLoadingState(false)
        loginSection.visibility = View.GONE
        folderSection.visibility = View.VISIBLE

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, folders)
        folderListView.adapter = adapter
        folderListView.setOnItemClickListener { _, _, position, _ ->
            selectedFolderPath = folders[position]
            selectedFolderTextView.text = "Seçilen: $selectedFolderPath"
        }
    }

    private fun handleSave() {
        if (selectedFolderPath == null) {
            Toast.makeText(this, "Lütfen bir klasör seçin.", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
        sharedPreferences.putString("server_url", serverUrlEditText.text.toString().trimEnd('/'))
        sharedPreferences.putString("username", usernameEditText.text.toString())
        sharedPreferences.putString("password", passwordEditText.text.toString()) // Storing password
        sharedPreferences.putString("photo_path", selectedFolderPath)
        sharedPreferences.apply()

        Toast.makeText(this, "Ayarlar kaydedildi!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun setLoadingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        connectButton.isEnabled = !isLoading
        loginSection.isEnabled = !isLoading
    }
}
