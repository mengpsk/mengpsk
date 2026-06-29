package com.example.engine

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// --- Data Classes for Parsed Responses ---

data class TranscriptionData(
    val transcription: String
)

data class SummaryTemplatesData(
    val executive_summary: String = "",
    val meeting_minutes: String = "",
    val short_notes: String = ""
)

data class ActionItem(
    val task: String,
    val assignee: String = "",
    val deadline: String = ""
)

data class ActionItemsData(
    val action_items: List<ActionItem> = emptyList()
)

data class MindMapBranch(
    val main_idea: String,
    val sub_ideas: List<String> = emptyList()
)

data class MindMapTree(
    val root: String,
    val branches: List<MindMapBranch> = emptyList()
)

data class MindMapData(
    val mind_map: MindMapTree? = null
)

data class ChatResponseData(
    val chat_response: String
)

data class ExportData(
    val export_markdown_content: String = "",
    val export_mindmap_clean: MindMapTree? = null
)

// --- Parser Implementation ---

object MenuResponseParser {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun parseTranscription(json: String): TranscriptionData? {
        return try {
            moshi.adapter(TranscriptionData::class.java).fromJson(json)
        } catch (e: Exception) {
            // Fallback manually if parser fails
            val textMatch = "\"transcription\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(json)
            if (textMatch != null) {
                TranscriptionData(textMatch.groupValues[1])
            } else {
                TranscriptionData(json)
            }
        }
    }

    fun parseSummaryTemplates(json: String): SummaryTemplatesData? {
        return try {
            moshi.adapter(SummaryTemplatesData::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun parseActionItems(json: String): ActionItemsData? {
        return try {
            moshi.adapter(ActionItemsData::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun parseMindMap(json: String): MindMapData? {
        return try {
            moshi.adapter(MindMapData::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun parseChatResponse(json: String): ChatResponseData? {
        return try {
            moshi.adapter(ChatResponseData::class.java).fromJson(json)
        } catch (e: Exception) {
            val textMatch = "\"chat_response\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(json)
            if (textMatch != null) {
                ChatResponseData(textMatch.groupValues[1])
            } else {
                ChatResponseData(json)
            }
        }
    }

    fun parseExport(json: String): ExportData? {
        return try {
            moshi.adapter(ExportData::class.java).fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
