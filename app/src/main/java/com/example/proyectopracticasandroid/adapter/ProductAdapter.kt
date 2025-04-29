package com.example.proyectopracticasandroid.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectopracticasandroid.R
import com.example.proyectopracticasandroid.model.Product


class ProductAdapter(private val lista: List<Product>) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    
        class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name = view.findViewById<TextView>(R.id.txt_name_product)
            val image = view.findViewById<ImageView>(R.id.img_product)
            val amount = view.findViewById<TextView>(R.id.txt_product_quantity)
            val btnSumar = view.findViewById<Button>(R.id.btn_add_product)
            val btnRestar = view.findViewById<Button>(R.id.btn_subtract_product)
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

            // Cargar imagen desde nombre de recurso (imagenUrl es tipo String, como "avion_rc")
            val context = holder.itemView.context
            if (!product.image.isNullOrEmpty()) {
                val resId = context.resources.getIdentifier(product.image, "drawable", context.packageName)
                if (resId != 0) { // Verifica que la imagen existe
                    holder.image.setImageResource(resId)
                } else {
                    // Si no se encuentra el recurso, asignar una imagen por defecto
                    holder.image.setImageResource(R.drawable.img_product)
                }
            } else {
                // Si product.image es null o vacío, imagen por defecto
                holder.image.setImageResource(R.drawable.img_product)
            }

            holder.btnSumar.setOnClickListener {
                product.amount++
                notifyItemChanged(position)
            }

            holder.btnRestar.setOnClickListener {
                if (product.amount > 0) {
                    product.amount--
                    notifyItemChanged(position)
                }
            }
        }

    override fun getItemCount(): Int {
        return lista.size
    }
}

