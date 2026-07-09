package com.mangaku.extension.senmanga

import eu.kanade.tachiyomi.source.model.SManga

/**
 * DTOs de la API JSON de SenManga, portados de keiyoushi/extensions-source (Apache 2.0). El original
 * usa kotlinx.serialization; aqui se parsean con Gson (los nombres de campo coinciden con el JSON).
 */
class DirectoryResponse {
    val currentPage: Int? = null
    val totalPages: Int? = null
    val series: List<SeriesDto> = emptyList()
}

class HomeResponse {
    val series: List<SeriesDto> = emptyList()
}

class SeriesDto {
    val title: String? = null
    val slug: String? = null
    val cover: String? = null
    val status: String? = null
    val genre: String? = null
    val description: String? = null
    val chapterList: List<ChapterDto>? = null

    fun toSManga(): SManga = SManga.create().apply {
        this.title = this@SeriesDto.title.orEmpty()
        this.url = slug.orEmpty()
        this.thumbnail_url = cover
        this.description = this@SeriesDto.description
        this.genre = this@SeriesDto.genre
        this.status = parseStatus(this@SeriesDto.status)
    }

    private fun parseStatus(statusString: String?): Int = when {
        statusString == null -> SManga.UNKNOWN
        statusString.contains("ongoing", ignoreCase = true) -> SManga.ONGOING
        statusString.contains("complete", ignoreCase = true) -> SManga.COMPLETED
        statusString.contains("hiatus", ignoreCase = true) -> SManga.ON_HIATUS
        statusString.contains("dropped", ignoreCase = true) -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }
}

class ChapterDto {
    val title: String? = null
    val url: String? = null
    val datetime: String? = null
}

class ReadResponse {
    val pages: List<String> = emptyList()
}
