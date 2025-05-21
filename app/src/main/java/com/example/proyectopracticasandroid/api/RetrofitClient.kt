package com.example.proyectopracticasandroid.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/" //http://192.168.1.129:8080/

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    init {
        // Registramos un mensaje para saber si Retrofit se inicializó correctamente
        println("RetrofitClient ha sido inicializado correctamente con la URL base: $BASE_URL")
    }
}
