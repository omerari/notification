package com.example.notification

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PhotoRepository(private val context: Context) {

    suspend fun checkPhotos(statusCallback: (String) -> Unit): Int = withContext(Dispatchers.IO) {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val serverUrl = sharedPreferences.getString("server_url", null)
        val photoPath = sharedPreferences.getString("photo_path", null)
        val username = sharedPreferences.getString("username", null)
        val password = sharedPreferences.getString("password", null)

        if (serverUrl.isNullOrBlank() || photoPath.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            statusCallback("Kurulum eksik. Lütfen Ayarlar menüsünden sunucu ve klasör seçimi yapın.")
            return@withContext -1 // Indicate setup is needed
        }

        val davUrlString = "${serverUrl.trimEnd('/')}/remote.php/dav/files/$username/${photoPath.trim('/')}/"
        val davUrl = davUrlString.toHttpUrlOrNull()!!

        val baseUrl = davUrl.newBuilder().encodedPath("/").build().toString()
        val apiService = ApiClient.create(baseUrl)
        val credential = Credentials.basic(username, password)

        val requestBody = """<?xml version="1.0"?>
            <d:propfind xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns">
              <d:prop><d:getlastmodified /></d:prop>
            </d:propfind>""".toRequestBody()

        try {
            val today = Calendar.getInstance()
            val todayStr = SimpleDateFormat("dd MMMM", Locale("tr")).format(today.time)
            statusCallback("Seçilen klasör kontrol ediliyor: $photoPath\nGeçmiş yıllardaki $todayStr tarihli resimler aranıyor...")

            val response = apiService.listFiles(davUrl.toString(), credential, "1", body = requestBody)

            if (response.isSuccessful) {
                val photoCount = parseAndCountPhotos(response.body()?.string() ?: "")
                val message = if (photoCount > 0) {
                    "Geçmiş yıllarda bugün ($todayStr) çekilmiş $photoCount adet fotoğraf bulundu!"
                } else {
                    "Geçmiş yıllarda bugüne ($todayStr) ait fotoğraf bulunamadı."
                }
                statusCallback(message)
                return@withContext photoCount
            } else {
                val error = "Sunucu hatası: ${response.code()} - Ayarları kontrol edin."
                statusCallback(error)
                return@withContext 0
            }
        } catch (e: Exception) {
            statusCallback("Ağ hatası: Sunucuya bağlanılamadı.")
            return@withContext 0
        }
    }

    private fun parseAndCountPhotos(xml: String): Int {
        if (xml.isBlank()) return 0

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var photoCount = 0
        val today = Calendar.getInstance()
        val todayMonthDay = SimpleDateFormat("MM-dd", Locale.getDefault()).format(today.time)

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "getlastmodified") {
                try {
                    val lastModified = parser.nextText()
                    val date = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).parse(lastModified)
                    if (date != null) {
                        val photoMonthDay = SimpleDateFormat("MM-dd", Locale.getDefault()).format(date)
                        if (photoMonthDay == todayMonthDay) {
                            photoCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PhotoRepository", "Tarih ayrıştırılamadı.")
                }
            }
            eventType = parser.next()
        }
        // The first result is always the folder itself, so subtract one if photos are found.
        return if (photoCount > 0) photoCount -1 else 0
    }
}
