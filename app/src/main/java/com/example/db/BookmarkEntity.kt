package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val baniName: String,
    val fileName: String,
    val lineIndex: Int,
    val verseLine: String,
    val translation: String,
    val timestamp: Long = System.currentTimeMillis()
)
