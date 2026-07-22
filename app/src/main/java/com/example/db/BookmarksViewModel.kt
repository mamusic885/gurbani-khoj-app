package com.example.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookmarksViewModel(private val repository: BookmarkRepository) : ViewModel() {
    val allBookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleBookmark(
        baniName: String,
        fileName: String,
        lineIndex: Int,
        verseLine: String,
        translation: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val bookmarksList = allBookmarks.value
            val existing = bookmarksList.find { it.fileName == fileName && it.lineIndex == lineIndex }
            if (existing != null) {
                repository.delete(fileName, lineIndex)
                onResult("ਬੁੱਕਮਾਰਕ ਹਟਾ ਦਿੱਤਾ ਗਿਆ॥")
            } else {
                repository.insert(
                    Bookmark(
                        baniName = baniName,
                        fileName = fileName,
                        lineIndex = lineIndex,
                        verseLine = verseLine,
                        translation = translation
                    )
                )
                onResult("ਬੁੱਕਮਾਰਕ ਸੇਵ ਹੋ ਗਿਆ॥")
            }
        }
    }

    fun importBookmarks(bookmarks: List<Bookmark>, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            repository.insertAll(bookmarks)
            onComplete(bookmarks.size)
        }
    }

    fun clearAllBookmarks(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAll()
            onComplete()
        }
    }
}

class BookmarksViewModelFactory(private val repository: BookmarkRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookmarksViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookmarksViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
