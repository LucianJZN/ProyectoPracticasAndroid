package com.example.proyectopracticasandroid.api

import android.content.Context
import android.content.SharedPreferences

class TokenStorage(context: Context) {
    private val sharedPreferences: SharedPreferences

    // Archivo del token
    private val TOKEN_FILE_NAME = "auth_prefs"
    // Clave para guardar el token
    private val AUTH_TOKEN_KEY = "auth_token"

    init {
        // MODE_PRIVATE solo esta app puede acceder a este archivo
        sharedPreferences = context.getSharedPreferences(TOKEN_FILE_NAME, Context.MODE_PRIVATE)
        println("TokenStorage inicializado correctamente.")
    }

    fun saveAuthToken(token: String) {
        sharedPreferences.edit()
            .putString(AUTH_TOKEN_KEY, token)
            .apply()
        println("Token guardado.")
    }

    // Función para obtener token
    fun getAuthToken(): String? {
        val token = sharedPreferences.getString(AUTH_TOKEN_KEY, null)
        println("Token: ${token}")
        return token
    }

    //Funcion para eliminar token
    fun deleteAuthToken() {
        sharedPreferences.edit()
            .remove(AUTH_TOKEN_KEY)
            .apply()
        println("Token eliminado.")
    }
}