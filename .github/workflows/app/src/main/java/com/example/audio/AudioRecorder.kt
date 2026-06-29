package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    var currentFile: File? = null
        private set

    fun startRecording(fileName: String): Boolean {
        currentFile = File(context.cacheDir, "$fileName.m4a")
        
        @Suppress("DEPRECATION")
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentFile!!.absolutePath)
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                return false
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return true
    }

    fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    fun getAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
