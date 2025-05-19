package com.example.proyectopracticasandroid.api

import com.example.proyectopracticasandroid.model.Product
import com.example.proyectopracticasandroid.model.User
import com.example.proyectopracticasandroid.model.UserLoginDTO
import com.example.proyectopracticasandroid.model.UserLoginResponseDTO
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    //USUARIOS
    @GET("users/getAll")
    //suspend fun getAllUsers(): List<User>
    suspend fun getAllUsers(): Response<List<User>>

    @POST("/auth/login")
    suspend fun login(@Body loginRequest: UserLoginDTO): Response<UserLoginResponseDTO>

    @POST("/users/new")
    suspend fun createUser(
        @Body user: User,
        @Header("Authorization") authToken: String
    ): Response<User>

    @PUT("/users/update/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: Long,
        @Body user: User,
        @Header("Authorization") authToken: String
    ): Response<User>

    @DELETE("/users/delete/{userId}")
    suspend fun deleteUser(
        @Path("userId") userId: Long,
        @Header("Authorization") authToken: String
    ): Response<Void>

    //PRODUCTOS
    @GET("products/getAll")
    //suspend fun getAllProducts(): List<Product>
    fun getAllProducts(): Call<List<Product>>

    @POST("/products/new")
    suspend fun createProduct(
        @Body products: Product,
        //@Header("Authorization") authToken: String    //Puede que sea necesario implementar el token tambien en productos
    ): Response<Product>

    @PUT("/products/update/{productId}")
    suspend fun updateProduct(
        @Path("productId") productId: Long,
        @Body product: Product,
        //@Header("Authorization") authToken: String
    ): Response<Product>

    @DELETE("/products/delete/{productId}")
    suspend fun deleteProduct(
        @Path("productId") productId: Long,
        //@Header("Authorization") authToken: String
    ): Response<Product>

}

/*
val token = RetrofitClient.tokenStorage.getAuthToken()
if (token != null) {
    val response = RetrofitClient.apiService.getAllUsers("Bearer $token")
    // ... manejar respuesta
} else {
    // Redirigir a login
}
 */
