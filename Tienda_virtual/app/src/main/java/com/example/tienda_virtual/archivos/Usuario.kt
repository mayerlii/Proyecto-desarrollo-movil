package com.example.tienda_virtual.archivos

/**
 * Modelo de datos para representar al Usuario autenticado en sesión.
 */
data class Usuario(
    val id: Int,
    var nombre: String,
    var correo: String,
    var celular: String,
    var foto: String = "",
    var direccionEnvio: String = "Av. Principal Los Naranjos, Torre B, Apto 4-B",
    val rol: Rol
)

/**
 * Roles del sistema para el control de acceso a características.
 */
enum class Rol {
    VENDEDOR,
    COMPRADOR
}
