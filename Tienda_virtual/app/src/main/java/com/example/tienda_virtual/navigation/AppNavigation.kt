package com.example.tienda_virtual.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tienda_virtual.autenticacion.LoginScreen
import com.example.tienda_virtual.carrito.CarritoScreen
import com.example.tienda_virtual.comprador.CompradorHomeScreen
import com.example.tienda_virtual.archivos.Rol
import com.example.tienda_virtual.pagos.PagoScreen
import com.example.tienda_virtual.pedidos.PedidosScreen
import com.example.tienda_virtual.perfil.PerfilScreen
import com.example.tienda_virtual.productos.AgregarProductoScreen
import com.example.tienda_virtual.productos.ProductoDetalleScreen
import com.example.tienda_virtual.productos.ProductosListaScreen
import com.example.tienda_virtual.vendedor.VendedorHomeScreen
import com.example.tienda_virtual.viewmodel.TiendaViewModel

/**
 * Definición de constantes para las rutas de navegación de la aplicación.
 */
object Routes {
    const val LOGIN = "login"
    const val COMPRADOR = "comprador"
    const val VENDEDOR = "vendedor"
    const val PERFIL = "perfil"
    const val AGREGAR_PRODUCTO = "agregar_producto"
    const val PRODUCTOS_LISTA = "productos_lista"
    const val PRODUCTO_DETALLE = "producto_detalle/{productoId}"
    const val CARRITO = "carrito"
    const val PAGOS = "pagos"
    const val PEDIDOS = "pedidos"
    const val NOTAS_CONFIDENCIALES = "notas_confidenciales"
}

/**
 * Componente principal de Navegación de la Aplicación.
 *
 * Administra el NavHost, la pila de destinos, la visibilidad condicional de la barra de navegación
 * inferior (BottomNavigationBar) y el enrutamiento según el rol del usuario (Comprador / Vendedor).
 */
@Composable
fun AppNavigation(
    viewModel: TiendaViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Lista de rutas en las que la barra inferior de navegación debe mostrarse
    val visibleRoutes = listOf(
        Routes.COMPRADOR,
        Routes.VENDEDOR,
        Routes.PERFIL,
        Routes.AGREGAR_PRODUCTO,
        Routes.PRODUCTOS_LISTA,
        Routes.CARRITO,
        Routes.PEDIDOS
    )

    // Oculta la barra de navegación inferior en la pantalla de Login
    val showBottomBar = currentRoute in visibleRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    viewModel = viewModel
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Pantalla 1: Inicio de Sesión y Registro
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = viewModel,
                    onLogin = {
                        // Enrutamiento dinámico según el rol validado del usuario activo
                        if (viewModel.usuarioActual?.rol == Rol.VENDEDOR) {
                            navController.navigate(Routes.VENDEDOR) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.COMPRADOR) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                )
            }

            // Pantalla 2: Panel de Control del Vendedor (Catálogo y Métricas Exclusivas)
            composable(Routes.VENDEDOR) {
                VendedorHomeScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // Pantalla 3: Inicio del Comprador (Catálogo General con Filtros por Categoría)
            composable(Routes.COMPRADOR) {
                CompradorHomeScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // Pantalla 4: Perfil de Usuario (Foto, Datos Personales y Cerrar Sesión)
            composable(Routes.PERFIL) {
                PerfilScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // Pantalla 5: Formulario de Publicación de Producto (Vendedor)
            composable(Routes.AGREGAR_PRODUCTO) {
                AgregarProductoScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // Pantalla 6: Catálogo en Lista de Productos
            composable(Routes.PRODUCTOS_LISTA) {
                ProductosListaScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // Pantalla 7: Detalle del Producto (Stock, Precio, Descripción del Vendedor)
            composable(route = Routes.PRODUCTO_DETALLE) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("productoId")?.toIntOrNull()
                val producto = viewModel.productos.find { it.id == id }

                if (producto != null) {
                    ProductoDetalleScreen(
                        producto = producto,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }

            // Pantalla 8: Carrito de Compras (Gestión de Cantidades y Dirección de Envío)
            composable(Routes.CARRITO) {
                CarritoScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // Pantalla 9: Proceso de Pago Seguro (Paso 2 Checkout)
            composable(Routes.PAGOS) {
                PagoScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // Pantalla 10: Seguimiento de Pedidos y Estados (Vendedor / Comprador)
            composable(Routes.PEDIDOS) {
                PedidosScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // Pantalla 11: Notas Confidenciales y Comparativa de Seguridad
            composable(Routes.NOTAS_CONFIDENCIALES) {
                com.example.tienda_virtual.seguridad.NotasConfidencialesScreen(
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}
