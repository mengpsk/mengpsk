package com.example.ui.components

import kotlinx.coroutines.delay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.*

private val AppBrandGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF6750A4), Color(0xFF9070E0))
)

// --- Export Action Row Composable for All Results ---
@Composable
fun ExportActionRow(
    title: String,
    content: String,
    isMindMap: Boolean = false,
    rawJson: String = ""
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "แบ่งปันและส่งออกไฟล์ความเห็น",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Box {
                Button(
                    onClick = { expanded = !expanded },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("แบ่งปันส่งออก...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (isMindMap) {
                        DropdownMenuItem(
                            text = { Text("แชร์โครงสร้างรูปภาพ (.PNG)") },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                expanded = false
                                ExportShareHelper.shareMindMapPng(context, "$title - Mindmap", title, rawJson)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("แชร์แก่นข้อความต้นไม้ (.TXT)") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                expanded = false
                                ExportShareHelper.shareTextFile(context, "$title - Mindmap_Text", "txt", content)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("ส่งออกไฟล์ข้อความดิบ (.TXT)") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                expanded = false
                                ExportShareHelper.shareTextFile(context, title, "txt", content)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ส่งออกไฟล์เวิร์ดออฟฟิศ (.DOC)") },
                            leadingIcon = { Icon(Icons.Default.Article, contentDescription = null, tint = Color(0xFF1F4E79), modifier = Modifier.size(16.dp)) },
                            onClick = {
                                expanded = false
                                ExportShareHelper.shareTextFile(context, title, "doc", content)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("ส่งออกไฟล์เอกสาร PDF (.PDF)") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                expanded = false
                                ExportShareHelper.sharePdfFile(context, title, title, content)
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- 1. Transcription Viewer ---
@Composable
fun TranscriptionView(json: String) {
    val data = MenuResponseParser.parseTranscription(json)
    if (data == null) {
        RawJsonFallbackView(json)
        return
    }

    val lines = data.transcription.split("\n")
    Card(
        modifier = Modifier.fillMaxWidth().testTag("transandformat_result_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("รายงานถอดความแยกเวทีผู้พูดอัจฉริยะ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            lines.filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val speaker = parts[0].trim().replace("[", "").replace("]", "")
                    val text = parts[1].trim()

                    // Assign consistent speaker gradient/color based on speaker suffix numeric
                    val speakerColor = when {
                        speaker.contains("1") || speaker.contains("สมยศ") || speaker.contains("ต้น") -> Color(0xFF6750A4)
                        speaker.contains("2") || speaker.contains("แพรว") || speaker.contains("สิทธิ์") -> Color(0xFF9070E0)
                        else -> Color(0xFF7D5260)
                    }

                    Column(modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(speakerColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = speaker,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = speakerColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                } else {
                    // Regular line
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ExportActionRow(
                title = "บทสนทนาถอดความแยกผู้พูด",
                content = data.transcription
            )
        }
    }
}

// --- 2. Summary Templates Viewer ---
@Composable
fun SummaryTemplatesView(json: String) {
    val data = MenuResponseParser.parseSummaryTemplates(json)
    if (data == null) {
        RawJsonFallbackView(json)
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ภาพรวมผู้บริหาร", "รายงานประชุมสากล", "โน้ตย่อสาระสำคัญ")

    Column(modifier = Modifier.fillMaxWidth().testTag("summary_templates_result")) {
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (selectedTab) {
                    0 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF00C6FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Executive Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = data.executive_summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        }
                    }
                    1 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFFA832FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Official Minutes Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = data.meeting_minutes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                    2 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFE7B10A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quick Smart Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = data.short_notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                val compiledSummary = """
                    === 1. Executive Summary (ภาพรวมผู้บริหาร) ===
                    ${data.executive_summary}
                    
                    === 2. Meeting Minutes (รายงานสากล) ===
                    ${data.meeting_minutes}
                    
                    === 3. Smart Notes (โน้ตย่อสำคัญ) ===
                    ${data.short_notes}
                """.trimIndent()
                ExportActionRow(
                    title = "รายงานสรุปวิเคราะห์การประชุม",
                    content = compiledSummary
                )
            }
        }
    }
}

// --- 3. Action Items Viewer ---
@Composable
fun ActionItemsView(json: String) {
    val data = MenuResponseParser.parseActionItems(json)
    if (data == null || data.action_items.isEmpty()) {
        RawJsonFallbackView(json)
        return
    }

    // Store checklist status locally so user can tap completed
    val checkedTasks = remember { mutableStateMapOf<Int, Boolean>() }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("action_items_result"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFF00FA9A))
                Spacer(modifier = Modifier.width(8.dp))
                Text("รายการสิ่งที่ต้องปฏิบัติ (Action Items)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("AI คัดกรองงานที่กำหนด คณะทำงาน และส่งมอบข้อมูลส่งมอบชัดเจน", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            data.action_items.forEachIndexed { index, item ->
                val isChecked = checkedTasks[index] ?: false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isChecked) MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                            else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                        .clickable { checkedTasks[index] = !isChecked }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checkedTasks[index] = it!! }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.task,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.assignee.isNotBlank()) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(item.assignee, fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                    modifier = Modifier.height(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            if (item.deadline.isNotBlank()) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(item.deadline, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val actionItemsText = data.action_items.mapIndexed { i, item ->
                "${i + 1}. [ผู้ปฏิบัติ: ${item.assignee.ifBlank { "ทั่วไป" }}] - งาน: ${item.task} (กำหนดส่งวันที่: ${item.deadline.ifBlank { "ไม่มีกำหนด" }})"
            }.joinToString("\n")
            ExportActionRow(
                title = "รายการบทบัญญัติข้อปฏิบัติ Action Items",
                content = actionItemsText
            )
        }
    }
}

// --- 4. Mindmap Viewer ---
@Composable
fun MindMapView(json: String) {
    val data = MenuResponseParser.parseMindMap(json)
    val tree = data?.mind_map
    if (tree == null) {
        RawJsonFallbackView(json)
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("mindmatch_result_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = Color(0xFF00C6FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("แก่นความคิดรูปแบบต้นไม้ (Interactive Mindmap)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("คลิกย่อเพื่อกางโครงสร้างออกเพื่อสำรวจรายละเอียดโครงข่ายเด่นชัด", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Render root central concept
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppBrandGradient)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tree.root,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Branches list
            tree.branches.forEach { branch ->
                var expanded by remember { mutableStateOf(true) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                        .clickable { expanded = !expanded }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0072FF))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = branch.main_idea,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(start = 14.dp, top = 8.dp)) {
                            branch.sub_ideas.forEach { sub ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("•", color = Color(0xFF00C6FF), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val mindMapOutlineText = buildString {
                appendLine("=== ผังความคิด: ${tree.root} ===")
                tree.branches.forEach { b ->
                    appendLine("├─ ${b.main_idea}")
                    b.sub_ideas.forEach { s ->
                        appendLine("│  └─ $s")
                    }
                }
            }
            ExportActionRow(
                title = tree.root,
                content = mindMapOutlineText,
                isMindMap = true,
                rawJson = json
            )
        }
    }
}

// --- 5. Interactive Chat Viewer ---
@Composable
fun InteractiveChatView(
    chatHistory: List<Pair<String, String>>,
    queryText: String,
    onQueryChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("interactive_chat_result_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFFA832FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("พูดคุยเจาะลึกเฉพาะบันทึก (Context Chat)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("ระบบจะจำกัดคำตอบเฉพาะข้อมูลภายในบทสนทนาอัดนี้เท่านั้น", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            if (chatHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ยังไม่มีหัวข้อแชท ลองพิมพ์คำถามเกี่ยวกับผู้รับผิดชอบหรือเวลาล่ม", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                chatHistory.forEach { log ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        // User message
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp, 1.dp, 12.dp, 12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(10.dp)
                            ) {
                                Text(log.first, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        // AI Response
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(1.dp, 12.dp, 12.dp, 12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
                                    .padding(10.dp)
                            ) {
                                Text(log.second, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Suggestions Quick Chips
            val chatSuggestions = listOf("มีงานด่วนของใครบ้าง?", "สรุปเวลาที่เกิดปัญหาให้หน่อย", "ข้อตกลงบทสรุปคืออะไร?")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chatSuggestions.forEach { suggest ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                            .clickable { onQueryChange(suggest) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(suggest, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Send field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = onQueryChange,
                    placeholder = { Text("พิมพ์คำถามของคุณที่นี่...", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSendClick,
                    enabled = queryText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (queryText.isNotBlank() && !isProcessing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "ส่งคำถาม", tint = if (queryText.isNotBlank()) Color.White else Color.Gray)
                    }
                }
            }
        }
    }
}

// --- 6. Prepare Export Viewer ---
@Composable
fun PrepareExportView(json: String) {
    val data = MenuResponseParser.parseExport(json)
    if (data == null) {
        RawJsonFallbackView(json)
        return
    }

    val clipboardManager = LocalClipboardManager.current
    var showCopiedAlert by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("prepare_export_result"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ImportExport, contentDescription = null, tint = Color(0xFF00FA9A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("เตรียมพร้อมการส่งออก (Pre-compiled Export)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(data.export_markdown_content))
                        showCopiedAlert = true
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("คัดลอก Markdown", fontSize = 10.sp)
                }
            }

            if (showCopiedAlert) {
                Text("✓ คัดลอก Markdown เรียบร้อยแล้ว!", color = Color(0xFF00875A), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                LaunchedEffect(Unit) {
                    delay(2000)
                    showCopiedAlert = false
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle 1: Doc/PDF
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Article, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ฟอร์แมตเอกสาร Markdown (สำหรับ Doc และ PDF):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(12.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = data.export_markdown_content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFD4D4D4),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle 2: Clean Mindmap PNG
            val cleanTree = data.export_mindmap_clean
            if (cleanTree != null) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Photo, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("สรุปคำรัดกุมพร้อมวาด (สำหรับ Mindmap PNG):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                        .padding(10.dp)
                ) {
                    Text("หัวข้อหลัก: ${cleanTree.root}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    cleanTree.branches.forEach { b ->
                        Text("├─ ขาไอเดียย่อย: ${b.main_idea}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        b.sub_ideas.forEach { s ->
                            Text("│  └─ $s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.size(dp: Float): Modifier {
    return this.size(dp.dp)
}

// --- Fallback View for debugging ---
@Composable
fun RawJsonFallbackView(json: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ผลลัพธ์ดิบ (Raw JSON Response)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2B2B2B))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = json,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFA9B7C6)
                        )
                    }
                }
            }
        }
    }
}
