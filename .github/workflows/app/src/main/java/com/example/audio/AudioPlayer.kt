package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    var isPlaying = false
        private set

    fun play(pathOrUri: String, onComplete: () -> Unit, onError: (String) -> Unit) {
        stop()
        try {
            mediaPlayer = MediaPlayer().apply {
                if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                    setDataSource(context, Uri.parse(pathOrUri))
                } else {
                    setDataSource(pathOrUri)
                }
                prepare()
                start()
                this@AudioPlayer.isPlaying = true
                setOnCompletionListener {
                    this@AudioPlayer.isPlaying = false
                    onComplete()
                }
                setOnErrorListener { _, _, _ ->
                    this@AudioPlayer.isPlaying = false
                    onError("การเล่นไฟล์เสียงล้มเหลว")
                    true
                }
            }
        } catch (e: Exception) {
            this@AudioPlayer.isPlaying = false
            onError(e.localizedMessage ?: "ไม่สามารถเล่นไฟล์เสียงได้")
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }
}
