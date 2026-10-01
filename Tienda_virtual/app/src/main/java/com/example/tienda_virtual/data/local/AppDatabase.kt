package com.example.tienda_virtual.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tienda_virtual.archivos.Producto

/**
 * Base de Datos Room Singleton para la Aplicación.
 *
 * Configura la entidad [Producto], gestiona la instancia única en memoria y aplica
 * la migración destructiva en desarrollo ante cambios de esquema.
 */
@Database(entities = [Producto::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Retorna la instancia singleton de la base de datos Room */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tienda_virtual_db",
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
