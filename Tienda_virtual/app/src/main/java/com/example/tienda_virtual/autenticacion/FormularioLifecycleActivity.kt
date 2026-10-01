package com.example.tienda_virtual.autenticacion

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tienda_virtual.ui.theme.Tienda_virtualTheme

/**
 * Actividad Autónoma de Registro Multi-Paso sin ViewModel (Demostración de Ciclo de Vida en Android).
 *
 * Implementa explícitamente los métodos del ciclo de vida nativo de Android:
 * - [onSaveInstanceState]: Preserva en un objeto Bundle el estado del formulario, campos de texto, foco del cursor y temporizador.
 * - [onRestoreInstanceState] y [onCreate]: Recuperan explícitamente los datos desde el Bundle tras la rotación o recreación de la pantalla.
 * - [onPause] y [onStop]: Guardan borradores persistentes en SharedPreferences y pausan el temporizador de sesión.
 * - [onResume]: Reanuda la ejecución del temporizador exactamente desde el segundo acumulado previo.
 */
class FormularioLifecycleActivity : ComponentActivity() {

    companion object {
        private const val TAG = "FormularioLifecycle"
        private const val PREFS_NAME = "borradores_lifecycle_prefs"

        // Claves constantes para el objeto Bundle y SharedPreferences
        private const val KEY_PASO = "key_paso_actual"
        private const val KEY_NOMBRE = "key_nombre_usuario"
        private const val KEY_CORREO = "key_correo_usuario"
        private const val KEY_TELEFONO = "key_telefono_usuario"
        private const val KEY_DIRECCION = "key_direccion_usuario"
        private const val KEY_NOTAS = "key_notas_extensas"
        private const val KEY_SEGUNDOS = "key_segundos_temporizador"
        private const val KEY_TEMPO_ACTIVO = "key_temporizador_activo"
        private const val KEY_FOCO_CAMPO = "key_foco_campo_cursor"
    }

    // Variables de estado local administradas directamente por la Actividad sin ViewModel
    private var pasoActual by mutableIntStateOf(1)
    private var nombre by mutableStateOf("")
    private var correo by mutableStateOf("")
    private var telefono by mutableStateOf("")
    private var direccion by mutableStateOf("")
    private var notasExtensas by mutableStateOf("")
    private var segundosTemporizador by mutableIntStateOf(0)
    private var temporizadorActivo by mutableStateOf(true)
    private var focoCampoActual by mutableStateOf("nombre")

    // Handler en el hilo principal para el conteo en tiempo real del temporizador
    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (temporizadorActivo) {
                segundosTemporizador++
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "onCreate invocado sin ViewModel.")

        // Si el estado previo guardado en Bundle existe (ej. por rotación de pantalla), se recupera explícitamente
        if (savedInstanceState != null) {
            recuperarEstadoDesdeBundle(savedInstanceState)
            Toast.makeText(this, "Estado del formulario y temporizador restaurado desde Bundle (onCreate)", Toast.LENGTH_SHORT).show()
        } else {
            cargarBorradorPersistente()
        }

        iniciarTemporizador()

        setContent {
            Tienda_virtualTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FormularioLifecycleContent(
                        pasoActual = pasoActual,
                        nombre = nombre,
                        correo = correo,
                        telefono = telefono,
                        direccion = direccion,
                        notasExtensas = notasExtensas,
                        segundos = segundosTemporizador,
                        temporizadorActivo = temporizadorActivo,
                        focoCampoActual = focoCampoActual,
                        onPasoChange = { pasoActual = it },
                        onNombreChange = { nombre = it },
                        onCorreoChange = { correo = it },
                        onTelefonoChange = { telefono = it },
                        onDireccionChange = { direccion = it },
                        onNotasChange = { notasExtensas = it },
                        onFocoChange = { focoCampoActual = it },
                        onToggleTimer = {
                            temporizadorActivo = !temporizadorActivo
                            if (temporizadorActivo) iniciarTemporizador()
                        },
                        onResetTimer = { segundosTemporizador = 0 },
                        onFinalizar = {
                            guardarBorradorPersistente()
                            Toast.makeText(this, "Formulario completado y borrador guardado.", Toast.LENGTH_LONG).show()
                            finish()
                        },
                        onVolver = { finish() }
                    )
                }
            }
        }
    }

    /**
     * Reanuda la ejecución del temporizador al volver a primer plano desde el tiempo acumulado.
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume invocado. Reanudando temporizador desde tiempo previo ($segundosTemporizador seg)...")
        cargarBorradorPersistente()
        iniciarTemporizador()
    }

    /**
     * Guarda el estado completo en el objeto Bundle antes de que la Actividad sea destruida o pausada.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState y onStop capturados. Preservando tiempo actual ($segundosTemporizador seg) en Bundle...")

        outState.putInt(KEY_PASO, pasoActual)
        outState.putString(KEY_NOMBRE, nombre)
        outState.putString(KEY_CORREO, correo)
        outState.putString(KEY_TELEFONO, telefono)
        outState.putString(KEY_DIRECCION, direccion)
        outState.putString(KEY_NOTAS, notasExtensas)
        outState.putInt(KEY_SEGUNDOS, segundosTemporizador)
        outState.putBoolean(KEY_TEMPO_ACTIVO, temporizadorActivo)
        outState.putString(KEY_FOCO_CAMPO, focoCampoActual)
    }

    /**
     * Recupera explícitamente los datos desde el Bundle tras la recreación de la pantalla.
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState invocado explícito para recuperación de datos.")
        recuperarEstadoDesdeBundle(savedInstanceState)
    }

    /**
     * Guarda un borrador persistente en SharedPreferences al pausar la Actividad.
     */
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause invocado. Guardando borrador y tiempo acumulado ($segundosTemporizador seg) en SharedPreferences...")
        guardarBorradorPersistente()
    }

    /**
     * Detiene el Runnable del temporizador y persiste el estado completo al detener la Actividad.
     */
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop capturado. Deteniendo temporizador y guardando estado ($segundosTemporizador seg) en SharedPreferences...")
        handler.removeCallbacks(timerRunnable)
        guardarBorradorPersistente()
    }

    /** Extrae y restaura los valores del objeto Bundle */
    private fun recuperarEstadoDesdeBundle(bundle: Bundle) {
        pasoActual = bundle.getInt(KEY_PASO, 1)
        nombre = bundle.getString(KEY_NOMBRE, "") ?: ""
        correo = bundle.getString(KEY_CORREO, "") ?: ""
        telefono = bundle.getString(KEY_TELEFONO, "") ?: ""
        direccion = bundle.getString(KEY_DIRECCION, "") ?: ""
        notasExtensas = bundle.getString(KEY_NOTAS, "") ?: ""
        segundosTemporizador = bundle.getInt(KEY_SEGUNDOS, 0)
        temporizadorActivo = bundle.getBoolean(KEY_TEMPO_ACTIVO, true)
        focoCampoActual = bundle.getString(KEY_FOCO_CAMPO, "nombre") ?: "nombre"
    }

    /** Guarda las variables actuales en SharedPreferences */
    private fun guardarBorradorPersistente() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_PASO, pasoActual)
            putString(KEY_NOMBRE, nombre)
            putString(KEY_CORREO, correo)
            putString(KEY_TELEFONO, telefono)
            putString(KEY_DIRECCION, direccion)
            putString(KEY_NOTAS, notasExtensas)
            putInt(KEY_SEGUNDOS, segundosTemporizador)
            putBoolean(KEY_TEMPO_ACTIVO, temporizadorActivo)
            putString(KEY_FOCO_CAMPO, focoCampoActual)
            apply()
        }
    }

    /** Carga el borrador previo almacenado en SharedPreferences */
    private fun cargarBorradorPersistente() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        pasoActual = prefs.getInt(KEY_PASO, 1)
        nombre = prefs.getString(KEY_NOMBRE, "") ?: ""
        correo = prefs.getString(KEY_CORREO, "") ?: ""
        telefono = prefs.getString(KEY_TELEFONO, "") ?: ""
        direccion = prefs.getString(KEY_DIRECCION, "") ?: ""
        notasExtensas = prefs.getString(KEY_NOTAS, "") ?: ""
        val segundosGuardados = prefs.getInt(KEY_SEGUNDOS, 0)
        if (segundosGuardados > 0) {
            segundosTemporizador = segundosGuardados
        }
        temporizadorActivo = prefs.getBoolean(KEY_TEMPO_ACTIVO, true)
        focoCampoActual = prefs.getString(KEY_FOCO_CAMPO, "nombre") ?: "nombre"
    }

    /** Inicia o reanuda el conteo por segundo del temporizador */
    private fun iniciarTemporizador() {
        handler.removeCallbacks(timerRunnable)
        if (temporizadorActivo) {
            handler.post(timerRunnable)
        }
    }
}

/**
 * Contenido Gráfico en Jetpack Compose para la Pantalla de Ciclo de Vida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioLifecycleContent(
    pasoActual: Int,
    nombre: String,
    correo: String,
    telefono: String,
    direccion: String,
    notasExtensas: String,
    segundos: Int,
    temporizadorActivo: Boolean,
    focoCampoActual: String,
    onPasoChange: (Int) -> Unit,
    onNombreChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onDireccionChange: (String) -> Unit,
    onNotasChange: (String) -> Unit,
    onFocoChange: (String) -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onFinalizar: () -> Unit,
    onVolver: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro Multi-Paso (Ciclo de Vida)") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tarjeta de estado del temporizador activo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Temporizador de Sesión (Sin ViewModel)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("${segundos / 60}m ${segundos % 60}s ($segundos segundos) • ${if (temporizadorActivo) "En reproducción" else "Pausado"}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                    }

                    Row {
                        TextButton(onClick = onToggleTimer) {
                            Text(if (temporizadorActivo) "Pausar" else "Reanudar")
                        }
                        TextButton(onClick = onResetTimer) {
                            Text("Reset")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indicador de paso actual y foco del cursor activo
            Text(
                "Paso $pasoActual de 3 • Foco Activo: $focoCampoActual",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Renderizado dinámico del paso actual
            when (pasoActual) {
                1 -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Paso 1: Datos Personales Básicos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = nombre,
                                onValueChange = onNombreChange,
                                label = { Text("Nombre Completo") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.isFocused) onFocoChange("Nombre Completo") },
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = correo,
                                onValueChange = onCorreoChange,
                                label = { Text("Correo Electrónico") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.isFocused) onFocoChange("Correo Electrónico") },
                                singleLine = true
                            )
                        }
                    }
                }
                2 -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Paso 2: Datos de Contacto y Ubicación", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = telefono,
                                onValueChange = onTelefonoChange,
                                label = { Text("Teléfono / Celular") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.isFocused) onFocoChange("Teléfono / Celular") },
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = direccion,
                                onValueChange = onDireccionChange,
                                label = { Text("Dirección de Envío") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.isFocused) onFocoChange("Dirección de Envío") },
                                singleLine = true
                            )
                        }
                    }
                }
                else -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Paso 3: Campos Extensos y Borrador", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = notasExtensas,
                                onValueChange = onNotasChange,
                                label = { Text("Notas Extensas / Observaciones del Registro") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .onFocusChanged { if (it.isFocused) onFocoChange("Notas / Observaciones") }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botones de navegación Anterior / Siguiente / Finalizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pasoActual > 1) {
                    OutlinedButton(onClick = { onPasoChange(pasoActual - 1) }) {
                        Text("Anterior")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (pasoActual < 3) {
                    Button(onClick = { onPasoChange(pasoActual + 1) }) {
                        Text("Siguiente")
                    }
                } else {
                    Button(onClick = onFinalizar) {
                        Text("Finalizar y Guardar")
                    }
                }
            }
        }
    }
}
