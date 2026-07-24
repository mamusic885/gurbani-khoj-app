package com.example.data

import kotlinx.coroutines.flow.Flow

class SggsRepository(private val sggsDao: SggsDao) {

    fun getLinesByAng(ang: Int): Flow<List<LineEntity>> {
        return sggsDao.getLinesByAng(ang)
    }

    suspend fun searchByAng(ang: Int): List<LineEntity> {
        return sggsDao.searchByAng(ang)
    }

    suspend fun searchByFirstLetters(query: String, asciiQuery: String = query): List<LineEntity> {
        val cleanQuery = query.replace(" ", "")
        if (cleanQuery.isEmpty()) return emptyList()
        val cleanAscii = asciiQuery.replace(" ", "")
        return sggsDao.searchByFirstLetters(cleanQuery, cleanAscii)
    }

    suspend fun searchByFullText(query: String, asciiQuery: String = query): List<LineEntity> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return emptyList()
        val cleanAscii = asciiQuery.trim()
        return sggsDao.searchByFullText(cleanQuery, cleanAscii)
    }

    suspend fun getShabadByShabadId(shabadId: String): List<LineEntity> {
        return sggsDao.getShabadByShabadId(shabadId)
    }
}
