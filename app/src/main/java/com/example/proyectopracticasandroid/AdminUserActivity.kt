package com.example.proyectopracticasandroid

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.proyectopracticasandroid.adapter.UserAdapter
import com.example.proyectopracticasandroid.api.RetrofitClient
import com.example.proyectopracticasandroid.decoration.UserDecoration
import com.example.proyectopracticasandroid.model.User
import kotlinx.coroutines.launch
import retrofit2.Response

class AdminUserActivity : BaseActivity() { //Hereda de BaseActivity

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterUser: UserAdapter
    private var listUsers: MutableList<User> = mutableListOf()
    // tokenStorage hereda de BaseActivity
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private lateinit var btnAddUser: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        //Llamamos primero al onCreate de BaseActivity para tener disponible todas sus implementaciones
        super.onCreate(savedInstanceState)
        // Log para debuguear. BaseActivity.onCreate terminó y el usuario/rol se obtuvieron
        Log.d("AdminUserActivity", "onCreate: super.onCreate finished. loggedInUser: ${loggedInUser?.name}, currentUserRole: $currentUserRole")

        // Hay que establecer el layout de esta Activity
        setContentView(R.layout.activity_admin_user)
        Log.d("AdminUserActivity", "onCreate: Layout activity_admin_user set.")

        // Configurar el nombre de la pantalla
        val navTitleTextView = findViewById<TextView>(R.id.nav_title)
        if (navTitleTextView == null) {
            Log.e("AdminUserActivity", "ERROR: nav_title TextView no encontrado!")
        } else {
            navTitleTextView.text = getString(R.string.title_activity_admin_user)
            Log.d("AdminUserActivity", "onCreate: nav_title TextView encontrado y texto asignado.")
        }

        // Configuración del RecyclerView y adaptador
        recyclerView = findViewById(R.id.recycler_users)
        if (recyclerView == null) {
            Log.e("AdminUserActivity", "ERROR: recycler_users RecyclerView no encontrado!")
        } else {
            Log.d("AdminUserActivity", "onCreate: RecyclerView encontrado.")
        }

        val spanCount = adjustColumnsToScreenWidth()
        recyclerView.layoutManager = GridLayoutManager(this, spanCount)
        val verticalSpaceHeight = resources.getDimensionPixelSize(R.dimen.vertical_space)
        recyclerView.addItemDecoration(UserDecoration.UserDecoration(verticalSpaceHeight))
        Log.d("AdminUserActivity", "onCreate: RecyclerView configurado.")


        // Botón para agregar usuarios
        // Asegúrate de que R.id.btn_add_user exista en tu layout
        btnAddUser = findViewById(R.id.btn_add_user)
        if (btnAddUser == null) {
            Log.e("AdminUserActivity", "ERROR: btn_add_user Button no encontrado!")
        } else {
            btnAddUser.setOnClickListener {showAddUserDialog() }
            Log.d("AdminUserActivity", "onCreate: btn_add_user Button encontrado y listener asignado.")
        }

        // Seleccionar imagen
        selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    selectedImageUri = uri
                    Log.d("AdminUserActivity", "ActivityResultLauncher: Imagen seleccionada URI: $uri")
                } else {
                    selectedImageUri = null
                    Log.d("AdminUserActivity", "ActivityResultLauncher: URI de imagen seleccionada es nula.")
                }
            } else {
                selectedImageUri = null
                Log.d("AdminUserActivity", "ActivityResultLauncher: Selección de imagen fallada o cancelada.")
            }
        }
        Log.d("AdminUserActivity", "onCreate: selectImageLauncher inicializado.")

        try {
            val appBarView = findViewById<View>(R.id.app_bar_main)
            if (appBarView == null) {
                Log.e("AdminUserActivity", "ERROR FATAL: Layout incluido (R.id.app_bar_main) NO encontrado!")
            } else {
                Log.d("AdminUserActivity", "onCreate: Layout incluido (R.id.app_bar_main) encontrado.")
                val btnMenu = appBarView.findViewById<View>(R.id.btn_simple_menu)
                if (btnMenu == null) {
                    Log.e("AdminUserActivity", "ERROR FATAL: Botón de menú (R.id.btn_simple_menu) NO encontrado dentro del layout incluido")
                } else {
                    // Si se encuentra, asigna el listener
                    Log.d("AdminUserActivity", "ÉXITO: Botón de menú (btn_simple_menu) encontrado correctamente.")
                    btnMenu.setOnClickListener { view ->
                        // Log para confirmar que el click llega al listener
                        Log.d("AdminUserActivity", "Botón de menú clickeado. Llamando a showSimpleMenu()")
                        showSimpleMenu(view)
                    }
                }
            }
        } catch (e: Exception) {
            // Captura cualquier excepción general durante la búsqueda de vistas
            Log.e("AdminUserActivity", "ERROR FATAL: Excepción general al buscar vistas en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error interno al inicializar la pantalla (Excepción en búsqueda).", Toast.LENGTH_LONG).show()
        }

        // Obtener los usuarios de la API
        Log.d("AdminUserActivity", "onCreate: Llamando a getAllUsersFromAPI().")
        getAllUsersFromAPI()
    }

    private fun adjustColumnsToScreenWidth(): Int {
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels / displayMetrics.density
        val itemWidth = 300 // dp aproximado de cada ítem
        return (screenWidth / itemWidth).toInt().coerceAtLeast(1)
    }

    private fun getAllUsersFromAPI() {
        Toast.makeText(this, getString(R.string.msg_calling_api), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val response: Response<List<User>> = RetrofitClient.apiService.getAllUsers()
                if (response.isSuccessful) {
                    val users = response.body()
                    if (users != null) {
                        val sortedUsers = users.sortedWith(compareBy<User> {
                            if (it.enabled == true) 0 else 1
                        }.thenBy {
                            it.name ?: ""
                        })

                        listUsers = sortedUsers.toMutableList()
                        adapterUser = UserAdapter(listUsers) { clickedUser ->
                            showEditUserDialog(clickedUser)
                        }
                        recyclerView.adapter = adapterUser
                        Toast.makeText(this@AdminUserActivity, getString(R.string.msg_users_loaded), Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("AdminUserActivity", "getAllUsersFromAPI: Respuesta afirmativa sin usuarios.")
                        Toast.makeText(this@AdminUserActivity, getString(R.string.error_loading_users), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e("AdminUserActivity", "Error de API al obtener usuarios: Código $errorCode, Contenido: $errorBody")
                    Toast.makeText(this@AdminUserActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AdminUserActivity", "Error al obtener usuarios.", e)
                Toast.makeText(this@AdminUserActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_user, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val editName = dialogView.findViewById<EditText>(R.id.edit_edit_name)
        val editMail = dialogView.findViewById<EditText>(R.id.edit_edit_mail)
        val editPassword = dialogView.findViewById<EditText>(R.id.edit_password)
        val spinnerRol = dialogView.findViewById<Spinner>(R.id.spinner_edit_rol)
        val switchEnabled = dialogView.findViewById<Switch>(R.id.switch_edit_enabled)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_edit_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_edit_cancel)
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_edit_delete)
        val imageUserPhoto = dialogView.findViewById<ImageView>(R.id.image_edit_user_photo)
        val btnSelectPhoto = dialogView.findViewById<Button>(R.id.btn_select_photo)

        dialogTitle?.text = "Agregar Nuevo Usuario"
        editName.setText("")
        editMail.setText("")
        editPassword.setText("")
        editPassword.visibility = View.VISIBLE
        switchEnabled.isChecked = true

        val roles = listOf("USUARIO", "ADMINISTRADOR")
        val adapterRoles = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRol.adapter = adapterRoles
        spinnerRol.setSelection(roles.indexOf("USUARIO"))

        btnDelete.visibility = View.GONE
        imageUserPhoto.setImageResource(R.drawable.img_product)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnSelectPhoto.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK)
            pickImageIntent.type = "image/*"
            selectImageLauncher.launch(pickImageIntent)
        }

        btnSave.setOnClickListener {
            val newName = editName.text.toString()
            val newMail = editMail.text.toString()
            val newPassword = editPassword.text.toString()
            val newRol = spinnerRol.selectedItem.toString()
            val newEnabled = switchEnabled.isChecked

            if (newName.isBlank()) {
                editName.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }
            if (newPassword.isBlank()) {
                editPassword.error = "La contraseña no puede estar vacía"
                return@setOnClickListener
            }

            val imageToSend = selectedImageUri?.toString()

            val newUser = User(
                userId = 0,
                name = newName,
                mail = newMail,
                image = imageToSend,
                pass = newPassword,
                rol = newRol,
                enabled = newEnabled
            )
            createUser(newUser, dialog)
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
            selectedImageUri = null
        }
        dialog.show()
    }

    private fun showEditUserDialog(userToEdit: User) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_user, null)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialog_title)
        val editName = dialogView.findViewById<EditText>(R.id.edit_edit_name)
        val editMail = dialogView.findViewById<EditText>(R.id.edit_edit_mail)
        val editPassword = dialogView.findViewById<EditText>(R.id.edit_password)
        val spinnerRol = dialogView.findViewById<Spinner>(R.id.spinner_edit_rol)
        val switchEnabled = dialogView.findViewById<Switch>(R.id.switch_edit_enabled)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_edit_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_edit_cancel)
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_edit_delete)
        val imageUserPhoto = dialogView.findViewById<ImageView>(R.id.image_edit_user_photo)
        val btnSelectPhoto = dialogView.findViewById<Button>(R.id.btn_select_photo)

        dialogTitle?.text = "Editar Usuario: ${userToEdit.name}"
        editName.setText(userToEdit.name)
        editMail.setText(userToEdit.mail)
        editPassword.visibility = View.GONE
        switchEnabled.isChecked = userToEdit.enabled ?: true

        val roles = listOf("USUARIO", "ADMINISTRADOR")
        val adapterRoles = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRol.adapter = adapterRoles
        val currentRoleIndex = roles.indexOf(userToEdit.rol)
        if (currentRoleIndex != -1) {
            spinnerRol.setSelection(currentRoleIndex)
        }

        btnDelete.visibility = View.VISIBLE

        if (!userToEdit.image.isNullOrBlank()) {
            imageUserPhoto.load(userToEdit.image) {
                crossfade(true)
                placeholder(R.drawable.img_product)
                error(R.drawable.img_product)
            }
        } else {
            imageUserPhoto.setImageResource(R.drawable.img_product)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnSelectPhoto.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK)
            pickImageIntent.type = "image/*"
            selectImageLauncher.launch(pickImageIntent)
        }

        btnSave.setOnClickListener {
            val newName = editName.text.toString()
            val newMail = editMail.text.toString()
            val newRol = spinnerRol.selectedItem.toString()
            val newEnabled = switchEnabled.isChecked

            if (newName.isBlank()) {
                editName.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }

            val imageToSend = selectedImageUri?.toString() ?: userToEdit.image

            val updatedUser = User(
                userId = userToEdit.userId,
                name = newName,
                mail = newMail,
                image = imageToSend,
                pass = userToEdit.pass,
                rol = newRol,
                enabled = newEnabled
            )
            updateUser(updatedUser, dialog)
        }

        btnDelete.setOnClickListener {
            dialog.dismiss()
            selectedImageUri = null
            showDeleteConfirmationDialog(userToEdit)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            selectedImageUri = null
        }
        dialog.show()
    }

    private fun showDeleteConfirmationDialog(userToDelete: User) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar a ${userToDelete.name}?")
            .setPositiveButton("Eliminar") { dialog, which ->
                val userIdToDelete = userToDelete.userId
                deleteUserFromAPI(userIdToDelete.toLong())
            }
            .setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }


    private fun deleteUserFromAPI(userId: Long) {
        Toast.makeText(this, "Eliminando usuario...", Toast.LENGTH_SHORT).show()
        // Usa la propiedad 'tokenStorage' heredada de BaseActivity
        val token = tokenStorage.getAuthToken()
        if (token == null) {
            Log.w("AdminUserActivity", "deleteUserFromAPI: No hay token guardado. Redirigiendo a login.")
            Toast.makeText(this, "Sesión expirada o no iniciada.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish()
            return
        }
        val authTokenHeader = "Bearer $token"
        Log.d("AdminUserActivity", "deleteUserFromAPI: Usando token: ${authTokenHeader}")

        lifecycleScope.launch {
            try {
                val response: Response<Void> = RetrofitClient.apiService.deleteUser(userId, authTokenHeader)
                if (response.isSuccessful) {
                    Log.d("AdminUserActivity", "Usuario con ID $userId eliminado correctamente en la API.")
                    Toast.makeText(this@AdminUserActivity, "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show()
                    getAllUsersFromAPI()
                } else if (response.code() == 401 || response.code() == 403) {
                    Log.w("AdminUserActivity", "deleteUserFromAPI: Acceso denegado. Código: ${response.code()}")
                    Toast.makeText(this@AdminUserActivity, "Acceso denegado. Por favor, inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
                    tokenStorage.deleteAuthToken()
                    startActivity(Intent(this@AdminUserActivity, LoginUserActivity::class.java))
                    finish()
                }
                else {
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e("AdminUserActivity", "Error de API al eliminar usuario con ID $userId: Código $errorCode, Contenido: $errorBody")
                    Toast.makeText(this@AdminUserActivity, "Error al eliminar usuario", Toast.LENGTH_SHORT).show()
                    if (!errorBody.isNullOrBlank()) {
                        Log.e("AdminUserActivity", "Contenido del error de eliminación: $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminUserActivity", "Excepción al eliminar usuario con ID $userId", e)
                Toast.makeText(this@AdminUserActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun createUser(newUser: User, dialog: AlertDialog) {
        Toast.makeText(this, "Creando usuario...", Toast.LENGTH_SHORT).show()
        // tokenStorage heredada de BaseActivity
        val token = tokenStorage.getAuthToken()
        if (token == null) {
            Log.w("AdminUserActivity", "createUser: No hay token guardado. Redirigiendo a login.")
            Toast.makeText(this, "Sesión expirada o no iniciada.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish()
            dialog.dismiss()
            return
        }

        val authTokenHeader = "Bearer $token"
        Log.d("AdminUserActivity", "createUser: Usando token: ${authTokenHeader}")

        lifecycleScope.launch {
            try {
                val response: Response<User> = RetrofitClient.apiService.createUser(newUser, authTokenHeader)

                if (response.isSuccessful && response.body() != null) {
                    val createdUser = response.body()
                    Log.d("AdminUserActivity", "Usuario creado correctamente en la API: ${createdUser?.name}")
                    Toast.makeText(this@AdminUserActivity, "Usuario creado correctamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    selectedImageUri = null
                    getAllUsersFromAPI()
                } else if (response.code() == 401 || response.code() == 403) {
                    Log.w("AdminUserActivity", "createUser: Acceso denegado. Código: ${response.code()}")
                    Toast.makeText(this@AdminUserActivity, "Acceso denegado. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
                    tokenStorage.deleteAuthToken()
                    startActivity(Intent(this@AdminUserActivity, LoginUserActivity::class.java))
                    finish()
                    dialog.dismiss()
                }
                else {
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e("AdminUserActivity", "Error de API al crear usuario: Código $errorCode, Contenido: $errorBody")
                    Toast.makeText(this@AdminUserActivity, "Error al crear usuario", Toast.LENGTH_SHORT).show()
                    if (!errorBody.isNullOrBlank()) {
                        Log.e("AdminUserActivity", "Cuerpo del error de creación: $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("AdminUserActivity", "Excepción al crear usuario", e)
                Toast.makeText(this@AdminUserActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Actualizar usuario
    private fun updateUser(updatedUser: User, dialog: AlertDialog) {
        Toast.makeText(this, "Actualizando usuario...", Toast.LENGTH_SHORT).show()
        // Usa la propiedad 'tokenStorage' heredada de BaseActivity
        val token = tokenStorage.getAuthToken()
        if (token == null) {
            Log.w("AdminUserActivity", "updateUser: No hay token guardado. Redirigiendo a login.")
            Toast.makeText(this, "Sesión expirada o no iniciada.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish()
            dialog.dismiss()
            return
        }
        val authTokenHeader = "Bearer $token"
        Log.d("AdminUserActivity", "updateUser: Usando token: ${authTokenHeader}") // Log para depurar

        // Llamar a la API (con token) para actualizar el usuario
        lifecycleScope.launch {
            try {
                val userIdToUpdate = updatedUser.userId
                val response: Response<User> = RetrofitClient.apiService.updateUser(userIdToUpdate.toLong(), updatedUser, authTokenHeader)
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminUserActivity, "Usuario actualizado correctamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    selectedImageUri = null
                    getAllUsersFromAPI()
                } else if (response.code() == 401 || response.code() == 403) {
                    Log.w("AdminUserActivity", "updateUser: Acceso denegado. Código: ${response.code()}")
                    Toast.makeText(this@AdminUserActivity, "Acceso denegado. Por favor, inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
                    tokenStorage.deleteAuthToken()
                    startActivity(Intent(this@AdminUserActivity, LoginUserActivity::class.java))
                    finish()
                    dialog.dismiss()
                }
                else {
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e("AdminUserActivity", "Error de API al actualizar usuario: Código $errorCode, Contenido: $errorBody")
                    Toast.makeText(this@AdminUserActivity, "Error al actualizar usuario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AdminUserActivity", "Excepción al actualizar usuario", e)
                Toast.makeText(this@AdminUserActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }
}