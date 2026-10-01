package com.example.app_b.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * Repositorio encargado de realizar consultas de lectura sobre la base de datos
 * expuesta por la aplicación "Tienda Virtual" mediante su ContentProvider.
 */
object ProductoRepository {

    private const val TAG = "ProductoRepository"

    /** Autoridad del ContentProvider expuesto por Tienda Virtual */
    private const val AUTHORITY = "com.example.tienda_virtual.provider"

    /** URI de contenido para consultar la lista de productos */
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/productos")

    /**
     * Consulta y recupera todos los productos ingresados por los vendedores en Tienda Virtual
     * haciendo uso de [android.content.ContentResolver].
     */
    fun obtenerProductosDesdeContentResolver(context: Context): List<ProductoModel> {
        val listaProductos = mutableListOf<ProductoModel>()

        try {
            val resolver = context.contentResolver
            val cursor: Cursor? = resolver.query(
                CONTENT_URI,
                null, // Trae todas las columnas
                null, // Sin filtro WHERE
                null, // Sin argumentos
                null  // Orden predeterminado (ID DESC)
            )

            cursor?.use { c ->
                val idIndex = c.getColumnIndex("id")
                val nombreIndex = c.getColumnIndex("nombre")
                val fotoIndex = c.getColumnIndex("foto")
                val categoriaIndex = c.getColumnIndex("categoria")
                val precioIndex = c.getColumnIndex("precio")
                val descripcionIndex = c.getColumnIndex("descripcion")
                val vendedorIdIndex = c.getColumnIndex("vendedorId")
                val vendedorNombreIndex = c.getColumnIndex("vendedorNombre")
                val vendedorCorreoIndex = c.getColumnIndex("vendedorCorreo")
                val stockIndex = c.getColumnIndex("stock")

                while (c.moveToNext()) {
                    val id = if (idIndex != -1) c.getInt(idIndex) else 0
                    val nombre = if (nombreIndex != -1) c.getString(nombreIndex) ?: "" else ""
                    val foto = if (fotoIndex != -1) c.getString(fotoIndex) ?: "" else ""
                    val categoria = if (categoriaIndex != -1) c.getString(categoriaIndex) ?: "" else ""
                    val precio = if (precioIndex != -1) c.getDouble(precioIndex) else 0.0
                    val descripcion = if (descripcionIndex != -1) c.getString(descripcionIndex) ?: "" else ""
                    val vendedorId = if (vendedorIdIndex != -1) c.getInt(vendedorIdIndex) else 0
                    val vendedorNombre = if (vendedorNombreIndex != -1) c.getString(vendedorNombreIndex) ?: "" else ""
                    val vendedorCorreo = if (vendedorCorreoIndex != -1) c.getString(vendedorCorreoIndex) ?: "" else ""
                    val stock = if (stockIndex != -1) c.getInt(stockIndex) else 0

                    Log.d(TAG, "Producto leído: id=$id, nombre=$nombre, foto length=${foto.length}, foto preview='${foto.take(60)}'")

                    listaProductos.add(
                        ProductoModel(
                            id = id,
                            nombre = nombre,
                            foto = foto,
                            categoria = categoria,
                            precio = precio,
                            descripcion = descripcion,
                            vendedorId = vendedorId,
                            vendedorNombre = vendedorNombre,
                            vendedorCorreo = vendedorCorreo,
                            stock = stock
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar ContentResolver en $CONTENT_URI", e)
        }

        return listaProductos
    }
}
