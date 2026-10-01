package com.example.tienda_virtual.perfil

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.tienda_virtual.navigation.Routes
import com.example.tienda_virtual.ui.utils.ImageUtils
import com.example.tienda_virtual.viewmodel.TiendaViewModel

/**
 * Pantalla de Perfil de Usuario.
 *
 * Características:
 * - Cambio de foto de perfil usando Cámara nativa (Bitmap Base64) o Galería (Uri).
 * - Edición de Nombre, Correo Electrónico y Número de Celular.
 * - Persistencia automática de la foto de perfil en SharedPreferences al guardar cambios.
 * - Conmutador de tema Modo Oscuro / Modo Claro.
 * - Cierre de sesión seguro limpiando usuario activo y carrito.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    viewModel: TiendaViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val usuario = viewModel.usuarioActual

    // Estados de datos personales
    var nombre by remember { mutableStateOf(usuario?.nombre ?: "") }
    var correo by remember { mutableStateOf(usuario?.correo ?: "") }
    var celular by remember { mutableStateOf(usuario?.celular ?: "") }
    var fotoModel by remember { mutableStateOf<Any?>(usuario?.foto.takeIf { !it.isNullByBlank() }) }
    var mensajeConfirmacion by remember { mutableStateOf<String?>(null) }

    // Launcher para tomar foto de perfil con la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val pathGuardada = ImageUtils.guardarBitmapEnAlmacenamientoInterno(context, it)
            fotoModel = pathGuardada ?: ImageUtils.bitmapToBase64(it)
        }
    }

    // Launcher para seleccionar foto de perfil de la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val pathGuardada = ImageUtils.guardarUriEnAlmacenamientoInterno(context, it)
            fotoModel = pathGuardada ?: it.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    // Botón para cambiar tema Claro / Oscuro
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
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contenedor circular para la foto de perfil
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (fotoModel != null && fotoModel.toString().isNotBlank()) {
                    AsyncImage(
                        model = fotoModel,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botones para Cámara o Galería
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) }
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tomar foto")
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Galería")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Formulario de Datos Personales
            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                    mensajeConfirmacion = null
                },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    mensajeConfirmacion = null
                },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = celular,
                onValueChange = {
                    celular = it
                    mensajeConfirmacion = null
                },
                label = { Text("Número de Celular") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            mensajeConfirmacion?.let { msg ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón de Guardar Cambios (Actualiza perfil y persiste la foto)
            Button(
                onClick = {
                    val fotoProcesada = ImageUtils.procesarYGuardarFoto(context, fotoModel)
                    viewModel.actualizarPerfil(
                        nombre = nombre,
                        correo = correo,
                        celular = celular,
                        foto = fotoProcesada
                    )
                    mensajeConfirmacion = "¡Perfil actualizado con éxito!"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar cambios")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón para acceder a Notas Confidenciales
            FilledTonalButton(
                onClick = { navController.navigate(Routes.NOTAS_CONFIDENCIALES) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Notas Confidenciales ", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de Cerrar Sesión
            OutlinedButton(
                onClick = {
                    viewModel.cerrarSesion()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }
        }
    }
}

private fun String?.isNullByBlank(): Boolean {
    return this.isNullOrBlank()
}
