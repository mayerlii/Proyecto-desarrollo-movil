package com.example.app_b.data

import android.database.Cursor

/**
 * Mapeador encargado de transformar filas de [Cursor] a instancias de [ProductoModel].
 */
object ProductoMapper {

    /**
     * Convierte la fila actual apuntada por el [Cursor] a un [ProductoModel].
     * Maneja índices no encontrados o valores nulos asignando valores por defecto seguros.
     */
    fun fromCursor(cursor: Cursor): ProductoModel {
        val idIndex = cursor.getColumnIndex("id")
        val nombreIndex = cursor.getColumnIndex("nombre")
        val fotoIndex = cursor.getColumnIndex("foto")
        val categoriaIndex = cursor.getColumnIndex("categoria")
        val precioIndex = cursor.getColumnIndex("precio")
        val descripcionIndex = cursor.getColumnIndex("descripcion")
        val vendedorIdIndex = cursor.getColumnIndex("vendedorId")
        val vendedorNombreIndex = cursor.getColumnIndex("vendedorNombre")
        val vendedorCorreoIndex = cursor.getColumnIndex("vendedorCorreo")
        val stockIndex = cursor.getColumnIndex("stock")

        val id = if (idIndex != -1 && !cursor.isNull(idIndex)) cursor.getInt(idIndex) else 0
        val nombre = if (nombreIndex != -1 && !cursor.isNull(nombreIndex)) cursor.getString(nombreIndex) ?: "" else ""
        val foto = if (fotoIndex != -1 && !cursor.isNull(fotoIndex)) cursor.getString(fotoIndex) ?: "" else ""
        val categoria = if (categoriaIndex != -1 && !cursor.isNull(categoriaIndex)) cursor.getString(categoriaIndex) ?: "" else ""
        val precio = if (precioIndex != -1 && !cursor.isNull(precioIndex)) cursor.getDouble(precioIndex) else 0.0
        val descripcion = if (descripcionIndex != -1 && !cursor.isNull(descripcionIndex)) cursor.getString(descripcionIndex) ?: "" else ""
        val vendedorId = if (vendedorIdIndex != -1 && !cursor.isNull(vendedorIdIndex)) cursor.getInt(vendedorIdIndex) else 0
        val vendedorNombre = if (vendedorNombreIndex != -1 && !cursor.isNull(vendedorNombreIndex)) cursor.getString(vendedorNombreIndex) ?: "" else ""
        val vendedorCorreo = if (vendedorCorreoIndex != -1 && !cursor.isNull(vendedorCorreoIndex)) cursor.getString(vendedorCorreoIndex) ?: "" else ""
        val stock = if (stockIndex != -1 && !cursor.isNull(stockIndex)) cursor.getInt(stockIndex) else 0

        return ProductoModel(
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
    }

    /**
     * Transforma todas las filas de un [Cursor] en una lista de [ProductoModel].
     * Nota: No cierra el cursor aquí para permitir que la llamada use el bloque `use {}`.
     */
    fun fromCursorToList(cursor: Cursor): List<ProductoModel> {
        val productos = mutableListOf<ProductoModel>()
        while (cursor.moveToNext()) {
            productos.add(fromCursor(cursor))
        }
        return productos
    }
}
