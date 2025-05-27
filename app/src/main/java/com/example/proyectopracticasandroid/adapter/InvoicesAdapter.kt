package com.example.proyectopracticasandroid.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectopracticasandroid.R
import com.example.proyectopracticasandroid.model.Invoice

class InvoicesAdapter(private var invoices: List<Invoice>) :
    RecyclerView.Adapter<InvoicesAdapter.InvoiceViewHolder>() {

    inner class InvoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txt_name_invoice)
        val txtCif: TextView = itemView.findViewById(R.id.txt_invoice_cif)
        val txtDate: TextView = itemView.findViewById(R.id.txt_invoice_date)
        val txtTotal: TextView = itemView.findViewById(R.id.txt_invoice_price)
        val checkboxPaid: CheckBox = itemView.findViewById(R.id.checkbox_invoice_paid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_invoice, parent, false)
        return InvoiceViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
        val invoice = invoices[position]
        holder.txtName.text = "ID: ${invoice.invoiceId}"
        holder.txtCif.text = "CIF: ${invoice.cif}"
        holder.txtDate.text = "Fecha: ${invoice.date}"
        holder.txtTotal.text = "Total: ${invoice.total} €"
        holder.checkboxPaid.isChecked = invoice.paid

    }

    override fun getItemCount() = invoices.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<Invoice>) {
        invoices = newList
        notifyDataSetChanged()
    }
}
