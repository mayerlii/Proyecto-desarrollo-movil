package com.example.tienda_virtual.archivos

/**
 * Modelo para un elemento o producto agregado al Carrito de Compras.
 */
data class CarritoItem(
    val producto: Producto,
    var cantidad: Int
) {
    /** Calcula el subtotal multiplicando precio unitario por cantidad */
    fun subtotal(): Double {
        return producto.precio * cantidad
    }
}
