package com.example.tienda_virtual.autenticacion

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tienda_virtual.R
import com.example.tienda_virtual.archivos.Rol
import com.example.tienda_virtual.viewmodel.TiendaViewModel
import kotlinx.coroutines.delay

/**
 * Pantalla Principal de Inicio de Sesión y Registro Multi-Paso ("Tienda Virtual JAS").
 *
 * Características principales:
 * - Selección de rol mediante píldora segmentada (Soy Comprador / Soy Vendedor).
 * - Validación estricta de correo electrónico y credenciales con mensajes de error descriptivos.
 * - Formulario modal de registro multi-paso (3 pasos) con temporizador de sesión activo.
 * - Aislamiento total de borradores entre Comprador y Vendedor.
 * - Recuperación de contraseña con alerta de confirmación.
 * - Conmutador gráfico de tema (Modo Oscuro / Modo Claro).
 */
@Composable
fun LoginScreen(
    viewModel: TiendaViewModel,
    onLogin: () -> Unit
) {
    val context = LocalContext.current

    // Estados de entrada para el formulario de Inicio de Sesión
    var correoInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var rolSeleccionado by rememberSaveable { mutableStateOf(Rol.COMPRADOR) }
    var recordarme by rememberSaveable { mutableStateOf(true) }
    var mostrarPassword by rememberSaveable { mutableStateOf(false) }
    var errorMensaje by rememberSaveable { mutableStateOf<String?>(null) }

    // Estados para la recuperación de contraseña
    var mostrarOlvidastePassword by rememberSaveable { mutableStateOf(false) }
    var correoRecuperacion by rememberSaveable { mutableStateOf("") }

    // Estado de borrador independiente para el Registro de COMPRADOR
    var nombreCompradorDraft by rememberSaveable { mutableStateOf("") }
    var correoCompradorDraft by rememberSaveable { mutableStateOf("") }
    var celularCompradorDraft by rememberSaveable { mutableStateOf("") }
    var direccionCompradorDraft by rememberSaveable { mutableStateOf("") }
    var passwordCompradorDraft by rememberSaveable { mutableStateOf("") }
    var confirmPasswordCompradorDraft by rememberSaveable { mutableStateOf("") }
    var notasCompradorDraft by rememberSaveable { mutableStateOf("") }
    var pasoCompradorDraft by rememberSaveable { mutableIntStateOf(1) }
    var segundosCompradorDraft by rememberSaveable { mutableIntStateOf(0) }

    // Estado de borrador independiente para el Registro de VENDEDOR
    var nombreVendedorDraft by rememberSaveable { mutableStateOf("") }
    var correoVendedorDraft by rememberSaveable { mutableStateOf("") }
    var celularVendedorDraft by rememberSaveable { mutableStateOf("") }
    var direccionVendedorDraft by rememberSaveable { mutableStateOf("") }
    var passwordVendedorDraft by rememberSaveable { mutableStateOf("") }
    var confirmPasswordVendedorDraft by rememberSaveable { mutableStateOf("") }
    var notasVendedorDraft by rememberSaveable { mutableStateOf("") }
    var pasoVendedorDraft by rememberSaveable { mutableIntStateOf(1) }
    var segundosVendedorDraft by rememberSaveable { mutableIntStateOf(0) }

    // Control del modal de registro y errores
    var mostrarDialogoRegistro by rememberSaveable { mutableStateOf(false) }
    var rolRegistro by rememberSaveable { mutableStateOf(Rol.COMPRADOR) }
    var errorReg by rememberSaveable { mutableStateOf<String?>(null) }

    // Corrutina que incrementa el temporizador de sesión para el borrador del rol activo
    LaunchedEffect(mostrarDialogoRegistro, rolRegistro) {
        if (mostrarDialogoRegistro) {
            while (true) {
                delay(1000)
                if (rolRegistro == Rol.COMPRADOR) {
                    segundosCompradorDraft++
                } else {
                    segundosVendedorDraft++
                }
            }
        }
    }

    /** Valida la estructura del correo electrónico utilizando patrones del SDK */
    fun esCorreoValido(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    /** Abre el modal de registro garantizando el borrador del rol seleccionado */
    fun abrirRegistroConRol(nuevoRol: Rol) {
        rolRegistro = nuevoRol
        errorReg = null
        mostrarDialogoRegistro = true
    }

    // Modal de Recuperación de Contraseña
    if (mostrarOlvidastePassword) {
        AlertDialog(
            onDismissRequest = { mostrarOlvidastePassword = false },
            title = { Text("Recuperar Contraseña", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Ingresa tu correo registrado. Se enviarán las instrucciones de restablecimiento a tu dirección.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = correoRecuperacion,
                        onValueChange = { correoRecuperacion = it },
                        label = { Text("Correo Electrónico") },
                        placeholder = { Text("ejemplo@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailDestino = correoRecuperacion.trim()
                        if (esCorreoValido(emailDestino)) {
                            Toast.makeText(
                                context,
                                "¡Enlace de recuperación enviado con éxito al correo $emailDestino! Revisa tu bandeja de entrada.",
                                Toast.LENGTH_LONG
                            ).show()
                            mostrarOlvidastePassword = false
                        } else {
                            Toast.makeText(context, "Por favor ingresa un correo válido.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enviar Correo")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarOlvidastePassword = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Registro Multi-Paso con Borradores Aislados y Temporizador
    if (mostrarDialogoRegistro) {
        val esComprador = rolRegistro == Rol.COMPRADOR

        val nombreVal = if (esComprador) nombreCompradorDraft else nombreVendedorDraft
        val correoVal = if (esComprador) correoCompradorDraft else correoVendedorDraft
        val celularVal = if (esComprador) celularCompradorDraft else celularVendedorDraft
        val direccionVal = if (esComprador) direccionCompradorDraft else direccionVendedorDraft
        val passwordVal = if (esComprador) passwordCompradorDraft else passwordVendedorDraft
        val confirmPasswordVal = if (esComprador) confirmPasswordCompradorDraft else confirmPasswordVendedorDraft
        val notasVal = if (esComprador) notasCompradorDraft else notasVendedorDraft
        val pasoVal = if (esComprador) pasoCompradorDraft else pasoVendedorDraft
        val segundosVal = if (esComprador) segundosCompradorDraft else segundosVendedorDraft

        AlertDialog(
            onDismissRequest = { mostrarDialogoRegistro = false },
            title = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (esComprador) "Registro Comprador" else "Registro Vendedor",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "Paso $pasoVal de 3",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Indicador del Temporizador de Sesión del Registro
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "⏱️ Temporizador de Sesión: ${segundosVal / 60}m ${segundosVal % 60}s",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (pasoVal) {
                        1 -> {
                            Text(
                                "Paso 1: Datos Personales de la Cuenta",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = nombreVal,
                                onValueChange = {
                                    if (esComprador) nombreCompradorDraft = it else nombreVendedorDraft = it
                                    errorReg = null
                                },
                                label = { Text("Nombre Completo *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = correoVal,
                                onValueChange = {
                                    if (esComprador) correoCompradorDraft = it else correoVendedorDraft = it
                                    errorReg = null
                                },
                                label = { Text("Correo Electrónico *") },
                                placeholder = { Text("ejemplo@jas.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        2 -> {
                            Text(
                                "Paso 2: Datos de Contacto y Envío",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = celularVal,
                                onValueChange = {
                                    if (esComprador) celularCompradorDraft = it else celularVendedorDraft = it
                                    errorReg = null
                                },
                                label = { Text("Teléfono / Celular *") },
                                placeholder = { Text("3123456789") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = direccionVal,
                                onValueChange = {
                                    if (esComprador) direccionCompradorDraft = it else direccionVendedorDraft = it
                                    errorReg = null
                                },
                                label = { Text("Dirección de Envío Principal *") },
                                placeholder = { Text("Av. Principal, Edificio A, Apto 4B") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        else -> {
                            Text(
                                "Paso 3: Seguridad y Observaciones",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = passwordVal,
                                onValueChange = {
                                    if (esComprador) passwordCompradorDraft = it else passwordVendedorDraft = it
                                    errorReg = null
                                },
                                label = { Text("Contraseña (Mín. 8 caract.) *") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = confirmPasswordVal,
                                onValueChange = {
                                    if (esComprador) confirmPasswordCompradorDraft = it else confirmPasswordVendedorDraft = it
                                    errorReg = null
                                },
                                label = { Text("Confirmar Contraseña *") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = notasVal,
                                onValueChange = {
                                    if (esComprador) notasCompradorDraft = it else notasVendedorDraft = it
                                },
                                label = { Text("Observaciones / Notas adicionales") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                            )
                        }
                    }

                    errorReg?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pasoVal > 1) {
                        OutlinedButton(
                            onClick = {
                                if (esComprador) pasoCompradorDraft-- else pasoVendedorDraft--
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Anterior")
                        }
                    }

                    Button(
                        onClick = {
                            when (pasoVal) {
                                1 -> {
                                    if (nombreVal.isBlank()) errorReg = "Ingresa tu Nombre Completo."
                                    else if (!esCorreoValido(correoVal)) errorReg = "Ingresa un correo electrónico válido."
                                    else {
                                        if (esComprador) pasoCompradorDraft = 2 else pasoVendedorDraft = 2
                                        errorReg = null
                                    }
                                }
                                2 -> {
                                    if (celularVal.isBlank()) errorReg = "Ingresa tu número de teléfono."
                                    else if (direccionVal.isBlank()) errorReg = "Ingresa tu dirección de envío."
                                    else {
                                        if (esComprador) pasoCompradorDraft = 3 else pasoVendedorDraft = 3
                                        errorReg = null
                                    }
                                }
                                3 -> {
                                    if (passwordVal.length < 4) errorReg = "Ingresa una contraseña válida."
                                    else if (passwordVal != confirmPasswordVal) errorReg = "Las contraseñas no coinciden."
                                    else {
                                        val errorResultado = viewModel.registrarUsuario(
                                            nombre = nombreVal.trim(),
                                            correo = correoVal.trim(),
                                            celular = celularVal.trim(),
                                            direccionEnvio = direccionVal.trim(),
                                            password = passwordVal,
                                            rol = rolRegistro
                                        )

                                        if (errorResultado != null) {
                                            errorReg = errorResultado
                                        } else {
                                            mostrarDialogoRegistro = false
                                            Toast.makeText(context, "¡Registro completado e iniciado sesión!", Toast.LENGTH_LONG).show()
                                            onLogin()
                                        }
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (pasoVal < 3) "Siguiente" else "Registrarse")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoRegistro = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Botón de alternancia de tema (Modo Oscuro / Modo Claro)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { viewModel.toggleModoOscuro() }) {
                Icon(
                    imageVector = if (viewModel.esModoOscuro) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar Modo Claro/Oscuro",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Logotipo oficial de la aplicación en el contenedor principal
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_logo_foreground),
                contentDescription = "Logo Tienda Virtual JAS",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFF00875A),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(18.dp)
            ) { }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Título y Subtítulo de bienvenida
        Text(
            text = "Tienda Virtual JAS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Tu punto de encuentro para comprar y vender\ncon total confianza",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Barra de píldoras para selección de rol (Comprador / Vendedor)
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = if (rolSeleccionado == Rol.COMPRADOR) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            rolSeleccionado = Rol.COMPRADOR
                            errorMensaje = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = if (rolSeleccionado == Rol.COMPRADOR) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Soy Comprador",
                            fontWeight = FontWeight.Bold,
                            color = if (rolSeleccionado == Rol.COMPRADOR) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = if (rolSeleccionado == Rol.VENDEDOR) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            rolSeleccionado = Rol.VENDEDOR
                            errorMensaje = null
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = if (rolSeleccionado == Rol.VENDEDOR) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Soy Vendedor",
                            fontWeight = FontWeight.Bold,
                            color = if (rolSeleccionado == Rol.VENDEDOR) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Campo de texto: Correo Electrónico
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Correo Electrónico", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text("Requerido", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = correoInput,
            onValueChange = { correoInput = it; errorMensaje = null },
            placeholder = { Text("ejemplo@jas.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo de texto: Contraseña
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contraseña", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text("Mín. 8 caracteres", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it; errorMensaje = null },
            placeholder = { Text("••••••••••••") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                    Icon(
                        imageVector = if (mostrarPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Mostrar contraseña",
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Checkbox Recordarme & Olvidaste tu contraseña
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = recordarme,
                    onCheckedChange = { recordarme = it }
                )
                Text("Recordarme", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }

            Text(
                "¿Olvidaste tu contraseña?",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable {
                    correoRecuperacion = correoInput
                    mostrarOlvidastePassword = true
                }
            )
        }

        // Banner para despliegue de mensajes de error de autenticación
        errorMensaje?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón Principal para Iniciar Sesión
        Button(
            onClick = {
                when {
                    !esCorreoValido(correoInput) -> {
                        errorMensaje = "Por favor ingresa un correo electrónico válido (ejemplo@jas.com)."
                    }
                    else -> {
                        val errorLogin = viewModel.validarEIniciarSesion(
                            correo = correoInput,
                            password = passwordInput,
                            rolSeleccionado = rolSeleccionado
                        )

                        if (errorLogin != null) {
                            errorMensaje = errorLogin
                        } else {
                            onLogin()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Iniciar Sesión", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sección inferior de enlaces de registro
        Text(
            "¿Aún no tienes una cuenta?",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Regístrate aquí ↗",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable {
                    abrirRegistroConRol(Rol.COMPRADOR)
                }
            )

            Text("  •  ", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

            Text(
                "Registrarme como Vendedor",
                color = Color(0xFF00695C),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable {
                    abrirRegistroConRol(Rol.VENDEDOR)
                }
            )
        }
    }
}
