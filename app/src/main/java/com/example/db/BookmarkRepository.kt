package com.example.db

import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    suspend fun insert(bookmark: Bookmark) {
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun insertAll(bookmarks: List<Bookmark>) {
        bookmarkDao.insertAll(bookmarks)
    }

    suspend fun delete(fileName: String, lineIndex: Int) {
        bookmarkDao.deleteBookmark(fileName, lineIndex)
    }

    suspend fun clearAll() {
        bookmarkDao.deleteAllBookmarks()
    }
}
