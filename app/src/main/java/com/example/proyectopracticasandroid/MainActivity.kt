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
import com.example.proyectopracticasandroid.databinding.ActivityMainBinding
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import com.example.proyectopracticasandroid.api.RetrofitClient
import com.example.proyectopracticasandroid.api.TokenStorage
import com.example.proyectopracticasandroid.model.User
import com.example.proyectopracticasandroid.model.UserLoginDTO
import com.example.proyectopracticasandroid.model.UserLoginResponseDTO
import kotlinx.coroutines.launch
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenStorage: TokenStorage
    private var currentUserRole: String? = null
    private var loggedInUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        // Obtenemos el rol del usuario desde el Intent
        currentUserRole = intent.getStringExtra("USER_ROLE")
        loggedInUser = intent.getSerializableExtra("USER") as? User

        Log.d("MainActivity", "Rol del usuario: $currentUserRole")

        if (currentUserRole == null || loggedInUser == null) {
            Log.e("MainActivity", "Error de sesión, redirigiendo a login.")
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
            showSimpleMenu(view)
        }
    }

    private fun showSimpleMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        val inflater = popupMenu.menuInflater

        if (currentUserRole.equals("ADMINISTRADOR", ignoreCase = true)) {
            inflater.inflate(R.menu.admin_menu, popupMenu.menu)
        } else {
            inflater.inflate(R.menu.user_menu, popupMenu.menu)
        }

        // Configurar listener para las opciones del menú
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_products -> {
                    startActivity(Intent(this, ProductActivity::class.java))
                    true
                }
                R.id.menu_admin_products -> {
                    startActivity(Intent(this, AdminProductActivity::class.java))
                    true
                }
                R.id.menu_admin_users -> {
                    requireAdminPasswordAndLaunch(Intent(this, AdminUserActivity::class.java))
                    true
                }
                R.id.menu_invoices -> {
                    requireAdminPasswordAndLaunch(Intent(this, AdminInvoicesActivity::class.java))
                    true
                }
                R.id.menu_inventory -> {
                    requireAdminPasswordAndLaunch(Intent(this, AdminInventoryActivity::class.java))
                    true
                }
                R.id.menu_generate_reports -> {
                    requireAdminPasswordAndLaunch(Intent(this, GenerateReportsActivity::class.java))
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
                                this@MainActivity,
                                getString(R.string.error_login_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else if (response.code() == 401) {
                        editPassword.error = getString(R.string.error_incorrect_password)
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_incorrect_password),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.error_api_connection),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error verificando contraseña", e)
                    Toast.makeText(
                        this@MainActivity,
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

    // Cerrar sesión
    private fun performLogout() {
        tokenStorage.deleteAuthToken()
        Log.d("MainActivity", "Token eliminado. Sesión cerrada.")

        // Redirigir al usuario a la pantalla de login
        val intent = Intent(this, LoginUserActivity::class.java)
        // Flags para actividades abiertas y evitar volver atrás
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
    }
}
