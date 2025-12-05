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
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PhotoRepository(private val context: Context) {

    private data class DavResource(val href: String, val lastModified: String?, val isCollection: Boolean)

    suspend fun listFolders(serverUrl: String, username: String, password: String, onError: (String) -> Unit): List<String>? = withContext(Dispatchers.IO) {
        val davUrlString = "${serverUrl.trimEnd('/')}/remote.php/dav/files/$username/"
        val davUrl = try {
            davUrlString.toHttpUrlOrNull() ?: throw IllegalArgumentException("Geçersiz URL formatı")
        } catch (e: Exception) {
            onError("Geçersiz sunucu adresi: ${e.message}")
            return@withContext null
        }

        val baseUrl = davUrl.newBuilder().encodedPath("/").build().toString()
        val apiService = ApiClient.create(baseUrl)
        val credential = Credentials.basic(username, password)

        val requestBody = """<?xml version="1.0"?>
            <d:propfind xmlns:d="DAV:">
              <d:prop>
                <d:resourcetype/>
              </d:prop>
            </d:propfind>""".toRequestBody()

        try {
            val response = apiService.listFiles(davUrl.toString(), credential, body = requestBody)
            if (response.isSuccessful) {
                val responseBody = response.body()?.string() ?: ""
                return@withContext parseFolders(responseBody, username)
            } else {
                onError("Bağlantı hatası: ${response.code()}. Lütfen sunucu ve giriş bilgilerini kontrol edin.")
                return@withContext null
            }
        } catch (e: Exception) {
            onError("Ağ hatası: Sunucuya bağlanılamadı. İnternet bağlantınızı kontrol edin.")
            return@withContext null
        }
    }

    suspend fun checkPhotos(statusCallback: (String) -> Unit): List<String>? = withContext(Dispatchers.IO) {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val serverUrl = sharedPreferences.getString("server_url", null)
        val photoPath = sharedPreferences.getString("photo_path", null)
        val username = sharedPreferences.getString("username", null)
        val password = sharedPreferences.getString("password", null)

        if (serverUrl.isNullOrBlank() || photoPath.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            statusCallback("Kurulum eksik. Lütfen Ayarlar menüsünden sunucu ve klasör seçimi yapın.")
            return@withContext null
        }

        val davUrlString = "${serverUrl.trimEnd('/')}/remote.php/dav/files/$username/${photoPath.trim('/')}/"
        val davUrl = davUrlString.toHttpUrlOrNull()!!

        val baseUrl = davUrl.newBuilder().encodedPath("/").build().toString()
        val apiService = ApiClient.create(baseUrl)
        val credential = Credentials.basic(username, password)

        val requestBody = """<?xml version="1.0"?>
            <d:propfind xmlns:d="DAV:" xmlns:oc="http://owncloud.org/ns">
              <d:prop>
                    <d:getlastmodified />
                    <d:resourcetype />
              </d:prop>
            </d:propfind>""".toRequestBody()

        try {
            val today = Calendar.getInstance()
            val todayStr = SimpleDateFormat("dd MMMM", Locale("tr")).format(today.time)
            statusCallback("Seçilen klasör kontrol ediliyor: $photoPath\nGeçmiş yıllardaki $todayStr tarihli resimler aranıyor...")

            val response = apiService.listFiles(davUrl.toString(), credential, body = requestBody)

            if (response.isSuccessful) {
                val photoUrls = parsePhotoUrls(response.body()?.string() ?: "", serverUrl)
                val message = if (photoUrls.isNotEmpty()) {
                    "Geçmiş yıllarda bugün ($todayStr) çekilmiş ${photoUrls.size} adet fotoğraf bulundu!"
                } else {
                    "Geçmiş yıllarda bugüne ($todayStr) ait fotoğraf bulunamadı."
                }
                statusCallback(message)
                return@withContext photoUrls
            } else {
                val error = "Sunucu hatası: ${response.code()} - Ayarları kontrol edin."
                statusCallback(error)
                return@withContext null
            }
        } catch (e: Exception) {
            statusCallback("Ağ hatası: Sunucuya bağlanılamadı.")
            return@withContext null
        }
    }

    private fun parseFolders(xml: String, username: String): List<String> {
        val resources = parseDavResources(xml)
        return resources.filter { it.isCollection && it.href.removePrefix("/remote.php/dav/files/$username/").trimEnd('/').isNotBlank() }
            .map { it.href.removePrefix("/remote.php/dav/files/$username/").trimEnd('/') }
    }

    private fun parsePhotoUrls(xml: String, serverUrl: String): List<String> {
        val resources = parseDavResources(xml)
        val today = Calendar.getInstance()
        val todayMonthDay = SimpleDateFormat("MM-dd", Locale.getDefault()).format(today.time)

        return resources.filter { !it.isCollection && it.lastModified != null }
            .filter { resource ->
                try {
                    val date = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).parse(resource.lastModified!!)
                    date != null && SimpleDateFormat("MM-dd", Locale.getDefault()).format(date) == todayMonthDay
                } catch (e: Exception) {
                    Log.w("PhotoRepository", "Tarih ayrıştırılamadı: ${resource.lastModified}")
                    false
                }
            }
            .map { "${serverUrl.trimEnd('/')}${it.href}" }
    }

    private fun parseDavResources(xml: String): List<DavResource> {
        if (xml.isBlank()) return emptyList()

        val resources = mutableListOf<DavResource>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "response") {
                var href: String? = null
                var lastModified: String? = null
                var isCollection = false

                while (!(eventType == XmlPullParser.END_TAG && parser.name == "response")) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name) {
                            "href" -> href = URLDecoder.decode(parser.nextText(), "UTF-8")
                            "getlastmodified" -> lastModified = parser.nextText()
                            "collection" -> isCollection = true
                        }
                    }
                    eventType = parser.next()
                }
                href?.let {
                    // Exclude the parent directory itself from the list
                    if (!it.endsWith("/") || it.count { c -> c == '/' } > 5) { // Heuristic to filter out the parent folder itself
                         resources.add(DavResource(it, lastModified, isCollection))
                    }
                }
            }
            eventType = parser.next()
        }
        return resources
    }
}