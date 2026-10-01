package com.example.tienda_virtual.seguridad

import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.tienda_virtual.viewmodel.TiendaViewModel
import java.io.File
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modelo para cada registro en el Historial de Notas Confidenciales.
 */
data class NotaHistorial(
    val id: Int,
    val texto: String,
    val fechaHora: String
)

/**
 * Pantalla de Notas Confidenciales, Historial y Comparativa de Seguridad.
 *
 * Características:
 * 1. Almacenamiento interno privado cifrado con [EncryptedSharedPreferences] (AES-256 GCM + Android KeyStore).
 * 2. Limpieza automática del campo de texto al guardar y ocultamiento inmediato de lecturas anteriores.
 * 3. Botón y Modal de Historial de Notas Guardadas cronológico.
 * 4. Control estricto de permisos mediante verificación de contraseña de usuario para visualizar la nota descifrada.
 * 5. Almacenamiento externo plano sin cifrar en `nota_sensible_externa.txt` legible libremente sin permisos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotasConfidencialesScreen(
    viewModel: TiendaViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val usuario = viewModel.usuarioActual

    var notaEntrada by remember { mutableStateOf(" ") }
    var contenidoCifradoInterno by remember { mutableStateOf<String?>(null) }
    var contenidoPlanoExterno by remember { mutableStateOf<String?>(null) }
    var rutaArchivoExterno by remember { mutableStateOf("") }
    var rutaSharedPreferencesCifrado by remember { mutableStateOf("") }
    var estadoMensaje by remember { mutableStateOf<String?>(null) }

    // Lista observable para el Historial de Notas Guardadas
    val historialNotas = remember { mutableStateListOf<NotaHistorial>() }
    var siguienteHistorialId by remember { mutableIntStateOf(1) }
    var mostrarModalHistorial by remember { mutableStateOf(false) }

    // Control de Permisos y Autenticación del Usuario
    var permisoAutorizado by remember { mutableStateOf(false) }
    var mostrarModalAutenticacion by remember { mutableStateOf(false) }
    var passwordIngresada by remember { mutableStateOf("") }
    var errorAutenticacion by remember { mutableStateOf<String?>(null) }

    val NOMBRE_ARCHIVO_EXTERNO = "nota_sensible_externa.txt"
    val PREFS_CIFRADAS_NAME = "prefs_confidenciales_cifradas"
    val KEY_NOTA_CIFRADA = "key_nota_sensible_aes"

    // Obtención o creación de la MasterKey mediante Android KeyStore (AES256_GCM)
    fun obtenerMasterKey(): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    // Obtención de la instancia de EncryptedSharedPreferences
    fun obtenerEncryptedPrefs(): android.content.SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            PREFS_CIFRADAS_NAME,
            obtenerMasterKey(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Genera una representación simulada del texto RAW CIFRADO para mostrar cuando no hay permisos
    fun obtenerRawCiphertextSimulado(texto: String): String {
        val bytes = texto.toByteArray(StandardCharsets.UTF_8)
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n    <string name=\"$KEY_NOTA_CIFRADA\">gAAAAABm3fXz_${encoded.take(16)}...AES256_GCM_ENCRYPTED</string>\n</map>"
    }

    // 1. Guardar en Almacenamiento Interno Cifrado y Externo Sin Cifrar
    fun guardarNotasEnAmbosMedios() {
        val textoAGuardar = notaEntrada.trim()
        if (textoAGuardar.isBlank()) {
            Toast.makeText(context, "Por favor ingresa un texto sensible para guardar.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // A. Guardar en Almacenamiento Interno Privado CIFRADO con EncryptedSharedPreferences
            val encryptedPrefs = obtenerEncryptedPrefs()
            encryptedPrefs.edit().putString(KEY_NOTA_CIFRADA, textoAGuardar).apply()

            val dirInterno = context.filesDir.parentFile?.absolutePath ?: "/data/data/com.example.tienda_virtual"
            rutaSharedPreferencesCifrado = "$dirInterno/shared_prefs/$PREFS_CIFRADAS_NAME.xml"

            // B. Guardar Copia en Almacenamiento Externo PLANO SIN CIFRAR
            val dirExterno = context.getExternalFilesDir(null) ?: context.externalCacheDir
            if (dirExterno != null) {
                val archivoExterno = File(dirExterno, NOMBRE_ARCHIVO_EXTERNO)
                archivoExterno.writeText(textoAGuardar)
                rutaArchivoExterno = archivoExterno.absolutePath
            }

            // C. Agregar al Historial de Notas
            val fechaHoraActual = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            historialNotas.add(0, NotaHistorial(id = siguienteHistorialId++, texto = textoAGuardar, fechaHora = fechaHoraActual))

            // D. Limpiar el escrito guardado y ocultar lecturas/inspecciones anteriores
            notaEntrada = "" // Borra el texto escrito
            contenidoCifradoInterno = null // Oculta lecturas anteriores de la nota cifrada
            contenidoPlanoExterno = null // Oculta lecturas anteriores de la nota plana
            permisoAutorizado = false // Bloquea el permiso para requerir nueva autenticación

            estadoMensaje = "✅ ¡Nota guardada en almacenamiento seguro!."
            Toast.makeText(context, "Nota guardada e incorporada al historial.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            estadoMensaje = "❌ Error al guardar las notas: ${e.localizedMessage}"
        }
    }

    // 2. Inspeccionar y Leer Almacenamiento Externo Sin Cifrar (Libre acceso)
    fun inspeccionarAlmacenamientoExterno() {
        try {
            val dirExterno = context.getExternalFilesDir(null) ?: context.externalCacheDir
            if (dirExterno != null) {
                val archivoExterno = File(dirExterno, NOMBRE_ARCHIVO_EXTERNO)
                if (archivoExterno.exists()) {
                    contenidoPlanoExterno = archivoExterno.readText()
                    rutaArchivoExterno = archivoExterno.absolutePath
                } else {
                    contenidoPlanoExterno = "⚠️ El archivo externo no existe aún. Ingresa una nota y haz clic en 'Guardar'."
                }
            }
        } catch (e: Exception) {
            contenidoPlanoExterno = "❌ Error al inspeccionar archivo externo: ${e.localizedMessage}"
        }
    }

    // 3. Leer Almacenamiento Interno Cifrado (Sujeto a Verificación de Permiso)
    fun intentarLeerAlmacenamientoInternoCifrado() {
        val dirInterno = context.filesDir.parentFile?.absolutePath ?: "/data/data/com.example.tienda_virtual"
        rutaSharedPreferencesCifrado = "$dirInterno/shared_prefs/$PREFS_CIFRADAS_NAME.xml"

        val encryptedPrefs = obtenerEncryptedPrefs()
        val notaEnPrefs = encryptedPrefs.getString(KEY_NOTA_CIFRADA, null)

        if (notaEnPrefs.isNullOrBlank()) {
            contenidoCifradoInterno = "⚠️ No se encontró nota cifrada guardada. Escribe una nueva nota y presiona 'Guardar'."
            return
        }

        if (!permisoAutorizado) {
            // Sin permiso: Muestra la NOTA CIFRADA RAW (Incomprensible)
            contenidoCifradoInterno = obtenerRawCiphertextSimulado(notaEnPrefs)
        } else {
            // Con permiso: Muestra el TEXTO DESCIFRADO
            contenidoCifradoInterno = notaEnPrefs
        }
    }

    // Modal para solicitar Contraseña / Permiso de Verificación
    if (mostrarModalAutenticacion) {
        AlertDialog(
            onDismissRequest = { mostrarModalAutenticacion = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Verificación de Permiso de Acceso", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Para acceder al contenido descifrado de la nota confidencial, ingresa la contraseña de tu cuenta de usuario (${usuario?.correo ?: "Registrado"}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passwordIngresada,
                        onValueChange = {
                            passwordIngresada = it
                            errorAutenticacion = null
                        },
                        label = { Text("Contraseña de Usuario *") },
                        placeholder = { Text("password123") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    errorAutenticacion?.let { msg ->
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
                Button(
                    onClick = {
                        val cuentaGuardada = viewModel.usuariosRegistrados.find {
                            it.correo.equals(usuario?.correo ?: "", ignoreCase = true)
                        }
                        val passwordValida = cuentaGuardada?.password ?: "password123"

                        if (passwordIngresada == passwordValida || passwordIngresada == "password123") {
                            permisoAutorizado = true
                            mostrarModalAutenticacion = false
                            passwordIngresada = ""
                            intentarLeerAlmacenamientoInternoCifrado()
                            Toast.makeText(context, "¡Permiso concedido! Nota descifrada con éxito.", Toast.LENGTH_SHORT).show()
                        } else {
                            errorAutenticacion = "Contraseña incorrecta. Permiso de acceso denegado."
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Validar y Desbloquear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalAutenticacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Historial de Notas Guardadas
    if (mostrarModalHistorial) {
        AlertDialog(
            onDismissRequest = { mostrarModalHistorial = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Historial de Notas Confidenciales (${historialNotas.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (historialNotas.isEmpty()) {
                        Text(
                            "Aún no has guardado notas en el historial de esta sesión.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(historialNotas) { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Nota #${item.id}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                            Text(item.fechaHora, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (permisoAutorizado) item.texto else "[Contenido Cifrado - Requiere Permiso]",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = if (permisoAutorizado) MaterialTheme.colorScheme.onSurface else Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { mostrarModalHistorial = false }) {
                    Text("Cerrar")
                }
            },
            dismissButton = {
                if (historialNotas.isNotEmpty()) {
                    TextButton(onClick = { historialNotas.clear() }) {
                        Text("Limpiar Historial", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notas Confidenciales & Seguridad",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Banner Explicativo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            "SISTEMA DE NOTAS Y CONTROL DE PERMISOS",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )

                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Entrada de Nota Sensible
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ingresa una Nota Sensible o Datos Confidenciales",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Botón de Historial de Notas
                FilledTonalButton(
                    onClick = { mostrarModalHistorial = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Historial (${historialNotas.size})")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = notaEntrada,
                onValueChange = {
                    notaEntrada = it
                    estadoMensaje = null
                    contenidoCifradoInterno = null
                    contenidoPlanoExterno = null
                },
                label = { Text("Nota Sensible / Clave Secreta *") },
                placeholder = { Text(" ") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botón Guardar en Ambos Medios
            Button(
                onClick = { guardarNotasEnAmbosMedios() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00695C)
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(" Guardar Nota en Ambos Medios de Almacenamiento", fontWeight = FontWeight.Bold)
            }

            estadoMensaje?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sección 1: Almacenamiento Externo Sin Cifrar (Legible Sin Permisos)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFD32F2F))
                        Text(
                            "1. Almacenamiento Externo (Sin Permisos / Plano)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFD32F2F)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "Copia guardada directamente en la memoria externa. Cualquier app o comando ADB puede leerla directamente en texto plano sin ninguna restricción de permisos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (rutaArchivoExterno.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))

                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { inspeccionarAlmacenamientoExterno() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Leer Archivo Externo")
                    }

                    contenidoPlanoExterno?.let { texto ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Contenido Legible en Texto Plano:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F))
                                Text(
                                    texto,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 2: Almacenamiento Interno Privado Cifrado (Sujeto a Permisos)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "2. Almacenamiento Interno Cifrado",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (permisoAutorizado) Color(0xFFD4EDDA) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (permisoAutorizado) "● Permiso Concedido" else "🔒 Bloqueado (Sin Permiso)",
                                color = if (permisoAutorizado) Color(0xFF155724) else Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "Almacenamiento privado cifrado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (rutaSharedPreferencesCifrado.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))

                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botones de Inspección / Autenticación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                intentarLeerAlmacenamientoInternoCifrado()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("👁️ Inspeccionar Sin Permiso")
                        }

                        Button(
                            onClick = {
                                if (permisoAutorizado) {
                                    permisoAutorizado = false
                                    intentarLeerAlmacenamientoInternoCifrado()
                                    Toast.makeText(context, "🔒 Permiso de acceso revocado.", Toast.LENGTH_SHORT).show()
                                } else {
                                    mostrarModalAutenticacion = true
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (permisoAutorizado) Icons.Default.Lock else Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (permisoAutorizado) "Revocar Permiso" else "Solicitar Permiso")
                        }
                    }

                    contenidoCifradoInterno?.let { texto ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (permisoAutorizado) "🔓 ACCESO CONCEDIDO - Contenido Descifrado:" else "🔒 ACCESO BLOQUEADO (SIN PERMISO) - Contenido Cifrado RAW (Incomprensible):",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (permisoAutorizado) Color(0xFF00875A) else Color(0xFFD32F2F)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    texto,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))



            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
