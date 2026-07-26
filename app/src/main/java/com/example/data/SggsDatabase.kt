package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.util.convertGurbaniAkharToUnicode
import java.io.File
import java.io.FileOutputStream

class SggsDatabase private constructor(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {

    private var openDb: SQLiteDatabase? = null

    override fun onCreate(db: SQLiteDatabase?) {
        // Pre-packaged database from assets. No creation script needed.
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Pre-packaged database. No migration script needed.
    }

    @Synchronized
    fun getReadableDb(): SQLiteDatabase {
        openDb?.let { if (it.isOpen) return it }

        val dbFile = context.getDatabasePath(DB_NAME)
        copyDatabaseIfNeeded(context, dbFile)

        Log.d(TAG, "database path: ${dbFile.absolutePath}")
        Log.d(TAG, "exists = ${dbFile.exists()}")
        Log.d(TAG, "file size = ${dbFile.length()}")

        val db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        openDb = db
        Log.d(TAG, "Room opened successfully")
        validateDatabase(db)
        return db
    }

    private fun validateDatabase(db: SQLiteDatabase) {
        try {
            val lineCount = getLineCount(db)
            val translationCount = getTranslationCount(db)
            Log.d(TAG, "=== Startup SQLite Database Validation ===")
            Log.d(TAG, "Database path: ${db.path}")
            Log.d(TAG, "Database exists: ${File(db.path).exists()}")
            Log.d(TAG, "Database size: ${File(db.path).length()} bytes")
            Log.d(TAG, "Line count (SELECT COUNT(*) FROM lines): $lineCount")
            Log.d(TAG, "Translation count (SELECT COUNT(*) FROM translations): $translationCount")

            val ang1Lines = searchByAng(1)
            Log.d(TAG, "Rows returned for Ang 1: ${ang1Lines.size}")
            if (ang1Lines.isNotEmpty()) {
                val sample = ang1Lines.first()
                Log.d(TAG, "Sample Ang 1 row: gurmukhi='${sample.line.gurmukhi}', translation='${sample.translation}'")
            } else {
                Log.e(TAG, "WARNING: 0 rows returned for Ang 1!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during database validation: ${e.message}", e)
        }
    }

    fun searchByAng(ang: Int): List<LineWithTranslation> {
        val db = getReadableDb()
        val sql = """
            SELECT l.id, l.shabad_id, l.source_page, l.source_line, l.first_letters, 
                   l.vishraam_first_letters, l.gurmukhi, l.pronunciation, 
                   l.pronunciation_information, l.type_id, l.order_id,
                   t.translation, COALESCE(tp3.translation, tp6.translation) AS punjabi_translation,
                   sec.name_gurmukhi AS raag
            FROM lines l 
            JOIN shabads s ON l.shabad_id = s.id 
            LEFT JOIN sections sec ON s.section_id = sec.id
            LEFT JOIN translations t ON l.id = t.line_id AND t.translation_source_id = 1 
            LEFT JOIN translations tp3 ON l.id = tp3.line_id AND tp3.translation_source_id = 3 
            LEFT JOIN translations tp6 ON l.id = tp6.line_id AND tp6.translation_source_id = 6 
            WHERE s.source_id = 1 AND l.source_page = ? 
            ORDER BY l.order_id ASC
        """.trimIndent()

        val list = mutableListOf<LineWithTranslation>()
        db.rawQuery(sql, arrayOf(ang.toString())).use { cursor ->
            val idIdx = cursor.getColumnIndex("id")
            val shabadIdIdx = cursor.getColumnIndex("shabad_id")
            val sourcePageIdx = cursor.getColumnIndex("source_page")
            val sourceLineIdx = cursor.getColumnIndex("source_line")
            val firstLettersIdx = cursor.getColumnIndex("first_letters")
            val vishraamIdx = cursor.getColumnIndex("vishraam_first_letters")
            val gurmukhiIdx = cursor.getColumnIndex("gurmukhi")
            val pronunciationIdx = cursor.getColumnIndex("pronunciation")
            val pronunciationInfoIdx = cursor.getColumnIndex("pronunciation_information")
            val typeIdIdx = cursor.getColumnIndex("type_id")
            val orderIdIdx = cursor.getColumnIndex("order_id")
            val translationIdx = cursor.getColumnIndex("translation")
            val punjabiTranslationIdx = cursor.getColumnIndex("punjabi_translation")
            val raagIdx = cursor.getColumnIndex("raag")

            while (cursor.moveToNext()) {
                val rawRaag = if (raagIdx >= 0 && !cursor.isNull(raagIdx)) cursor.getString(raagIdx) else ""
                val raagUnicode = convertGurbaniAkharToUnicode(rawRaag)

                val line = LineEntity(
                    id = if (idIdx >= 0 && !cursor.isNull(idIdx)) cursor.getString(idIdx) else "",
                    shabad_id = if (shabadIdIdx >= 0 && !cursor.isNull(shabadIdIdx)) cursor.getString(shabadIdIdx) else "",
                    source_page = if (sourcePageIdx >= 0 && !cursor.isNull(sourcePageIdx)) cursor.getInt(sourcePageIdx) else 1,
                    source_line = if (sourceLineIdx >= 0 && !cursor.isNull(sourceLineIdx)) cursor.getInt(sourceLineIdx) else null,
                    first_letters = if (firstLettersIdx >= 0 && !cursor.isNull(firstLettersIdx)) cursor.getString(firstLettersIdx) else null,
                    vishraam_first_letters = if (vishraamIdx >= 0 && !cursor.isNull(vishraamIdx)) cursor.getString(vishraamIdx) else null,
                    gurmukhi = if (gurmukhiIdx >= 0 && !cursor.isNull(gurmukhiIdx)) cursor.getString(gurmukhiIdx) else "",
                    pronunciation = if (pronunciationIdx >= 0 && !cursor.isNull(pronunciationIdx)) cursor.getString(pronunciationIdx) else null,
                    pronunciation_information = if (pronunciationInfoIdx >= 0 && !cursor.isNull(pronunciationInfoIdx)) cursor.getString(pronunciationInfoIdx) else null,
                    type_id = if (typeIdIdx >= 0 && !cursor.isNull(typeIdIdx)) cursor.getInt(typeIdIdx) else null,
                    order_id = if (orderIdIdx >= 0 && !cursor.isNull(orderIdIdx)) cursor.getInt(orderIdIdx) else 0,
                    raag = raagUnicode
                )
                val translation = if (translationIdx >= 0 && !cursor.isNull(translationIdx)) cursor.getString(translationIdx) else null
                val punjabiTranslation = if (punjabiTranslationIdx >= 0 && !cursor.isNull(punjabiTranslationIdx)) cursor.getString(punjabiTranslationIdx) else null
                list.add(LineWithTranslation(line = line, translation = translation, punjabiTranslation = punjabiTranslation))
            }
        }
        return list
    }

    fun getShabadByShabadId(shabadId: String): List<LineWithTranslation> {
        val db = getReadableDb()
        val sql = """
            SELECT l.id, l.shabad_id, l.source_page, l.source_line, l.first_letters, 
                   l.vishraam_first_letters, l.gurmukhi, l.pronunciation, 
                   l.pronunciation_information, l.type_id, l.order_id,
                   t.translation, COALESCE(tp3.translation, tp6.translation) AS punjabi_translation,
                   sec.name_gurmukhi AS raag
            FROM lines l 
            JOIN shabads s ON l.shabad_id = s.id 
            LEFT JOIN sections sec ON s.section_id = sec.id
            LEFT JOIN translations t ON l.id = t.line_id AND t.translation_source_id = 1 
            LEFT JOIN translations tp3 ON l.id = tp3.line_id AND tp3.translation_source_id = 3 
            LEFT JOIN translations tp6 ON l.id = tp6.line_id AND tp6.translation_source_id = 6 
            WHERE s.source_id = 1 AND l.shabad_id = ? 
            ORDER BY l.order_id ASC
        """.trimIndent()

        val list = mutableListOf<LineWithTranslation>()
        db.rawQuery(sql, arrayOf(shabadId)).use { cursor ->
            val idIdx = cursor.getColumnIndex("id")
            val shabadIdIdx = cursor.getColumnIndex("shabad_id")
            val sourcePageIdx = cursor.getColumnIndex("source_page")
            val sourceLineIdx = cursor.getColumnIndex("source_line")
            val firstLettersIdx = cursor.getColumnIndex("first_letters")
            val vishraamIdx = cursor.getColumnIndex("vishraam_first_letters")
            val gurmukhiIdx = cursor.getColumnIndex("gurmukhi")
            val pronunciationIdx = cursor.getColumnIndex("pronunciation")
            val pronunciationInfoIdx = cursor.getColumnIndex("pronunciation_information")
            val typeIdIdx = cursor.getColumnIndex("type_id")
            val orderIdIdx = cursor.getColumnIndex("order_id")
            val translationIdx = cursor.getColumnIndex("translation")
            val punjabiTranslationIdx = cursor.getColumnIndex("punjabi_translation")
            val raagIdx = cursor.getColumnIndex("raag")

            while (cursor.moveToNext()) {
                val rawRaag = if (raagIdx >= 0 && !cursor.isNull(raagIdx)) cursor.getString(raagIdx) else ""
                val raagUnicode = convertGurbaniAkharToUnicode(rawRaag)

                val line = LineEntity(
                    id = if (idIdx >= 0 && !cursor.isNull(idIdx)) cursor.getString(idIdx) else "",
                    shabad_id = if (shabadIdIdx >= 0 && !cursor.isNull(shabadIdIdx)) cursor.getString(shabadIdIdx) else "",
                    source_page = if (sourcePageIdx >= 0 && !cursor.isNull(sourcePageIdx)) cursor.getInt(sourcePageIdx) else 1,
                    source_line = if (sourceLineIdx >= 0 && !cursor.isNull(sourceLineIdx)) cursor.getInt(sourceLineIdx) else null,
                    first_letters = if (firstLettersIdx >= 0 && !cursor.isNull(firstLettersIdx)) cursor.getString(firstLettersIdx) else null,
                    vishraam_first_letters = if (vishraamIdx >= 0 && !cursor.isNull(vishraamIdx)) cursor.getString(vishraamIdx) else null,
                    gurmukhi = if (gurmukhiIdx >= 0 && !cursor.isNull(gurmukhiIdx)) cursor.getString(gurmukhiIdx) else "",
                    pronunciation = if (pronunciationIdx >= 0 && !cursor.isNull(pronunciationIdx)) cursor.getString(pronunciationIdx) else null,
                    pronunciation_information = if (pronunciationInfoIdx >= 0 && !cursor.isNull(pronunciationInfoIdx)) cursor.getString(pronunciationInfoIdx) else null,
                    type_id = if (typeIdIdx >= 0 && !cursor.isNull(typeIdIdx)) cursor.getInt(typeIdIdx) else null,
                    order_id = if (orderIdIdx >= 0 && !cursor.isNull(orderIdIdx)) cursor.getInt(orderIdIdx) else 0,
                    raag = raagUnicode
                )
                val translation = if (translationIdx >= 0 && !cursor.isNull(translationIdx)) cursor.getString(translationIdx) else null
                val punjabiTranslation = if (punjabiTranslationIdx >= 0 && !cursor.isNull(punjabiTranslationIdx)) cursor.getString(punjabiTranslationIdx) else null
                list.add(LineWithTranslation(line = line, translation = translation, punjabiTranslation = punjabiTranslation))
            }
        }
        return list
    }

    fun searchByFirstLetters(query: String, asciiQuery: String): List<LineWithTranslation> {
        val db = getReadableDb()
        val sql = """
            SELECT l.id, l.shabad_id, l.source_page, l.source_line, l.first_letters, 
                   l.vishraam_first_letters, l.gurmukhi, l.pronunciation, 
                   l.pronunciation_information, l.type_id, l.order_id,
                   t.translation, COALESCE(tp3.translation, tp6.translation) AS punjabi_translation 
            FROM lines l 
            JOIN shabads s ON l.shabad_id = s.id 
            LEFT JOIN translations t ON l.id = t.line_id AND t.translation_source_id = 1 
            LEFT JOIN translations tp3 ON l.id = tp3.line_id AND tp3.translation_source_id = 3 
            LEFT JOIN translations tp6 ON l.id = tp6.line_id AND tp6.translation_source_id = 6 
            WHERE s.source_id = 1 AND (l.first_letters LIKE ? || '%' OR l.first_letters LIKE ? || '%') 
            ORDER BY l.order_id ASC 
            LIMIT 100
        """.trimIndent()

        val list = mutableListOf<LineWithTranslation>()
        db.rawQuery(sql, arrayOf(query, asciiQuery)).use { cursor ->
            val idIdx = cursor.getColumnIndex("id")
            val shabadIdIdx = cursor.getColumnIndex("shabad_id")
            val sourcePageIdx = cursor.getColumnIndex("source_page")
            val sourceLineIdx = cursor.getColumnIndex("source_line")
            val firstLettersIdx = cursor.getColumnIndex("first_letters")
            val vishraamIdx = cursor.getColumnIndex("vishraam_first_letters")
            val gurmukhiIdx = cursor.getColumnIndex("gurmukhi")
            val pronunciationIdx = cursor.getColumnIndex("pronunciation")
            val pronunciationInfoIdx = cursor.getColumnIndex("pronunciation_information")
            val typeIdIdx = cursor.getColumnIndex("type_id")
            val orderIdIdx = cursor.getColumnIndex("order_id")
            val translationIdx = cursor.getColumnIndex("translation")
            val punjabiTranslationIdx = cursor.getColumnIndex("punjabi_translation")

            while (cursor.moveToNext()) {
                val line = LineEntity(
                    id = if (idIdx >= 0 && !cursor.isNull(idIdx)) cursor.getString(idIdx) else "",
                    shabad_id = if (shabadIdIdx >= 0 && !cursor.isNull(shabadIdIdx)) cursor.getString(shabadIdIdx) else "",
                    source_page = if (sourcePageIdx >= 0 && !cursor.isNull(sourcePageIdx)) cursor.getInt(sourcePageIdx) else 1,
                    source_line = if (sourceLineIdx >= 0 && !cursor.isNull(sourceLineIdx)) cursor.getInt(sourceLineIdx) else null,
                    first_letters = if (firstLettersIdx >= 0 && !cursor.isNull(firstLettersIdx)) cursor.getString(firstLettersIdx) else null,
                    vishraam_first_letters = if (vishraamIdx >= 0 && !cursor.isNull(vishraamIdx)) cursor.getString(vishraamIdx) else null,
                    gurmukhi = if (gurmukhiIdx >= 0 && !cursor.isNull(gurmukhiIdx)) cursor.getString(gurmukhiIdx) else "",
                    pronunciation = if (pronunciationIdx >= 0 && !cursor.isNull(pronunciationIdx)) cursor.getString(pronunciationIdx) else null,
                    pronunciation_information = if (pronunciationInfoIdx >= 0 && !cursor.isNull(pronunciationInfoIdx)) cursor.getString(pronunciationInfoIdx) else null,
                    type_id = if (typeIdIdx >= 0 && !cursor.isNull(typeIdIdx)) cursor.getInt(typeIdIdx) else null,
                    order_id = if (orderIdIdx >= 0 && !cursor.isNull(orderIdIdx)) cursor.getInt(orderIdIdx) else 0
                )
                val translation = if (translationIdx >= 0 && !cursor.isNull(translationIdx)) cursor.getString(translationIdx) else null
                val punjabiTranslation = if (punjabiTranslationIdx >= 0 && !cursor.isNull(punjabiTranslationIdx)) cursor.getString(punjabiTranslationIdx) else null
                list.add(LineWithTranslation(line = line, translation = translation, punjabiTranslation = punjabiTranslation))
            }
        }
        return list
    }

    fun searchByFullText(query: String, asciiQuery: String): List<LineWithTranslation> {
        val db = getReadableDb()
        val sql = """
            SELECT l.id, l.shabad_id, l.source_page, l.source_line, l.first_letters, 
                   l.vishraam_first_letters, l.gurmukhi, l.pronunciation, 
                   l.pronunciation_information, l.type_id, l.order_id,
                   t.translation, COALESCE(tp3.translation, tp6.translation) AS punjabi_translation 
            FROM lines l 
            JOIN shabads s ON l.shabad_id = s.id 
            LEFT JOIN translations t ON l.id = t.line_id AND t.translation_source_id = 1 
            LEFT JOIN translations tp3 ON l.id = tp3.line_id AND tp3.translation_source_id = 3 
            LEFT JOIN translations tp6 ON l.id = tp6.line_id AND tp6.translation_source_id = 6 
            WHERE s.source_id = 1 AND l.gurmukhi LIKE '%' || ? || '%' 
            ORDER BY l.order_id ASC 
            LIMIT 100
        """.trimIndent()

        val list = mutableListOf<LineWithTranslation>()
        db.rawQuery(sql, arrayOf(query)).use { cursor ->
            val idIdx = cursor.getColumnIndex("id")
            val shabadIdIdx = cursor.getColumnIndex("shabad_id")
            val sourcePageIdx = cursor.getColumnIndex("source_page")
            val sourceLineIdx = cursor.getColumnIndex("source_line")
            val firstLettersIdx = cursor.getColumnIndex("first_letters")
            val vishraamIdx = cursor.getColumnIndex("vishraam_first_letters")
            val gurmukhiIdx = cursor.getColumnIndex("gurmukhi")
            val pronunciationIdx = cursor.getColumnIndex("pronunciation")
            val pronunciationInfoIdx = cursor.getColumnIndex("pronunciation_information")
            val typeIdIdx = cursor.getColumnIndex("type_id")
            val orderIdIdx = cursor.getColumnIndex("order_id")
            val translationIdx = cursor.getColumnIndex("translation")
            val punjabiTranslationIdx = cursor.getColumnIndex("punjabi_translation")

            while (cursor.moveToNext()) {
                val line = LineEntity(
                    id = if (idIdx >= 0 && !cursor.isNull(idIdx)) cursor.getString(idIdx) else "",
                    shabad_id = if (shabadIdIdx >= 0 && !cursor.isNull(shabadIdIdx)) cursor.getString(shabadIdIdx) else "",
                    source_page = if (sourcePageIdx >= 0 && !cursor.isNull(sourcePageIdx)) cursor.getInt(sourcePageIdx) else 1,
                    source_line = if (sourceLineIdx >= 0 && !cursor.isNull(sourceLineIdx)) cursor.getInt(sourceLineIdx) else null,
                    first_letters = if (firstLettersIdx >= 0 && !cursor.isNull(firstLettersIdx)) cursor.getString(firstLettersIdx) else null,
                    vishraam_first_letters = if (vishraamIdx >= 0 && !cursor.isNull(vishraamIdx)) cursor.getString(vishraamIdx) else null,
                    gurmukhi = if (gurmukhiIdx >= 0 && !cursor.isNull(gurmukhiIdx)) cursor.getString(gurmukhiIdx) else "",
                    pronunciation = if (pronunciationIdx >= 0 && !cursor.isNull(pronunciationIdx)) cursor.getString(pronunciationIdx) else null,
                    pronunciation_information = if (pronunciationInfoIdx >= 0 && !cursor.isNull(pronunciationInfoIdx)) cursor.getString(pronunciationInfoIdx) else null,
                    type_id = if (typeIdIdx >= 0 && !cursor.isNull(typeIdIdx)) cursor.getInt(typeIdIdx) else null,
                    order_id = if (orderIdIdx >= 0 && !cursor.isNull(orderIdIdx)) cursor.getInt(orderIdIdx) else 0
                )
                val translation = if (translationIdx >= 0 && !cursor.isNull(translationIdx)) cursor.getString(translationIdx) else null
                val punjabiTranslation = if (punjabiTranslationIdx >= 0 && !cursor.isNull(punjabiTranslationIdx)) cursor.getString(punjabiTranslationIdx) else null
                list.add(LineWithTranslation(line = line, translation = translation, punjabiTranslation = punjabiTranslation))
            }
        }
        return list
    }

    fun getLineCount(db: SQLiteDatabase = getReadableDb()): Int {
        db.rawQuery("SELECT COUNT(*) FROM lines", null).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)
            }
        }
        return 0
    }

    fun getTranslationCount(db: SQLiteDatabase = getReadableDb()): Int {
        db.rawQuery("SELECT COUNT(*) FROM translations", null).use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0)
            }
        }
        return 0
    }

    @Volatile
    private var cachedPunjabiMap: Map<String, String>? = null

    fun getPunjabiTranslationMap(): Map<String, String> {
        cachedPunjabiMap?.let { return it }
        return synchronized(this) {
            cachedPunjabiMap?.let { return@synchronized it }
            val map = HashMap<String, String>()
            try {
                val db = getReadableDb()
                val sql = """
                    SELECT l.gurmukhi, COALESCE(tp3.translation, tp6.translation) AS punjabi_translation
                    FROM lines l
                    LEFT JOIN translations tp3 ON l.id = tp3.line_id AND tp3.translation_source_id = 3
                    LEFT JOIN translations tp6 ON l.id = tp6.line_id AND tp6.translation_source_id = 6
                    WHERE tp3.translation IS NOT NULL OR tp6.translation IS NOT NULL
                """.trimIndent()
                db.rawQuery(sql, null).use { cursor ->
                    val gurmukhiIdx = cursor.getColumnIndex("gurmukhi")
                    val punjabiIdx = cursor.getColumnIndex("punjabi_translation")
                    while (cursor.moveToNext()) {
                        val rawGurmukhi = if (gurmukhiIdx >= 0 && !cursor.isNull(gurmukhiIdx)) cursor.getString(gurmukhiIdx) else ""
                        val punjabi = if (punjabiIdx >= 0 && !cursor.isNull(punjabiIdx)) cursor.getString(punjabiIdx) else ""
                        if (rawGurmukhi.isNotEmpty() && punjabi.isNotEmpty()) {
                            val unicodeLine = convertGurbaniAkharToUnicode(rawGurmukhi)
                            map[rawGurmukhi] = punjabi
                            map[unicodeLine] = punjabi
                            val cleanLine = unicodeLine.replace("॥", "").replace("।", "").replace("|", "").trim()
                            if (cleanLine.isNotEmpty()) {
                                map[cleanLine] = punjabi
                            }
                            val cleanRaw = rawGurmukhi.replace("॥", "").replace("।", "").replace("|", "").trim()
                            if (cleanRaw.isNotEmpty()) {
                                map[cleanRaw] = punjabi
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error building Punjabi translation map: ${e.message}", e)
            }
            cachedPunjabiMap = map
            map
        }
    }

    companion object {
        private const val DB_NAME = "database.db"
        private const val TAG = "SggsDatabase"
        private const val MIN_SIZE = 100 * 1024 * 1024L // 100 MB
        private const val DB_URL = "https://github.com/mamusic885/gurbani-khoj-app/releases/download/v1.0-database/database.db"

        @Volatile
        private var INSTANCE: SggsDatabase? = null

        fun getInstance(context: Context): SggsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SggsDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun copyDatabaseIfNeeded(context: Context, dbFile: File) {
            Log.d(TAG, "database path: ${dbFile.absolutePath}")
            Log.d(TAG, "exists = ${dbFile.exists()}")
            Log.d(TAG, "file size = ${if (dbFile.exists()) dbFile.length() else 0}")

            if (dbFile.exists() && dbFile.length() > MIN_SIZE) {
                Log.d(TAG, "Valid database file already exists (${dbFile.length()} bytes)")
                return
            }

            val parentDir = dbFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                val created = parentDir.mkdirs()
                Log.d(TAG, "Created databases directory: $created")
            }

            var copiedFromAssets = false
            val localTmpDb = File("/tmp/database.db")
            if (localTmpDb.exists() && localTmpDb.length() > MIN_SIZE) {
                try {
                    Log.d(TAG, "Copying local /tmp/database.db to ${dbFile.absolutePath}...")
                    localTmpDb.copyTo(dbFile, overwrite = true)
                    if (dbFile.exists() && dbFile.length() > MIN_SIZE) {
                        Log.d(TAG, "Local copy successful")
                        copiedFromAssets = true
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Local /tmp/database.db copy failed: ${e.message}")
                }
            }

            if (!copiedFromAssets) {
                try {
                    val assetList = context.assets.list("") ?: emptyArray()
                    if (DB_NAME in assetList) {
                        Log.d(TAG, "Copying assets/$DB_NAME to ${dbFile.absolutePath}...")
                        context.assets.open(DB_NAME).use { input ->
                            FileOutputStream(dbFile).use { output ->
                                input.copyTo(output)
                                output.flush()
                            }
                        }
                        if (dbFile.exists() && dbFile.length() > MIN_SIZE) {
                            Log.d(TAG, "copy successful")
                            copiedFromAssets = true
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Assets copy skipped or failed: ${e.message}")
                }
            }

            if (copiedFromAssets) return

            Log.d(TAG, "Downloading database from $DB_URL...")
            val tempFile = File(dbFile.parentFile, "$DB_NAME.tmp")
            if (tempFile.exists()) {
                try { tempFile.delete() } catch (_: Exception) {}
            }

            try {
                var currentUrl = DB_URL
                var connection: java.net.HttpURLConnection? = null
                var redirects = 0
                val maxRedirects = 5

                while (redirects < maxRedirects) {
                    val url = java.net.URL(currentUrl)
                    connection = url.openConnection() as java.net.HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 30000
                    connection.readTimeout = 120000
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")

                    val responseCode = connection.responseCode
                    if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == 303 || responseCode == 307 || responseCode == 308
                    ) {
                        val newUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (!newUrl.isNullOrEmpty()) {
                            currentUrl = newUrl
                            redirects++
                            continue
                        }
                    }
                    break
                }

                val conn = connection ?: throw IllegalStateException("Unable to open connection")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }

                if (tempFile.exists() && tempFile.length() > MIN_SIZE) {
                    if (dbFile.exists()) {
                        try { dbFile.delete() } catch (_: Exception) {}
                    }
                    if (!tempFile.renameTo(dbFile)) {
                        tempFile.copyTo(dbFile, overwrite = true)
                        try { tempFile.delete() } catch (_: Exception) {}
                    }
                    Log.d(TAG, "copy successful")
                } else {
                    val downloadedSize = if (tempFile.exists()) tempFile.length() else 0
                    if (tempFile.exists()) {
                        try { tempFile.delete() } catch (_: Exception) {}
                    }
                    throw IllegalStateException("Downloaded database size ($downloadedSize bytes) is invalid or <= 100 MB")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download database: ${e.message}", e)
                if (tempFile.exists()) {
                    try { tempFile.delete() } catch (_: Exception) {}
                }
                throw IllegalStateException("Failed to initialize database from assets or $DB_URL: ${e.message}", e)
            }
        }
    }
}



