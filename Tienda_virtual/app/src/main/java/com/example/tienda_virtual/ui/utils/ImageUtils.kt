package com.example.tienda_virtual.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utilidades para la conversión, procesamiento y formateo Base64 de imágenes en Android.
 */
object ImageUtils {

    /**
     * Redimensiona un [Bitmap] a un tamaño máximo para optimizar el peso de la cadena Base64
     * y evitar exceder los límites del CursorWindow en SQLite.
     */
    fun redimensionarBitmap(bitmap: Bitmap, maxAnchoAlto: Int = 600): Bitmap {
        val anchoOriginal = bitmap.width
        val altoOriginal = bitmap.height
        if (anchoOriginal <= maxAnchoAlto && altoOriginal <= maxAnchoAlto) {
            return bitmap
        }

        val relacionAspecto = anchoOriginal.toFloat() / altoOriginal.toFloat()
        val nuevoAncho: Int
        val nuevoAlto: Int

        if (anchoOriginal > altoOriginal) {
            nuevoAncho = maxAnchoAlto
            nuevoAlto = (maxAnchoAlto / relacionAspecto).toInt()
        } else {
            nuevoAlto = maxAnchoAlto
            nuevoAncho = (maxAnchoAlto * relacionAspecto).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, nuevoAncho, nuevoAlto, true)
    }

    /**
     * Convierte un [Bitmap] en un String Base64 optimizado con prefijo "data:image/jpeg;base64,".
     */
    fun bitmapABase64Optimizado(bitmap: Bitmap, calidadJpeg: Int = 75): String {
        val bitmapRedimensionado = redimensionarBitmap(bitmap, 600)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmapRedimensionado.compress(Bitmap.CompressFormat.JPEG, calidadJpeg, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Convierte un objeto Bitmap en una cadena Base64.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        return bitmapABase64Optimizado(bitmap)
    }

    /**
     * Método Solución Definitiva:
     * Recibe cualquier origen de imagen ([Bitmap], [Uri], ruta de archivo `/data/data/...`, o `content://...`)
     * y la convierte de forma limpia, robusta y optimizada a una cadena de texto en formato Base64:
     * "data:image/jpeg;base64,..."
     *
     * De esta manera, la imagen viaja directamente dentro del Cursor del ContentProvider
     * y App_B la puede decodificar instantáneamente sin lidiar con permisos de archivos ni FileProvider.
     */
    fun procesarYGuardarFoto(context: Context, fotoSource: Any?): String {
        if (fotoSource == null) return ""

        when (fotoSource) {
            is Bitmap -> {
                return bitmapABase64Optimizado(fotoSource)
            }
            is Uri -> {
                return try {
                    val inputStream = context.contentResolver.openInputStream(fotoSource)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        bitmapABase64Optimizado(bitmap)
                    } else ""
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }
            is String -> {
                val str = fotoSource.trim()
                if (str.isBlank()) return ""

                // Si ya es un String Base64 válido con formato "data:image...", retornarlo
                if (str.startsWith("data:image") && str.contains("base64,")) {
                    return str
                }

                // Si es un String Base64 puro sin prefijo, agregar el prefijo "data:image/jpeg;base64,"
                if (str.length > 100 && !str.contains("://") && !str.startsWith("/")) {
                    return if (str.startsWith("data:image")) str else "data:image/jpeg;base64,$str"
                }

                // Si es una ruta de archivo local (/data/data/... o /storage/...)
                if (str.startsWith("/") || str.startsWith("file://")) {
                    return try {
                        val path = if (str.startsWith("file://")) Uri.parse(str).path else str
                        if (path != null && File(path).exists()) {
                            val bitmap = BitmapFactory.decodeFile(path)
                            if (bitmap != null) {
                                bitmapABase64Optimizado(bitmap)
                            } else ""
                        } else ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                }

                // Si es una URI de contenido (content://...)
                if (str.startsWith("content://")) {
                    return try {
                        val uri = Uri.parse(str)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        if (bitmap != null) {
                            bitmapABase64Optimizado(bitmap)
                        } else ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ""
                    }
                }

                return str
            }
        }
        return fotoSource.toString()
    }

    /**
     * Mantenimiento de compatibilidad para guardado local en disco si fuera requerido.
     */
    fun guardarBitmapEnAlmacenamientoInterno(context: Context, bitmap: Bitmap): String? {
        return try {
            val carpeta = File(context.filesDir, "imagenes").apply {
                if (!exists()) mkdirs()
            }
            val archivo = File(carpeta, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(archivo).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            archivo.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copia la imagen proveniente de una [Uri] al almacenamiento interno privado de la aplicación.
     */
    fun guardarUriEnAlmacenamientoInterno(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val carpeta = File(context.filesDir, "imagenes").apply {
                if (!exists()) mkdirs()
            }
            val archivo = File(carpeta, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
            FileOutputStream(archivo).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            archivo.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
