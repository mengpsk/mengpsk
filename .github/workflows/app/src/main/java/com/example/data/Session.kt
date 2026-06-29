package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val audioDurationSec: Int = 0,
    val rawText: String = "",
    val transAndFormatResult: String? = null,
    val summaryResult: String? = null,
    val actionItemsResult: String? = null,
    val mindMapResult: String? = null,
    val chatHistoryJson: String? = null, // JSON representation of historical interactive chat
    val prepareExportResult: String? = null,
    val audioPath: String? = null
)
