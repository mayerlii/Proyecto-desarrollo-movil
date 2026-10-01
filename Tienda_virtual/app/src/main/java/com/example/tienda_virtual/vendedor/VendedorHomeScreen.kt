package com.example.tienda_virtual.vendedor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.tienda_virtual.archivos.Producto
import com.example.tienda_virtual.navigation.Routes
import com.example.tienda_virtual.viewmodel.TiendaViewModel
import java.util.Locale

/**
 * Panel de Control Principal para la Gestión del Vendedor.
 *
 * Características principales:
 * - Aislamiento multi-vendedor: Solo muestra los productos publicados por la cuenta en sesión.
 * - Métricas de ventas y pedidos recibidos exclusivas para el vendedor activo.
 * - Buscador y filtro por nivel de stock (Activos vs. Bajo Stock).
 * - Modal para edición rápida de nombre, precio y stock.
 * - Notificaciones exclusivas de compras realizadas sobre productos propios.
 * - Alternador de tema gráfico (Modo Claro / Modo Oscuro).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendedorHomeScreen(
    viewModel: TiendaViewModel,
    navController: NavController
) {
    // Estados de búsqueda, filtro de stock, edición y notificaciones
    var textoBusqueda by remember { mutableStateOf("") }
    var filtroSeleccionado by remember { mutableIntStateOf(0) }
    var productoAEditar by remember { mutableStateOf<Producto?>(null) }
    var mostrarNotificaciones by remember { mutableStateOf(false) }

    val usuario = viewModel.usuarioActual

    // Aislamiento de Productos: Muestra únicamente los productos pertenecientes al vendedor activo
    val productosFiltrados = viewModel.productos.filter { prod ->
        val esDeEsteVendedor = if (usuario != null && usuario.correo.isNotBlank()) {
            prod.vendedorCorreo.equals(usuario.correo, ignoreCase = true) ||
                    (prod.vendedorCorreo.isBlank() && usuario.correo.equals("vendedor@jas.com", ignoreCase = true))
        } else {
            false
        }

        val coincideBusqueda = prod.nombre.contains(textoBusqueda, ignoreCase = true) ||
                prod.categoria.contains(textoBusqueda, ignoreCase = true)

        val coincideFiltro = when (filtroSeleccionado) {
            1 -> prod.stock > 3
            2 -> prod.stock in 1..3
            else -> true
        }

        esDeEsteVendedor && coincideBusqueda && coincideFiltro
    }

    // Filtro exclusivo de notificaciones dirigidas al correo de este vendedor
    val misNotificaciones = viewModel.notificacionesVendedorList.filter { notif ->
        if (usuario != null && usuario.correo.isNotBlank()) {
            notif.vendedorCorreo.equals(usuario.correo, ignoreCase = true) ||
                    (notif.vendedorCorreo.isBlank() && usuario.correo.equals("vendedor@gmail.com", ignoreCase = true))
        } else false
    }

    // Cálculo exclusivo de Ventas Totales generadas por los productos de este vendedor
    val totalVentasDelVendedor = viewModel.pedidos.sumOf { pedido ->
        pedido.productos.filter { item ->
            if (usuario != null && usuario.correo.isNotBlank()) {
                item.producto.vendedorCorreo.equals(usuario.correo, ignoreCase = true) ||
                        (item.producto.vendedorCorreo.isBlank() && usuario.correo.equals("vendedor@gmail.com", ignoreCase = true))
            } else false
        }.sumOf { it.producto.precio * it.cantidad }
    }

    // Conteo exclusivo de pedidos recibidos para este vendedor
    val misPedidosCount = viewModel.pedidos.count { pedido ->
        pedido.productos.any { item ->
            if (usuario != null && usuario.correo.isNotBlank()) {
                item.producto.vendedorCorreo.equals(usuario.correo, ignoreCase = true) ||
                        (item.producto.vendedorCorreo.isBlank() && usuario.correo.equals("vendedor@gmail.com", ignoreCase = true))
            } else false
        }
    }

    // Modal de Notificaciones del Vendedor
    if (mostrarNotificaciones) {
        AlertDialog(
            onDismissRequest = {
                mostrarNotificaciones = false
                viewModel.marcarNotificacionesLeidas()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Notificaciones de tus Ventas", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (misNotificaciones.isEmpty()) {
                        Text(
                            "No tienes notificaciones de ventas recientes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(misNotificaciones) { notif ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        notif.mensaje,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarNotificaciones = false
                        viewModel.marcarNotificacionesLeidas()
                    }
                ) {
                    Text("Entendido")
                }
            }
        )
    }

    // Modal para Edición Rápida de Producto
    if (productoAEditar != null) {
        val prod = productoAEditar!!
        var nombreEdit by remember { mutableStateOf(prod.nombre) }
        var precioEdit by remember { mutableStateOf(prod.precio.toString()) }
        var stockEdit by remember { mutableStateOf(prod.stock.toString()) }

        AlertDialog(
            onDismissRequest = { productoAEditar = null },
            title = { Text("Editar Producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombreEdit,
                        onValueChange = { nombreEdit = it },
                        label = { Text("Nombre") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = precioEdit,
                        onValueChange = { precioEdit = it },
                        label = { Text("Precio ($)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stockEdit,
                        onValueChange = { stockEdit = it },
                        label = { Text("Stock Disponible") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = precioEdit.toDoubleOrNull() ?: prod.precio
                        val s = stockEdit.toIntOrNull() ?: prod.stock
                        prod.nombre = nombreEdit
                        prod.precio = p
                        prod.stock = s
                        viewModel.actualizarProducto(prod)
                        productoAEditar = null
                    }
                ) {
                    Text("Guardar Cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { productoAEditar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                "MARKETPLACE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00875A),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Perfil de vendedor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Alternador de Tema Modo Claro / Modo Oscuro
                    IconButton(onClick = { viewModel.toggleModoOscuro() }) {
                        Icon(
                            imageVector = if (viewModel.esModoOscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar Modo Claro/Oscuro"
                        )
                    }

                    // Campana de notificaciones de ventas
                    IconButton(
                        onClick = {
                            mostrarNotificaciones = true
                            viewModel.marcarNotificacionesLeidas()
                        }
                    ) {
                        if (misNotificaciones.isNotEmpty() && viewModel.notificacionesNoLeidas > 0) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(misNotificaciones.size.toString())
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificaciones"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notificaciones"
                            )
                        }
                    }

                    // Botón de Perfil
                    IconButton(onClick = { navController.navigate(Routes.PERFIL) }) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Perfil",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Cabecera de Tienda Verificada y Botón Publicar Nuevo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00875A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "TIENDA VERIFICADA (${usuario?.nombre ?: "Vendedor"})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00875A)
                        )
                    }
                    Text(
                        "Mis Productos",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { navController.navigate(Routes.AGREGAR_PRODUCTO) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F51B5)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Nuevo", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas de Métricas Exclusivas para el Vendedor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Métrica 1: Productos Activos del vendedor
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Mis Productos Activos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            productosFiltrados.size.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "↗ 100% disponibles",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Métrica 2: Ventas Totales exclusivas de este vendedor
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Mis Ventas Totales",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFF3E0),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "$${String.format(Locale.US, "%.2f", totalVentasDelVendedor)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "↑ $misPedidosCount pedidos recibidos",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de búsqueda en el inventario del vendedor
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                placeholder = { Text("Buscar por nombre, SKU o etiqueta...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fichas de filtro de stock
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtroSeleccionado == 0,
                    onClick = { filtroSeleccionado = 0 },
                    label = { Text("Todos (${productosFiltrados.size})") }
                )

                FilterChip(
                    selected = filtroSeleccionado == 1,
                    onClick = { filtroSeleccionado = 1 },
                    label = { Text("Activos (${productosFiltrados.count { it.stock > 3 }})") }
                )

                FilterChip(
                    selected = filtroSeleccionado == 2,
                    onClick = { filtroSeleccionado = 2 },
                    label = { Text("Bajo Stock (${productosFiltrados.count { it.stock in 1..3 }})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de Productos del Vendedor
            if (productosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No has publicado productos en este catálogo.\n¡Toca '+ Nuevo' para publicar uno!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = productosFiltrados,
                        key = { it.id }
                    ) { prod ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (prod.foto.isNotBlank()) {
                                            AsyncImage(
                                                model = prod.foto,
                                                contentDescription = prod.nombre,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingBag,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = prod.categoria.take(6),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                prod.categoria.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF00875A),
                                                fontWeight = FontWeight.Bold
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (prod.stock > 3) Color(0xFFD4EDDA) else Color(0xFFFFF3CD)
                                            ) {
                                                Text(
                                                    text = if (prod.stock > 3) "● Activo" else "● Bajo Stock",
                                                    color = if (prod.stock > 3) Color(0xFF155724) else Color(0xFF856404),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            prod.nombre,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            "Vendido por: ${prod.vendedorNombre}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "$${String.format(Locale.US, "%.2f", prod.precio)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF3F51B5)
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "📦 ${prod.stock} uds disp.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Acciones de Editar y Eliminar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { productoAEditar = prod },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Editar")
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.eliminarProducto(prod) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(20.dp)
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
