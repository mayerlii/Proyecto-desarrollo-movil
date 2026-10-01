package com.example.app_b.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_b.data.ProductoModel
import com.example.app_b.data.ProductoRepository
import com.example.app_b.data.ResultadoConsulta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona la carga y el estado del catálogo de productos.
 * Mantiene las operaciones de ContentResolver fuera de los composables y en un hilo secundario (IO).
 */
class ProductosViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ProductoRepository = ProductoRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<CatalogoEstado>(CatalogoEstado.Cargando)
    val uiState: StateFlow<CatalogoEstado> = _uiState.asStateFlow()

    var filtroBusqueda by mutableStateOf("")
        private set

    init {
        cargarProductos()
    }

    fun onFiltroChanged(nuevoFiltro: String) {
        filtroBusqueda = nuevoFiltro
    }

    /**
     * Realiza la consulta de lectura al ContentProvider en Dispatchers.IO
     * y actualiza el estado de la UI según el resultado.
     */
    fun cargarProductos() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = CatalogoEstado.Cargando

            val resultado = repository.consultarCatalogo(getApplication())

            _uiState.value = when (resultado) {
                is ResultadoConsulta.Exito -> {
                    if (resultado.productos.isEmpty()) {
                        CatalogoEstado.Vacio
                    } else {
                        CatalogoEstado.Exito(resultado.productos)
                    }
                }
                is ResultadoConsulta.Error.PermisoOFirma -> {
                    CatalogoEstado.Error.PermisoOFirma(
                        mensaje = resultado.mensaje,
                        detalle = resultado.detalle
                    )
                }
                is ResultadoConsulta.Error.AppNoInstalada -> {
                    CatalogoEstado.Error.ProviderNoDisponible(
                        mensaje = resultado.mensaje,
                        detalle = resultado.detalle
                    )
                }
                is ResultadoConsulta.Error.ErrorInesperado -> {
                    CatalogoEstado.Error.ErrorInesperado(
                        mensaje = resultado.mensaje,
                        detalle = resultado.detalle
                    )
                }
            }
        }
    }

    /**
     * Acción para reintentar la consulta ante errores o catálogo vacío.
     */
    fun reintentar() {
        cargarProductos()
    }

    /**
     * Filtra los productos según el término de búsqueda ingresado por el usuario.
     */
    fun filtrarProductos(productos: List<ProductoModel>): List<ProductoModel> {
        val query = filtroBusqueda.trim()
        if (query.isBlank()) return productos
        return productos.filter { p ->
            p.nombre.contains(query, ignoreCase = true) ||
            p.categoria.contains(query, ignoreCase = true) ||
            p.vendedorNombre.contains(query, ignoreCase = true) ||
            p.descripcion.contains(query, ignoreCase = true)
        }
    }
}
