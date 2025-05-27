package com.example.proyectopracticasandroid

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Switch
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import android.text.TextWatcher
import android.text.Editable
import android.view.View
import android.widget.EditText

class ProductActivity : BaseActivity() { // Hereda de BaseActivity

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterProduct: ProductAdapter
    private var listaProductos: MutableList<Product> = mutableListOf()
    private var originalProductList: List<Product> = listOf()

    private lateinit var btnAddProduct: Button //Boton para agregar
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private lateinit var filterEditText: EditText


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
        btnSimpleMenu.setOnClickListener { view ->
            showSimpleMenu(view) // Método heredado
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

        // Obtenemos el TextInputEditText del filtro en tiempo real por nombre
        val includedLayout = findViewById<View>(R.id.item_filter)
        if (includedLayout != null) {
            filterEditText = includedLayout.findViewById(R.id.editTextText)

            // Agregamos TextWatcher al campo de filtro para que rastree si escribimos
            filterEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Este método se llama cada vez que el texto cambia
                    filterList(s.toString()) // Llama a la función de filtrado con el texto actual
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })
            Log.d("ProductActivity", "onCreate: Filtro TextInputEditText y TextWatcher configurados.")
        } else {
            Log.e("ProductActivity", "onCreate: ERROR: Layout incluido para filtro (R.id.item_filter) no encontrado!")
        }

        btnAddProduct = findViewById(R.id.btn_add_product)
        // Establece el listener para abrir el diálogo al hacer clic
        btnAddProduct.setOnClickListener {
            showAddProductDialog() // Llama a la función que muestra el diálogo
        }
        Log.d("ProductActivity", "onCreate: fab_add_product FAB/Button encontrado y listener asignado.")

        // Configura el seleccionador de imágenes
        selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    selectedImageUri = uri // Guarda la URI de la imagen seleccionada
                    Log.d("ProductActivity", "ActivityResultLauncher: Imagen seleccionada URI: $uri")
                } else {
                    selectedImageUri = null
                    Log.d("ProductActivity", "ActivityResultLauncher: URI de imagen seleccionada es nula.")
                }
            } else {
                selectedImageUri = null
                Log.d("ProductActivity", "ActivityResultLauncher: Selección de imagen fallada o cancelada.")
            }
        }
        Log.d("ProductActivity", "onCreate: selectImageLauncher inicializado.")

        //Mostrar lo que devuelve la API
        obtenerYImprimirProductos()

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

    // Función para filtrar la lista de productos
    private fun filterList(query: String) {
        Log.d("ProductActivity", "Filtrando por: '$query'") // Log para ver el texto del filtro - debuguear
        val trimmedQuery = query.trim() // Elimina espacios en blanco al inicio/final
        val filteredList = if (trimmedQuery.isBlank()) {
            // Si la consulta está vacía o solo espacios, mostrar la lista original
            originalProductList
        } else {
            // Si hay texto, filtrar la lista original
            originalProductList.filter { product ->
                product.name.trim().startsWith(trimmedQuery, ignoreCase = true)
            }
        }

        Log.d("ProductActivity", "Resultados del filtro: ${filteredList.size} productos encontrados.") // Log para ver cuántos resultados hay

        // Actualizar la lista en el adaptador y notificar al RecyclerView
        listaProductos = filteredList.toMutableList() // Actualiza la lista mutable que usa el adaptador
        adapterProduct.updateList(listaProductos) // Llama al nuevo método del adaptador
    }

    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetTextI18n")
    private fun showAddProductDialog() {
        // Infla el layout del diálogo
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)

        // Enlaza las vistas del layout del diálogo (incluyendo todos los campos de tu Product)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title_product)
        val editName = dialogView.findViewById<TextInputEditText>(R.id.edit_product_name)
        val editAmount = dialogView.findViewById<TextInputEditText>(R.id.edit_product_amount)
        val editMinimumAmount = dialogView.findViewById<TextInputEditText>(R.id.edit_product_minimum_amount)
        val editPrice = dialogView.findViewById<TextInputEditText>(R.id.edit_product_price)
        val editSellPrice = dialogView.findViewById<TextInputEditText>(R.id.edit_product_sell_price)
        val editDescription = dialogView.findViewById<TextInputEditText>(R.id.edit_product_description)

        val switchSeason = dialogView.findViewById<Switch>(R.id.switch_product_season)
        val switchEnabled = dialogView.findViewById<Switch>(R.id.switch_product_enabled) // Corregido ID

        val btnSave = dialogView.findViewById<Button>(R.id.btn_product_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_product_cancel)
        val imageProductPhoto = dialogView.findViewById<ImageView>(R.id.image_product_photo_dialog)
        val btnSelectPhoto = dialogView.findViewById<Button>(R.id.btn_select_photo_product)

        // Configuramos los campos iniciales para añadir un nuevo producto
        dialogTitle?.text = "Agregar Nuevo Producto"
        editName.setText("")
        editAmount.setText("0")
        editMinimumAmount.setText("0")
        editPrice.setText("")
        editSellPrice.setText("")
        editDescription.setText("")

        switchSeason.isChecked = false // Por defecto, no es de temporada
        switchEnabled.isChecked = true // Por defecto, está habilitado

        imageProductPhoto.setImageResource(R.drawable.img_product)

        // Crea el AlertDialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // No se puede cerrar tocando fuera
            .create()

        btnSelectPhoto.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK)
            pickImageIntent.type = "image/*" // Intención para seleccionar cualquier tipo de imagen
            selectImageLauncher.launch(pickImageIntent) // Lanza la actividad de selección
        }

        // Listener para el botón "Guardar" (Crear Producto)
        btnSave.setOnClickListener {
            // Recoge los valores de los campos
            val newName = editName.text.toString().trim()
            val amountText = editAmount.text.toString().trim()
            val minimumAmountText = editMinimumAmount.text.toString().trim()
            val priceText = editPrice.text.toString().trim()
            val sellPriceText= editSellPrice.text.toString().trim()
            editDescription.text.toString().trim()

            val newSeason = switchSeason.isChecked
            val newEnabled = switchEnabled.isChecked

            // Realiza la validación de los campos
            if (newName.isBlank()) {
                editName.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }
            if (amountText.isBlank()) {
                editAmount.error = "La cantidad no puede estar vacía"
                return@setOnClickListener
            }
            val newAmount: Int = try {
                amountText.toInt()
            } catch (e: NumberFormatException) {
                editAmount.error = "Cantidad inválida"
                return@setOnClickListener
            }
            if (newAmount < 0) {
                editAmount.error = "La cantidad no puede ser negativa"
                return@setOnClickListener
            }

            if (minimumAmountText.isBlank()) {
                editMinimumAmount.error = "La cantidad mínima no puede estar vacía"
                return@setOnClickListener
            }
            val newMinimumAmount: Int = try {
                minimumAmountText.toInt()
            } catch (e: NumberFormatException) {
                editMinimumAmount.error = "Cantidad mínima inválida"
                return@setOnClickListener
            }
            if (newMinimumAmount < 0) {
                editMinimumAmount.error = "La cantidad mínima no puede ser negativa"
                return@setOnClickListener
            }

            if (priceText.isBlank()) {
                editPrice.error = "El precio de compra no puede estar vacío"
                return@setOnClickListener
            }
            val newPrice: Double = try {
                priceText.toDouble()
            } catch (e: NumberFormatException) {
                editPrice.error = "Precio de compra inválido"
                Log.e("ProductActivity", "Error al parsear precio de compra: ${e.message}")
                return@setOnClickListener
            }
            if (newPrice < 0) {
                editPrice.error = "El precio de compra no puede ser negativo"
                return@setOnClickListener
            }

            // Validar y parsear precio de venta (puede ser null)
            val newSellPrice: Double? = if (sellPriceText.isNotBlank()) {
                try {
                    val parsedPrice = sellPriceText.toDouble()
                    if (parsedPrice < 0) {
                        editSellPrice.error = "El precio de venta no puede ser negativo"
                        return@setOnClickListener
                    }
                    parsedPrice
                } catch (e: NumberFormatException) {
                    editSellPrice.error = "Precio de venta inválido"
                    Log.e("ProductActivity", "Error al parsear precio de venta: ${e.message}")
                    return@setOnClickListener
                }
            } else {
                null // Si está vacío, es null
            }

            // Validar que se haya seleccionado una imagen
            if (selectedImageUri == null) {
                Toast.makeText(this, "Por favor, selecciona una imagen para el producto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val imageToSend = selectedImageUri.toString() // Convierte la URI a String


            // Creamos el objeto Product con todos los datos recogidos
            val newProduct = Product(
                productId = 0L,
                image = imageToSend,
                name = newName,
                amount = newAmount,
                minimumAmount = newMinimumAmount,
                season = newSeason,
                enabled = newEnabled,
                price = newPrice,
                sellPrice = newSellPrice // Puede ser null
            )

            // Llama a la función para crear el producto en la API
            createProduct(newProduct, dialog)
        }

        // Listener para el botón "Cancelar"
        btnCancel.setOnClickListener {
            dialog.dismiss() // Cierra el diálogo
            selectedImageUri = null // Limpia la URI de la imagen seleccionada
        }

        // Muestra el diálogo
        dialog.show()
    }

    // Esta función hace la llamada a la API para crear el producto
    private fun createProduct(newProduct: Product, dialog: AlertDialog) {
        Toast.makeText(this, "Creando producto...", Toast.LENGTH_SHORT).show()
        // El token no está implementado, lo dejo para mas adelante
        val token = tokenStorage.getAuthToken()
        if (token == null) {
            Log.w("ProductActivity", "createProduct: No hay token guardado. Redirigiendo a login.")
            Toast.makeText(this, "Sesión expirada o no iniciada.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish()
            dialog.dismiss()
            return
        }

        val authTokenHeader = "Bearer $token" // Formato del encabezado de autorización
        Log.d("ProductActivity", "createProduct: Usando token: ${authTokenHeader}")

        // Usa lifecycleScope.launch para ejecutar la llamada a la API en una corrutina, asíncrono
        lifecycleScope.launch {
            try {
                // Llama al método createProduct de tu ApiService
                val response: Response<Product> = RetrofitClient.apiService.createProduct(newProduct)

                if (response.isSuccessful && response.body() != null) {
                    val createdProduct = response.body()
                    Log.d("ProductActivity", "Producto creado correctamente en la API: ${createdProduct?.name}")
                    Toast.makeText(this@ProductActivity, "Producto creado correctamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Cierra el diálogo al tener éxito
                    selectedImageUri = null // Limpia la URI de la imagen
                    getAllProductsFromAPI() // Refresca la lista
                } else if (response.code() == 401 || response.code() == 403) {
                    Log.w("ProductActivity", "createProduct: Acceso denegado. Código: ${response.code()}")
                    Toast.makeText(this@ProductActivity, "Acceso denegado. Por favor, inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
                    tokenStorage.deleteAuthToken() // Elimina el token inválido
                    startActivity(Intent(this@ProductActivity, LoginUserActivity::class.java))
                    finish()
                    dialog.dismiss()
                }
                else {
                    // Maneja otros códigos de error
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e("ProductActivity", "Error de API al crear producto: Código $errorCode, Contenido: $errorBody")
                    Toast.makeText(this@ProductActivity, "Error al crear producto", Toast.LENGTH_SHORT).show()
                    if (!errorBody.isNullOrBlank()) {
                        Log.e("ProductActivity", "Cuerpo del error de creación: $errorBody")
                    }
                }
            } catch (e: Exception) {
                // Maneja excepciones de red o inesperadas
                Log.e("ProductActivity", "Excepción al crear producto", e)
                Toast.makeText(this@ProductActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
            }
        }
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
        Toast.makeText(this, "Llamando a la API para obtener productos", Toast.LENGTH_SHORT).show()
        RetrofitClient.apiService.getAllProducts().enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                Log.d("ProductActivity", "Respuesta API productos: ${response.code()}")
                if (response.isSuccessful) {
                    val productos = response.body()
                    if (productos != null) {
                        originalProductList = productos.toList() //Guarda la lista original
                        listaProductos = productos.toMutableList()
                        //Log para debuguear la lista
                        Log.d("ProductActivity", "Lista original de productos cargada: ${originalProductList.size} items")
                        originalProductList.forEach { product ->
                            Log.d("ProductActivity", " - Producto: ${product.name}")
                        }
                        // Pasamos la instancia de apiService, lifecycleScope y el Context (this@ProductActivity) al adaptador
                        adapterProduct = ProductAdapter(
                            listaProductos,
                            RetrofitClient.apiService,
                            lifecycleScope,          // El scope de corrutinas de la Activity
                            this@ProductActivity     // Contexto de la Activity
                        )

                        recyclerView.adapter = adapterProduct
                        if (::filterEditText.isInitialized) {
                            val currentFilterText = filterEditText.text.toString()
                            filterList(currentFilterText)   //Filtramos los productos segun lo que escribamos en vivo
                        } else {
                            Log.w("ProductActivity", "getAllProductsFromAPI: filterEditText no inicializado al cargar productos.")
                        }
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