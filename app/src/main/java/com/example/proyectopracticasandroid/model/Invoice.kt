package com.example.proyectopracticasandroid.model

import java.time.LocalDateTime

data class Invoice(
    val invoiceId: Int,
    val date: LocalDateTime,
    val total: Double,
    val paid: Boolean,
    val cif: String
)