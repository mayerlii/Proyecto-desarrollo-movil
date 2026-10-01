package com.example.tienda_virtual.archivos

/**
 * Modelo de datos para representar una Orden de Compra o Pedido.
 */
data class Pedido(
    val id: Int,
    val compradorId: Int,
    val productos: List<CarritoItem>,
    val total: Double,
    var estado: EstadoPedido
)

/**
 * Estados secuenciales de procesamiento del pedido.
 */
enum class EstadoPedido {
    SOLICITUD_RECIBIDA,
    EN_PREPARACION,
    ENTREGADO,
    CANCELADO
}
