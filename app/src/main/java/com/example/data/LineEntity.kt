package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lines",
    indices = [
        Index(value = ["first_letters"]),
        Index(value = ["shabad_id"]),
        Index(value = ["source_page"])
    ]
)
data class LineEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String = "",
    @ColumnInfo(name = "shabad_id") val shabad_id: String = "",
    @ColumnInfo(name = "source_page") val source_page: Int = 1,
    @ColumnInfo(name = "source_line") val source_line: Int = 0,
    @ColumnInfo(name = "order_id") val order_id: Int = 0,
    @ColumnInfo(name = "gurmukhi") val gurmukhi: String = "",
    @ColumnInfo(name = "first_letters") val first_letters: String = "",
    @ColumnInfo(name = "vishraam_first_letters") val vishraam_first_letters: String = "",
    @ColumnInfo(name = "pronunciation") val pronunciation: String = "",
    @ColumnInfo(name = "pronunciation_information") val pronunciation_information: String = "",
    @ColumnInfo(name = "type_id") val type_id: Int = 0
) {
    @Ignore var transliteration: String = ""
    @Ignore var translation: String = ""
    @Ignore var raag: String = ""
    @Ignore var writer: String = ""

    constructor(
        id: String,
        shabad_id: String,
        source_page: Int,
        source_line: Int,
        order_id: Int,
        gurmukhi: String,
        first_letters: String,
        vishraam_first_letters: String,
        pronunciation: String,
        pronunciation_information: String,
        type_id: Int,
        transliteration: String,
        translation: String,
        raag: String,
        writer: String
    ) : this(
        id, shabad_id, source_page, source_line, order_id, gurmukhi,
        first_letters, vishraam_first_letters, pronunciation, pronunciation_information, type_id
    ) {
        this.transliteration = transliteration
        this.translation = translation
        this.raag = raag
        this.writer = writer
    }
}

