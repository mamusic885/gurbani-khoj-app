package com.example.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookmarks: List<Bookmark>)

    @Query("DELETE FROM bookmarks WHERE fileName = :fileName AND lineIndex = :lineIndex")
    suspend fun deleteBookmark(fileName: String, lineIndex: Int)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
}
