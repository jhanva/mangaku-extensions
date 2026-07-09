package com.mangaku.extension.comick

import com.google.gson.annotations.SerializedName

/**
 * DTOs de la API JSON de ComicK (api.comick.io). `tachiyomi=true` pide la forma compacta que
 * consumen los clientes. Todos los campos son nullable: el mapeo degrada de forma segura ante datos
 * ausentes en vez de fallar.
 */

class ComicKSearchItemDto {
    val hid: String? = null
    val slug: String? = null
    val title: String? = null
    @SerializedName("md_covers") val mdCovers: List<ComicKCoverDto>? = null
}

class ComicKCoverDto {
    val b2key: String? = null
}

class ComicKComicDetailDto {
    val comic: ComicKComicDto? = null
}

class ComicKComicDto {
    val hid: String? = null
    val slug: String? = null
    val title: String? = null
    val desc: String? = null
    /** 1 = en emision, 2 = completado, 3 = cancelado, 4 = en pausa. */
    val status: Int? = null
    @SerializedName("md_covers") val mdCovers: List<ComicKCoverDto>? = null
    @SerializedName("md_comic_md_genres") val genres: List<ComicKGenreLinkDto>? = null
}

class ComicKGenreLinkDto {
    @SerializedName("md_genres") val mdGenres: ComicKGenreDto? = null
}

class ComicKGenreDto {
    val name: String? = null
}

class ComicKChaptersDto {
    val chapters: List<ComicKChapterDto>? = null
    val total: Int? = null
}

class ComicKChapterDto {
    val hid: String? = null
    val chap: String? = null
    val vol: String? = null
    val title: String? = null
    val lang: String? = null
    @SerializedName("group_name") val groupName: List<String>? = null
    @SerializedName("created_at") val createdAt: String? = null
}

class ComicKChapterPagesDto {
    val chapter: ComicKChapterImagesDto? = null
}

class ComicKChapterImagesDto {
    val hid: String? = null
    @SerializedName("md_images") val mdImages: List<ComicKImageDto>? = null
}

class ComicKImageDto {
    val b2key: String? = null
}
