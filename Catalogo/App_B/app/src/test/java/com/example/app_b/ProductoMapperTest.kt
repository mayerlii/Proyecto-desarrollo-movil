package com.example.app_b

import com.example.app_b.data.ProductoMapper
import com.example.app_b.data.ProductoModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para la transformación de Cursor a ProductoModel y manejo del Cursor.
 */
class ProductoMapperTest {

    private val columnasEstandar = listOf(
        "id",
        "nombre",
        "foto",
        "categoria",
        "precio",
        "descripcion",
        "vendedorId",
        "vendedorNombre",
        "vendedorCorreo",
        "stock"
    )

    @Test
    fun testTransformacionDeCursorAProductoModelCompleto() {
        val fila = mapOf(
            "id" to 101,
            "nombre" to "Laptop Gamer Pro",
            "foto" to "content://com.example.tienda_virtual.provider/fotos/101.jpg",
            "categoria" to "Tecnología",
            "precio" to 1299.99,
            "descripcion" to "Laptop de alto rendimiento para desarrollo y gaming",
            "vendedorId" to 5,
            "vendedorNombre" to "Carlos Tienda",
            "vendedorCorreo" to "carlos@tienda.com",
            "stock" to 15
        )

        val cursor = FakeCursor(columnasEstandar, listOf(fila))

        cursor.use { c ->
            assertTrue("El cursor debería tener una fila", c.moveToNext())
            val producto = ProductoMapper.fromCursor(c)

            assertNotNull(producto)
            assertEquals(101, producto.id)
            assertEquals("Laptop Gamer Pro", producto.nombre)
            assertEquals("content://com.example.tienda_virtual.provider/fotos/101.jpg", producto.foto)
            assertEquals("Tecnología", producto.categoria)
            assertEquals(1299.99, producto.precio, 0.001)
            assertEquals("Laptop de alto rendimiento para desarrollo y gaming", producto.descripcion)
            assertEquals(5, producto.vendedorId)
            assertEquals("Carlos Tienda", producto.vendedorNombre)
            assertEquals("carlos@tienda.com", producto.vendedorCorreo)
            assertEquals(15, producto.stock)
        }

        assertTrue("El cursor debe cerrarse tras el bloque use {}", cursor.isClosed)
    }

    @Test
    fun testTransformacionConValoresNulosYColumnasFaltantesAsignaValoresSeguros() {
        // Cursor con solo unas pocas columnas
        val columnasReducidas = listOf("id", "nombre", "precio")
        val filaConNulos = mapOf(
            "id" to 42,
            "nombre" to "Teclado Mecánico",
            "precio" to null // valor nulo explícito
        )

        val cursor = FakeCursor(columnasReducidas, listOf(filaConNulos))

        cursor.use { c ->
            assertTrue(c.moveToNext())
            val producto = ProductoMapper.fromCursor(c)

            assertEquals(42, producto.id)
            assertEquals("Teclado Mecánico", producto.nombre)
            assertEquals(0.0, producto.precio, 0.001) // Valor nulo mapeado a defecto
            assertEquals("", producto.foto)            // Columna ausente mapeada a defecto
            assertEquals("", producto.categoria)       // Columna ausente mapeada a defecto
            assertEquals("", producto.descripcion)
            assertEquals(0, producto.vendedorId)
            assertEquals("", producto.vendedorNombre)
            assertEquals("", producto.vendedorCorreo)
            assertEquals(0, producto.stock)
        }

        assertTrue(cursor.isClosed)
    }

    @Test
    fun testTransformacionMultipleFilas() {
        val filas = listOf(
            mapOf("id" to 1, "nombre" to "Producto A", "precio" to 10.0, "stock" to 5),
            mapOf("id" to 2, "nombre" to "Producto B", "precio" to 25.5, "stock" to 0),
            mapOf("id" to 3, "nombre" to "Producto C", "precio" to 99.0, "stock" to 12)
        )

        val cursor = FakeCursor(columnasEstandar, filas)

        val productos: List<ProductoModel> = cursor.use { c ->
            ProductoMapper.fromCursorToList(c)
        }

        assertEquals(3, productos.size)
        assertEquals(1, productos[0].id)
        assertEquals("Producto A", productos[0].nombre)
        assertEquals(2, productos[1].id)
        assertEquals("Producto B", productos[1].nombre)
        assertEquals(3, productos[2].id)
        assertEquals("Producto C", productos[2].nombre)

        assertTrue(cursor.isClosed)
    }
}
