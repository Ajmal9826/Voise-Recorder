package com.example.voicerecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnRecord: Button
    private lateinit var btnPause: Button
    private lateinit var btnPlay: Button
    private lateinit var btnShare: Button
    private lateinit var btnDelete: Button
    private lateinit var tvTimer: TextView
    private lateinit var waveformView: WaveformView

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPaused = false
    private var currentFileName: String? = null
    
    private var seconds = 0
    private var timer: Handler? = null
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        btnPause = findViewById(R.id.btnPause)
        btnPlay = findViewById(R.id.btnPlay)
        btnShare = findViewById(R.id.btnShare)
        btnDelete = findViewById(R.id.btnDelete)
        tvTimer = findViewById(R.id.tvTimer)
        waveformView = findViewById(R.id.waveformView)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        
        btnPause.isEnabled = false
        btnPlay.isEnabled = false
        btnShare.isEnabled = false

        btnRecord.setOnClickListener { 
            if (!isRecording) startRecording() else stopRecording() 
        }
        btnPause.setOnClickListener { 
            if (!isPaused) pauseRecording() else resumeRecording() 
        }
        btnPlay.setOnClickListener { playRecording() }
        btnShare.setOnClickListener { shareRecording() }
        btnDelete.setOnClickListener { deleteRecording() }
    }

    private fun startRecording() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        currentFileName = "REC_$timeStamp.mp3"
        val file = File(getExternalFilesDir("VoiceRecorder"), currentFileName)
        file.parentFile?.mkdirs()

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        isRecording = true
        isPaused = false
        btnRecord.text = "⏹ Stop"
        btnPause.isEnabled = true
        btnPlay.isEnabled = false
        btnShare.isEnabled = false
        startTimer()
        Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) { e.printStackTrace() }
        mediaRecorder = null
        isRecording = false
        isPaused = false
        btnRecord.text = "🎤 Record"
        btnPause.text = "⏸ Pause"
        btnPause.isEnabled = false
        btnPlay.isEnabled = true
        btnShare.isEnabled = true
        stopTimer()
        Toast.makeText(this, "Recording Saved", Toast.LENGTH_SHORT).show()
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
            isPaused = true
            btnPause.text = "▶ Resume"
            stopTimer()
            Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
            isPaused = false
            btnPause.text = "⏸ Pause"
            startTimer()
            Toast.makeText(this, "Resumed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playRecording() {
        val file = File(getExternalFilesDir("VoiceRecorder"), currentFileName)
        if (file.exists()) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { release() }
            }
            Toast.makeText(this, "Playing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareRecording() {
        val file = File(getExternalFilesDir("VoiceRecorder"), currentFileName)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Recording"))
        }
    }

    private fun deleteRecording() {
        val file = File(getExternalFilesDir("VoiceRecorder"), currentFileName)
        if (file.exists()) file.delete()
        currentFileName = null
        btnPlay.isEnabled = false
        btnShare.isEnabled = false
        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
    }

    private fun startTimer() {
        timer = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                seconds++
                tvTimer.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
                timer?.postDelayed(this, 1000)
            }
        }
        timer?.post(runnable!!)
    }

    private fun stopTimer() {
        timer?.removeCallbacks(runnable!!)
        seconds = 0
        tvTimer.text = "00:00"
    }
}
