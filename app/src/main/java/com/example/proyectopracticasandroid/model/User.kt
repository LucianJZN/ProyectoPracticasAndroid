package com.example.proyectopracticasandroid.model

import java.io.Serializable

data class User(
    val userId: Int,
    val name: String,
    val mail: String,
    val image: String?,
    val pass: String,
    val rol: String,
    val enabled: Boolean
): Serializable
