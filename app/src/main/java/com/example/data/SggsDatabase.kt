package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
        private const val TAG = "SggsDatabase"

        suspend fun downloadIfNeeded(context: Context) {
            withContext(Dispatchers.IO) {
                val dbFile = context.getDatabasePath(DB_NAME)
                if (!dbFile.exists() || dbFile.length() < 10000000) {
                    dbFile.parentFile?.mkdirs()
                    Log.d(TAG, "Download started")
                    try {
                        var currentUrl = DB_URL
                        var inputStream: java.io.InputStream? = null
                        var redirectCount = 0

                        while (redirectCount < 10) {
                            val url = URL(currentUrl)
                            val conn = url.openConnection() as HttpURLConnection
                            conn.connectTimeout = 30000
                            conn.readTimeout = 60000
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                            conn.instanceFollowRedirects = true

                            val code = conn.responseCode
                            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                                code == HttpURLConnection.HTTP_SEE_OTHER ||
                                code == 307 || code == 308
                            ) {
                                val location = conn.getHeaderField("Location")
                                if (location.isNullOrEmpty()) break
                                currentUrl = location
                                conn.disconnect()
                                redirectCount++
                            } else if (code == HttpURLConnection.HTTP_OK) {
                                inputStream = conn.inputStream
                                break
                            } else {
                                Log.e(TAG, "HTTP error response code: $code")
                                break
                            }
                        }

                        if (inputStream != null) {
                            val tempFile = File(dbFile.parentFile, "$DB_NAME.tmp")
                            inputStream.use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (tempFile.exists() && tempFile.length() > 10000000) {
                                if (dbFile.exists()) {
                                    dbFile.delete()
                                }
                                tempFile.renameTo(dbFile)
                                Log.d(TAG, "Download completed")
                            } else {
                                Log.e(TAG, "Downloaded file is invalid or too small: ${tempFile.length()} bytes")
                            }
                        } else {
                            Log.e(TAG, "Failed to obtain input stream for database download")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error downloading database: ${e.message}", e)
                    }
                } else {
                    Log.d(TAG, "Database file already exists (${dbFile.length()} bytes)")
                }
            }
        }

        suspend fun getDatabase(context: Context): SggsDatabase {
            return withContext(Dispatchers.IO) {
                val db = INSTANCE ?: run {
                    downloadIfNeeded(context.applicationContext)
                    synchronized(this) {
                        INSTANCE ?: run {
                            val instance = Room.databaseBuilder(
                                context.applicationContext,
                                SggsDatabase::class.java,
                                DB_NAME
                            )
                                .fallbackToDestructiveMigration()
                                .build()

                            Log.d(TAG, "Database opened")
                            INSTANCE = instance
                            instance
                        }
                    }
                }
                try {
                    val count = db.sggsDao().getLineCount()
                    Log.d(TAG, "Total line count after opening: $count")
                } catch (e: Exception) {
                    Log.e(TAG, "Error querying line count: ${e.message}", e)
                }
                db
            }
        }
    }
}


