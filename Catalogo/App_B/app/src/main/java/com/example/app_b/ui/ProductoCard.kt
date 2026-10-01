package com.example.app_b.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.app_b.data.ProductoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

/**
 * Tarjeta para mostrar la información detallada del producto obtenido a través del ContentResolver.
 * Incluye nombre, categoría, precio, descripción, stock e información del vendedor.
 */
@Composable
fun ProductoCard(
    producto: ProductoModel,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contenedor de la Imagen o Ícono predeterminado
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = producto.foto) {
                        value = withContext(Dispatchers.IO) {
                            val fotoStr = producto.foto.trim()
                            android.util.Log.d("ProductoCard", "Cargando foto para '${producto.nombre}': length=${fotoStr.length}, preview='${fotoStr.take(60)}'")
                            if (fotoStr.isBlank()) {
                                android.util.Log.w("ProductoCard", "La cadena de foto está vacía para ${producto.nombre}")
                                null
                            } else {
                                var decodedBitmap: Bitmap? = null

                                // 1. Intentar como data:image o Base64 con prefijo
                                if (fotoStr.startsWith("data:image") || fotoStr.contains(";base64,")) {
                                    try {
                                        val cleanBase64 = if (fotoStr.contains(",")) fotoStr.substringAfter(",") else fotoStr
                                        val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                                        decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        if (decodedBitmap != null) {
                                            android.util.Log.d("ProductoCard", "Éxito decodificando Base64 con prefijo")
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProductoCard", "Error decodificando Base64 con prefijo", e)
                                    }
                                }

                                // 2. Intentar como URI (content:// o file://)
                                if (decodedBitmap == null && (fotoStr.startsWith("content://") || fotoStr.startsWith("file://"))) {
                                    try {
                                        val uri = Uri.parse(fotoStr)
                                        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                                        decodedBitmap = BitmapFactory.decodeStream(inputStream)
                                        inputStream?.close()
                                        if (decodedBitmap != null) {
                                            android.util.Log.d("ProductoCard", "Éxito decodificando desde URI: $uri")
                                        }
                                    } catch (e: SecurityException) {
                                        android.util.Log.e("ProductoCard", "SecurityException al abrir URI (falta permiso o grant): $fotoStr", e)
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProductoCard", "Error abriendo InputStream desde URI: $fotoStr", e)
                                    }
                                }

                                // 3. Intentar como URL HTTP / HTTPS
                                if (decodedBitmap == null && (fotoStr.startsWith("http://") || fotoStr.startsWith("https://"))) {
                                    try {
                                        val url = java.net.URL(fotoStr)
                                        val inputStream = url.openStream()
                                        decodedBitmap = BitmapFactory.decodeStream(inputStream)
                                        inputStream?.close()
                                        if (decodedBitmap != null) {
                                            android.util.Log.d("ProductoCard", "Éxito descargando imagen desde URL: $fotoStr")
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProductoCard", "Error descargando imagen desde URL: $fotoStr", e)
                                    }
                                }

                                // 4. Intentar como ruta de archivo local
                                if (decodedBitmap == null && (fotoStr.startsWith("/") || fotoStr.startsWith("file:/"))) {
                                    try {
                                        val filePath = if (fotoStr.startsWith("file://")) Uri.parse(fotoStr).path else fotoStr
                                        if (filePath != null) {
                                            decodedBitmap = BitmapFactory.decodeFile(filePath)
                                            if (decodedBitmap != null) {
                                                android.util.Log.d("ProductoCard", "Éxito decodificando archivo local '$filePath'")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProductoCard", "Error decodificando archivo local: $fotoStr", e)
                                    }
                                }

                                // 5. Intentar como Base64 plano (sin prefijo)
                                if (decodedBitmap == null) {
                                    try {
                                        val sanitizedBase64 = fotoStr.replace("\\s".toRegex(), "")
                                        val bytes = Base64.decode(sanitizedBase64, Base64.DEFAULT)
                                        if (bytes != null && bytes.isNotEmpty()) {
                                            decodedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            if (decodedBitmap != null) {
                                                android.util.Log.d("ProductoCard", "Éxito decodificando Base64 plano")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.d("ProductoCard", "No es Base64 plano válido")
                                    }
                                }

                                // 6. Intentar como recurso drawable (local o remoto de tienda_virtual)
                                if (decodedBitmap == null) {
                                    try {
                                        val resId = context.resources.getIdentifier(fotoStr, "drawable", context.packageName)
                                        if (resId != 0) {
                                            decodedBitmap = BitmapFactory.decodeResource(context.resources, resId)
                                            android.util.Log.d("ProductoCard", "Éxito cargando recurso drawable local: $fotoStr")
                                        } else {
                                            try {
                                                val remoteRes = context.packageManager.getResourcesForApplication("com.example.tienda_virtual")
                                                val remoteResId = remoteRes.getIdentifier(fotoStr, "drawable", "com.example.tienda_virtual")
                                                if (remoteResId != 0) {
                                                    decodedBitmap = BitmapFactory.decodeResource(remoteRes, remoteResId)
                                                    android.util.Log.d("ProductoCard", "Éxito cargando recurso drawable remoto de tienda_virtual: $fotoStr")
                                                }
                                            } catch (e: Exception) {
                                                // Ignorar si no se puede acceder al paquete remoto
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ProductoCard", "Error cargando como recurso drawable: $fotoStr", e)
                                    }
                                }

                                if (decodedBitmap == null) {
                                    android.util.Log.w("ProductoCard", "Fallo total al decodificar imagen para '${producto.nombre}' con fotoStr='$fotoStr'")
                                }

                                decodedBitmap
                            }
                        }
                    }

                    val currentBitmap = bitmap
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap.asImageBitmap(),
                            contentDescription = producto.nombre,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = producto.categoria.ifBlank { "General" },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$ ${String.format(Locale.US, "%.2f", producto.precio)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (producto.descripcion.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = producto.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Información del Vendedor que ingresó el producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vendedor: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = producto.vendedorNombre.ifBlank { "Vendedor JAS" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (producto.vendedorCorreo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = producto.vendedorCorreo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stock disponible:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Badge(
                        containerColor = if (producto.stock > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "${producto.stock} unidades",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
