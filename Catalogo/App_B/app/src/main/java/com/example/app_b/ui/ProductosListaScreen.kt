package com.example.app_b.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.app_b.data.ProductoModel

/**
 * Pantalla para mostrar la lista de productos obtenidos mediante el ContentResolver
 * desde el ContentProvider expuesto por la App "Tienda Virtual".
 *
 * Mantiene las operaciones de lectura en [ProductosViewModel] desacopladas de la UI
 * y reacciona a los estados de Carga, Éxito, Catálogo Vacío y Errores específicos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosListaScreen(
    onVolverClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtroBusqueda = viewModel.filtroBusqueda

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Catálogo Tienda Virtual",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolverClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al inicio"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reintentar() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar productos"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val estado = uiState) {
                // 1. Estado de CARGA
                is CatalogoEstado.Cargando -> {
                    EstadoCargandoView(modifier = Modifier.fillMaxSize())
                }

                // 2. Estado de CATÁLOGO VACÍO (Consulta exitosa pero sin productos)
                is CatalogoEstado.Vacio -> {
                    EstadoVacioView(
                        onReintentar = { viewModel.reintentar() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Estados de ERROR (Seguridad/Firma, Provider/App no disponible o Inesperado)
                is CatalogoEstado.Error -> {
                    EstadoErrorView(
                        error = estado,
                        onReintentar = { viewModel.reintentar() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 4. Estado de ÉXITO con productos
                is CatalogoEstado.Exito -> {
                    val productosFiltrados = remember(estado.productos, filtroBusqueda) {
                        viewModel.filtrarProductos(estado.productos)
                    }

                    EstadoExitoView(
                        productosOriginales = estado.productos,
                        productosFiltrados = productosFiltrados,
                        filtroBusqueda = filtroBusqueda,
                        onFiltroChanged = { viewModel.onFiltroChanged(it) },
                        onRecargar = { viewModel.reintentar() }
                    )
                }
            }
        }
    }
}

/**
 * Vista mostrada durante la consulta al ContentResolver.
 */
@Composable
private fun EstadoCargandoView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Consultando ContentResolver...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Leyendo productos desde 'com.example.tienda_virtual.provider'",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Vista mostrada cuando el ContentProvider responde con un Cursor vacío (sin productos).
 */
@Composable
private fun EstadoVacioView(
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Catálogo Vacío",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "La consulta al ContentProvider de Tienda Virtual se realizó con éxito, pero actualmente no hay productos registrados en el catálogo.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onReintentar,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reintentar consulta")
                }
            }
        }
    }
}

/**
 * Vista mostrada ante fallas de consulta (Seguridad/Firma, Provider no instalado o Error inesperado).
 */
@Composable
private fun EstadoErrorView(
    error: CatalogoEstado.Error,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icono, contenedorColor, contenidoColor) = when (error) {
        is CatalogoEstado.Error.PermisoOFirma -> Triple(
            Icons.Default.Security,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        is CatalogoEstado.Error.ProviderNoDisponible -> Triple(
            Icons.Default.CloudOff,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.onErrorContainer
        )
        is CatalogoEstado.Error.ErrorInesperado -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = contenedorColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = contenidoColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = contenidoColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = error.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = contenidoColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = error.mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    color = contenidoColor
                )

                if (error.detalle != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error.detalle!!,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = contenidoColor.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onReintentar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reintentar consulta")
                }
            }
        }
    }
}

/**
 * Vista que renderiza el buscador y la lista de tarjetas de productos.
 */
@Composable
private fun EstadoExitoView(
    productosOriginales: List<ProductoModel>,
    productosFiltrados: List<ProductoModel>,
    filtroBusqueda: String,
    onFiltroChanged: (String) -> Unit,
    onRecargar: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de búsqueda y conteo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = filtroBusqueda,
                onValueChange = onFiltroChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, categoría o vendedor...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null
                    )
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mostrando ${productosFiltrados.size} de ${productosOriginales.size} productos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AssistChip(
                    onClick = onRecargar,
                    label = {
                        Text(
                            text = "Actualizar",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (productosFiltrados.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron productos que coincidan con \"$filtroBusqueda\"",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(
                    items = productosFiltrados,
                    key = { it.id }
                ) { producto ->
                    ProductoCard(producto = producto)
                }
            }
        }
    }
}
