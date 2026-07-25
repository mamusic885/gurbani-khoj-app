package com.example.data

data class LineEntity(
    val id: String = "",
    val shabad_id: String = "",
    val source_page: Int = 1,
    val source_line: Int? = null,
    val first_letters: String? = null,
    val vishraam_first_letters: String? = null,
    val gurmukhi: String = "",
    val pronunciation: String? = null,
    val pronunciation_information: String? = null,
    val type_id: Int? = null,
    val order_id: Int = 0,
    var transliteration: String = "",
    var translation: String = "",
    var punjabiTranslation: String = "",
    var raag: String = "",
    var writer: String = ""
)

data class LineWithTranslation(
    val line: LineEntity,
    val translation: String?,
    val punjabiTranslation: String? = null
)



