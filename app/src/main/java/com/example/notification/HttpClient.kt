package com.example.notification

import android.content.Context
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object HttpClient {

    fun getAuthenticatedClient(context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Helps debugging by logging request and response bodies
        }

        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val username = sharedPreferences.getString("username", "")!!
                val password = sharedPreferences.getString("password", "")!!
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Authorization", Credentials.basic(username, password))
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }
}
