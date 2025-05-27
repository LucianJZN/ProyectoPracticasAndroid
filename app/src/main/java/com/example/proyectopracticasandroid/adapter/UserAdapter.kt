package com.example.proyectopracticasandroid.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.proyectopracticasandroid.R
import com.example.proyectopracticasandroid.model.User

class UserAdapter(
    private val list: List<User>, private val onUserClick: (User) -> Unit) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() { //onUserClick sirve para detectar cuando se ha hecho click en un usuario
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.txt_name_user)
        val image = view.findViewById<ImageView>(R.id.img_user)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = list[position]
        holder.name.text = user.name

        //Al hacer click en cualquier parte del item ejecuta la función de dentro
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }

        // Cargar imagen
        val context = holder.itemView.context
        if (!user.image.isNullOrEmpty()) {
            Glide.with(context)
                .load(user.image)
                .placeholder(R.drawable.usuario)
                .error(R.drawable.usuario)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.usuario)
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }
}