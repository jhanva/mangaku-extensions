package eu.kanade.tachiyomi.source.model

interface SChapter {
    var url: String
    var name: String
    var date_upload: Long
    var chapter_number: Float
    var scanlator: String?

    fun copyFrom(other: SChapter) {
        name = other.name
        url = other.url
        date_upload = other.date_upload
        chapter_number = other.chapter_number
        scanlator = other.scanlator
    }

    companion object {
        fun create(): SChapter = SChapterImpl()
    }
}

private class SChapterImpl : SChapter {
    override var url: String = ""
    override var name: String = ""
    override var date_upload: Long = 0
    override var chapter_number: Float = -1f
    override var scanlator: String? = null
}
