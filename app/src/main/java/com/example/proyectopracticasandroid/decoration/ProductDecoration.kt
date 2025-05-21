package com.example.proyectopracticasandroid.decoration

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ProductDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        // Se obtiene el índice del elemento en el RecyclerView
        val position = parent.getChildAdapterPosition(view)
        val spanCount = (parent.layoutManager as GridLayoutManager).spanCount

        // Espaciado superior solo si no es el primer elemento de la fila
        if (position >= spanCount) {
            outRect.top = verticalSpaceHeight // Espaciado superior
        }

        // Siempre aplicar espaciado inferior
        outRect.bottom = verticalSpaceHeight
    }
}
