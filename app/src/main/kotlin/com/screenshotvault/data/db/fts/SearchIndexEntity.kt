package com.screenshotvault.data.db.fts

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "search_index")
@Fts4(contentEntity = SearchableContent::class)
data class SearchIndexEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Int = 0,

    @ColumnInfo(name = "screenshot_id")
    val screenshotId: String,

    val title: String,

    val summary: String,

    val topics: String,

    val entities: String,

    @ColumnInfo(name = "ocr_text")
    val ocrText: String,

    val domain: String,

    val type: String,
)

@Entity(tableName = "searchable_content")
data class SearchableContent(
    @PrimaryKey
    @ColumnInfo(name = "screenshot_id")
    val screenshotId: String,

    val title: String = "",

    val summary: String = "",

    val topics: String = "",

    val entities: String = "",

    @ColumnInfo(name = "ocr_text")
    val ocrText: String = "",

    val domain: String = "",

    val type: String = "",
)
