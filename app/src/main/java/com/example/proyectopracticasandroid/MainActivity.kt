package com.example.proyectopracticasandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectopracticasandroid.databinding.ActivityMainBinding
import androidx.appcompat.widget.PopupMenu
import com.example.proyectopracticasandroid.api.TokenStorage

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenStorage: TokenStorage
    private var currentUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        // Obtenemos el rol del usuario desde el Intent
        currentUserRole = intent.getStringExtra("USER_ROLE")
        if (currentUserRole == null) {
            // Si no hay rol, redirigir a login
            Log.e("MainActivity", "No se recibió el rol del usuario, redirigiendo a login.")
            Toast.makeText(this, "Error de sesión, inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish()
            return
        }
        Log.d("MainActivity", "Usuario logueado con rol: $currentUserRole")

        // TokenStorage
        tokenStorage = TokenStorage(this)

        // Botón del menú desplegable
        binding.appBarMain.btnSimpleMenu?.setOnClickListener { view ->
            showSimpleMenu(view) // Mostrar el menú
        }
    }

    // Mostrar menú simple
    private fun showSimpleMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        val inflater = popupMenu.menuInflater

        // Mostrar menú correcto basado en el rol del usuario
        if (currentUserRole == "ADMINISTRADOR") {
            inflater.inflate(R.menu.admin_menu, popupMenu.menu)
        } else {    //Menú para usuarios no administradores
            inflater.inflate(R.menu.user_menu, popupMenu.menu)
        }

        // Configurar listener para las opciones del menú
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_products -> {
                    val intent = Intent(this, ProductActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this, "Ir a Productos", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_admin_products -> {
                val intent = Intent(this, AdminProductActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Ir a Productos", Toast.LENGTH_SHORT).show()
                true
            }
                //Para ir a login usuarios basta con Salir
                /*R.id.menu_users -> {
                    Toast.makeText(this, "Ir a login Usuarios", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginUserActivity::class.java)
                    startActivity(intent)
                    true
                }*/
                R.id.menu_admin_users -> {
                    Toast.makeText(this, "Ir a administrador de Usuarios", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AdminUserActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_invoices -> {
                    Toast.makeText(this, "Ir a administrador de Albaranes", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AdminInvoicesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_inventory -> {
                    Toast.makeText(this, "Ir a administrador Inventario", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AdminInventoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_generate_reports -> {
                    Toast.makeText(this, "Ir a Generar Informes", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, GenerateReportsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.menu_logout -> { // Cerrar sesión
                    performLogout()
                    true
                }
                else -> false
            }
        }
        popupMenu.show() // Mostrar el menú desplegable
    }

    // Cerrar sesión
    private fun performLogout() {
        tokenStorage.deleteAuthToken()  // Eliminar el token guardado
        Log.d("MainActivity", "Token eliminado. Sesión cerrada.")

        // Redirigir al usuario a la pantalla de login
        val intent = Intent(this, LoginUserActivity::class.java)
        // Flags para actividades abiertas y evitar volver atrás
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Cierra MainActivity
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }
}