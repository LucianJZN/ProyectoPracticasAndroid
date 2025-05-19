package com.example.proyectopracticasandroid.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PhotosUtils {

    /**
     * Convierte un Uri (imagen seleccionada de la galeria) a un archivo físico en la caché.
     * @param context Contexto para acceder a contentResolver y cacheDir.
     * @param uri Uri de la imagen seleccionada.
     * @return Archivo físico temporal o null si hay error.
     */
    fun uriToFile(context: Context, uri: Uri): File? {
        val contentResolver: ContentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            Log.e("PhotosUtils", "Error al convertir Uri a File", e)
            null
        }
    }
}
