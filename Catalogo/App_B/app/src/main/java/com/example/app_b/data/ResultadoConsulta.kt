package com.example.app_b.data

/**
 * Representa el resultado de una operación de consulta hacia el ContentProvider de App A.
 */
sealed class ResultadoConsulta {

    /**
     * Consulta completada exitosamente.
     * Si [productos] está vacía, representa un catálogo sin registros (Cursor vacío).
     */
    data class Exito(val productos: List<ProductoModel>) : ResultadoConsulta()

    /**
     * Representa los diferentes tipos de error que pueden ocurrir al consultar el ContentProvider.
     */
    sealed class Error(
        open val mensaje: String,
        open val detalle: String? = null,
        open val causa: Throwable? = null
    ) : ResultadoConsulta() {

        /**
         * Ocurre cuando la aplicación 'Tienda Virtual' (App A) o su ContentProvider no están instalados
         * o no se pueden resolver en el dispositivo.
         */
        data class AppNoInstalada(
            override val mensaje: String = "La aplicación 'Tienda Virtual' o su ContentProvider no están instalados.",
            override val detalle: String? = "No se localizó el proveedor con autoridad 'com.example.tienda_virtual.provider' en este dispositivo.",
            override val causa: Throwable? = null
        ) : Error(mensaje, detalle, causa)

        /**
         * Ocurre por SecurityException: falta el permiso READ_PRODUCTS en el manifiesto
         * o las dos aplicaciones no están firmadas con el mismo certificado (protectionLevel="signature").
         */
        data class PermisoOFirma(
            override val mensaje: String = "Acceso denegado: falta el permiso READ_PRODUCTS o las firmas no coinciden.",
            override val detalle: String? = "Se requiere el permiso 'com.example.tienda_virtual.permission.READ_PRODUCTS' y que ambas aplicaciones compartan el mismo certificado de firma digital.",
            override val causa: Throwable? = null
        ) : Error(mensaje, detalle, causa)

        /**
         * Cualquier otro error no previsto ocurrido durante la consulta al ContentResolver.
         */
        data class ErrorInesperado(
            override val mensaje: String = "Ocurrió un error inesperado al consultar el catálogo de productos.",
            override val detalle: String? = null,
            override val causa: Throwable? = null
        ) : Error(mensaje, detalle, causa)
    }
}
