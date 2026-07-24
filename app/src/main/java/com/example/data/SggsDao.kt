package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SggsDao {
    @Query("SELECT * FROM lines WHERE source_page = :ang ORDER BY order_id ASC")
    fun getLinesByAng(ang: Int): Flow<List<LineEntity>>

    @Query("SELECT * FROM lines WHERE source_page = :ang ORDER BY order_id ASC")
    suspend fun searchByAng(ang: Int): List<LineEntity>

    @Query("SELECT * FROM lines WHERE first_letters LIKE :query || '%' OR first_letters LIKE :asciiQuery || '%' ORDER BY source_page ASC, order_id ASC LIMIT 200")
    suspend fun searchByFirstLetters(query: String, asciiQuery: String = query): List<LineEntity>

    @Query("SELECT * FROM lines WHERE gurmukhi LIKE '%' || :query || '%' OR gurmukhi LIKE '%' || :asciiQuery || '%' ORDER BY source_page ASC, order_id ASC LIMIT 200")
    suspend fun searchByFullText(query: String, asciiQuery: String = query): List<LineEntity>

    @Query("SELECT * FROM lines WHERE shabad_id = :shabadId ORDER BY order_id ASC")
    suspend fun getShabadByShabadId(shabadId: String): List<LineEntity>

    @Query("SELECT COUNT(*) FROM lines")
    suspend fun getLineCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lines: List<LineEntity>)
}
