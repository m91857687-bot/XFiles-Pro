package com.xfiles.pro.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xfiles.pro.data.local.database.dao.IndexedFileDao
import com.xfiles.pro.data.local.database.entity.IndexedFileEntity

/**
 * Main Room Database for XFiles Pro local persistent storage.
 */
@Database(
    entities = [IndexedFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class XFilesDatabase : RoomDatabase() {

    abstract fun indexedFileDao(): IndexedFileDao

    companion object {
        private const val DATABASE_NAME = "xfiles_database.db"

        @Volatile
        private var instance: XFilesDatabase? = null

        fun getInstance(context: Context): XFilesDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): XFilesDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                XFilesDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
        }
    }
}
