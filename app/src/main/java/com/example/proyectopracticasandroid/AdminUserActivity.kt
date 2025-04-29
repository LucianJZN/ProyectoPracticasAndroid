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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.proyectopracticasandroid.adapter.UserAdapter
import com.example.proyectopracticasandroid.api.RetrofitClient
import com.example.proyectopracticasandroid.api.TokenStorage
import com.example.proyectopracticasandroid.decoration.UserDecoration
import com.example.proyectopracticasandroid.model.User
import kotlinx.coroutines.launch
import retrofit2.Response

class AdminUserActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterUser: UserAdapter
    private var listUsers: MutableList<User> = mutableListOf()
    private lateinit var tokenStorage: TokenStorage
    private lateinit var selectImageLauncher: ActivityResultLauncher<Intent>    //para seleccionar imágenes de la galería
    private var selectedImageUri: Uri? = null   // url de la imagen seleccionada en el diálogo
    private lateinit var btnAddUser: Button     // botón para crear usuario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_user)
        tokenStorage = TokenStorage(this)

        // Seleccionar imagen
        selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {   // La imagen fue seleccionada correctamente
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    selectedImageUri = uri
                    Log.d("AdminUserActivity", "Imagen seleccionada URI: $uri")
                }
            } else {    //Selección de imagen fallada
                selectedImageUri = null
                Log.d("AdminUserActivity", "Selección de imagen fallada.")
            }
        }
        // Configurar el nombre de la pantalla
        findViewById<TextView>(R.id.nav_title).text = getString(R.string.title_activity_admin_user)

        recyclerView = findViewById(R.id.recycler_users)

        //Ajustar columnas
        val spanCount = adjustColumnsToScreenWidth()
        recyclerView.layoutManager = GridLayoutManager(this, spanCount)
        val verticalSpaceHeight = resources.getDimensionPixelSize(R.dimen.vertical_space) // Asegúrate de que este recurso existe
        recyclerView.addItemDecoration(UserDecoration.UserDecoration(verticalSpaceHeight))

        //Boton para agregar usuarios
        btnAddUser = findViewById(R.id.btn_add_user)
        btnAddUser.setOnClickListener {showAddUserDialog() }

        getAllUsersFromAPI()
    }

    private fun adjustColumnsToScreenWidth(): Int {
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels / displayMetrics.density
        val itemWidth = 300 // dp aproximado de cada ítem
        return (screenWidth / itemWidth).toInt().coerceAtLeast(1)
    }

    // Función para obtener todos los usuarios desde la API (PÚBLICA - NO NECESITA TOKEN)
    private fun getAllUsersFromAPI() {
        Toast.makeText(this, getString(R.string.msg_calling_api), Toast.LENGTH_SHORT).show()

        //Mostrar usuarios habilitados
        lifecycleScope.launch {
            try {
                val response: Response<List<User>> = RetrofitClient.apiService.getAllUsers()

                if (response.isSuccessful) {
                    val users = response.body()
                    if (users != null) {
                        //Ahora ordenamos: primero los habilitados y después alfabeticamente
                        val sortedUsers = users.sortedWith(compareBy<User> {
                            if (it.enabled == true) 0 else 1    //los habilitados aparecen primero
                        }.thenBy {
                            it.name ?: ""   //hace una ordenación por nombre automaticamente
                        })

                        listUsers = sortedUsers.toMutableList()
                        adapterUser = UserAdapter(listUsers) { clickedUser ->
                            showEditUserDialog(clickedUser)     //Dialogo para editar/borrar
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

                    //Si devuelve 401 puede ser porque el token expiró, tiene que llevar al login
                }
            } catch (e: Exception) {
                Log.e("AdminUserActivity", "Error al obtener usuarios.", e)
                Toast.makeText(this@AdminUserActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Diálogo de agregar Usuario
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

        // Configurar el diálogo para agregar usuario
        dialogTitle?.text = "Agregar Nuevo Usuario"
        editName.setText("")
        editMail.setText("")
        editPassword.setText("")
        editPassword.visibility = View.VISIBLE
        switchEnabled.isChecked = true // Por defecto el usuario esta habilitado

        // Configurar Spinner para el Rol
        val roles = listOf("USUARIO", "ADMINISTRADOR")
        val adapterRoles = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRol.adapter = adapterRoles
        spinnerRol.setSelection(roles.indexOf("USUARIO")) // Seleccionar rol USUARIO por defecto

        // Ocultar el botón Eliminar en el modo agregar
        btnDelete.visibility = View.GONE

        // Mostrar la imagen por defecto
        imageUserPhoto.setImageResource(R.drawable.img_product)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // El diálogo no se cierre al tocar fuera
            .create()

        btnSelectPhoto.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK)
            pickImageIntent.type = "image/*"
            selectImageLauncher.launch(pickImageIntent)
        }

        // Botón para guardar el usuario creado
        btnSave.setOnClickListener {
            val newName = editName.text.toString()
            val newMail = editMail.text.toString()
            val newPassword = editPassword.text.toString()
            val newRol = spinnerRol.selectedItem.toString()
            val newEnabled = switchEnabled.isChecked

            // Validaciones básicas
            if (newName.isBlank()) {
                editName.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }
            if (newPassword.isBlank()) {
                editPassword.error = "La contraseña no puede estar vacía"
                return@setOnClickListener
            }

            // Enviamos URI de la imagen seleccionada como String.
            val imageToSend = selectedImageUri?.toString()

            val newUser = User( //Creamos el usuario que mandaremos al servidor
                userId = 0,
                name = newName,
                mail = newMail,
                image = imageToSend,
                pass = newPassword,
                rol = newRol,
                enabled = newEnabled
            )
            createUser(newUser, dialog) // Llamada a la API para crear usuario
        }
        btnCancel.setOnClickListener {   // Cerrar el dialogo
            dialog.dismiss()
            selectedImageUri = null // Limpiar la URI temporal
        }
        dialog.show()
    }   //showAddUserDialog()

    // Diálogo de editar Usuario
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

        // Configurar el diálogo para editar
        dialogTitle?.text = "Editar Usuario: ${userToEdit.name}" // Título Editar "Nombre Usuario"
        editName.setText(userToEdit.name) // Cargar datos actuales del usuario
        editMail.setText(userToEdit.mail)
        switchEnabled.isChecked = userToEdit.enabled ?: true // Usuario habilitado por defecto si es null, por se acaso

        // Configurar Spinner para el Rol
        val roles = listOf("USUARIO", "ADMINISTRADOR")
        val adapterRoles = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRol.adapter = adapterRoles
        val currentRoleIndex = roles.indexOf(userToEdit.rol)
        if (currentRoleIndex != -1) {
            spinnerRol.setSelection(currentRoleIndex)
        }

        btnDelete.visibility = View.VISIBLE // Botón Eliminar esté visible en modo editar

        // Cargar la imagen actual del usuario
        if (!userToEdit.image.isNullOrBlank()) {
            imageUserPhoto.load(userToEdit.image) {
                crossfade(true)
                placeholder(R.drawable.img_product) // Imagen mientras carga
                error(R.drawable.img_product) // Imagen si falla la carga
            }
        } else {
            imageUserPhoto.setImageResource(R.drawable.img_product) // Si no hay imagen, mostrar la imagen por defecto
        }

        val dialog = AlertDialog.Builder(this)
            //.setTitle("Editar Usuario: ${userToEdit.name}")
            .setView(dialogView)
            .setCancelable(false) // No se cierra al tocar fuera
            .create()

        btnSelectPhoto.setOnClickListener {
            val pickImageIntent = Intent(Intent.ACTION_PICK)
            pickImageIntent.type = "image/*"
            selectImageLauncher.launch(pickImageIntent)
        }

        // Botón para guardar el usuario actualizado
        btnSave.setOnClickListener {
            // Obtener los nuevos datos del diálogo
            val newName = editName.text.toString()
            val newMail = editMail.text.toString()
            val newRol = spinnerRol.selectedItem.toString()
            val newEnabled = switchEnabled.isChecked

            if (newName.isBlank()) {
                editName.error = "El nombre no puede estar vacío"
                return@setOnClickListener // Salir del listener si falla validación
            }

            val imageToSend = selectedImageUri?.toString() ?: userToEdit.image //ruta de la imagen seleccionada o imagen original
            //Creamos un user con los datos actualizados
            val updatedUser = User(
                userId = userToEdit.userId, // Se mantiene el ID original
                name = newName,
                mail = newMail,
                image = imageToSend,
                pass = userToEdit.pass,
                rol = newRol,
                enabled = newEnabled,
                //invoices = null, // Sin relaciones
                //sales = null // Sin relaciones
            )
            updateUser(updatedUser, dialog) // Llamar a la API para actualizar usuario
        }
        // Boton para eliminar usuario
        btnDelete.setOnClickListener {
            dialog.dismiss() // Cerramos el dialogon antes de la confirmación
            selectedImageUri = null // Limpiar la URI temporal si se elimina
            showDeleteConfirmationDialog(userToEdit) // Mostrar el diálogo de confirmación de eliminación
        }
        // Botón para cancelar/salir del dialogo
        btnCancel.setOnClickListener {
            dialog.dismiss()
            selectedImageUri = null
        }
        dialog.show()   // Mostrar el diálogo de edición
    }

    // Diálogo para confirmar la eliminación del usuario
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
        val token = tokenStorage.getAuthToken()
        if (token == null) {        // Verificar si hay token , si no redirigir a login
            Log.w("AdminUserActivity", "deleteUserFromAPI: No hay token guardado. Redirigiendo a login.")
            Toast.makeText(this, "Sesión expirada o no iniciada.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish()
            return
        }
        // Formatear el token con Bearer
        val authTokenHeader = "Bearer $token"
        Log.d("AdminUserActivity", "deleteUserFromAPI: Usando token: ${authTokenHeader}")

        lifecycleScope.launch {
            try {
                val response: Response<Void> = RetrofitClient.apiService.deleteUser(userId, authTokenHeader) // Pasar el token
                if (response.isSuccessful) {
                    Log.d("AdminUserActivity", "Usuario con ID $userId eliminado correctamente en la API.")
                    Toast.makeText(this@AdminUserActivity, "Usuario eliminado correctamente", Toast.LENGTH_SHORT).show()
                    getAllUsersFromAPI()    // Refrescar la lista de usuarios después de eliminar para que sea visible la eliminación del user
                } else if (response.code() == 401 || response.code() == 403) {  // Acceso denegado: token inválido, expirado o sin permisos
                    Log.w("AdminUserActivity", "deleteUserFromAPI: Acceso denegado. Código: ${response.code()}")
                    Toast.makeText(this@AdminUserActivity, "Acceso denegado. Por favor, inicie sesión de nuevo.", Toast.LENGTH_LONG).show()
                    tokenStorage.deleteAuthToken()
                    startActivity(Intent(this@AdminUserActivity, LoginUserActivity::class.java))
                    finish()
                }
                else {
                    // Otros errores: 404 Not Found si el usuario no existe, 500 Internal Server Error por si sucede alguna otra cosa más
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e("AdminUserActivity", "Error de API al eliminar usuario con ID $userId: Código $errorCode, Contenido: $errorBody")
                    Toast.makeText(this@AdminUserActivity, "Error al eliminar usuario", Toast.LENGTH_SHORT).show()

                    if (!errorBody.isNullOrBlank()) {
                        Log.e("AdminUserActivity", "Contenido del error de eliminación: $errorBody")
                    }
                }
            } catch (e: Exception) {
                // Error de red o excepción inesperada
                Log.e("AdminUserActivity", "Excepción al eliminar usuario con ID $userId", e)
                Toast.makeText(this@AdminUserActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dialogo para crear usuario
    private fun createUser(newUser: User, dialog: AlertDialog) {
        Toast.makeText(this, "Creando usuario...", Toast.LENGTH_SHORT).show()
        val token = tokenStorage.getAuthToken()
        if (token == null) {    // Verificar si hay token, si no redirigir a login
            Log.w("AdminUserActivity", "createUser: No hay token guardado. Redirigiendo a login.")
            Toast.makeText(this, "Sesión expirada o no iniciada.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginUserActivity::class.java))
            finish()
            dialog.dismiss()
            return
        }

        // Token con Bearer para que no necesite otro login
        val authTokenHeader = "Bearer $token"
        Log.d("AdminUserActivity", "createUser: Usando token: ${authTokenHeader}") // Log para depurar

        lifecycleScope.launch {     //Corutina
            try {
                val response: Response<User> = RetrofitClient.apiService.createUser(newUser, authTokenHeader) // Le pasamos a la API el token + objeto User

                if (response.isSuccessful && response.body() != null) {     //Respuesta
                    val createdUser = response.body()   // La respuesta incluye el usuario creado
                    Log.d("AdminUserActivity", "Usuario creado correctamente en la API: ${createdUser?.name}")
                    Toast.makeText(this@AdminUserActivity, "Usuario creado correctamente", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    selectedImageUri = null
                    getAllUsersFromAPI() // Refrescar la lista después de crear
                } else if (response.code() == 401 || response.code() == 403) {  // Acceso denegado
                    Log.w("AdminUserActivity", "createUser: Acceso denegado. Código: ${response.code()}")
                    Toast.makeText(this@AdminUserActivity, "Acceso denegado. Por favor, inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
                    tokenStorage.deleteAuthToken()
                    startActivity(Intent(this@AdminUserActivity, LoginUserActivity::class.java))
                    finish()
                    dialog.dismiss()
                }
                else {  //Otros errores
                    val errorBody = response.errorBody()?.string()
                    val errorCode = response.code()
                    Log.e("AdminUserActivity", "Error de API al crear usuario: Código $errorCode, Contenido: $errorBody")
                    Toast.makeText(this@AdminUserActivity, "Error al crear usuario", Toast.LENGTH_SHORT).show()
                    if (!errorBody.isNullOrBlank()) {
                        Log.e("AdminUserActivity", "Cuerpo del error de creación: $errorBody")
                    }
                }
            } catch (e: Exception) {    // Error de red o excepción inesperada
                Log.e("AdminUserActivity", "Excepción al crear usuario", e)
                Toast.makeText(this@AdminUserActivity, getString(R.string.error_api_connection), Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Actualizar usuario
    private fun updateUser(updatedUser: User, dialog: AlertDialog) {
        Toast.makeText(this, "Actualizando usuario...", Toast.LENGTH_SHORT).show()
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
