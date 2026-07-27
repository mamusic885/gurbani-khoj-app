package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.util.LruCache
import com.example.util.convertGurbaniAkharToUnicode
import java.io.File
import java.io.FileOutputStream

class SggsDatabase private constructor(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1) {

    @Volatile
    private var openDb: SQLiteDatabase? = null

    // In-memory caches for fast retrieval
    private val shabadCache = LruCache<String, List<LineWithTranslation>>(200)
    private val angCache = LruCache<Int, List<LineWithTranslation>>(200)

    // Pre-mapped verse caches for UI composables
    private val shabadVerseCache = LruCache<String, Pair<List<com.example.Verse>, String>>(200)
    private val angVerseCache = LruCache<Int, List<com.example.Verse>>(200)

    fun getCachedShabadVerses(shabadId: String): Pair<List<com.example.Verse>, String>? = shabadVerseCache.get(shabadId)
    fun putCachedShabadVerses(shabadId: String, verses: List<com.example.Verse>, title: String) {
        shabadVerseCache.put(shabadId, Pair(verses, title))
    }

    fun getCachedAngVerses(ang: Int): List<com.example.Verse>? = angVerseCache.get(ang)
    fun putCachedAngVerses(ang: Int, verses: List<com.example.Verse>) {
        angVerseCache.put(ang, verses)
    }

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
            SQLiteDatabase.OPEN_READWRITE
        )
        openDb = db
        Log.d(TAG, "SQLite Database opened successfully")
        ensureIndexesAndPragmas(db)
        return db
    }

    private fun ensureIndexesAndPragmas(db: SQLiteDatabase) {
        try {
            db.execSQL("PRAGMA journal_mode = WAL;")
            db.execSQL("PRAGMA synchronous = NORMAL;")
            db.execSQL("PRAGMA cache_size = -8000;")

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lines_shabad_id ON lines(shabad_id);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lines_source_page ON lines(source_page);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lines_order_id ON lines(order_id);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_lines_first_letters ON lines(first_letters);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shabads_source_id ON shabads(source_id);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_translations_line_source ON translations(line_id, translation_source_id);")
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring pragmas or indexes: ${e.message}")
        }
    }

    fun searchByAng(ang: Int): List<LineWithTranslation> {
        angCache.get(ang)?.let { return it }

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
                val raagUnicode = if (rawRaag.isNotEmpty()) convertGurbaniAkharToUnicode(rawRaag) else ""

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
        angCache.put(ang, list)
        return list
    }

    fun getShabadByShabadId(shabadId: String): List<LineWithTranslation> {
        shabadCache.get(shabadId)?.let { return it }

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
                val raagUnicode = if (rawRaag.isNotEmpty()) convertGurbaniAkharToUnicode(rawRaag) else ""

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
        shabadCache.put(shabadId, list)
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

    fun hasPunjabiMap(): Boolean = cachedPunjabiMap != null

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
            if (dbFile.exists() && dbFile.length() > MIN_SIZE) {
                return
            }

            val parentDir = dbFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs()
            }

            var copiedFromAssets = false
            val localTmpDb = File("/tmp/database.db")
            if (localTmpDb.exists() && localTmpDb.length() > MIN_SIZE) {
                try {
                    localTmpDb.copyTo(dbFile, overwrite = true)
                    if (dbFile.exists() && dbFile.length() > MIN_SIZE) {
                        copiedFromAssets = true
                    }
                } catch (_: Exception) {}
            }

            if (!copiedFromAssets) {
                try {
                    val assetList = context.assets.list("") ?: emptyArray()
                    if (DB_NAME in assetList) {
                        context.assets.open(DB_NAME).use { input ->
                            FileOutputStream(dbFile).use { output ->
                                input.copyTo(output)
                                output.flush()
                            }
                        }
                        if (dbFile.exists() && dbFile.length() > MIN_SIZE) {
                            copiedFromAssets = true
                        }
                    }
                } catch (_: Exception) {}
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
                throw IllegalStateException("Failed to initialize database: ${e.message}", e)
            }
        }
    }
}
