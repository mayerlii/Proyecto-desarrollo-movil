package com.example.app_b.data

/**
 * Modelo de datos para representar un Producto leído desde el ContentProvider de Tienda Virtual.
 */
data class ProductoModel(
    val id: Int = 0,
    val nombre: String = "",
    val foto: String = "",
    val categoria: String = "",
    val precio: Double = 0.0,
    val descripcion: String = "",
    val vendedorId: Int = 0,
    val vendedorNombre: String = "",
    val vendedorCorreo: String = "",
    val stock: Int = 0
)
