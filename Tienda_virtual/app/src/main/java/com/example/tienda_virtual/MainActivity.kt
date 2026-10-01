package com.example.tienda_virtual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tienda_virtual.navigation.AppNavigation
import com.example.tienda_virtual.ui.theme.Tienda_virtualTheme
import com.example.tienda_virtual.viewmodel.TiendaViewModel

/**
 * Actividad Principal de la Aplicación "Tienda Virtual JAS".
 *
 * Punto de entrada de la aplicación en Android. Configura la interfaz de usuario en Jetpack Compose,
 * habilita el diseño sin bordes (Edge-To-Edge), inicializa el ViewModel global (TiendaViewModel)
 * y aplica el tema dinámico (Modo Claro / Modo Oscuro).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Habilita bordes transparentes y barras de sistema adaptables
        enableEdgeToEdge()
        
        setContent {
            // Inicialización única del ViewModel que comparte el estado global en toda la jerarquía
            val viewModel: TiendaViewModel = viewModel()

            // Envoltorio del tema de la aplicación con soporte para Modo Oscuro y Modo Claro
            Tienda_virtualTheme(
                darkTheme = viewModel.esModoOscuro,
                dynamicColor = false
            ) {
                // Contenedor principal que aplica el color de fondo correspondiente al tema activo
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Carga el sistema de navegación pasando la instancia del ViewModel
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
