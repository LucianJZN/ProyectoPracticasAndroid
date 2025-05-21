package com.example.proyectopracticasandroid.adapter

import android.annotation.SuppressLint
import android.content.Context // Import Context for Toast
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.proyectopracticasandroid.R
import com.example.proyectopracticasandroid.api.ApiService
import com.example.proyectopracticasandroid.model.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ProductAdapter(
    private var lista: List<Product>,
    private val apiService: ApiService,
    private val coroutineScope: CoroutineScope,
    private val context: Context
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.txt_name_product)
        val image = view.findViewById<ImageView>(R.id.img_product)
        val amount = view.findViewById<TextView>(R.id.txt_product_quantity)
        val btnSumar = view.findViewById<Button>(R.id.btn_add_product)
        val btnRestar = view.findViewById<Button>(R.id.btn_subtract_product)
        val checkbox = view.findViewById<CheckBox>(R.id.checkbox_product)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = lista[position]

        holder.name.text = product.name
        holder.amount.text = "Cantidad: ${product.amount}"

        // Cambiar el color del nombre según disponibilidad del producto
        if (product.amount <= product.minimumAmount) {
            holder.name.setBackgroundResource(R.drawable.border_name_product_red)
            holder.checkbox.isChecked = true
            // Opcional: Si también quieres marcar el CheckBox:
            // holder.checkbox.isChecked = true
        } else {
            // La cantidad es suficiente, poner nombre en el color por defecto (ej. negro)
            holder.name.setBackgroundResource(R.drawable.border_name_product)
            holder.checkbox.isChecked = false
            // Opcional: Desmarcar el CheckBox:
            // holder.checkbox.isChecked = false
        }

        Log.d("ProductAdapter", "Cargando imagen para ${product.name}: ${product.image}")

        if (!product.image.isNullOrEmpty()) {
            Log.d("DEBUG", "Cargando imagen: ${product.image}")
            val uri = Uri.parse(product.image)
            holder.image.load(uri) {
                placeholder(R.drawable.img_product)
                error(R.drawable.img_product)
            }
        } else {
            holder.image.setImageResource(R.drawable.img_product)
        }

        holder.btnSumar.setOnClickListener {
            // Desactivamos los botones (+-) para evita multiples click mientras la API trabaja
            holder.btnSumar.isEnabled = false
            holder.btnRestar.isEnabled = false

            product.amount++
            notifyItemChanged(position) //Actualiza solo ese item

            // Corutina para llamar la API
            coroutineScope.launch {
                try {
                    val updatedProduct = product.copy(amount = product.amount)

                    // Llamamos a la API para actualizar el producto
                    val response = apiService.updateProduct(product.productId, updatedProduct)

                    withContext(Dispatchers.Main) { // Switch back to the main thread for UI updates/Toasts
                        if (response.isSuccessful) {
                            Log.d("ProductAdapter", "Se ha decrementado la cantidad de ${product.name} mediante la API.")
                        } else {
                            Log.e("ProductAdapter", "Error al cambiar la cantidad del producto: ${product.name}. Error: ${response.code()} - ${response.message()}")
                            product.amount-- // Revertir el cambio local
                            notifyItemChanged(position) // Revertir la IU
                            Toast.makeText(context, "Error al actualizar cantidad (Código: ${response.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: IOException) {
                    Log.e("ProductAdapter", "Error de red al actualizar cantidad ${product.name}: ${e.message}")
                    withContext(Dispatchers.Main) {
                        product.amount-- // Revertir el cambio local
                        notifyItemChanged(position) // Revertir la IU
                        Toast.makeText(context, "Error de red al actualizar cantidad", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: HttpException) {
                    // Errores HTTP (400, 500, etc.)
                    Log.e("ProductAdapter", "Error HTTP al actualizar el producto ${product.name}: ${e.message()}")
                    withContext(Dispatchers.Main) {
                        product.amount-- // Revertir el cambio local
                        notifyItemChanged(position) // Revertir la IU
                        Toast.makeText(context, "Error del servidor al actualizar cantidad", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("ProductAdapter", "Error desconocido al actualizar cantidad ${product.name}: ${e.message}")
                    withContext(Dispatchers.Main) {
                        product.amount-- // Revertir el cambio local
                        notifyItemChanged(position) // Revertir la IU
                        Toast.makeText(context, "Error desconocido al actualizar cantidad", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    // Habilitamos los botones para poder sumar o restar cantidad de producto
                    withContext(Dispatchers.Main) {
                        holder.btnSumar.isEnabled = true
                        holder.btnRestar.isEnabled = true
                    }
                }
            }
        }

        holder.btnRestar.setOnClickListener {
            if (product.amount > 0) {
                // Desactivamos los botones (+-) para evita multiples click mientras la API trabaja
                holder.btnSumar.isEnabled = false
                holder.btnRestar.isEnabled = false

                product.amount--
                notifyItemChanged(position)

                coroutineScope.launch {
                    try {
                        val updatedProduct = product.copy(amount = product.amount)

                        val response = apiService.updateProduct(product.productId, updatedProduct) // Assuming product.id is the productId

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Log.d("ProductAdapter", "Se ha decrementado la cantidad de ${product.name} mediante la API.")
                            } else {
                                Log.e("ProductAdapter", "Error al cambiar la cantidad del producto: ${product.name}. Error: ${response.code()} - ${response.message()}")
                                product.amount++
                                notifyItemChanged(position)
                                Toast.makeText(context, "Error al actualizar cantidad (Código: ${response.code()})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: IOException) {
                        // Network error
                        Log.e("ProductAdapter", "Error de red al actualizar cantidad ${product.name}: ${e.message}")
                        withContext(Dispatchers.Main) {
                            product.amount++
                            notifyItemChanged(position)
                            Toast.makeText(context, "Error de red al actualizar cantidad", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: HttpException) {
                        // HTTP errors
                        Log.e("ProductAdapter", "Error HTTP al actualizar el producto ${product.name}: ${e.message()}")
                        withContext(Dispatchers.Main) {
                            product.amount++
                            notifyItemChanged(position)
                            Toast.makeText(context, "Error del servidor al actualizar cantidad", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        // Any other unexpected error
                        Log.e("ProductAdapter", "Error desconocido al actualizar cantidad ${product.name}: ${e.message}")
                        withContext(Dispatchers.Main) {
                            product.amount++
                            notifyItemChanged(position)
                            Toast.makeText(context, "Error desconocido al actualizar cantidad", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        // Re-enable buttons
                        withContext(Dispatchers.Main) {
                            holder.btnSumar.isEnabled = true
                            holder.btnRestar.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return lista.size
    }

    // Método para actualizar la lista del adaptador
    @SuppressLint("NotifyDataSetChanged") // Usamos NotifyDataSetChanged por simplicidad para el filtro
    fun updateList(newList: List<Product>) {
        this.lista = newList // Reemplaza la lista actual con la nueva lista filtrada
        notifyDataSetChanged() // Notifica al RecyclerView que los datos han cambiado completamente
    }
}