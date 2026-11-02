package com.xfiles.pro.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xfiles.pro.data.local.database.dao.IndexedFileDao
import com.xfiles.pro.data.local.database.entity.IndexedFileEntity
import com.xfiles.pro.data.local.database.entity.IndexedFileFtsEntity

/**
 * Main Room Database for XFiles Pro local persistent storage and fast FTS search.
 */
@Database(
    entities = [
        IndexedFileEntity::class,
        IndexedFileFtsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class XFilesDatabase : RoomDatabase() {

    abstract fun indexedFileDao(): IndexedFileDao

    companion object {
        private const val DATABASE_NAME = "xfiles_database.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to indexed_files table
                db.execSQL("ALTER TABLE `indexed_files` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'STORAGE'")
                db.execSQL("ALTER TABLE `indexed_files` ADD COLUMN `volume_id` TEXT NOT NULL DEFAULT 'primary'")
                db.execSQL("ALTER TABLE `indexed_files` ADD COLUMN `can_read` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `indexed_files` ADD COLUMN `can_write` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `indexed_files` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `indexed_files` ADD COLUMN `last_indexed_at` INTEGER NOT NULL DEFAULT 0")

                // Add indices for performance
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_indexed_files_volume_id` ON `indexed_files` (`volume_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_indexed_files_source` ON `indexed_files` (`source`)")

                // Create FTS4 virtual table backed by indexed_files with unicode61 tokenizer
                db.execSQL(
                    "CREATE VIRTUAL TABLE IF NOT EXISTS `indexed_files_fts` USING FTS4(" +
                        "`name` TEXT NOT NULL, " +
                        "`extension` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`parent_path` TEXT NOT NULL, " +
                        "content=`indexed_files`, " +
                        "tokenize=unicode61)"
                )

                // Room FTS triggers for external content sync
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_indexed_files_fts_BEFORE_UPDATE " +
                        "BEFORE UPDATE ON `indexed_files` BEGIN " +
                        "DELETE FROM `indexed_files_fts` WHERE `docid`=OLD.`id`; END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_indexed_files_fts_BEFORE_DELETE " +
                        "BEFORE DELETE ON `indexed_files` BEGIN " +
                        "DELETE FROM `indexed_files_fts` WHERE `docid`=OLD.`id`; END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_indexed_files_fts_AFTER_UPDATE " +
                        "AFTER UPDATE ON `indexed_files` BEGIN " +
                        "INSERT INTO `indexed_files_fts`(`docid`, `name`, `extension`, `category`, `parent_path`) " +
                        "VALUES (NEW.`id`, NEW.`name`, NEW.`extension`, NEW.`category`, NEW.`parent_path`); END"
                )
                db.execSQL(
                    "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_indexed_files_fts_AFTER_INSERT " +
                        "AFTER INSERT ON `indexed_files` BEGIN " +
                        "INSERT INTO `indexed_files_fts`(`docid`, `name`, `extension`, `category`, `parent_path`) " +
                        "VALUES (NEW.`id`, NEW.`name`, NEW.`extension`, NEW.`category`, NEW.`parent_path`); END"
                )

                // Populate initial FTS records from existing records if any
                db.execSQL(
                    "INSERT INTO `indexed_files_fts`(`docid`, `name`, `extension`, `category`, `parent_path`) " +
                        "SELECT `id`, `name`, `extension`, `category`, `parent_path` FROM `indexed_files`"
                )
            }
        }

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
            ).addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
