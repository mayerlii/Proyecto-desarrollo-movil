package com.example.tienda_virtual.data.local

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tienda_virtual.archivos.Producto
import kotlinx.coroutines.flow.Flow

/**
 * Interface DAO (Data Access Object) para la entidad Producto en Room DB.
 *
 * Contiene las consultas SQL anotadas para el manejo del inventario y soporte para ContentProvider.
 */
@Dao
interface ProductoDao {

    /** Consulta todos los productos ordenados del más reciente al más antiguo (Flow para UI) */
    @Query("SELECT * FROM productos ORDER BY id DESC")
    fun obtenerTodosLosProductos(): Flow<List<Producto>>

    /** Consulta productos filtrados por ID de vendedor */
    @Query("SELECT * FROM productos WHERE vendedorId = :vendedorId ORDER BY id DESC")
    fun obtenerProductosPorVendedor(vendedorId: Int): Flow<List<Producto>>

    /** Obtiene un producto específico por su ID primario */
    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun obtenerProductoPorId(id: Int): Producto?

    /** Inserta o reemplaza un producto en la base de datos */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarProducto(producto: Producto): Long

    /** Actualiza la información o el stock de un producto existente */
    @Update
    suspend fun actualizarProducto(producto: Producto)

    /** Elimina un producto de la base de datos */
    @Delete
    suspend fun eliminarProducto(producto: Producto)

    // =========================================================================
    // CONSULTAS SÍNCRONAS CON CURSOR PARA CONTENTPROVIDER
    // =========================================================================

    /** Retorna un Cursor con todos los productos para el ContentProvider */
    @Query("SELECT * FROM productos ORDER BY id DESC")
    fun obtenerTodosLosProductosCursor(): Cursor

    /** Retorna un Cursor con un producto específico por ID para el ContentProvider */
    @Query("SELECT * FROM productos WHERE id = :id")
    fun obtenerProductoPorIdCursor(id: Long): Cursor

    /** Inserta un producto de forma síncrona para el ContentProvider */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarProductoSync(producto: Producto): Long

    /** Actualiza un producto de forma síncrona para el ContentProvider */
    @Update
    fun actualizarProductoSync(producto: Producto): Int

    /** Elimina un producto por ID de forma síncrona para el ContentProvider */
    @Query("DELETE FROM productos WHERE id = :id")
    fun eliminarProductoPorIdSync(id: Long): Int

    /** Elimina todos los productos de forma síncrona para el ContentProvider */
    @Query("DELETE FROM productos")
    fun eliminarTodosProductosSync(): Int
}
