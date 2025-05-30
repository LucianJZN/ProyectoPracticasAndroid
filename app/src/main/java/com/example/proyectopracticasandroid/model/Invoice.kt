package com.example.proyectopracticasandroid.model

import java.time.LocalDateTime

data class Invoice(
    val invoiceId: Long? = null,
    val date: String,
    val total: String,
    val paid: Boolean,
    val cif: String,
    val userId: Long? = null
)