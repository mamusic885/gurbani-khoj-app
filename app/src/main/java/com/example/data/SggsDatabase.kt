package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [LineEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SggsDatabase : RoomDatabase() {
    abstract fun sggsDao(): SggsDao

    companion object {
        @Volatile
        private var INSTANCE: SggsDatabase? = null
        private const val DB_NAME = "sggs_database.db"
        private const val DB_URL = "https://github.com/mamusic885/gurbani-khoj-app/releases/download/v1.0-database/database.db"

        suspend fun downloadIfNeeded(context: Context) {
            withContext(Dispatchers.IO) {
                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists() || dbFile.length() < 100000) {
                    dbFile.parentFile?.mkdirs()
                    try {
                        val url = URL(DB_URL)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 15000
                        conn.readTimeout = 30000
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                        if (conn.responseCode == HttpURLConnection.HTTP_OK || conn.responseCode == HttpURLConnection.HTTP_MOVED_TEMP || conn.responseCode == 302) {
                            val actualConn = if (conn.responseCode == 302 || conn.responseCode == 301) {
                                val redirectUrl = conn.getHeaderField("Location")
                                val rUrl = URL(redirectUrl)
                                (rUrl.openConnection() as HttpURLConnection).apply {
                                    setRequestProperty("User-Agent", "Mozilla/5.0")
                                }
                            } else conn
                            actualConn.inputStream.use { input ->
                                FileOutputStream(dbFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun getDatabase(context: Context): SggsDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    SggsDatabase::class.java,
                    DB_NAME
                )
                
                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists()) {
                    builder.createFromAsset("database.db")
                }
                
                val instance = builder
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

