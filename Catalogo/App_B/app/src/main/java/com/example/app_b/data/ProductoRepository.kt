package com.example.app_b.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * Repositorio encargado de realizar consultas de lectura sobre la base de datos
 * expuesta por la aplicación "Tienda Virtual" mediante su ContentProvider.
 *
 * App B es únicamente un consumidor de lectura del catálogo de productos.
 */
object ProductoRepository {

    private const val TAG = "ProductoRepository"

    /** Autoridad del ContentProvider expuesto por Tienda Virtual */
    const val AUTHORITY = "com.example.tienda_virtual.provider"

    /** Cadena de la URI de contenido para consultar la lista de productos */
    const val URI_STRING = "content://$AUTHORITY/productos"

    /** URI de contenido para consultar la lista de productos */
    val CONTENT_URI: Uri
        get() = Uri.parse(URI_STRING)

    /** Proyección explícita con todas las columnas del catálogo */
    val PROJECTION: Array<String> = arrayOf(
        "id",
        "nombre",
        "foto",
        "categoria",
        "precio",
        "descripcion",
        "vendedorId",
        "vendedorNombre",
        "vendedorCorreo",
        "stock"
    )

    /** Criterio de ordenamiento explícito: id DESC */
    const val SORT_ORDER = "id DESC"

    /**
     * Comprueba si el ContentProvider de Tienda Virtual está registrado en el sistema.
     */
    fun isProviderInstalled(context: Context): Boolean {
        return try {
            val providerInfo = context.packageManager.resolveContentProvider(AUTHORITY, 0)
            providerInfo != null
        } catch (e: Exception) {
            Log.w(TAG, "Error al verificar si el ContentProvider está instalado", e)
            false
        }
    }

    /**
     * Consulta el catálogo de productos a través del [ContentResolver] de Android.
     * Retorna una instancia de [ResultadoConsulta] que encapsula éxito (con o sin productos)
     * o errores específicos (falta de permisos/firma, app no instalada o error inesperado).
     */
    fun consultarCatalogo(context: Context): ResultadoConsulta {
        // Verificar si la aplicación o el ContentProvider existen en el dispositivo
        if (!isProviderInstalled(context)) {
            Log.w(TAG, "El ContentProvider '$AUTHORITY' no fue encontrado en el PackageManager.")
            return ResultadoConsulta.Error.AppNoInstalada()
        }

        return ejecutarConsulta(context.contentResolver)
    }

    /**
     * Ejecuta la consulta delegando en un ejecutor de consulta (útil para pruebas unitarias).
     */
    fun ejecutarConsulta(
        queryExecutor: (Uri?, Array<String>?, String?, Array<String>?, String?) -> Cursor?,
        isProviderAvailable: Boolean = true
    ): ResultadoConsulta {
        if (!isProviderAvailable) {
            return ResultadoConsulta.Error.AppNoInstalada()
        }

        return try {
            val uri = try { CONTENT_URI } catch (e: Throwable) { null }
            val cursor: Cursor? = queryExecutor(
                uri,
                PROJECTION,
                null,
                null,
                SORT_ORDER
            )

            if (cursor == null) {
                Log.w(TAG, "El cursor devuelto por ContentResolver fue nulo para URI $URI_STRING")
                return ResultadoConsulta.Error.AppNoInstalada()
            }

            cursor.use { c ->
                val listaProductos = ProductoMapper.fromCursorToList(c)
                Log.d(TAG, "Consulta exitosa: se leyeron ${listaProductos.size} productos.")
                ResultadoConsulta.Exito(listaProductos)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException al consultar ContentProvider. Permiso denegado o firmas distintas.", e)
            ResultadoConsulta.Error.PermisoOFirma(
                mensaje = "Falta el permiso READ_PRODUCTS o las aplicaciones no tienen la misma firma digital (protectionLevel=signature).",
                detalle = e.localizedMessage,
                causa = e
            )
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException al consultar ContentResolver: posible URI no soportada o provider no instalado.", e)
            ResultadoConsulta.Error.AppNoInstalada(
                mensaje = "La URI del ContentProvider no pudo ser resuelta.",
                detalle = e.localizedMessage,
                causa = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al consultar ContentResolver en $URI_STRING", e)
            ResultadoConsulta.Error.ErrorInesperado(
                mensaje = "Ocurrió un error inesperado al consultar los productos.",
                detalle = e.localizedMessage,
                causa = e
            )
        }
    }

    /**
     * Ejecuta directamente la consulta sobre el [ContentResolver].
     */
    fun ejecutarConsulta(
        resolver: ContentResolver,
        isProviderAvailable: Boolean = true
    ): ResultadoConsulta = ejecutarConsulta(
        queryExecutor = { uri, projection, selection, selectionArgs, sortOrder ->
            resolver.query(uri ?: CONTENT_URI, projection, selection, selectionArgs, sortOrder)
        },
        isProviderAvailable = isProviderAvailable
    )

    /**
     * Método de compatibilidad que retorna directamente la lista de productos
     * o una lista vacía en caso de error.
     */
    fun obtenerProductosDesdeContentResolver(context: Context): List<ProductoModel> {
        return when (val resultado = consultarCatalogo(context)) {
            is ResultadoConsulta.Exito -> resultado.productos
            is ResultadoConsulta.Error -> emptyList()
        }
    }
}
