package com.example.trumpcards

import okhttp3.OkHttpClient

object ApiConfig {
    // Change this to point to your authentication service if separate
    const val BASE_URL = "http://192.168.1.99:5000"

    // Separate gameplay server URL (in case you run two services)
    const val GAME_URL = "http://192.168.1.99:5000"

    // Shared OkHttp client for making API calls
    val client: OkHttpClient = OkHttpClient()
}
