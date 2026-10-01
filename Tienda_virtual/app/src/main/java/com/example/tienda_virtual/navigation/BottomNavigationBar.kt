package com.example.tienda_virtual.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.example.tienda_virtual.archivos.Rol
import com.example.tienda_virtual.viewmodel.TiendaViewModel

/**
 * Modelo de datos para representar cada pestaña de la barra de navegación inferior.
 */
data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

/**
 * Componente Reutilizable de Barra de Navegación Inferior (Material 3).
 *
 * Se adapta dinámicamente según el rol del usuario autenticado:
 * - VENDEDOR: Mi Tienda, Publicar Producto, Pedidos de Clientes, Perfil.
 * - COMPRADOR: Inicio Catálogo, Carrito (con badge activo), Mis Pedidos, Perfil.
 */
@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String?,
    viewModel: TiendaViewModel
) {
    // Obtiene el rol del usuario actual en sesión
    val rol = viewModel.usuarioActual?.rol ?: Rol.COMPRADOR

    // Configuración condicional de los botones según el rol
    val items = if (rol == Rol.VENDEDOR) {
        listOf(
            BottomNavItem(
                title = "Mi Tienda",
                route = Routes.VENDEDOR,
                icon = Icons.Default.Store
            ),
            BottomNavItem(
                title = "Agregar",
                route = Routes.AGREGAR_PRODUCTO,
                icon = Icons.Default.AddCircle
            ),
            BottomNavItem(
                title = "Pedidos",
                route = Routes.PEDIDOS,
                icon = Icons.AutoMirrored.Filled.ReceiptLong
            ),
            BottomNavItem(
                title = "Perfil",
                route = Routes.PERFIL,
                icon = Icons.Default.Person
            )
        )
    } else {
        listOf(
            BottomNavItem(
                title = "Inicio",
                route = Routes.COMPRADOR,
                icon = Icons.Default.Home
            ),
            BottomNavItem(
                title = "Carrito",
                route = Routes.CARRITO,
                icon = Icons.Default.ShoppingCart,
                badgeCount = viewModel.cantidadCarrito()
            ),
            BottomNavItem(
                title = "Pedidos",
                route = Routes.PEDIDOS,
                icon = Icons.AutoMirrored.Filled.ReceiptLong
            ),
            BottomNavItem(
                title = "Perfil",
                route = Routes.PERFIL,
                icon = Icons.Default.Person
            )
        )
    }

    NavigationBar {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        val startDestination = if (rol == Rol.VENDEDOR) Routes.VENDEDOR else Routes.COMPRADOR
                        navController.navigate(item.route) {
                            popUpTo(startDestination) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    // Muestra una insignia (badge) si el elemento contiene un contador mayor a 0
                    if (item.badgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(item.badgeCount.toString())
                                }
                            }
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        }
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title
                        )
                    }
                },
                label = {
                    Text(text = item.title)
                }
            )
        }
    }
}
