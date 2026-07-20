package com.example.voicerecorder

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var isPaused = false
    private var timer: Timer? = null
    private var seconds = 0

    private lateinit var tvTimer: TextView
    private lateinit var waveformView: WaveformView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTimer = findViewById(R.id.tvTimer)
        waveformView = findViewById(R.id.waveformView)

        findViewById<Button>(R.id.btnRecord).setOnClickListener { startRecording() }
        findViewById<Button>(R.id.btnPause).setOnClickListener { pauseRecording() }
        findViewById<Button>(R.id.btnResume).setOnClickListener { resumeRecording() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopRecording() }
        findViewById<Button>(R.id.btnPlay).setOnClickListener { playRecording() }
        findViewById<Button>(R.id.btnShare).setOnClickListener { shareRecording() }
        findViewById<Button>(R.id.btnDelete).setOnClickListener { deleteRecording() }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    private fun startRecording() {
        val folder = File(getExternalFilesDir(null), "VoiceRecorder")
        folder.mkdirs()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        outputFile = File(folder, "REC_$timeStamp.mp3")

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            outputFile?.let { setOutputFile(it.absolutePath) }
            prepare()
            start()
        }
        isRecording = true
        startTimer()
        startWaveform()
        Toast.makeText(this, "Recording Started", Toast.LENGTH_SHORT).show()
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.pause()
            isPaused = true
            stopTimer()
        }
    }

    private fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            recorder?.resume()
            isPaused = false
            startTimer()
        }
    }

    private fun stopRecording() {
        recorder?.stop()
        recorder?.release()
        recorder = null
        isRecording = false
        stopTimer()
        waveformView.clear()
        Toast.makeText(this, "Saved: ${outputFile?.name}", Toast.LENGTH_LONG).show()
    }

    private fun playRecording() {
        player = MediaPlayer().apply {
            setDataSource(outputFile?.absolutePath)
            prepare()
            start()
        }
    }

    private fun shareRecording() {
        outputFile?.let {
            val uri = FileProvider.getUriForFile(this, "com.example.voicerecorder.fileprovider", it)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Recording"))
        }
    }

    private fun deleteRecording() {
        outputFile?.delete()
        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
    }

    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    seconds++
                    tvTimer.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
                }
            }
        }, 1000, 1000)
    }

    private fun stopTimer() {
        timer?.cancel()
    }

    private fun startWaveform() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (isRecording && !isPaused) {
                    val amp = recorder?.maxAmplitude?.toFloat() ?: 0f
                    runOnUiThread { waveformView.addAmplitude(amp / 32767f) }
                }
            }
        }, 0, 100)
    }
}
