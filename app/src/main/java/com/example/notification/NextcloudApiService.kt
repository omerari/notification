package com.example.notification

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.HTTP
import retrofit2.http.Url

interface NextcloudApiService {
    @HTTP(method = "PROPFIND", hasBody = true)
    suspend fun listFiles(
        @Url url: String,
        @Header("Authorization") auth: String, // Add auth header back
        @Header("Depth") depth: String = "1",
        @Body body: RequestBody
    ): Response<ResponseBody>
}
