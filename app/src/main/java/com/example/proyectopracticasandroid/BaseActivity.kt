package com.example.proyectopracticasandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.example.proyectopracticasandroid.api.RetrofitClient
import com.example.proyectopracticasandroid.api.TokenStorage
import com.example.proyectopracticasandroid.model.User
import com.example.proyectopracticasandroid.model.UserLoginDTO
import com.example.proyectopracticasandroid.model.UserLoginResponseDTO
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.Serializable

// Open permite que otras clases hereden de esta
open class BaseActivity : AppCompatActivity() {
    //Variables
    protected lateinit var tokenStorage: TokenStorage
    protected var currentUserRole: String? = null
    protected var loggedInUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenStorage = TokenStorage(this)

        loggedInUser = intent.getSerializableExtra("USER") as? User

        // Ahora, si el objeto User se recibió correctamente, derivamos el rol de él
        if (loggedInUser != null) {
            currentUserRole = loggedInUser?.rol
            Log.d("BaseActivity", "BaseActivity iniciada. Objeto User recibido. Rol: $currentUserRole")
        }

        // Verifica la sesión, si son null, es que no hay usuario logueado, por lo tanto vuelve al login
        if (loggedInUser == null || currentUserRole == null) {
            Log.e("BaseActivity", "No se pudo obtener el objeto User o el rol. Redirigiendo a login.")
            Toast.makeText(this, "Error de sesión. Inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish() // Cierra la actividad actual
            return
        }

        // Si llegamos aquí, significa que el objeto User (loggedInUser) y el rol se obtuvieron correctamente
        Log.e("BaseActivity", "loggedInUser y rol obtenidos correctamente.")
    }

    // Función para mostrar el menú simple, utiliza currentUserRole que deriva del loggedInUser.
    protected open fun showSimpleMenu(view: View) {
        val popupMenu = PopupMenu(this, view) // this se refiere a la Activity que hereda
        val inflater = popupMenu.menuInflater

        // Mostrar menú correcto basado en el rol del usuario (currentUserRole)
        if (currentUserRole == "ADMINISTRADOR") {
            inflater.inflate(R.menu.admin_menu, popupMenu.menu) // Mostrar menú de admin
        } else {
            inflater.inflate(R.menu.user_menu, popupMenu.menu) // Mostrar menú de usuario común
        }

        // Al lanzar una Activitie que hereden de BaseActivity,
        // hay que pasarle el objeto User completo -> loggedInUser

        // Configurar listener para las opciones del menú
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_products -> {
                    //Navegar a ProudctAcivity
                    // val intent = Intent(this, ProductActivity::class.java) // <-- si ProductActivity heredara de BaseActivity
                    // if (loggedInUser != null) intent.putExtra("USER", loggedInUser as Serializable) // <-- Pasa el objeto User
                    // startActivity(intent)
                    // O si ProductActivity NO hereda de BaseActivity (como en tu código actual de ProductActivity):
                    val intent = Intent(this, ProductActivity::class.java)
                    if (loggedInUser != null) intent.putExtra("USER", loggedInUser as Serializable)
                    startActivity(intent)
                    Toast.makeText(this, "Ir a Productos", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_admin_products -> {
                    //Navegar a ProudctAcivity
                    // val intent = Intent(this, ProductActivity::class.java) // <-- si ProductActivity heredara de BaseActivity
                    // if (loggedInUser != null) intent.putExtra("USER", loggedInUser as Serializable) // <-- Pasa el objeto User
                    // startActivity(intent)
                    // O si ProductActivity NO hereda de BaseActivity (como en tu código actual de ProductActivity):
                    val intent = Intent(this, AdminProductActivity::class.java)
                    if (loggedInUser != null) intent.putExtra("USER", loggedInUser as Serializable)
                    requireAdminPasswordAndLaunch(intent)
                    Toast.makeText(this, "Ir a Administrar Productos", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_admin_users -> {
                    // Navegar a AdminUserActivity
                    val intent = Intent(this, AdminUserActivity::class.java)
                    if (loggedInUser != null) {
                        intent.putExtra("USER", loggedInUser as Serializable)
                        Log.d("BaseActivity", "Lanzando AdminUserActivity con objeto User.")
                    } else {
                        Log.e("BaseActivity", "Error al lanzar AdminUserActivity: loggedInUser es null.")
                        // Mensaje para debuguear, para saber si no hay un usuarios logueado.
                    }
                    requireAdminPasswordAndLaunch(intent)
                    Toast.makeText(this, "Ir a Administrador de Usuarios", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_invoices -> {
                    val intent = Intent(this, AdminInvoicesActivity::class.java)
                    if (loggedInUser != null) intent.putExtra("USER", loggedInUser as Serializable)
                    requireAdminPasswordAndLaunch(intent)
                    Toast.makeText(this, "Ir a Administrador de Albaranes", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_inventory -> {
                    val intent = Intent(this, AdminInventoryActivity::class.java)
                    if (loggedInUser != null) intent.putExtra("USER", loggedInUser as Serializable)
                    requireAdminPasswordAndLaunch(intent)
                    Toast.makeText(this, "Ir a Administrador de Inventario", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_generate_reports -> {
                    val intent = Intent(this, GenerateReportsActivity::class.java)
                    if (loggedInUser != null) intent.putExtra("USER", loggedInUser as Serializable)
                    requireAdminPasswordAndLaunch(intent)
                    Toast.makeText(this, "Ir a Generar Informes", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }
    private fun requireAdminPasswordAndLaunch(targetIntent: Intent) {
        if (currentUserRole.equals("administrador", ignoreCase = true) && loggedInUser != null) {
            showPasswordDialog(loggedInUser!!) {
                startActivity(targetIntent)
            }
        } else {
            Toast.makeText(this, "No tiene permisos de administrador", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showPasswordDialog(clickedUser: User, onSuccess: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password, null)
        val editPassword = dialogView.findViewById<EditText>(R.id.edit_password)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val enteredPassword = editPassword.text.toString()
            if (enteredPassword.isBlank()) {
                editPassword.error = getString(R.string.error_password_empty)
                return@setOnClickListener
            }

            val adminLoginIdentifier = clickedUser.name

            lifecycleScope.launch {
                try {
                    val loginRequest = UserLoginDTO(adminLoginIdentifier, enteredPassword)
                    val response: Response<UserLoginResponseDTO> =
                        RetrofitClient.apiService.login(loginRequest)

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        if (loginResponse != null) {
                            val token = loginResponse.token
                            Log.d("MainActivity", "Verificación exitosa. Token recibido: $token")
                            saveToken(token)
                            dialog.dismiss()
                            onSuccess()
                        } else {
                            Toast.makeText(
                                this@BaseActivity,
                                getString(R.string.error_login_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (response.code() == 401) {
                        editPassword.error = getString(R.string.error_incorrect_password)
                        Toast.makeText(
                            this@BaseActivity,
                            getString(R.string.error_incorrect_password),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@BaseActivity,
                            getString(R.string.error_api_connection),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error verificando contraseña", e)
                    Toast.makeText(
                        this@BaseActivity,
                        getString(R.string.error_api_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.show()
    }
    private fun saveToken(token: String) {
        tokenStorage.saveAuthToken(token)
    }
    // Función para cerrar sesión - Usa tokenStorage que está en BaseActivity
    protected fun performLogout() {
        tokenStorage.deleteAuthToken()  // Eliminamos el token guardado
        Log.d("BaseActivity", "Token eliminado. Sesión cerrada.")

        // Redirige al usuario a la pantalla de login
        val intent = Intent(this, LoginUserActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Cierra la actividad actual
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }
}