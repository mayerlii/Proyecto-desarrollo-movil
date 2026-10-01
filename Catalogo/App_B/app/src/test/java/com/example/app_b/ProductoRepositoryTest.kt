package com.example.app_b

import com.example.app_b.data.ProductoRepository
import com.example.app_b.data.ResultadoConsulta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias para ProductoRepository:
 * - Consulta con éxito (con datos).
 * - Catálogo vacío (Cursor vacío).
 * - Error de permisos o firma (SecurityException).
 * - Provider o App A no disponible (isProviderAvailable = false, cursor null, URL desconocida).
 * - Error inesperado durante la consulta.
 * - Validación de URI, projection explícita y orden (id DESC).
 */
class ProductoRepositoryTest {

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
    fun testConstantesDeConsultaSonCorrectas() {
        assertEquals("com.example.tienda_virtual.provider", ProductoRepository.AUTHORITY)
        assertEquals("content://com.example.tienda_virtual.provider/productos", ProductoRepository.URI_STRING)
        assertEquals("id DESC", ProductoRepository.SORT_ORDER)

        val expectedColumns = setOf(
            "id", "nombre", "foto", "categoria", "precio",
            "descripcion", "vendedorId", "vendedorNombre", "vendedorCorreo", "stock"
        )
        assertEquals(expectedColumns, ProductoRepository.PROJECTION.toSet())
    }

    @Test
    fun testConsultaExitosaConDatosRetornaResultadoExitoYCierraCursor() {
        val fila1 = mapOf(
            "id" to 10,
            "nombre" to "Cámara Fotográfica",
            "foto" to "content://media/camara.png",
            "categoria" to "Fotografía",
            "precio" to 450.0,
            "descripcion" to "Cámara profesional 24MP",
            "vendedorId" to 1,
            "vendedorNombre" to "Foto Store",
            "vendedorCorreo" to "fotostore@mail.com",
            "stock" to 8
        )
        val fila2 = mapOf(
            "id" to 9,
            "nombre" to "Trípode Universal",
            "foto" to "",
            "categoria" to "Accesorios",
            "precio" to 45.0,
            "descripcion" to "Trípode de aluminio ligero",
            "vendedorId" to 1,
            "vendedorNombre" to "Foto Store",
            "vendedorCorreo" to "fotostore@mail.com",
            "stock" to 20
        )

        val cursor = FakeCursor(columnasEstandar, listOf(fila1, fila2))

        val resultado = ProductoRepository.ejecutarConsulta(
            queryExecutor = { uri, proj, sel, args, sort ->
                assertEquals(ProductoRepository.SORT_ORDER, sort)
                assertEquals(ProductoRepository.PROJECTION.toList(), proj?.toList())
                cursor
            },
            isProviderAvailable = true
        )

        assertTrue("El resultado debe ser Exito", resultado is ResultadoConsulta.Exito)
        val exito = resultado as ResultadoConsulta.Exito
        assertEquals(2, exito.productos.size)
        assertEquals("Cámara Fotográfica", exito.productos[0].nombre)
        assertEquals(10, exito.productos[0].id)
        assertEquals("Trípode Universal", exito.productos[1].nombre)

        // Verificar que el cursor fue cerrado correctamente con use {}
        assertTrue("El cursor debe haberse cerrado", cursor.isClosed)
    }

    @Test
    fun testCatalogoVacioRetornaExitoConListaVaciaYCierraCursor() {
        // Cursor sin filas
        val cursorVacio = FakeCursor(columnasEstandar, emptyList())

        val resultado = ProductoRepository.ejecutarConsulta(
            queryExecutor = { _, _, _, _, _ -> cursorVacio },
            isProviderAvailable = true
        )

        assertTrue(resultado is ResultadoConsulta.Exito)
        val exito = resultado as ResultadoConsulta.Exito
        assertTrue("La lista de productos debe estar vacía", exito.productos.isEmpty())
        assertTrue("El cursor vacío debe haberse cerrado", cursorVacio.isClosed)
    }

    @Test
    fun testErrorDePermisosOSignatureExceptionRetornaPermisoOFirma() {
        val securityException = SecurityException("Permission Denial: reading com.example.tienda_virtual.provider requires com.example.tienda_virtual.permission.READ_PRODUCTS")

        val resultado = ProductoRepository.ejecutarConsulta(
            queryExecutor = { _, _, _, _, _ -> throw securityException },
            isProviderAvailable = true
        )

        assertTrue("Debe retornar Error.PermisoOFirma", resultado is ResultadoConsulta.Error.PermisoOFirma)
        val error = resultado as ResultadoConsulta.Error.PermisoOFirma
        assertTrue(error.mensaje.contains("READ_PRODUCTS") || error.mensaje.contains("permiso"))
        assertEquals(securityException, error.causa)
    }

    @Test
    fun testProviderNoDisponibleCuandoFlagIsProviderAvailableEsFalso() {
        val resultado = ProductoRepository.ejecutarConsulta(
            queryExecutor = { _, _, _, _, _ -> null },
            isProviderAvailable = false
        )

        assertTrue("Debe retornar AppNoInstalada si el proveedor no está disponible", resultado is ResultadoConsulta.Error.AppNoInstalada)
    }

    @Test
    fun testProviderNoDisponibleCuandoCursorEsNulo() {
        val resultado = ProductoRepository.ejecutarConsulta(
            queryExecutor = { _, _, _, _, _ -> null },
            isProviderAvailable = true
        )

        assertTrue("Debe retornar AppNoInstalada si el resolver devuelve null", resultado is ResultadoConsulta.Error.AppNoInstalada)
    }

    @Test
    fun testProviderNoDisponibleCuandoLanzaIllegalArgumentException() {
        val resultado = ProductoRepository.ejecutarConsulta(
            queryExecutor = { _, _, _, _, _ -> throw IllegalArgumentException("Unknown URL content://com.example.tienda_virtual.provider/productos") },
            isProviderAvailable = true
        )

        assertTrue(resultado is ResultadoConsulta.Error.AppNoInstalada)
    }

    @Test
    fun testErrorInesperadoDuranteConsultaRetornaErrorInesperado() {
        val exception = IllegalStateException("Base de datos no inicializada o disco lleno")

        val resultado = ProductoRepository.ejecutarConsulta(
            queryExecutor = { _, _, _, _, _ -> throw exception },
            isProviderAvailable = true
        )

        assertTrue("Debe retornar ErrorInesperado ante excepciones no previstas", resultado is ResultadoConsulta.Error.ErrorInesperado)
        val error = resultado as ResultadoConsulta.Error.ErrorInesperado
        assertEquals(exception, error.causa)
    }
}
