package com.example.tienda_virtual.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.example.tienda_virtual.archivos.Producto
import com.example.tienda_virtual.data.local.AppDatabase
import com.example.tienda_virtual.ui.utils.ImageUtils

/**
 * ContentProvider Personalizado sobre la base de datos Room [AppDatabase] ("tienda_virtual_db").
 *
 * Expone la tabla "productos" a través de la URI de contenido:
 * content://com.example.tienda_virtual.provider/productos
 *
 * Soporta de forma flexible nombres de claves/columnas de imagen comunes para integraciones externas (app_b):
 * "foto", "imagen", "fotoUrl", "url_imagen", "url".
 */
class ProductoContentProvider : ContentProvider() {

    companion object {
        /** Autoridad del ContentProvider */
        const val AUTHORITY = "com.example.tienda_virtual.provider"

        /** URI de Contenido Base para la tabla de productos */
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/productos")

        // Códigos numéricos para el UriMatcher
        private const val CODE_PRODUCTOS_DIR = 1
        private const val CODE_PRODUCTO_ITEM = 2

        // Configuración del UriMatcher para validar URIs entrantes
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "productos", CODE_PRODUCTOS_DIR)
            addURI(AUTHORITY, "productos/#", CODE_PRODUCTO_ITEM)
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    /**
     * Consulta productos en Room DB a través del ContentProvider.
     */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val context = context ?: return null
        val database = AppDatabase.getDatabase(context)
        val productoDao = database.productoDao()

        val cursor: Cursor = when (uriMatcher.match(uri)) {
            CODE_PRODUCTOS_DIR -> {
                productoDao.obtenerTodosLosProductosCursor()
            }
            CODE_PRODUCTO_ITEM -> {
                val id = ContentUris.parseId(uri)
                productoDao.obtenerProductoPorIdCursor(id)
            }
            else -> throw IllegalArgumentException("URI no soportada para query: $uri")
        }

        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    /**
     * Retorna el tipo MIME para la URI de contenido proporcionada.
     */
    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            CODE_PRODUCTOS_DIR -> "vnd.android.cursor.dir/vnd.$AUTHORITY.productos"
            CODE_PRODUCTO_ITEM -> "vnd.android.cursor.item/vnd.$AUTHORITY.productos"
            else -> throw IllegalArgumentException("URI desconocida: $uri")
        }
    }

    /**
     * Inserta un nuevo producto a través de ContentValues en Room DB.
     *
     * Detecta alias flexibles para el campo de imagen ("foto", "imagen", "fotoUrl", "url_imagen", "url")
     * y procesa el almacenamiento permanente de la imagen para aplicaciones externas (app_b).
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val context = context ?: return null
        if (uriMatcher.match(uri) != CODE_PRODUCTOS_DIR) {
            throw IllegalArgumentException("URI no válida para inserción: $uri")
        }

        values ?: throw IllegalArgumentException("ContentValues no pueden ser nulos")

        // Extrae el valor de imagen soportando múltiples alias de clave de columna
        val fotoInput = when {
            values.containsKey("foto") -> values.getAsString("foto")
            values.containsKey("imagen") -> values.getAsString("imagen")
            values.containsKey("fotoUrl") -> values.getAsString("fotoUrl")
            values.containsKey("url_imagen") -> values.getAsString("url_imagen")
            values.containsKey("url") -> values.getAsString("url")
            else -> ""
        } ?: ""

        // Procesa y persiste la imagen localmente para garantizar disponibilidad permanente
        val fotoProcesada = ImageUtils.procesarYGuardarFoto(context, fotoInput)

        val producto = Producto(
            id = if (values.containsKey("id")) values.getAsInteger("id") ?: 0 else 0,
            nombre = values.getAsString("nombre") ?: "",
            foto = fotoProcesada,
            categoria = values.getAsString("categoria") ?: "General",
            precio = values.getAsDouble("precio") ?: 0.0,
            descripcion = values.getAsString("descripcion") ?: "",
            vendedorId = values.getAsInteger("vendedorId") ?: 0,
            vendedorNombre = values.getAsString("vendedorNombre") ?: "Vendedor JAS",
            vendedorCorreo = values.getAsString("vendedorCorreo") ?: "",
            stock = values.getAsInteger("stock") ?: 10
        )

        val idInsertado = AppDatabase.getDatabase(context).productoDao().insertarProductoSync(producto)
        if (idInsertado > 0) {
            val newUri = ContentUris.withAppendedId(CONTENT_URI, idInsertado)
            context.contentResolver.notifyChange(newUri, null)
            return newUri
        }

        return null
    }

    /**
     * Actualiza un producto existente en Room DB.
     */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val context = context ?: return 0
        values ?: return 0

        val id = when (uriMatcher.match(uri)) {
            CODE_PRODUCTO_ITEM -> ContentUris.parseId(uri).toInt()
            CODE_PRODUCTOS_DIR -> values.getAsInteger("id") ?: 0
            else -> throw IllegalArgumentException("URI no soportada para actualización: $uri")
        }

        if (id <= 0) return 0

        // Extrae el valor de imagen soportando múltiples alias de clave de columna
        val fotoInput = when {
            values.containsKey("foto") -> values.getAsString("foto")
            values.containsKey("imagen") -> values.getAsString("imagen")
            values.containsKey("fotoUrl") -> values.getAsString("fotoUrl")
            values.containsKey("url_imagen") -> values.getAsString("url_imagen")
            values.containsKey("url") -> values.getAsString("url")
            else -> ""
        } ?: ""

        val fotoProcesada = if (fotoInput.isNotBlank()) ImageUtils.procesarYGuardarFoto(context, fotoInput) else ""

        val producto = Producto(
            id = id,
            nombre = values.getAsString("nombre") ?: "",
            foto = fotoProcesada,
            categoria = values.getAsString("categoria") ?: "General",
            precio = values.getAsDouble("precio") ?: 0.0,
            descripcion = values.getAsString("descripcion") ?: "",
            vendedorId = values.getAsInteger("vendedorId") ?: 0,
            vendedorNombre = values.getAsString("vendedorNombre") ?: "Vendedor JAS",
            vendedorCorreo = values.getAsString("vendedorCorreo") ?: "",
            stock = values.getAsInteger("stock") ?: 10
        )

        val filasAfectadas = AppDatabase.getDatabase(context).productoDao().actualizarProductoSync(producto)
        if (filasAfectadas > 0) {
            context.contentResolver.notifyChange(uri, null)
        }
        return filasAfectadas
    }

    /**
     * Elimina uno o todos los productos de Room DB.
     */
    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val context = context ?: return 0
        val productoDao = AppDatabase.getDatabase(context).productoDao()

        val filasEliminadas = when (uriMatcher.match(uri)) {
            CODE_PRODUCTOS_DIR -> productoDao.eliminarTodosProductosSync()
            CODE_PRODUCTO_ITEM -> {
                val id = ContentUris.parseId(uri)
                productoDao.eliminarProductoPorIdSync(id)
            }
            else -> throw IllegalArgumentException("URI no soportada para eliminación: $uri")
        }

        if (filasEliminadas > 0) {
            context.contentResolver.notifyChange(uri, null)
        }
        return filasEliminadas
    }
}
