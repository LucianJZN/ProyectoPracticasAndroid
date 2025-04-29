package com.example.proyectopracticasandroid

import com.example.proyectopracticasandroid.decoration.ProductDecoration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectopracticasandroid.adapter.ProductAdapter
import com.example.proyectopracticasandroid.model.Product

import com.example.proyectopracticasandroid.api.RetrofitClient
import com.example.proyectopracticasandroid.model.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.PopupMenu
import com.example.proyectopracticasandroid.api.TokenStorage

class ProductActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterProduct: ProductAdapter
    private var listaProductos: MutableList<Product> = mutableListOf()
    private lateinit var tokenStorage: TokenStorage
    private var currentUserRole: String? = null
    private var loggedInUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Obtener el objeto User pasado desde LoginUserActivity
        loggedInUser = intent.getSerializableExtra("USER") as? User //"USER" LoginUserActivity

        if (loggedInUser != null) {
            currentUserRole = loggedInUser?.rol
            Log.d("ProductActivity", "Usuario logueado con rol: $currentUserRole")
            Toast.makeText(this, "Bienvenido ${loggedInUser?.name}", Toast.LENGTH_SHORT).show() // Mostrar bienvenida
        } else {
            // Si User está null, redirigir a login
            Log.e("ProductActivity", "No se recibió el objeto User. Redirigiendo a login.")
            Toast.makeText(this, "Error de sesión. Inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish()
            return
        }
        // Inicializar TokenStorage
        tokenStorage = TokenStorage(this)

        //Botón del menú
        val btnSimpleMenu: ImageButton = findViewById(R.id.btn_simple_menu)
        btnSimpleMenu.setOnClickListener { view ->
            showSimpleMenu(view)
        }
        //Configuramos el título del ProductActivity
        findViewById<TextView>(R.id.nav_title)?.text = getString(R.string.title_product_activity)

        // Configuramos la separación horizontal, además en item_product hay un layout_marginStart de 8dp
        recyclerView = findViewById(R.id.recycler_products)
        val spanCount = calcularColumnasSegunAnchoPantalla()
        recyclerView.layoutManager = GridLayoutManager(this, spanCount) // Gestionamos columnas

        // Configuramos la separación entre filas
        val verticalSpaceHeight = resources.getDimensionPixelSize(R.dimen.vertical_space)
        recyclerView.addItemDecoration(ProductDecoration(verticalSpaceHeight))

        //Mostrar lo que devuelve la API
        // obtenerYImprimirProductos()

        // Obtenemos los productos de la API
        getAllProductsFromAPI()
        Toast.makeText(this, "Obtenemos los productos de la API", Toast.LENGTH_SHORT).show()
    }

    private fun calcularColumnasSegunAnchoPantalla(): Int {
        val displayMetrics = Resources.getSystem().displayMetrics
        val anchoPantalla = displayMetrics.widthPixels / displayMetrics.density
        val anchoItem = 300 // ancho aproximado en dp de cada ítem_producto
        return (anchoPantalla / anchoItem).toInt()
    }

    // Función utilizada para comprobar que la API funciona
    private fun obtenerYImprimirProductos() {
        RetrofitClient.apiService.getAllProducts().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    val productos = response.body()
                    if (productos != null) {
                        // Imprime todos los productos recibidos
                        productos.forEach { producto ->
                            println("Producto recibido (debug): ${producto.name}")
                        }
                    } else {
                        println("No se recibieron productos (debug)")
                    }
                } else {
                    println("Error en la respuesta de la API (debug): ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                println("Error en la conexión (debug): ${t.message}")
            }
        })
    }

    // Función para obtener todos los productos de la API
    private fun getAllProductsFromAPI() {
        Toast.makeText(this, "Llamando a la API", Toast.LENGTH_SHORT).show()
        RetrofitClient.apiService.getAllProducts().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                println("RetrofitClient1234: ${RetrofitClient.apiService}")
                Toast.makeText(this@ProductActivity, "API test", Toast.LENGTH_SHORT).show()
                println("Respuesta de la API Hola1: ${response.body()}")  // Verifica que la respuesta tenga productos
                if (response.isSuccessful) {
                    val productos = response.body()
                    if (productos != null) {
                        listaProductos = productos.toMutableList()

                        adapterProduct = ProductAdapter(listaProductos) // Aquí actualizamos el adapter en el hilo principal
                        recyclerView.adapter = adapterProduct
                    }
                } else {
                    Toast.makeText(this@ProductActivity, "Error al cargar los productos", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Toast.makeText(this@ProductActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // Mostrar el menú simple
    private fun showSimpleMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        val inflater = popupMenu.menuInflater

        // Mostrar menú correcto basado en el rol del usuario
        if (currentUserRole == "ADMINISTRADOR") {
            inflater.inflate(R.menu.admin_menu, popupMenu.menu) // Mostrar menú de admin para admin
        } else {
            inflater.inflate(R.menu.user_menu, popupMenu.menu) // Mostrar menú de usuario común
        }
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_products -> {
                    Toast.makeText(this, "Ya estás en Productos", Toast.LENGTH_SHORT).show()
                    //Puede que se necesite actualizar otra vez los productos
                    // getAllProductsFromAPI()
                    true
                }
                R.id.menu_users -> {
                    // Navegar a la pantalla de Usuarios (si es diferente de AdminUsers)
                    // val intent = Intent(this, UsersActivity::class.java) // Asumiendo una UsersActivity
                    // startActivity(intent)
                    Toast.makeText(this, "Ir a Usuarios", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_admin_users -> {
                    // Navegar a la pantalla de Administrar Usuarios
                    val intent = Intent(this, AdminUserActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_invoices -> {
                    // Navegar a la pantalla de Albaranes
                    // val intent = Intent(this, InvoicesActivity::class.java) // Asumiendo una InvoicesActivity
                    // startActivity(intent)
                    Toast.makeText(this, "Ir a Albaranes", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_inventory -> {
                    // Navegar a la pantalla de Inventario
                    // val intent = Intent(this, InventoryActivity::class.java) // Asumiendo una InventoryActivity
                    // startActivity(intent)
                    Toast.makeText(this, "Ir a Inventario", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_generate_reports -> {
                    // Navegar a la pantalla de Generar Informes
                    // val intent = Intent(this, ReportsActivity::class.java) // Asumiendo una ReportsActivity
                    // startActivity(intent)
                    Toast.makeText(this, "Ir a Generar Informes", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_logout -> {
                    // Cerrar sesión
                    performLogout()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    // Cerrar sesión
    private fun performLogout() {
        tokenStorage.deleteAuthToken()  //Eliminamos el token.
        Log.d("ProductActivity", "Token eliminado. Sesión cerrada.")

        // Redirigir al usuario a la pantalla de login
        val intent = Intent(this, LoginUserActivity::class.java)
        // Eliminamos actividades para evitar volver atrás
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }

}
