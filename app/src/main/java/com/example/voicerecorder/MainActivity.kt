package com.example.voicerecorder

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import android.widget.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var btnRecord: Button
    private lateinit var btnPause: Button
    private lateinit var btnSave: Button
    private lateinit var btnPlay: Button
    private lateinit var btnShare: Button
    private lateinit var btnDelete: Button
    private lateinit var tvTimer: TextView
    private lateinit var waveformView: WaveformView
    private lateinit var lvRecordings: ListView

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPaused = false
    private var tempFileName: String? = null
    private var selectedFile: File? = null

    private var seconds = 0
    private var timer: Handler? = null
    private var runnable: Runnable? = null
    private lateinit var recordingsAdapter: ArrayAdapter<String>
    private val recordingsList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        btnPause = findViewById(R.id.btnPause)
        btnSave = findViewById(R.id.btnSave)
        btnPlay = findViewById(R.id.btnPlay)
        btnShare = findViewById(R.id.btnShare)
        btnDelete = findViewById(R.id.btnDelete)
        tvTimer = findViewById(R.id.tvTimer)
        waveformView = findViewById(R.id.waveformView)
        lvRecordings = findViewById(R.id.lvRecordings)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)

        btnPause.isEnabled = false
        btnSave.isEnabled = false
        btnPlay.isEnabled = false
        btnShare.isEnabled = false
        btnDelete.isEnabled = false

        loadRecordings()

        btnRecord.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }
        btnPause.setOnClickListener {
            if (!isPaused) pauseRecording() else resumeRecording()
        }
        btnSave.setOnClickListener { saveRecording() }
        btnPlay.setOnClickListener { playRecording() }
        btnShare.setOnClickListener { shareRecording() }
        btnDelete.setOnClickListener { deleteRecording() }

        lvRecordings.setOnItemClickListener { _, position, _ ->
    selectedFile = File(getExternalFilesDir("VoiceRecorder"), recordingsList[position])
    btnPlay.isEnabled = true
    btnShare.isEnabled = true
    btnDelete.isEnabled = true
}
    }

    private fun startRecording() {
        if (isRecording) return

        waveformView.startRecording()

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        tempFileName = "TEMP_$timeStamp.mp3"
        val file = File(getExternalFilesDir("VoiceRecorder"), tempFileName)
        file.parentFile?.mkdirs()

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        isRecording = true
        btnRecord.text = "Stop"
        btnPause.isEnabled = true
        btnSave.isEnabled = false
        startTimer()
        Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        waveformView.stopRecording()

        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false
        btnRecord.text = "Record"
        btnPause.text = "Pause"
        btnPause.isEnabled = false
        btnSave.isEnabled = true
        stopTimer()
        Toast.makeText(this, "Stopped. Press Save", Toast.LENGTH_SHORT).show()
    }

    private fun saveRecording() {
        if (tempFileName == null) return
        val tempFile = File(getExternalFilesDir("VoiceRecorder"), tempFileName)
        val finalName = "REC_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.mp3"
        val finalFile = File(getExternalFilesDir("VoiceRecorder"), finalName)
        tempFile.renameTo(finalFile)

        tempFileName = null
        btnSave.isEnabled = false
        loadRecordings()
        Toast.makeText(this, "Saved: $finalName", Toast.LENGTH_SHORT).show()
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording) {
            mediaRecorder?.pause()
            isPaused = true
            btnPause.text = "Resume"
            stopTimer()
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isRecording) {
            mediaRecorder?.resume()
            isPaused = false
            btnPause.text = "Pause"
            startTimer()
        }
    }

    private fun playRecording() {
        selectedFile?.let { file ->
            if (file.exists()) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener { release() }
                }
                Toast.makeText(this, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareRecording() {
        selectedFile?.let { file ->
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Share Recording"))
            }
        }
    }

    private fun deleteRecording() {
        selectedFile?.let { file ->
            if (file.exists() && file.delete()) {
                selectedFile = null
                btnPlay.isEnabled = false
                btnShare.isEnabled = false
                btnDelete.isEnabled = false
                loadRecordings()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadRecordings() {
        recordingsList.clear()
        val dir = getExternalFilesDir("VoiceRecorder")
        dir?.listFiles()?.filter { it.name.endsWith(".mp3") &&!it.name.startsWith("TEMP") }
          ?.forEach { recordingsList.add(it.name) }

        recordingsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recordingsList)
        lvRecordings.adapter = recordingsAdapter
    }

    private fun startTimer() {
        timer = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                seconds++
                tvTimer.text = String.format("%02d:%02d", seconds / 60, seconds % 60)

                if (isRecording && mediaRecorder!= null) {
                    val amp = mediaRecorder!!.maxAmplitude.toFloat()
                    waveformView.updateAmplitude(amp)
                }

                timer?.postDelayed(this, 100)
            }
        }
        timer?.post(runnable!!)
    }

    private fun stopTimer() {
        timer?.removeCallbacks(runnable!!)
        seconds = 0
        tvTimer.text = "00:00"
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
        timer?.removeCallbacks(runnable!!)
    }
}
