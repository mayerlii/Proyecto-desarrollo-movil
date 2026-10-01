package com.example.app_b.ui

import com.example.app_b.data.ProductoModel

/**
 * Estados de la interfaz de usuario para el catálogo de productos de App B.
 */
sealed interface CatalogoEstado {

    /**
     * Estado mientras se realiza la consulta al ContentResolver en un hilo secundario.
     */
    object Cargando : CatalogoEstado

    /**
     * Estado cuando la consulta fue exitosa y se obtuvieron productos.
     */
    data class Exito(val productos: List<ProductoModel>) : CatalogoEstado

    /**
     * Estado cuando la consulta fue exitosa pero el cursor no contiene ningún registro (catálogo vacío).
     */
    object Vacio : CatalogoEstado

    /**
     * Estados de error con mensaje y detalles para el usuario.
     */
    sealed class Error(
        open val titulo: String,
        open val mensaje: String,
        open val detalle: String? = null
    ) : CatalogoEstado {

        /**
         * Error por SecurityException: falta de permiso o firmas no coincidentes.
         */
        data class PermisoOFirma(
            override val mensaje: String = "Falta el permiso READ_PRODUCTS o las aplicaciones no tienen la misma firma digital.",
            override val detalle: String? = "Ambas aplicaciones deben estar firmadas con el mismo certificado (protectionLevel=signature)."
        ) : Error(
            titulo = "Permiso o Firma Inválida",
            mensaje = mensaje,
            detalle = detalle
        )

        /**
         * Error cuando la aplicación 'Tienda Virtual' o su ContentProvider no están instalados.
         */
        data class ProviderNoDisponible(
            override val mensaje: String = "La aplicación 'Tienda Virtual' (App A) o su ContentProvider no están disponibles.",
            override val detalle: String? = "Verifica que la aplicación Tienda Virtual esté instalada en el mismo dispositivo."
        ) : Error(
            titulo = "App A No Encontrada",
            mensaje = mensaje,
            detalle = detalle
        )

        /**
         * Error inesperado durante la lectura del ContentResolver.
         */
        data class ErrorInesperado(
            override val mensaje: String = "Ocurrió un problema al leer el catálogo desde el ContentResolver.",
            override val detalle: String? = null
        ) : Error(
            titulo = "Error de Consulta",
            mensaje = mensaje,
            detalle = detalle
        )
    }
}
