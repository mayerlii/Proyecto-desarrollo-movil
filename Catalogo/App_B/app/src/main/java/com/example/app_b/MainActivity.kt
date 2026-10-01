package com.example.app_b

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.app_b.ui.InicioScreen
import com.example.app_b.ui.ProductosListaScreen
import com.example.app_b.ui.theme.App_BTheme

enum class PantallaApp {
    INICIO,
    LISTA_PRODUCTOS
}

/**
 * Actividad Principal de la App B.
 * Gestiona la navegación entre la Pantalla de Inicio y la Lista de Productos
 * leídos mediante ContentResolver desde Tienda Virtual.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App_BTheme {
                var pantallaActual by remember { mutableStateOf(PantallaApp.INICIO) }

                when (pantallaActual) {
                    PantallaApp.INICIO -> {
                        InicioScreen(
                            onVisualizarProductosClick = {
                                pantallaActual = PantallaApp.LISTA_PRODUCTOS
                            }
                        )
                    }
                    PantallaApp.LISTA_PRODUCTOS -> {
                        ProductosListaScreen(
                            onVolverClick = {
                                pantallaActual = PantallaApp.INICIO
                            }
                        )
                    }
                }
            }
        }
    }
}
