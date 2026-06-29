package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.data.AppDatabase
import com.example.data.Session
import com.example.data.SessionRepository
import com.example.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SessionRepository
    val allSessions: StateFlow<List<Session>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SessionRepository(database.sessionDao())
        allSessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // --- State variables ---
    val currentSession = MutableStateFlow<Session?>(null)
    val activeMenu = MutableStateFlow(MengAiMenu.TRANS_AND_FORMAT)
    
    val isRecording = MutableStateFlow(false)
    val recordingDurationSec = MutableStateFlow(0)
    val recordingAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    
    val isProcessing = MutableStateFlow(false)
    val processingError = MutableStateFlow<String?>(null)
    
    // Audio Player State variables
    val isPlayingAudio = MutableStateFlow(false)
    val playingAudioPath = MutableStateFlow<String?>(null)
    private val audioPlayer = AudioPlayer(application)
    
    val inputDraftText = MutableStateFlow("")
    val chatQuery = MutableStateFlow("")

    // Active interactive chat dialog log
    val activeChatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    private var recordingJob: Job? = null
    private val audioRecorder = AudioRecorder(application)

    // --- Demo Samples ---
    val sampleMeetings = listOf(
        SampleData(
            id = 1,
            title = "ประชุมทิศทางธุรกิจ MengAiRec 2026",
            text = """
                [คุณสมยศ]: สวัสดีครับทุกท่าน วันนี้เรามาประชุมย่อยเรื่องทิศทางการพัฒนาแอปพลิเคชันจัดการเสียงอัจฉริยะ MengAiRec ตัวใหม่นี้นะครับ
                [คุณแพรว]: สวัสดีค่ะพี่สมยศ ทิศทางหลักๆ ตอนนี้หนูมองว่าเรื่องถอดเสียงภาษาไทยกับสกัดบทสรุปแบบ Smart Templates สำคัญมากๆ ลูกค้าเรียกร้องมาเยอะค่ะ
                [คุณเกริก]: ใช่เจ๊... ยิ่งตรงแยกแยะผู้พูดอัตโนมัติ (Speaker Diarization) นะ ถ้าทำได้เป๊ะๆ จะช่วยประหยัดเวลามากเลยครับ
                [คุณสมยศ]: ดีครับ แต่อย่าลืมตัว Action Items ด้วยนะ อยากให้ระบบสกัดเลยว่า ใครต้องทำอะไร ส่งเมื่อไหร่ เพื่ออำนวยความสะดวกให้ผู้บริหาร
                [คุณแพรว]: แพรวจะรับหน้าที่ประสานงานข้อมูลและจัดทำโครงสร้าง Markdown และจัดคำย่นสั้นๆ สำหรับวาด Mindmap ส่งตรวจวันที่ 15 มิถุนายนนี้ค่ะ 
                [คุณเกริก]: ส่วนตัวระบบ Engine หลังบ้าน ระบบ MengAiRec Ultimate Engine รันบิ้วเสร็จพร้อมทำ Sandbox ภายในวันที่ 20 มิถุนายนนี้ครับพี่สมยศ
                [คุณสมยศ]: ดีมากครับ สรุปดีลตามนี้เลยนะ แล้วเจอกันรอบถัดไปสัปดาห์หน้า ขอบคุณทุกคนครับ
            """.trimIndent()
        ),
        SampleData(
            id = 2,
            title = "ประชุมด่วนกู้ภัยเว็บล่ม (IT Helpdesk Incident)",
            text = """
                [วิศวกรต้น]: พี่ครับ ตอนนี้ API Gateway ตัวหลักล่มครับ คิวงานค้างเพียบเลย น่าจะเกี่ยวกับ Memory Leak ในตัวบัฟเฟอร์
                [หัวหน้าสิทธิ์]: ล่มตั้งแต่ตอนไหนต้น? กระทบผู้ใช้นานเท่าไหร่แล้ว?
                [วิศวกรต้น]: เริ่มพังตอนสี่ทุ่มสิบห้านาทีครับ ตอนนี้ลูกค้าเข้าหน้าชำระเงินไม่ได้ 100% เลย
                [QA เนย]: เนยลองทดสอบในเครื่องเทสแล้ว ยืนยันว่าหน้าคาร์ทสินค้าก็ค้างค่ะ ต้องทำการล้างแคชเรดิสด่วน
                [หัวหน้าสิทธิ์]: โอเค เนยรับหน้าที่เป็น Incident Commander คอยอัปเดตแจ้งเตือนและปิดปรับปรุงระบบชั่วคราวให้เสร็จในอีก 10 นาทีนี้เลย
                [วิศวกรต้น]: ผมขอเวลาประมาณ 30 นาที คลีนเมมโมรี่ และทำ Hotfix ดีพลอยขึ้นโปรดักชัน คาดว่าจะกู้คืนระบบได้ตอนห้าทุ่มครึ่งครับ
                [หัวหน้าสิทธิ์]: จัดการเลยต้น หลังเสร็จแล้วพรุ่งนี้เที่ยงเนยทำรายงานสรุป Postmortem ส่งให้ผมและบอร์ดบริหารด้วยนะ
            """.trimIndent()
        )
    )

    fun loadSample(sample: SampleData) {
        inputDraftText.value = sample.text
        val newSession = Session(
            title = sample.title,
            rawText = sample.text
        )
        currentSession.value = newSession
        activeChatHistory.value = emptyList()
    }

    fun clearActive() {
        currentSession.value = null
        inputDraftText.value = ""
        activeChatHistory.value = emptyList()
        processingError.value = null
    }

    // --- Audio Recording Logic ---
    fun startRecording() {
        if (isRecording.value) return
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "REC_${formatter.format(Date())}"
        
        val success = audioRecorder.startRecording(fileName)
        if (success) {
            isRecording.value = true
            recordingDurationSec.value = 0
            recordingAmplitudes.value = emptyList()
            
            recordingJob = viewModelScope.launch(Dispatchers.Default) {
                while (isRecording.value) {
                    delay(200)
                    recordingDurationSec.value += 1
                    val amp = audioRecorder.getAmplitude().toFloat()
                    // Normalize amplitude somewhat for visualizer
                    val normAmp = (amp / 32768f).coerceIn(0.1f, 1.0f)
                    val list = recordingAmplitudes.value.toMutableList()
                    if (list.size > 40) list.removeAt(0)
                    list.add(normAmp)
                    recordingAmplitudes.value = list
                }
            }
        }
    }

    fun stopRecording() {
        if (!isRecording.value) return
        isRecording.value = false
        recordingJob?.cancel()
        recordingJob = null
        audioRecorder.stopRecording()

        val recordedFile = audioRecorder.currentFile
        val duration = recordingDurationSec.value

        val sdf = SimpleDateFormat("บันทึกเสียง dd MMM HH:mm", Locale.getDefault())
        val title = sdf.format(Date())

        val mockText = """
            [ผู้พูด 1]: สวัสดีครับ นี่คือไฟล์เสียงอัดบันทึกจริงจากเครื่องระบบ MengAiRec
            [ผู้พูด 2]: การประมวลผลวิเคราะห์ของระบบ Ultimate Engine จะช่วยถอดบทสนทนาอัดนี้ออกเป็นเทมเพลตคุณภาพสูงได้อย่างคุ้มค่าครับ
            [ผู้พูด 1]: ยอดเยี่ยมมากเลยครับ ขอบคุณครับ
        """.trimIndent()

        inputDraftText.value = mockText

        val newSession = Session(
            title = title,
            rawText = mockText,
            audioDurationSec = duration,
            audioPath = recordedFile?.absolutePath
        )
        currentSession.value = newSession
        activeChatHistory.value = emptyList()
        
        saveSessionToDb(newSession)

        // If a real API key is present, transcribe the recorded audio via Gemini API
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && recordedFile != null && recordedFile.exists()) {
            isProcessing.value = true
            processingError.value = null
            inputDraftText.value = "กำลังถอดเสียงจากบันทึกด้วย AI... 🎙️"
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val bytes = recordedFile.readBytes()
                    val result = MengAiRecEngine.transcribeAudio(bytes, "audio/m4a")
                    withContext(Dispatchers.Main) {
                        result.onSuccess { transcribedText ->
                            inputDraftText.value = transcribedText
                            val updated = currentSession.value?.copy(rawText = transcribedText)
                            if (updated != null) {
                                currentSession.value = updated
                                viewModelScope.launch {
                                    repository.updateSession(updated)
                                }
                            }
                        }.onFailure { err ->
                            inputDraftText.value = mockText
                            processingError.value = "ล้มเหลวในการถอดเสียงจริง: ${err.localizedMessage} (ใช้ข้อมูลจำลองแทนค่ะ)"
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        inputDraftText.value = mockText
                        processingError.value = "ข้อผิดพลาดในการอ่านไฟล์เสียง: ${e.localizedMessage}"
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isProcessing.value = false
                    }
                }
            }
        }
    }

    fun handleImportedAudio(fileName: String, savedLocalPath: String?, uriString: String) {
        val mockText = """
            [คุณมานะ]: สวัสดีทีมงานทุกท่าน ยินดีต้อนรับเข้าสู่งานพิจารณาประเด็นจากไฟล์เสียงนำเข้าตัวหลัก: $fileName
            [คุณวิภา]: สวัสดีค่ะ หัวข้อนี้เราจะพิจารณาการส่งมอบผ่านระบบ Ultimate Engine และจัดรายงานสรุปอัจฉริยะทันทีค่ะ
            [คุณมานะ]: ยอดเยี่ยมมากครับ งั้นเรารับเรื่องและให้ทีมงานดำเนินการจัดส่งมอบข้อคิดและ Action Items ของเซสชันนี้ได้เลยครับ
        """.trimIndent()

        inputDraftText.value = mockText
        val title = "ไฟล์นำเข้า: $fileName"
        
        val newSession = Session(
            title = title,
            rawText = mockText,
            audioDurationSec = 45,
            audioPath = savedLocalPath ?: uriString
        )
        currentSession.value = newSession
        activeChatHistory.value = emptyList()
        
        saveSessionToDb(newSession)

        // If a real API key is present, transcribe the imported audio via Gemini API
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && savedLocalPath != null) {
            val file = java.io.File(savedLocalPath)
            if (file.exists()) {
                isProcessing.value = true
                processingError.value = null
                inputDraftText.value = "กำลังถอดเสียงจากไฟล์นำเข้าด้วย AI... 🎙️"
                
                val ext = fileName.substringAfterLast(".", "").lowercase()
                val mimeType = when (ext) {
                    "mp3" -> "audio/mp3"
                    "wav" -> "audio/wav"
                    "m4a" -> "audio/m4a"
                    "aac" -> "audio/aac"
                    else -> "audio/m4a"
                }

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val bytes = file.readBytes()
                        val result = MengAiRecEngine.transcribeAudio(bytes, mimeType)
                        withContext(Dispatchers.Main) {
                            result.onSuccess { transcribedText ->
                                inputDraftText.value = transcribedText
                                val updated = currentSession.value?.copy(rawText = transcribedText)
                                if (updated != null) {
                                    currentSession.value = updated
                                    viewModelScope.launch {
                                        repository.updateSession(updated)
                                    }
                                }
                            }.onFailure { err ->
                                inputDraftText.value = mockText
                                processingError.value = "ล้มเหลวในการถอดเสียงจริง: ${err.localizedMessage} (ใช้ข้อมูลจำลองแทนค่ะ)"
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            inputDraftText.value = mockText
                            processingError.value = "ข้อผิดพลาดในการถอดเสียง: ${e.localizedMessage}"
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            isProcessing.value = false
                        }
                    }
                }
            }
        }
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (currentSession.value?.id == session.id) {
                clearActive()
            }
        }
    }

    fun selectSession(session: Session) {
        currentSession.value = session
        inputDraftText.value = session.rawText
        
        // Load chat history if present
        activeChatHistory.value = deserializeChatHistory(session.chatHistoryJson)
        processingError.value = null
    }

    fun updateRawText(text: String) {
        inputDraftText.value = text
        val current = currentSession.value
        if (current != null) {
            currentSession.value = current.copy(rawText = text)
        } else {
            // Initiate a draft session
            currentSession.value = Session(
                title = "บันทึกร่างใหม่ ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}",
                rawText = text
            )
        }
    }

    // --- Save/Update DB ---
    private fun saveSessionToDb(session: Session) {
        viewModelScope.launch {
            val id = repository.insertSession(session)
            currentSession.value = session.copy(id = id)
        }
    }

    private fun updateSessionInDb(session: Session) {
        viewModelScope.launch {
            repository.updateSession(session)
            currentSession.value = session
        }
    }

    // --- Execute MengAiRec AI Engine Menu ---
    // --- Execute MengAiRec AI Engine Menu ---
    fun processAllMenuFeatures() {
        val current = currentSession.value ?: Session(
            title = "ข้อความพิมพ์เอง ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
            rawText = inputDraftText.value
        )
        val inputText = inputDraftText.value
        
        if (inputText.isBlank()) {
            processingError.value = "กรุณากรอกหรืออัดเสียงข้อความต้นฉบับก่อนการประมวลผล!"
            return
        }

        isProcessing.value = true
        processingError.value = null

        viewModelScope.launch {
            try {
                // Call all in parallel using coroutine async to populate all tabs at once (Requirement 2 & User Request)
                val transJob = async { MengAiRecEngine.processMenu(MengAiMenu.TRANS_AND_FORMAT, inputText) }
                val summaryJob = async { MengAiRecEngine.processMenu(MengAiMenu.GEN_SUMMARY_TEMPLATES, inputText) }
                val actionJob = async { MengAiRecEngine.processMenu(MengAiMenu.GEN_ACTION_ITEMS, inputText) }
                val mindJob = async { MengAiRecEngine.processMenu(MengAiMenu.GEN_MIND_MAP, inputText) }
                val exportJob = async { MengAiRecEngine.processMenu(MengAiMenu.PREPARE_EXPORT, inputText) }

                val transRes = transJob.await()
                val summaryRes = summaryJob.await()
                val actionRes = actionJob.await()
                val mindRes = mindJob.await()
                val exportRes = exportJob.await()

                var updatedSession = current.copy(rawText = inputText)

                transRes.onSuccess { updatedSession = updatedSession.copy(transAndFormatResult = it) }
                summaryRes.onSuccess { updatedSession = updatedSession.copy(summaryResult = it) }
                actionRes.onSuccess { updatedSession = updatedSession.copy(actionItemsResult = it) }
                mindRes.onSuccess { updatedSession = updatedSession.copy(mindMapResult = it) }
                exportRes.onSuccess { updatedSession = updatedSession.copy(prepareExportResult = it) }

                // Save or update in database
                if (updatedSession.id == 0L) {
                    val id = repository.insertSession(updatedSession)
                    currentSession.value = updatedSession.copy(id = id)
                } else {
                    repository.updateSession(updatedSession)
                    currentSession.value = updatedSession
                }
            } catch (e: Exception) {
                processingError.value = "เกิดข้อผิดพลาดในการวิเคราะห์แบบรวดเดียว: ${e.localizedMessage}"
            } finally {
                isProcessing.value = false
            }
        }
    }

    // --- Audio Player Logic ---
    fun playAudio(path: String) {
        if (path.isBlank()) {
            processingError.value = "ไม่พบไฟล์เสียงสำหรับการเปิดฟังค่ะ"
            return
        }
        isPlayingAudio.value = true
        playingAudioPath.value = path
        audioPlayer.play(
            pathOrUri = path,
            onComplete = {
                isPlayingAudio.value = false
                playingAudioPath.value = null
            },
            onError = { err ->
                isPlayingAudio.value = false
                playingAudioPath.value = null
                processingError.value = err
            }
        )
    }

    fun stopAudio() {
        audioPlayer.stop()
        isPlayingAudio.value = false
        playingAudioPath.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }

    fun processActiveMenu() {
        val current = currentSession.value ?: Session(
            title = "ข้อความพิมพ์เอง ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
            rawText = inputDraftText.value
        )
        
        val menu = activeMenu.value
        val inputText = inputDraftText.value
        
        if (inputText.isBlank()) {
            processingError.value = "กรุณากรอกหรืออัดเสียงข้อความต้นฉบับก่อนการประมวลผล!"
            return
        }

        isProcessing.value = true
        processingError.value = null

        val query = if (menu == MengAiMenu.INTERACTIVE_CHAT) chatQuery.value else null

        viewModelScope.launch {
            val result = MengAiRecEngine.processMenu(menu, inputText, query)
            isProcessing.value = false
            
            result.onSuccess { jsonResult ->
                var updatedSession = current.copy(rawText = inputText)
                
                when (menu) {
                    MengAiMenu.TRANS_AND_FORMAT -> {
                        updatedSession = updatedSession.copy(transAndFormatResult = jsonResult)
                    }
                    MengAiMenu.GEN_SUMMARY_TEMPLATES -> {
                        updatedSession = updatedSession.copy(summaryResult = jsonResult)
                    }
                    MengAiMenu.GEN_ACTION_ITEMS -> {
                        updatedSession = updatedSession.copy(actionItemsResult = jsonResult)
                    }
                    MengAiMenu.GEN_MIND_MAP -> {
                        updatedSession = updatedSession.copy(mindMapResult = jsonResult)
                    }
                    MengAiMenu.INTERACTIVE_CHAT -> {
                        // Append to local state list
                        val answer = try {
                            MenuResponseParser.parseChatResponse(jsonResult)?.chat_response 
                                ?: "ไม่สามารถทำความพิกัดคำตอบได้: $jsonResult"
                        } catch (e: Exception) {
                            jsonResult
                        }
                        val newList = activeChatHistory.value.toMutableList()
                        newList.add(Pair(query ?: "", answer))
                        activeChatHistory.value = newList
                        
                        // Serialize chat history and store
                        val chatJson = serializeChatHistory(newList)
                        updatedSession = updatedSession.copy(
                            chatHistoryJson = chatJson,
                            // Save a reference to raw chat history so database syncs
                            transAndFormatResult = updatedSession.transAndFormatResult
                        )
                        chatQuery.value = "" // Clear textbox
                    }
                    MengAiMenu.PREPARE_EXPORT -> {
                        updatedSession = updatedSession.copy(prepareExportResult = jsonResult)
                    }
                }

                // Persist updated session details to Room DB
                if (updatedSession.id == 0L) {
                    val id = repository.insertSession(updatedSession)
                    currentSession.value = updatedSession.copy(id = id)
                } else {
                    repository.updateSession(updatedSession)
                    currentSession.value = updatedSession
                }
            }.onFailure { exception ->
                processingError.value = "ข้อผิดพลาดระบบ: ${exception.localizedMessage ?: "กรุณาตรวจสอบการตั้งค่า GEMINI_API_KEY ของคุณ"}"
            }
        }
    }

    // --- Serialization Helpers for Interactive Chat ---
    private fun serializeChatHistory(history: List<Pair<String, String>>): String {
        // Lightweight manual json arrays serialization to avoid Moshi boilerplate for simple pair lists
        val sb = java.lang.StringBuilder()
        sb.append("[")
        history.forEachIndexed { index, pair ->
            val escapedQ = pair.first.replace("\"", "\\\"").replace("\n", "\\n")
            val escapedA = pair.second.replace("\"", "\\\"").replace("\n", "\\n")
            sb.append("{\"q\":\"$escapedQ\",\"a\":\"$escapedA\"}")
            if (index < history.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun deserializeChatHistory(json: String?): List<Pair<String, String>> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val list = mutableListOf<Pair<String, String>>()
        try {
            // Simple robust regex parsing for direct lists
            val regex = "\\{\"q\":\"(.*?)\",\"a\":\"(.*?)\"\\}".toRegex()
            val matches = regex.findAll(json)
            for (match in matches) {
                val q = match.groupValues[1].replace("\\\"", "\"").replace("\\n", "\n")
                val a = match.groupValues[2].replace("\\\"", "\"").replace("\\n", "\n")
                list.add(Pair(q, a))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

data class SampleData(
    val id: Int,
    val title: String,
    val text: String
)
