package com.example.app_b

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle

/**
 * Cursor simulado en memoria para pruebas unitarias sin dependencias de emulador ni stubs de Android.
 */
@Suppress("DEPRECATION")
class FakeCursor(
    private val columnNames: List<String>,
    private val rows: List<Map<String, Any?>>
) : Cursor {

    private var currentIndex = -1
    private var closed = false

    override fun getCount(): Int = rows.size
    override fun getPosition(): Int = currentIndex
    override fun getColumnNames(): Array<String> = columnNames.toTypedArray()
    override fun getColumnCount(): Int = columnNames.size

    override fun getColumnIndex(columnName: String): Int = columnNames.indexOf(columnName)
    override fun getColumnIndexOrThrow(columnName: String): Int {
        val index = getColumnIndex(columnName)
        if (index == -1) throw IllegalArgumentException("Columna $columnName no existe")
        return index
    }
    override fun getColumnName(columnIndex: Int): String = columnNames[columnIndex]

    override fun moveToNext(): Boolean {
        if (currentIndex + 1 < rows.size) {
            currentIndex++
            return true
        }
        currentIndex = rows.size
        return false
    }

    override fun moveToFirst(): Boolean {
        return moveToPosition(0)
    }

    override fun moveToPosition(position: Int): Boolean {
        return if (position in 0 until rows.size) {
            currentIndex = position
            true
        } else {
            false
        }
    }

    override fun moveToLast(): Boolean = moveToPosition(rows.size - 1)
    override fun moveToPrevious(): Boolean = moveToPosition(currentIndex - 1)

    override fun isFirst(): Boolean = currentIndex == 0 && rows.isNotEmpty()
    override fun isLast(): Boolean = currentIndex == rows.size - 1 && rows.isNotEmpty()
    override fun isBeforeFirst(): Boolean = currentIndex < 0
    override fun isAfterLast(): Boolean = currentIndex >= rows.size

    override fun isNull(columnIndex: Int): Boolean {
        val colName = columnNames[columnIndex]
        return rows[currentIndex][colName] == null
    }

    override fun getString(columnIndex: Int): String? {
        val colName = columnNames[columnIndex]
        return rows[currentIndex][colName]?.toString()
    }

    override fun getInt(columnIndex: Int): Int {
        val colName = columnNames[columnIndex]
        val value = rows[currentIndex][colName]
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    override fun getDouble(columnIndex: Int): Double {
        val colName = columnNames[columnIndex]
        val value = rows[currentIndex][colName]
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
    }

    override fun getShort(columnIndex: Int): Short = getInt(columnIndex).toShort()
    override fun getLong(columnIndex: Int): Long {
        val colName = columnNames[columnIndex]
        val value = rows[currentIndex][colName]
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    override fun getFloat(columnIndex: Int): Float = getDouble(columnIndex).toFloat()
    override fun getBlob(columnIndex: Int): ByteArray = byteArrayOf()
    override fun getType(columnIndex: Int): Int = Cursor.FIELD_TYPE_STRING

    override fun close() {
        closed = true
    }

    override fun isClosed(): Boolean = closed

    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer?) {}
    override fun deactivate() {}
    override fun requery(): Boolean = true
    override fun registerContentObserver(observer: ContentObserver?) {}
    override fun unregisterContentObserver(observer: ContentObserver?) {}
    override fun registerDataSetObserver(observer: DataSetObserver?) {}
    override fun unregisterDataSetObserver(observer: DataSetObserver?) {}
    override fun setNotificationUri(cr: ContentResolver?, uri: Uri?) {}
    override fun getNotificationUri(): Uri? = null
    override fun getNotificationUris(): List<Uri>? = null
    override fun setNotificationUris(cr: ContentResolver, uris: List<Uri>) {}
    override fun getWantsAllOnMoveCalls(): Boolean = false
    override fun setExtras(extras: Bundle?) {}
    override fun getExtras(): Bundle = Bundle.EMPTY
    override fun respond(extras: Bundle?): Bundle = Bundle.EMPTY
    override fun move(offset: Int): Boolean = moveToPosition(currentIndex + offset)
}
