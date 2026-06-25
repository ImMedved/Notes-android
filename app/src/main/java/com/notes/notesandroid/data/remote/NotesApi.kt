package com.notes.notesandroid.data.remote

import com.notes.notesandroid.data.model.AppPreferences
import com.notes.notesandroid.data.model.DeleteResponse
import com.notes.notesandroid.data.model.Note
import com.notes.notesandroid.data.model.ServerSnapshot
import com.notes.notesandroid.data.model.TimerEntry
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface NotesApi {
    @GET("api/v1/snapshot")
    suspend fun snapshot(): ServerSnapshot

    @PUT("api/v1/notes/{id}")
    suspend fun putNote(@Path("id") id: String, @Body note: Note): Note

    @DELETE("api/v1/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): DeleteResponse

    @PUT("api/v1/timers/{id}")
    suspend fun putTimer(@Path("id") id: String, @Body timer: TimerEntry): TimerEntry

    @DELETE("api/v1/timers/{id}")
    suspend fun deleteTimer(@Path("id") id: String): DeleteResponse
}

class NotesApiFactory {
    fun create(settings: AppPreferences): NotesApi? {
        val baseUrl = settings.baseUrl.trim().trimEnd('/').plus("/").toHttpUrlOrNull() ?: return null
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Client-Id", settings.clientId)
                    .header("X-Client-Platform", "android")
                    .header("X-Client-Version", "2.0")
                    .apply {
                        if (settings.apiKey.isNotBlank()) {
                            header("X-Notes-Api-Key", settings.apiKey.trim())
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NotesApi::class.java)
    }
}
