package com.example.proyectopracticasandroid

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectopracticasandroid.adapter.InvoicesAdapter
import com.example.proyectopracticasandroid.api.RetrofitClient
import com.example.proyectopracticasandroid.model.Invoice
import com.google.ar.imp.view.View
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AdminInvoicesActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InvoicesAdapter
    private lateinit var createInvoiceButton: Button
    private var invoicesList = mutableListOf<Invoice>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invoice)

        recyclerView = findViewById(R.id.recycler_products)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = InvoicesAdapter(emptyList())
        recyclerView.adapter = adapter

        createInvoiceButton = findViewById(R.id.createInvoice)
        createInvoiceButton.setOnClickListener {
            showCreateInvoiceDialog()
        }

        loadInvoices()
    }

    private fun loadInvoices() {
        Toast.makeText(this, "Cargando facturas...", Toast.LENGTH_SHORT).show()
        Log.d("AdminInvoicesActivity", "Llamando a la API para obtener facturas")
        RetrofitClient.apiService.getAllInvoices().enqueue(object : Callback<List<Invoice>> {
            override fun onResponse(call: Call<List<Invoice>>, response: Response<List<Invoice>>) {
                if (response.isSuccessful) {
                    val invoices = response.body() ?: emptyList()
                    Log.d("AdminInvoicesActivity", "Invoices recibidas: ${invoices.size}")
                    invoicesList = invoices.toMutableList()
                    adapter.updateList(invoicesList)
                } else {
                    Log.e("AdminInvoicesActivity", "Error en la respuesta: ${response.code()}")
                    Toast.makeText(this@AdminInvoicesActivity, "Error al cargar facturas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<Invoice>>, t: Throwable) {
                Log.e("AdminInvoicesActivity", "Fallo en la llamada: ${t.message}")
                Toast.makeText(this@AdminInvoicesActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCreateInvoiceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Crear Factura")

        val view = layoutInflater.inflate(R.layout.dialog_create_albaran, null)
        builder.setView(view)

        val editTextTotal = view.findViewById<EditText>(R.id.editTextTotal)
        val editTextCif = view.findViewById<EditText>(R.id.editTextCif)
        val checkboxPaid = view.findViewById<CheckBox>(R.id.checkboxPaid)

        builder.setPositiveButton("Crear") { _, _ ->
            val total = editTextTotal.text.toString()
            val cif = editTextCif.text.toString()
            val paid = checkboxPaid.isChecked

            if (total.isNotBlank() && cif.isNotBlank()) {
                val currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val newInvoice = Invoice(
                    invoiceId = null,
                    date = currentDateTime,
                    total = total,
                    paid = paid,
                    cif = cif,
                    userId = 1L
                )
                createInvoiceApiCall(newInvoice)
            } else {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun createInvoiceApiCall(invoice: Invoice) {
        val gson = Gson()
        Log.d("JSON_ENVIADO", gson.toJson(invoice)) // 👈 Aquí ves qué se está enviando realmente
        RetrofitClient.apiService.createInvoice(invoice).enqueue(object : Callback<Invoice> {
            override fun onResponse(call: Call<Invoice>, response: Response<Invoice>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@AdminInvoicesActivity, "Factura creada correctamente", Toast.LENGTH_SHORT).show()
                    response.body()?.let {
                        invoicesList.add(it)
                        adapter.updateList(invoicesList)
                    }
                } else {
                    Toast.makeText(this@AdminInvoicesActivity, "Error al crear la factura", Toast.LENGTH_SHORT).show()
                    Log.e("AdminInvoicesActivity", "Error en crear factura: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Invoice>, t: Throwable) {
                Toast.makeText(this@AdminInvoicesActivity, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("AdminInvoicesActivity", "Fallo en crear factura: ${t.message}")
            }
        })
    }
}
