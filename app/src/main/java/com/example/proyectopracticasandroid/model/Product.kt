package com.example.proyectopracticasandroid.model

data class Product(
    var productId: Long,
    var image: String?,
    var name: String,
    var amount: Int,
    var minimumAmount: Int,
    var season: Boolean,
    var enabled: Boolean,
    var price: Double,
    var sellPrice: Double?
)