package com.vegerot.apodesktop

import org.junit.Assert.assertEquals
import org.junit.Test

class ApodEntryTest {
    @Test
    fun newApiEntriesKeepNewestImageLastRegardlessOfResponseOrder() {
        val entries = ApodEntry.fromJsonArray(
            """[
                {"date":"2026-10-02","media_type":"image","hdurl":"https://example.com/today.png","url":"https://example.com/article"},
                {"date":"2026-09-30","media_type":"image","hdurl":"https://example.com/older.png"},
                {"date":"2026-10-01","media_type":"image","hdurl":"https://example.com/yesterday.png"}
            ]""",
        )
        assertEquals("2026-10-02", entries.last().date)
        assertEquals("https://example.com/today.png", entries.last().hdUrl)
        assertEquals("2026-10-01", entries[entries.lastIndex - 1].date)
    }

    @Test
    fun skipsVideosAndMissingImageUrlsInsteadOfDownloadingArticlePages() {
        val entries = ApodEntry.fromJsonArray(
            """[
                {"media_type":"video","hdurl":"https://example.com/video"},
                {"media_type":"image","url":"https://example.com/article"},
                {"media_type":"image","hdurl":null},
                {"media_type":"image","hdurl":""},
                {"media_type":"image","hdurl":"  "},
                {"media_type":"image","hdurl":"https://example.com/image.png"}
            ]""",
        )
        assertEquals(listOf("https://example.com/image.png"), entries.map { it.hdUrl })
    }
}
