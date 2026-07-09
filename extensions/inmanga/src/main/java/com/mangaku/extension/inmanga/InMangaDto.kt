package com.mangaku.extension.inmanga

import com.google.gson.annotations.SerializedName

/**
 * La API de InManga devuelve `{ "data": "<json string>" }`: el campo `data` es a su vez un JSON
 * serializado como cadena, que hay que volver a parsear ([InMangaChapterResult]).
 */
class InMangaResultDto(
    @SerializedName("data") val data: String? = null,
)

class InMangaChapterResult(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("result") val result: List<InMangaChapterDto> = emptyList(),
)

class InMangaChapterDto(
    @SerializedName("Number") val number: Double? = null,
    @SerializedName("RegistrationDate") val registrationDate: String = "",
    @SerializedName("Identification") val identification: String? = "",
    @SerializedName("FriendlyChapterNumber") val friendlyChapterNumber: String? = "",
)
