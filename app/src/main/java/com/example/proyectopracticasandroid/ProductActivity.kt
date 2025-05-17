package com.example.proyectopracticasandroid

import com.example.proyectopracticasandroid.decoration.ProductDecoration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
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
import android.view.View // Necesario para el listener del botón del menú
import android.widget.ImageButton
import androidx.appcompat.widget.Toolbar

class ProductActivity : BaseActivity() { // Hereda de BaseActivity!

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterProduct: ProductAdapter
    private var listaProductos: MutableList<Product> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ProductActivity", "onCreate: super.onCreate finished. Usuario logueado con rol: $currentUserRole")
        Toast.makeText(this, "Bienvenido ${loggedInUser?.name}", Toast.LENGTH_SHORT).show() // Usa loggedInUser heredado

        // Establece el layout específico de esta Activity
        setContentView(R.layout.activity_product)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Botón del menú
        val btnSimpleMenu: ImageButton = findViewById(R.id.btn_simple_menu)
        if (btnSimpleMenu == null) {
            Log.e("ProductActivity", "ERROR: Botón de menú (R.id.btn_simple_menu) no encontrado en el layout.")
        } else {
            btnSimpleMenu.setOnClickListener { view ->
                showSimpleMenu(view) // Método heredado
            }
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
        return (anchoPantalla / anchoItem).toInt().coerceAtLeast(1) // Asegurar al menos 1 columna
    }

    // Función utilizada para comprobar que la API funciona
    private fun obtenerYImprimirProductos() {
        RetrofitClient.apiService.getAllProducts().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    val productos = response.body()
                    if (productos != null) {
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
                Log.d("ProductActivity", "Respuesta API productos: ${response.code()}")
                if (response.isSuccessful) {
                    val productos = response.body()
                    if (productos != null) {
                        listaProductos = productos.toMutableList()
                        adapterProduct = ProductAdapter(listaProductos)
                        recyclerView.adapter = adapterProduct
                    } else {
                        Log.w("ProductActivity", "getAllProductsFromAPI: Respuesta exitosa pero body es null.")
                        Toast.makeText(this@ProductActivity, "Error: Datos de productos vacíos.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e("ProductActivity", "Error API al cargar productos: Código $errorCode, Contenido: $errorBody")
                    Toast.makeText(this@ProductActivity, "Error al cargar los productos (${errorCode})", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Log.e("ProductActivity", "Fallo de conexión al obtener productos: ${t.message}", t)
                Toast.makeText(this@ProductActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }
}