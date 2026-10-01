package com.example.tienda_virtual.comprador

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.tienda_virtual.navigation.Routes
import com.example.tienda_virtual.viewmodel.TiendaViewModel
import java.util.Locale

/**
 * Pantalla Principal para el Rol de Comprador.
 *
 * Incluye:
 * - Buscador en tiempo real de productos.
 * - Filtros por fichas de categoría (Tecnología, Moda, Hogar, Deportes, Belleza).
 * - Grilla de productos destacados mostrando el nombre de la tienda/vendedor correspondiente.
 * - Botón flotante del carrito con insignias (badges) activas.
 * - Campana de notificaciones con avisos de cambios de estado en pedidos.
 * - Conmutador de Tema Claro / Oscuro en la barra superior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompradorHomeScreen(
    viewModel: TiendaViewModel,
    navController: NavController
) {
    // Estados de búsqueda, filtro de categoría y notificación
    var textoBusqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todas") }
    var mostrarNotificaciones by remember { mutableStateOf(false) }
    val listaFavoritos = remember { mutableStateListOf<Int>() }

    // Filtrado en tiempo real por búsqueda y categoría
    val productosFiltrados = viewModel.productos.filter { prod ->
        val coincideBusqueda = prod.nombre.contains(textoBusqueda, ignoreCase = true) ||
                prod.categoria.contains(textoBusqueda, ignoreCase = true)

        val coincideCategoria = if (categoriaSeleccionada == "Todas") true
        else prod.categoria.equals(categoriaSeleccionada, ignoreCase = true)

        coincideBusqueda && coincideCategoria
    }

    // Modal de Notificaciones del Comprador (Actualizaciones de Estado de Pedidos)
    if (mostrarNotificaciones) {
        AlertDialog(
            onDismissRequest = {
                mostrarNotificaciones = false
                viewModel.marcarNotificacionesCompradorLeidas()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Notificaciones de Compras", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (viewModel.notificacionesComprador.isEmpty()) {
                        Text(
                            "No tienes actualizaciones de pedidos recientes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(viewModel.notificacionesComprador) { notif ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        notif,
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
                        viewModel.marcarNotificacionesCompradorLeidas()
                    }
                ) {
                    Text("Entendido")
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
                                    imageVector = Icons.Default.ShoppingBag,
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
                                "Explorar Marketplace",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    // Botón para conmutar Modo Oscuro / Modo Claro
                    IconButton(onClick = { viewModel.toggleModoOscuro() }) {
                        Icon(
                            imageVector = if (viewModel.esModoOscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar Modo Claro/Oscuro"
                        )
                    }

                    // Campana de notificaciones con insignia (badge)
                    IconButton(
                        onClick = {
                            mostrarNotificaciones = true
                            viewModel.marcarNotificacionesCompradorLeidas()
                        }
                    ) {
                        if (viewModel.notificacionesCompradorNoLeidas > 0) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(viewModel.notificacionesCompradorNoLeidas.toString())
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

                    // Botón de acceso directo al Perfil
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
        },
        floatingActionButton = {
            // Botón flotante para acceder al Carrito cuando contiene ítems
            if (viewModel.cantidadCarrito() > 0) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = Color.Red, contentColor = Color.White) {
                            Text(viewModel.cantidadCarrito().toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    FloatingActionButton(
                        onClick = { navController.navigate(Routes.CARRITO) },
                        containerColor = Color(0xFF3F51B5),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Carrito",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Campo de búsqueda principal
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                placeholder = { Text("Buscar en MercadoVirtual...") },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Fichas de Selección de Categoría
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Explorar Categorías",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = { categoriaSeleccionada = "Todas" }) {
                    Text(
                        "VER TODO",
                        color = Color(0xFF3F51B5),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val listaCategorias = listOf("Todas", "Tecnología", "Moda", "Hogar", "Deportes", "Belleza")
                items(listaCategorias.size) { index ->
                    val cat = listaCategorias[index]
                    val esSeleccionada = categoriaSeleccionada.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = esSeleccionada,
                        onClick = { categoriaSeleccionada = cat },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (cat) {
                                    "Tecnología" -> Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    "Moda" -> Icon(Icons.Default.Checkroom, contentDescription = null, modifier = Modifier.size(16.dp))
                                    "Hogar" -> Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                                    "Deportes" -> Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                    "Belleza" -> Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                                    else -> Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(cat)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF3F51B5),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grilla de Productos Filtrados
            if (productosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay productos en esta categoría.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = productosFiltrados,
                        key = { it.id }
                    ) { prod ->
                        val esFav = listaFavoritos.contains(prod.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("producto_detalle/${prod.id}")
                                },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column {
                                // Imagen del Producto
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
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
                                            modifier = Modifier.size(44.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Etiqueta de Categoría
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = prod.categoria,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Corazón de Favorito
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(30.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (esFav) listaFavoritos.remove(prod.id)
                                                else listaFavoritos.add(prod.id)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (esFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                tint = if (esFav) Color.Red else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Información de la tarjeta
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            prod.vendedorNombre,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF00875A),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        prod.nombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", prod.precio)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A237E)
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF3F51B5),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.agregarAlCarrito(prod) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddShoppingCart,
                                                    contentDescription = "Agregar",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
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
}
