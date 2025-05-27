package com.example.proyectopracticasandroid

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectopracticasandroid.adapter.UserAdapter
import com.example.proyectopracticasandroid.api.RetrofitClient
import com.example.proyectopracticasandroid.decoration.UserDecoration
import com.example.proyectopracticasandroid.model.User
import com.example.proyectopracticasandroid.model.UserLoginDTO
import com.example.proyectopracticasandroid.model.UserLoginResponseDTO
import retrofit2.Response

import androidx.lifecycle.lifecycleScope
import com.example.proyectopracticasandroid.api.TokenStorage
import kotlinx.coroutines.launch

class LoginUserActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterUser: UserAdapter
    private var listUsers: MutableList<User> = mutableListOf()
    private lateinit var tokenStorage: TokenStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_user)

        //Configuramos el nombre de la pantalla
        findViewById<TextView>(R.id.nav_title).text = getString(R.string.title_activity_login_user)

        recyclerView = findViewById(R.id.recycler_users)

        val spanCount = adjustColumnsToScreenWidth()
        recyclerView.layoutManager = GridLayoutManager(this, spanCount)

        val verticalSpaceHeight = resources.getDimensionPixelSize(R.dimen.vertical_space)
        recyclerView.addItemDecoration(UserDecoration.UserDecoration(verticalSpaceHeight))

        tokenStorage = TokenStorage(this) //

        getUsersAndPrintThemOnTerminal()  //solo para debuguear
        getAllUsersFromAPI()
    }

    private fun adjustColumnsToScreenWidth(): Int {
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels / displayMetrics.density
        val itemWidth = 300 // dp aproximado de cada ítem //Esto igual habria que tener una variable en values y tomar el valor de allí
        return (screenWidth / itemWidth).toInt().coerceAtLeast(1) // Mínimo 1 columna
    }

    //Función utilizada para comprobar que la API funciona
    private fun getUsersAndPrintThemOnTerminal() {
        lifecycleScope.launch { // Usar coroutine para la llamada suspend
            try {
                // Llamada directa a la suspend fun
                val response: Response<List<User>> = RetrofitClient.apiService.getAllUsers()

                if (response.isSuccessful) {
                    val users = response.body()
                    users?.forEach { user ->
                        println("Usuario recibido: ${user.name}")
                    } ?: println("No se recibieron usuarios")
                } else {
                    println("Error en la respuesta de la API: ${response.code()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e("API_ERROR", "Error al obtener usuarios para debug: ${response.code()}, Cuerpo: $errorBody")
                }
            } catch (t: Throwable) {
                println("Error en la conexión (debug): ${t.message}")
                Log.e("API_ERROR", "Fallo al conectar con la API (debug)", t)
                Toast.makeText(this@LoginUserActivity, "Error (debug): ${t.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getAllUsersFromAPI() {
        Toast.makeText(this, getString(R.string.msg_calling_api), Toast.LENGTH_SHORT)
            .show() //Llamando a la API

        lifecycleScope.launch { // Usar coroutine para la llamada suspend
            try {
                val response: Response<List<User>> = RetrofitClient.apiService.getAllUsers()

                if (response.isSuccessful) {
                    val users = response.body()
                    if (users != null) {
                        val sortedUsers = users.sortedBy {  //Ordena alfabeticamente
                            it.name
                                ?: ""   //el ? es por si acaso hay un usuario sin nombre que no deberia pasar realmente
                        }
                        listUsers = sortedUsers.filter { it.enabled }
                            .toMutableList() //Se queda solo los usuarios habilitados/enabled

                        //Al hacer click sobre un usuario comprobamos si es admin para pedir pass o no
                        adapterUser = UserAdapter(listUsers) { clickedUser ->
                                goToProductActivity(clickedUser)
                        }
                        recyclerView.adapter = adapterUser
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e(
                        "LoginUserActivity",
                        "Error de API al obtener usuarios: Código $errorCode, Contenido: $errorBody"
                    )
                    Toast.makeText(
                        this@LoginUserActivity,
                        getString(R.string.error_loading_users),
                        Toast.LENGTH_SHORT
                    ).show() //Error al cargar los usuarios
                }
            } catch (t: Throwable) {
                Log.e("LoginUserActivity", "Excepción al obtener usuarios", t)
                Toast.makeText(
                    this@LoginUserActivity,
                    getString(R.string.error_api_connection),
                    Toast.LENGTH_SHORT
                ).show() //Error de conexión
            }
        }
    }

    private fun goToProductActivity(clickedUser: User) {
        // Creamos un intent para ir a productos
        val intent = Intent(this, ProductActivity::class.java)
        // Pasamos el usuario al ProductActivity
        intent.putExtra("USER", clickedUser)
        startActivity(intent)
    }

    private fun saveToken(token: String) {
        tokenStorage.saveAuthToken(token)
        Log.d("LoginUserActivity", "Token guardado: $token")
    }
}
