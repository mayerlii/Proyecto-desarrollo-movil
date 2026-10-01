package com.example.tienda_virtual.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.tienda_virtual.archivos.EstadoPedido
import com.example.tienda_virtual.archivos.Rol
import com.example.tienda_virtual.viewmodel.TiendaViewModel
import java.util.Locale

/**
 * Pantalla de Gestión y Seguimiento de Pedidos.
 *
 * Características:
 * - Filtro exclusivo por rol:
 *   - VENDEDOR: Visualiza únicamente las órdenes y productos comprados de su tienda.
 *   - COMPRADOR: Visualiza todas sus compras realizadas.
 * - Despliegue de fotografías, descripción, precios unitarios y total calculado.
 * - Modificación de estados en tiempo real por parte del vendedor (*Solicitud Recibida, En Preparación, Entregado, Cancelado*).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    viewModel: TiendaViewModel,
    navController: NavController
) {
    val usuario = viewModel.usuarioActual
    val esVendedor = usuario?.rol == Rol.VENDEDOR

    // Filtra los pedidos para mostrar solo las órdenes pertenecientes al usuario/vendedor activo
    val pedidosFiltrados = if (esVendedor) {
        viewModel.pedidos.filter { pedido ->
            pedido.productos.any { item ->
                if (usuario != null && usuario.correo.isNotBlank()) {
                    item.producto.vendedorCorreo.equals(usuario.correo, ignoreCase = true) ||
                            (item.producto.vendedorCorreo.isBlank() && usuario.correo.equals("vendedor@jas.com", ignoreCase = true))
                } else false
            }
        }
    } else {
        viewModel.pedidos.filter { it.compradorId == (usuario?.id ?: 0) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esVendedor) "Pedidos Generados por Clientes" else "Mis Pedidos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    // Alternador de tema Modo Oscuro / Modo Claro
                    IconButton(onClick = { viewModel.toggleModoOscuro() }) {
                        Icon(
                            imageVector = if (viewModel.esModoOscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar Modo Claro/Oscuro"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (pedidosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (esVendedor) "Aún no has recibido pedidos de tus productos."
                        else "No has realizado ningún pedido aún.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = pedidosFiltrados,
                        key = { it.id }
                    ) { pedido ->
                        // Productos exclusivos asignados al vendedor activo
                        val productosDelPedido = if (esVendedor) {
                            pedido.productos.filter { item ->
                                if (usuario != null && usuario.correo.isNotBlank()) {
                                    item.producto.vendedorCorreo.equals(usuario.correo, ignoreCase = true) ||
                                            (item.producto.vendedorCorreo.isBlank() && usuario.correo.equals("vendedor@jas.com", ignoreCase = true))
                                } else false
                            }
                        } else {
                            pedido.productos
                        }

                        val totalDelVendedor = productosDelPedido.sumOf { it.producto.precio * it.cantidad }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Pedido #${pedido.id}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            if (esVendedor) "Estado actual del pedido:" else "Estado fijado por Vendedor:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    EstadoBadge(estado = pedido.estado)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    if (esVendedor) "Tus Productos en este Pedido:" else "Detalle de Productos Comprados:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Lista de productos del pedido
                                productosDelPedido.forEach { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(65.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (item.producto.foto.isNotBlank()) {
                                                    AsyncImage(
                                                        model = item.producto.foto,
                                                        contentDescription = item.producto.nombre,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.ShoppingBag,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(28.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    item.producto.nombre,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                if (item.producto.descripcion.isNotBlank()) {
                                                    Text(
                                                        item.producto.descripcion,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "Precio: $${String.format(Locale.US, "%.2f", item.producto.precio)} x${item.cantidad}",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )

                                                    Text(
                                                        "Subtotal: $${String.format(Locale.US, "%.2f", item.producto.precio * item.cantidad)}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF3F51B5)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (esVendedor) "Total de tus Productos:" else "Precio Total del Pedido:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "$${String.format(Locale.US, "%.2f", if (esVendedor) totalDelVendedor else pedido.total)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00695C)
                                    )
                                }

                                // Selector interactivo de estado para el Vendedor
                                if (esVendedor) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Modificar estado para el comprador:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = pedido.estado == EstadoPedido.SOLICITUD_RECIBIDA,
                                                onClick = { viewModel.cambiarEstadoPedido(pedido, EstadoPedido.SOLICITUD_RECIBIDA) },
                                                label = { Text("Solicitud Recibida") }
                                            )

                                            FilterChip(
                                                selected = pedido.estado == EstadoPedido.EN_PREPARACION,
                                                onClick = { viewModel.cambiarEstadoPedido(pedido, EstadoPedido.EN_PREPARACION) },
                                                label = { Text("En Preparación") }
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = pedido.estado == EstadoPedido.ENTREGADO,
                                                onClick = { viewModel.cambiarEstadoPedido(pedido, EstadoPedido.ENTREGADO) },
                                                label = { Text("Entregado") }
                                            )

                                            FilterChip(
                                                selected = pedido.estado == EstadoPedido.CANCELADO,
                                                onClick = { viewModel.cambiarEstadoPedido(pedido, EstadoPedido.CANCELADO) },
                                                label = { Text("Cancelado") },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Componente gráfico para desplegar la insignia de estado del pedido */
@Composable
fun EstadoBadge(estado: EstadoPedido) {
    val (texto, colorFondo, colorTexto) = when (estado) {
        EstadoPedido.SOLICITUD_RECIBIDA -> Triple("Solicitud Recibida", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        EstadoPedido.EN_PREPARACION -> Triple("En Preparación", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        EstadoPedido.ENTREGADO -> Triple("Entregado", Color(0xFFD4EDDA), Color(0xFF155724))
        EstadoPedido.CANCELADO -> Triple("Cancelado", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
    }

    Surface(
        color = colorFondo,
        contentColor = colorTexto,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
