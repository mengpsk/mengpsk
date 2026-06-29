package com.example

import android.Manifest
import android.content.Context
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.BorderStroke
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Session
import com.example.engine.MengAiMenu
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

private val BrandCyan = Color(0xFF6750A4)
private val BrandBlue = Color(0xFF9070E0)
private val AppBrandGradient = Brush.horizontalGradient(
    colors = listOf(BrandCyan, BrandBlue)
)

private val SlateBg = Color(0xFFF7F2FA)
private val CardSurface = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Collect StateFlows from ViewModel
    val sessions by viewModel.allSessions.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val activeMenu by viewModel.activeMenu.collectAsState()
    
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val recordingAmplitudes by viewModel.recordingAmplitudes.collectAsState()
    
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingError by viewModel.processingError.collectAsState()
    
    val inputDraftText by viewModel.inputDraftText.collectAsState()
    val chatQuery by viewModel.chatQuery.collectAsState()
    val chatHistory by viewModel.activeChatHistory.collectAsState()

    var showHistoryDrawer by remember { mutableStateOf(false) }

    // Audio Permission Request Handler
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "จำเป็นต้องใช้สิทธิ์การอัดเสียงเพื่ออัดเสียงจริง!", Toast.LENGTH_SHORT).show()
        }
    }

    // Storage/Media Permissions Request (to fulfill "ให้สิทธิ์เข้าใช้งานสื่อและไฟล์")
    var hasStoragePermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val isGranted = permissionsMap.values.any { it }
        hasStoragePermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "สิทธิ์เข้าใช้งานสื่อและไฟล์สำเร็จ!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "กรุณาเปิดสิทธิ์เข้าใช้งานไฟล์เสียงในเมนูตั้งค่าเพื่อใช้งานนำเข้า!", Toast.LENGTH_LONG).show()
        }
    }

    // Helper function to resolve name from URI
    fun getFileNameFromUri(context: android.content.Context, uri: android.net.Uri): String {
        var name = "audio_record.mp3"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            name = uri.lastPathSegment ?: "audio_record.mp3"
        }
        return name
    }

    // File Picker for specialized audio files: .mp3, .wav, .m4a, .acc, .aac (Requirement 4)
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            val lowercaseName = fileName.lowercase()
            val isValid = lowercaseName.endsWith(".mp3") || 
                          lowercaseName.endsWith(".wav") || 
                          lowercaseName.endsWith(".m4a") || 
                          lowercaseName.endsWith(".acc") || 
                          lowercaseName.endsWith(".aac")

            if (!isValid) {
                Toast.makeText(context, "ระบบไม่รองรับไฟล์นี้! กรุณาเลือกอัปโหลดเฉพาะ .mp3, .wav, .m4a, .acc หรือ .aac เท่านั้นค่ะ", Toast.LENGTH_LONG).show()
            } else {
                // Copy selected file to application cache for persistent playback
                var savedLocalPath: String? = null
                try {
                    val cacheDir = java.io.File(context.cacheDir, "audios")
                    if (!cacheDir.exists()) cacheDir.mkdirs()
                    val destFile = java.io.File(cacheDir, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    savedLocalPath = destFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Handle the imported audio with real background transcription
                viewModel.handleImportedAudio(fileName, savedLocalPath, uri.toString())
                
                Toast.makeText(context, "นำเข้าไฟล์เสียง $fileName สำเร็จ!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Main Canvas Layout
    Box(
        modifier = modifier
            .background(SlateBg)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // --- HEADER TITLE BAR ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF6750A4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MengAiRec",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1B1F),
                            modifier = Modifier.testTag("app_title")
                        )
                        Text(
                            text = "ULTIMATE ENGINE V2.4",
                            color = Color(0xFF6750A4),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Active Engine Status Pill
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Engine: Active",
                            color = Color(0xFF1D192B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // History button trigger
                    IconButton(
                        onClick = { showHistoryDrawer = true },
                        modifier = Modifier
                            .testTag("history_button")
                            .size(36.dp)
                            .background(Color(0xFFFFFFFF), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "คลังบันทึกประวัติ",
                            tint = Color(0xFF1C1B1F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable central workspace
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- SECTION 1: TRANSCRIPT SOURCE INPUT ---
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(CardSurface)
                            .border(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        // Title header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "กรอกข้อความหรืออัดเสียงสนทนา",
                                color = Color(0xFF1C1B1F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (currentSession != null) {
                                TextButton(
                                    onClick = { viewModel.clearActive() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6750A4)),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ทำใหม่", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Modern Control Panel containing Audio recording and Import File buttons side by side
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 1.15f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Microphone Recording control (Requirement 3)
                                    Button(
                                        onClick = {
                                            if (isRecording) {
                                                viewModel.stopRecording()
                                            } else {
                                                if (hasAudioPermission) {
                                                    viewModel.startRecording()
                                                } else {
                                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRecording) Color.Red else Color(0xFF6750A4)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("record_button")
                                            .scale(if (isRecording) pulseScale else 1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isRecording) "หยุดอัด" else "อัดเสียงจริง",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // 2. Audio File Import control (.mp3, .wav, .m4a, .acc) (Requirement 3 & 4)
                                    Button(
                                        onClick = {
                                            val requiredPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                Manifest.permission.READ_MEDIA_AUDIO
                                            } else {
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            }
                                            
                                            val isGranted = ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
                                            if (isGranted) {
                                                fileChooserLauncher.launch("audio/*")
                                            } else {
                                                val listToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
                                                } else {
                                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                                }
                                                storagePermissionLauncher.launch(listToRequest)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFE8DEF8),
                                            contentColor = Color(0xFF6750A4)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("import_file_button")
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            tint = Color(0xFF6750A4),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "นำเข้าไฟล์เสียง",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Status / Wave display (Requirement 3)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isRecording) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = String.format(
                                                    Locale.getDefault(),
                                                    "กำลังบันทึกเสียงสด... %02d:%02d",
                                                    recordingDurationSec / 60,
                                                    recordingDurationSec % 60
                                                ),
                                                color = Color.Red,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                recordingAmplitudes.forEach { amp ->
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.5.dp)
                                                            .height((amp * 20).coerceIn(4f, 20f).dp)
                                                            .clip(RoundedCornerShape(1.dp))
                                                            .background(Color.Red)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color(0xFF6750A4).copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (currentSession != null) {
                                                val prefix = if (currentSession!!.title.contains("ไฟล์นำเข้า")) "แฟ้มเสียงนำเข้า" else "ความยาวเสียงอัด"
                                                "$prefix: ${currentSession!!.audioDurationSec} วินาที | สกุลรองรับ .mp3, .wav, .m4a, .acc"
                                            } else {
                                                "อัดจากไมค์โดยตรง หรือนำกลุ่มไฟล์สกุล .mp3, .wav, .m4a, .acc ที่บันทึกไว้เข้าวิเคราะห์"
                                            },
                                            color = Color(0xFF49454F),
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Audio Player Controls Card (PLAY / STOP) (User Request & Requirement)
                        val sessionAudioPath = currentSession?.audioPath
                        if (!sessionAudioPath.isNullOrBlank()) {
                            val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .testTag("audio_player_control_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF6750A4).copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF6750A4))
                                            .clickable {
                                                if (isPlayingAudio) {
                                                    viewModel.stopAudio()
                                                } else {
                                                    viewModel.playAudio(sessionAudioPath)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "เล่น/หยุดเสียง",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isPlayingAudio) "กำลังเล่นไฟล์เสียงหลัก... 🔊" else "เปิดฟังคลิปเสียงบันทึกต้นฉบับ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1C1B1F)
                                        )
                                        Text(
                                            text = currentSession?.title ?: "ความยาว ${currentSession?.audioDurationSec ?: 0} วินาที",
                                            fontSize = 11.sp,
                                            color = Color(0xFF49454F)
                                        )
                                    }
                                    
                                    if (isPlayingAudio) {
                                        // Visual audio spectrum waves simulation effect
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf(8.dp, 16.dp, 10.dp, 20.dp, 12.dp).forEach { h ->
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.dp)
                                                        .height(h)
                                                        .background(Color(0xFF6750A4), RoundedCornerShape(1.dp))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // TextField to view/edit raw transcript text
                        OutlinedTextField(
                            value = inputDraftText,
                            onValueChange = { viewModel.updateRawText(it) },
                            placeholder = { Text("ระบุเนื้อหาร่างสนทนา หรืออัปโหลด/อัดคลื่นคลิปเสียงที่ต้องการจัดทำโครงสร้าง...", color = Color(0xFF49454F).copy(alpha = 0.7f), fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("source_transcript_input"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0),
                                focusedTextColor = Color(0xFF1C1B1F),
                                unfocusedTextColor = Color(0xFF1C1B1F),
                                focusedContainerColor = Color(0xFFFFFFFF),
                                unfocusedContainerColor = Color(0xFFFFFFFF)
                            ),
                            textStyle = TextStyle(fontSize = 13.sp, lineHeight = 20.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Demo Quick Loaders tags
                        Text(
                            text = "หรือทดลองใช้จากตัวอย่างด่วนภาษาไทยด้านล่าง:",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(viewModel.sampleMeetings) { sample ->
                                AssistChip(
                                    onClick = { viewModel.loadSample(sample) },
                                    label = { Text(sample.title, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Color(0xFFF3EDF7),
                                        labelColor = Color(0xFF1C1B1F)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                                    modifier = Modifier.testTag("sample_chip_${sample.id}")
                                )
                            }
                        }
                    }
                }

                // --- SECTION 2: AI ROUTING MENUS CHIPS ---
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "เลือกเมนูสั่งการประมวลผล (MengAi Menu Routing)",
                            color = Color(0xFF1C1B1F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Render 6 menus in vertical list or smart grid
                        MengAiMenu.values().forEach { menu ->
                            val isSelected = activeMenu == menu
                            val borderColState by animateColorAsState(if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0).copy(alpha = 0.4f), label = "border")
                            val bgColState by animateColorAsState(if (isSelected) Color(0xFFE8DEF8) else CardSurface, label = "bg")
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bgColState)
                                    .border(1.dp, borderColState, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.activeMenu.value = menu }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic premium icons representing key functions
                                val icon = when (menu) {
                                    MengAiMenu.TRANS_AND_FORMAT -> Icons.Outlined.InterpreterMode
                                    MengAiMenu.GEN_SUMMARY_TEMPLATES -> Icons.Outlined.ContentPaste
                                    MengAiMenu.GEN_ACTION_ITEMS -> Icons.Outlined.AssignmentTurnedIn
                                    MengAiMenu.GEN_MIND_MAP -> Icons.Outlined.Timeline
                                    MengAiMenu.INTERACTIVE_CHAT -> Icons.Outlined.QuestionAnswer
                                    MengAiMenu.PREPARE_EXPORT -> Icons.Outlined.SettingsBackupRestore
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF6750A4).copy(alpha = 0.15f) else Color(0xFFF3EDF7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF6750A4) else Color(0xFF49454F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = menu.menuName,
                                        color = Color(0xFF1C1B1F),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = menu.iconDescription,
                                        color = Color(0xFF49454F),
                                        fontSize = 11.sp
                                    )
                                }
                                
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.activeMenu.value = menu },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF6750A4),
                                        unselectedColor = Color(0xFFCAC4D0)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dual-action processing panel (User Request & Requirement 2)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // "Process All" super button!
                            Button(
                                onClick = { viewModel.processAllMenuFeatures() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("process_all_button"),
                                shape = RoundedCornerShape(20.dp),
                                enabled = !isProcessing && inputDraftText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00C853), // Deep green for premium comprehensive action
                                    disabledContainerColor = Color(0xFFCAC4D0).copy(alpha = 0.4f),
                                    contentColor = Color.White,
                                    disabledContentColor = Color(0xFF1C1B1F).copy(alpha = 0.4f)
                                )
                            ) {
                                if (isProcessing) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("กำลังถอดคำพูด + จัดสรุป + สกัดสิ่งที่ต้องทำ...", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ประมวลผล AI ทุกฟังก์ชันในคลิกเดียว ✨", color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // "Process Active Menu Only" secondary button
                            Button(
                                onClick = { viewModel.processActiveMenu() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("process_button"),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isProcessing && inputDraftText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    disabledContainerColor = Color(0xFFCAC4D0).copy(alpha = 0.4f),
                                    contentColor = Color.White,
                                    disabledContentColor = Color(0xFF1C1B1F).copy(alpha = 0.4f)
                                )
                            ) {
                                if (isProcessing) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("กำลังสกัดวิเคราะห์เมนูที่เลือก...", color = Color.White, fontSize = 12.sp)
                                    }
                                } else {
                                    Text("ประมวลผลเฉพาะเมนู: ${activeMenu.menuName}", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // --- SECTION 3: RENDER ENGINE RESULTS ---
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "สำรวจและสลับดูชุดเอกสาร (Quick File & Result Tabs)",
                            color = Color(0xFF1C1B1F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (processingError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = processingError!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        } else {
                            var selectedTab by remember { mutableIntStateOf(0) }
                            val tabs = listOf("ไฟล์นำเข้า/บันทึก", "ข้อความไฟล์ถอด", "รายงานไฟล์สรุป")

                            Column(modifier = Modifier.fillMaxWidth()) {
                                TabRow(
                                    selectedTabIndex = selectedTab,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                                    containerColor = Color(0xFFF3EDF7)
                                ) {
                                    tabs.forEachIndexed { idx, title ->
                                        Tab(
                                            selected = selectedTab == idx,
                                            onClick = { selectedTab = idx },
                                            text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                when (selectedTab) {
                                    0 -> {
                                        // 1. RAW IMPORT / MICROPHONE RECORD DISPLAY (Requirement 2)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.15f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AudioFile, contentDescription = null, tint = Color(0xFF6750A4), modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (currentSession != null) currentSession!!.title else "ร่างเสียงนำเข้าล่าสุด ปัจจุบัน",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1C1B1F)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "ข้อความเต็มที่อัดบันทึก / นำเข้าไฟล์:",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color(0xFFF3EDF7).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                                        .padding(12.dp)
                                                ) {
                                                    Text(
                                                        text = if (inputDraftText.isNotBlank()) inputDraftText else "ยังไม่มีการนำเข้าคลื่นไฟล์เสียงหรือร่างข้อความใดๆ ค่ะ",
                                                        style = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
                                                        color = Color(0xFF1C1B1F)
                                                    )
                                                }

                                                if (inputDraftText.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    ExportActionRow(
                                                        title = if (currentSession != null) currentSession!!.title else "ข้อความไฟล์เสียงนำเข้าหลัก",
                                                        content = inputDraftText
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        // 2. TRANSCRIBED SCRIPT DISCUSSION DISPLAY (Requirement 2)
                                        val session = currentSession
                                        val transJson = session?.transAndFormatResult
                                        if (transJson.isNullOrBlank()) {
                                            NoResultPlaceholder("บทสนทนาถอดความ", "ไม่พบข้อความถอดแยกสี! โปรดคลิกปุ่ม 'ประมวลผล AI ทุกฟังก์ชันในคลิกเดียว ✨' ด้านบน เพื่อทำการวิเคราะห์โครงสร้างทั้งหมดโดยอัจฉริยะในคลิกเดียวค่ะ")
                                        } else {
                                            TranscriptionView(transJson)
                                        }
                                    }
                                    2 -> {
                                        // 3. SUMMARIZED INTERACTIVE ENGINE DISPLAY (Requirement 2)
                                        val session = currentSession
                                        val resultJsonForActiveType = when (activeMenu) {
                                            MengAiMenu.TRANS_AND_FORMAT -> session?.transAndFormatResult
                                            MengAiMenu.GEN_SUMMARY_TEMPLATES -> session?.summaryResult
                                            MengAiMenu.GEN_ACTION_ITEMS -> session?.actionItemsResult
                                            MengAiMenu.GEN_MIND_MAP -> session?.mindMapResult
                                            MengAiMenu.INTERACTIVE_CHAT -> "CHAT"
                                            MengAiMenu.PREPARE_EXPORT -> session?.prepareExportResult
                                        }

                                        if (resultJsonForActiveType.isNullOrBlank()) {
                                            NoResultPlaceholder("บทรายงานสรุปภาพรวม", "ยังไม่ได้ทำการวิเคราะห์เมนู `${activeMenu.menuName}`! กรุณาเลือกเมนูแล้วกดประมวลผลด้านบน หรือคลิกปุ่มสีเขียวเพื่อประมวลผลทั้งหมดในคลิกเดียวค่ะ")
                                        } else {
                                            when (activeMenu) {
                                                MengAiMenu.TRANS_AND_FORMAT -> TranscriptionView(resultJsonForActiveType)
                                                MengAiMenu.GEN_SUMMARY_TEMPLATES -> SummaryTemplatesView(resultJsonForActiveType)
                                                MengAiMenu.GEN_ACTION_ITEMS -> ActionItemsView(resultJsonForActiveType)
                                                MengAiMenu.GEN_MIND_MAP -> MindMapView(resultJsonForActiveType)
                                                MengAiMenu.INTERACTIVE_CHAT -> {
                                                    InteractiveChatView(
                                                        chatHistory = chatHistory,
                                                        queryText = chatQuery,
                                                        onQueryChange = { viewModel.chatQuery.value = it },
                                                        onSendClick = { viewModel.processActiveMenu() },
                                                        isProcessing = isProcessing
                                                    )
                                                }
                                                MengAiMenu.PREPARE_EXPORT -> PrepareExportView(resultJsonForActiveType)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // --- HISTORY DRAWER SLIDEOUT NAVIGATION ---
        if (showHistoryDrawer) {
            // Semi-transparent scrim backplate background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showHistoryDrawer = false }
            )

            // Real sliding navigation drawer panel
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .align(Alignment.CenterStart)
                    .background(Color(0xFFF3EDF7))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                    .padding(16.dp)
                    .testTag("history_drawer")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF6750A4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "คลังประวัติไฟล์อัด",
                            color = Color(0xFF1C1B1F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = { showHistoryDrawer = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "ปิด", tint = Color(0xFF1C1B1F))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ไม่มีไฟล์บันทึกประวัติ\nระบบจะบันทึกอัตโนมัติเมื่อกดพิมพ์หรือแชท",
                            color = Color(0xFF49454F),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions) { session ->
                            val isSelected = currentSession?.id == session.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFE8DEF8) else Color.White)
                                    .border(1.dp, if (isSelected) Color(0xFF6750A4) else Color(0xFFCAC4D0).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectSession(session)
                                        showHistoryDrawer = false
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (session.audioDurationSec > 0) Icons.Default.MusicNote else Icons.Default.TextSnippet,
                                        contentDescription = null,
                                        tint = Color(0xFF6750A4),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = session.title,
                                            color = Color(0xFF1C1B1F),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(session.timestamp)),
                                            color = Color(0xFF49454F),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSession(session) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "ลบไฟล์",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoResultPlaceholder(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1C1B1F))
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, fontSize = 12.sp, color = Color(0xFF49454F), textAlign = TextAlign.Center, lineHeight = 18.sp)
        }
    }
}

// Custom Helper Operator mapping for plus-like symbol substitution
private operator fun Any.plus(any: Any): Any {
    return this
}
