package com.example.tienda_virtual.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tienda_virtual.data.local.AppDatabase
import com.example.tienda_virtual.archivos.CarritoItem
import com.example.tienda_virtual.archivos.Categoria
import com.example.tienda_virtual.archivos.EstadoPedido
import com.example.tienda_virtual.archivos.Pedido
import com.example.tienda_virtual.archivos.Producto
import com.example.tienda_virtual.archivos.Rol
import com.example.tienda_virtual.archivos.Usuario
import com.example.tienda_virtual.ui.utils.ImageUtils
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Modelo de cuenta registrada en el sistema.
 */
data class UsuarioCuenta(
    val nombre: String,
    val correo: String,
    val celular: String,
    val direccionEnvio: String,
    val password: String,
    val rol: Rol,
    var foto: String = ""
)

/**
 * Modelo de notificación exclusiva para vendedores.
 */
data class NotificacionVendedor(
    val vendedorCorreo: String,
    val mensaje: String
)

/**
 * ViewModel Central de la Aplicación "Tienda Virtual JAS".
 *
 * Administra el estado global de la aplicación, incluyendo:
 * - Persistencia de productos en Room DB (operaciones asíncronas con Coroutines y Flow).
 * - Registro y autenticación estricta con filtro de roles (Comprador vs. Vendedor).
 * - Persistencia de cuentas de usuario y fotos de perfil en SharedPreferences.
 * - Alternancia de tema gráfico (Modo Oscuro / Modo Claro).
 * - Gestión de Carrito de Compras, proceso Checkout y deducción automática de stock.
 * - Notificaciones exclusivas e independientes para Compradores y Vendedores.
 */
class TiendaViewModel(application: Application) : AndroidViewModel(application) {

    // Instancia DAO para operaciones CRUD en la base de datos Room
    private val productoDao = AppDatabase.getDatabase(application).productoDao()
    private val PREFS_TIENDA = "prefs_tienda_jas"
    private val KEY_USUARIOS_DATA = "key_usuarios_data_json"

    // Estado observable del usuario autenticado en la sesión activa
    var usuarioActual by mutableStateOf<Usuario?>(null)
        private set

    // Estado observable del tema activo (Modo Oscuro / Modo Claro)
    var esModoOscuro by mutableStateOf(false)

    /** Alterna el tema de la aplicación entre Modo Claro y Modo Oscuro */
    fun toggleModoOscuro() {
        esModoOscuro = !esModoOscuro
    }

    // Dirección de envío predeterminada / guardada
    var direccionEnvioGuardada by mutableStateOf("Av. Principal Los Naranjos, Torre B, Apto 4-B, Caracas")

    // Lista observable de cuentas de usuario registradas en la aplicación
    val usuariosRegistrados = mutableStateListOf(
        UsuarioCuenta(
            nombre = "Carlos Mendoza",
            correo = "ejemplo@gmail.com",
            celular = "3123456789",
            direccionEnvio = "Av. Principal Los Naranjos, Torre B, Apto 4-B, Caracas",
            password = "",
            rol = Rol.COMPRADOR,
            foto = ""
        ),
        UsuarioCuenta(
            nombre = "TechZone Oficial",
            correo = "vendedor@gmail.com",
            celular = "3000000000",
            direccionEnvio = "Oficinas Comerciales MercadoVirtual",
            password = "",
            rol = Rol.VENDEDOR,
            foto = ""
        )
    )

    // Listas observables para el sistema de notificaciones de Vendedor y Comprador
    val notificacionesVendedorList = mutableStateListOf<NotificacionVendedor>()
    var notificacionesNoLeidas by mutableIntStateOf(0)

    val notificacionesComprador = mutableStateListOf<String>()
    var notificacionesCompradorNoLeidas by mutableIntStateOf(0)

    // Lista de categorías disponibles en la tienda
    val categorias = mutableStateListOf(
        Categoria(1, "Tecnología"),
        Categoria(2, "Moda"),
        Categoria(3, "Hogar"),
        Categoria(4, "Deportes"),
        Categoria(5, "Belleza"),
    )

    // Listas observables del catálogo de productos, carrito de compras y pedidos
    val productos = mutableStateListOf<Producto>()
    val carrito = mutableStateListOf<CarritoItem>()
    val pedidos = mutableStateListOf<Pedido>()

    private var siguientePedidoId = 1

    init {
        // Carga cuentas guardadas desde SharedPreferences al iniciar la aplicación
        cargarUsuariosGuardados()
        
        // Recolecta en tiempo real los productos almacenados en Room DB mediante Flow
        viewModelScope.launch {
            productoDao.obtenerTodosLosProductos().collect { listaProductos ->
                // Asegura que las fotos con URI temporal se migren a almacenamiento interno permanente
                listaProductos.forEach { prod ->
                    if (prod.foto.isNotBlank() && (prod.foto.startsWith("content://") || prod.foto.startsWith("data:image"))) {
                        val fotoPermanente = ImageUtils.procesarYGuardarFoto(getApplication(), prod.foto)
                        if (fotoPermanente != prod.foto && !fotoPermanente.startsWith("content://")) {
                            prod.foto = fotoPermanente
                            viewModelScope.launch {
                                productoDao.actualizarProducto(prod)
                            }
                        }
                    }
                }
                productos.clear()
                productos.addAll(listaProductos)
            }
        }
    }

    /**
     * Guarda la lista de usuarios y fotos de perfil en SharedPreferences.
     */
    private fun guardarUsuariosEnPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_TIENDA, Context.MODE_PRIVATE)
        val sb = StringBuilder()
        usuariosRegistrados.forEach { u ->
            sb.append("${u.nombre}|${u.correo}|${u.celular}|${u.direccionEnvio}|${u.password}|${u.rol.name}|${u.foto}\n")
        }
        prefs.edit().putString(KEY_USUARIOS_DATA, sb.toString()).apply()
    }

    /**
     * Recupera y restaura la lista de usuarios registrados desde SharedPreferences.
     */
    private fun cargarUsuariosGuardados() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences(PREFS_TIENDA, Context.MODE_PRIVATE)
            val data = prefs.getString(KEY_USUARIOS_DATA, null) ?: return
            if (data.isNotBlank()) {
                val lineas = data.split("\n")
                lineas.forEach { linea ->
                    if (linea.isNotBlank()) {
                        val partes = linea.split("|")
                        if (partes.size >= 6) {
                            val correoLimpio = partes[1].trim().lowercase()
                            val fotoCargada = if (partes.size >= 7) partes[6] else ""
                            val idx = usuariosRegistrados.indexOfFirst { it.correo.equals(correoLimpio, ignoreCase = true) }
                            if (idx != -1) {
                                usuariosRegistrados[idx] = usuariosRegistrados[idx].copy(foto = fotoCargada)
                            } else {
                                usuariosRegistrados.add(
                                    UsuarioCuenta(
                                        nombre = partes[0],
                                        correo = correoLimpio,
                                        celular = partes[2],
                                        direccionEnvio = partes[3],
                                        password = partes[4],
                                        rol = if (partes[5] == "VENDEDOR") Rol.VENDEDOR else Rol.COMPRADOR,
                                        foto = fotoCargada
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Manejo seguro para evitar cierres de la app ante datos corruptos
        }
    }

    /**
     * Registra una nueva cuenta de usuario validando correo único y rol seleccionado.
     */
    fun registrarUsuario(
        nombre: String,
        correo: String,
        celular: String,
        direccionEnvio: String,
        password: String,
        rol: Rol
    ): String? {
        val correoLimpio = correo.trim().lowercase()
        val existente = usuariosRegistrados.find { it.correo.equals(correoLimpio, ignoreCase = true) }
        if (existente != null) {
            return "El correo '$correoLimpio' ya se encuentra registrado. Por favor inicia sesión o usa otro correo."
        }

        val nuevaCuenta = UsuarioCuenta(
            nombre = nombre.ifBlank { if (rol == Rol.VENDEDOR) "Vendedor JAS" else "Comprador JAS" },
            correo = correoLimpio,
            celular = celular.ifBlank { "3000000000" },
            direccionEnvio = direccionEnvio.ifBlank { "Calle 10 # 5-20, Nariño" },
            password = password.ifBlank { "password123" },
            rol = rol,
            foto = ""
        )
        usuariosRegistrados.add(nuevaCuenta)
        guardarUsuariosEnPrefs()

        iniciarSesionConCuenta(nuevaCuenta)
        return null
    }

    /**
     * Valida credenciales de acceso y restringe el inicio de sesión si el rol no coincide.
     */
    fun validarEIniciarSesion(
        correo: String,
        password: String,
        rolSeleccionado: Rol
    ): String? {
        val correoLimpio = correo.trim().lowercase()

        val cuentaPorCorreo = usuariosRegistrados.find { it.correo.equals(correoLimpio, ignoreCase = true) }

        if (cuentaPorCorreo == null) {
            return "El usuario no existe o el correo ingresado no está registrado."
        }

        if (cuentaPorCorreo.password != password) {
            return "Contraseña incorrecta. Por favor verifique e intente nuevamente."
        }

        if (cuentaPorCorreo.rol != rolSeleccionado) {
            val rolCorrectoTexto = if (cuentaPorCorreo.rol == Rol.VENDEDOR) "Vendedor" else "Comprador"
            return "Este correo está registrado como '$rolCorrectoTexto'. Por favor selecciona 'Soy $rolCorrectoTexto' para iniciar sesión."
        }

        iniciarSesionConCuenta(cuentaPorCorreo)
        return null
    }

    /**
     * Establece el usuario autenticado activo en la sesión.
     */
    private fun iniciarSesionConCuenta(cuenta: UsuarioCuenta) {
        direccionEnvioGuardada = cuenta.direccionEnvio
        usuarioActual = Usuario(
            id = if (cuenta.rol == Rol.VENDEDOR) 1 else 2,
            nombre = cuenta.nombre,
            correo = cuenta.correo,
            celular = cuenta.celular,
            direccionEnvio = cuenta.direccionEnvio,
            rol = cuenta.rol,
            foto = cuenta.foto
        )
    }

    fun registrarOUIniciarSesion(
        nombre: String,
        correo: String,
        celular: String = "",
        direccionEnvio: String = "",
        rol: Rol
    ) {
        registrarUsuario(nombre, correo, celular, direccionEnvio, "password123", rol)
    }

    fun iniciarSesion(correo: String, rol: Rol) {
        registrarOUIniciarSesion(
            nombre = "",
            correo = correo,
            celular = "3000000000",
            direccionEnvio = direccionEnvioGuardada,
            rol = rol
        )
    }

    /** Cierra la sesión activa y limpia el carrito de compras */
    fun cerrarSesion() {
        usuarioActual = null
        carrito.clear()
    }

    /**
     * Actualiza el perfil del usuario activo y persiste los cambios de foto en SharedPreferences.
     */
    fun actualizarPerfil(
        nombre: String,
        correo: String,
        celular: String,
        foto: String = ""
    ) {
        usuarioActual?.let {
            it.nombre = nombre
            it.correo = correo
            it.celular = celular
            it.foto = foto
        }

        val correoLimpio = usuarioActual?.correo?.lowercase() ?: ""
        val idx = usuariosRegistrados.indexOfFirst { it.correo.equals(correoLimpio, ignoreCase = true) }
        if (idx != -1) {
            val actual = usuariosRegistrados[idx]
            usuariosRegistrados[idx] = actual.copy(
                nombre = nombre,
                correo = correo,
                celular = celular,
                foto = foto
            )
            guardarUsuariosEnPrefs()
        }

        usuarioActual = usuarioActual?.copy()
    }

    /** Actualiza la dirección de envío del comprador en memoria */
    fun actualizarDireccionEnvio(nuevaDireccion: String) {
        direccionEnvioGuardada = nuevaDireccion
        usuarioActual?.direccionEnvio = nuevaDireccion
        usuarioActual = usuarioActual?.copy()
    }

    /**
     * Inserta un nuevo producto asignado al vendedor activo en Room DB.
     */
    fun agregarProducto(
        nombre: String,
        foto: String,
        categoria: String,
        precio: Double,
        descripcion: String,
        stock: Int = 10
    ) {
        val vendedor = usuarioActual ?: return

        viewModelScope.launch {
            val fotoPermanente = ImageUtils.procesarYGuardarFoto(getApplication(), foto)
            val nuevoProducto = Producto(
                id = 0,
                nombre = nombre,
                foto = fotoPermanente,
                categoria = categoria,
                precio = precio,
                descripcion = descripcion,
                vendedorId = vendedor.id,
                vendedorNombre = vendedor.nombre,
                vendedorCorreo = vendedor.correo,
                stock = stock
            )
            productoDao.insertarProducto(nuevoProducto)
        }
    }

    /** Actualiza las propiedades de un producto en Room DB */
    fun actualizarProducto(producto: Producto) {
        viewModelScope.launch {
            productoDao.actualizarProducto(producto)
        }
    }

    /** Elimina un producto de Room DB y lo quita del carrito de compras */
    fun eliminarProducto(producto: Producto) {
        viewModelScope.launch {
            productoDao.eliminarProducto(producto)
            carrito.removeAll { it.producto.id == producto.id }
        }
    }

    /** Agrega un producto al carrito de compras o incrementa su cantidad */
    fun agregarAlCarrito(producto: Producto) {
        val existente = carrito.find {
            it.producto.id == producto.id
        }

        if (existente != null) {
            existente.cantidad++
        } else {
            carrito.add(
                CarritoItem(
                    producto = producto,
                    cantidad = 1
                )
            )
        }
    }

    /** Incrementa las unidades de un producto en el carrito */
    fun aumentarCantidad(productoId: Int) {
        carrito.find {
            it.producto.id == productoId
        }?.let {
            it.cantidad++
        }
    }

    /** Decrementa las unidades de un producto en el carrito */
    fun disminuirCantidad(productoId: Int) {
        val item = carrito.find {
            it.producto.id == productoId
        }

        if (item != null) {
            if (item.cantidad > 1) {
                item.cantidad--
            } else {
                carrito.remove(item)
            }
        }
    }

    /** Retorna el total de ítems agregados al carrito */
    fun cantidadCarrito(): Int {
        return carrito.sumOf { it.cantidad }
    }

    /** Calcula la suma exacta del valor de los productos en el carrito */
    fun totalCarrito(): Double {
        return carrito.sumOf {
            it.producto.precio * it.cantidad
        }
    }

    /**
     * Confirma la compra: crea el pedido, descuenta el stock en Room DB y genera notificaciones exclusivas.
     */
    fun realizarCompra() {
        val comprador = usuarioActual ?: return

        if (carrito.isEmpty()) return

        val nuevoPedido = Pedido(
            id = siguientePedidoId++,
            compradorId = comprador.id,
            productos = carrito.map {
                CarritoItem(
                    producto = it.producto,
                    cantidad = it.cantidad
                )
            },
            total = totalCarrito(),
            estado = EstadoPedido.SOLICITUD_RECIBIDA
        )

        pedidos.add(nuevoPedido)

        // Descuenta el stock correspondiente en la base de datos Room DB
        carrito.forEach { item ->
            val p = item.producto
            val nuevoStock = (p.stock - item.cantidad).coerceAtLeast(0)
            p.stock = nuevoStock
            viewModelScope.launch {
                productoDao.actualizarProducto(p)
            }
        }

        // Agrupa por vendedor para enviar notificaciones de compra exclusivas
        val vendedoresAgrupados = carrito.groupBy {
            if (it.producto.vendedorCorreo.isNotBlank()) it.producto.vendedorCorreo else "vendedor@jas.com"
        }

        vendedoresAgrupados.forEach { (vendedorEmail, itemsDeEsteVendedor) ->
            val subtotalVendedor = itemsDeEsteVendedor.sumOf { it.producto.precio * it.cantidad }
            notificacionesVendedorList.add(
                0,
                NotificacionVendedor(
                    vendedorCorreo = vendedorEmail,
                    mensaje = "📦 ¡Nuevo pedido #${nuevoPedido.id} de ${comprador.nombre}! Total de tus productos: $${String.format(Locale.US, "%.2f", subtotalVendedor)}"
                )
            )
        }

        notificacionesNoLeidas++
        carrito.clear()
    }

    fun marcarNotificacionesLeidas() {
        notificacionesNoLeidas = 0
    }

    fun marcarNotificacionesCompradorLeidas() {
        notificacionesCompradorNoLeidas = 0
    }

    /**
     * Cambia el estado de un pedido y genera una notificación en tiempo real para el comprador.
     */
    fun cambiarEstadoPedido(
        pedido: Pedido,
        estado: EstadoPedido
    ) {
        pedido.estado = estado
        val estadoTexto = when (estado) {
            EstadoPedido.SOLICITUD_RECIBIDA -> "Solicitud Recibida"
            EstadoPedido.EN_PREPARACION -> "En Preparación"
            EstadoPedido.ENTREGADO -> "Entregado"
            EstadoPedido.CANCELADO -> "Cancelado"
        }
        notificacionesComprador.add(0, "📦 Cambió el estado de tu Pedido #${pedido.id} a: '$estadoTexto'")
        notificacionesCompradorNoLeidas++
    }
}
