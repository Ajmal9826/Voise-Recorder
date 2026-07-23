package com.example.voicerecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*  // இதுல SeekBar, TextView, Button எல்லாம் வரும்
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat  // புதுசு 1
import java.util.Date                // புதுசு 2
import java.util.Locale              // புதுசு 3
import java.util.Timer               // புதுசு 4
import java.util.TimerTask           // புதுசு 5

class MainActivity : AppCompatActivity(), RecordingAdapter.OnItemClickListener {

    private lateinit var btnRecord: Button; private lateinit var btnPause: Button
    private lateinit var rvRecordings: RecyclerView; private lateinit var tvTimer: TextView
    private lateinit var waveformView: WaveformView
    private var mediaRecorder: MediaRecorder? = null; private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null; private var currentPlayingFile: File? = null
    private var noiseSuppressor: NoiseSuppressor? = null; private var echoCanceler: AcousticEchoCanceler? = null
    private lateinit var adapter: RecordingAdapter; private val recordingFiles = ArrayList<File>()
    private var timer: Timer? = null; private var seconds = 0
    private var isRecording = false; private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnRecord = findViewById(R.id.btnRecord); btnPause = findViewById(R.id.btnPause)
        rvRecordings = findViewById(R.id.rvRecordings); tvTimer = findViewById(R.id.tvTimer)
        waveformView = findViewById(R.id.waveformView)
        adapter = RecordingAdapter(recordingFiles, this)
        rvRecordings.layoutManager = LinearLayoutManager(this); rvRecordings.adapter = adapter
        
        loadRecordings() // 1. function இருக்கு
        requestPermission() // 2. function இருக்கு

        btnRecord.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                Toast.makeText(this, "முதல்ல Play ஆகுறத நிறுத்துட்டு Record பண்ணு", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isRecording) startRecording() else stopRecording()
        }
        btnPause.setOnClickListener { if (isPaused) resumeRecording() else pauseRecording() }
    }

    private fun startRecording() {
        isRecording = true; isPaused = false; btnRecord.text = "STOP"; btnPause.isEnabled = true; btnPause.text = "PAUSE"
        startTimer(); waveformView.startRecording()
        val dir = File(getExternalFilesDir(null), "VoiceRecorder"); if (!dir.exists()) dir.mkdirs()

        val sdf = SimpleDateFormat("dd-MM-yyyy_hh-mm-ss-SSS-a", Locale.getDefault())
        val timeStamp = sdf.format(Date())
        currentFilePath = "${dir.absolutePath}/REC_${timeStamp}.m4a"

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(currentFilePath); prepare(); start()
        }
        
        // audioSessionId fix
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val sessionId = mediaRecorder?.audioSessionId ?: 0
                if (sessionId != 0) {
                    if (NoiseSuppressor.isAvailable()) noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
                    if (AcousticEchoCanceler.isAvailable()) echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
    
    private fun pauseRecording() { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { 
            mediaRecorder?.pause(); isPaused = true; btnPause.text = "RESUME"; stopTimer(); waveformView.pauseRecording() 
        } 
    }
    
    private fun resumeRecording() { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { 
            mediaRecorder?.resume(); isPaused = false; btnPause.text = "PAUSE"; startTimer(); waveformView.resumeRecording() 
        } 
    }
    
    private fun stopRecording() {
        isRecording = false; btnRecord.text = "START RECORDING"; btnPause.isEnabled = false
        stopTimer(); waveformView.stopRecording()
        try { mediaRecorder?.stop() } catch (e: Exception) {}
        mediaRecorder?.release(); mediaRecorder = null; noiseSuppressor?.release(); echoCanceler?.release()
        loadRecordings()
    }
    
    // scheduleAtFixedRate fix: ; நீக்கிட்டேன்
    private fun startTimer() { 
        seconds = 0 
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() { 
            override fun run() { 
                seconds++
                runOnUiThread { tvTimer.text = String.format("%02d:%02d", seconds / 60, seconds % 60) } 
            } 
        }, 0, 1000) 
    }
    
    private fun stopTimer() { 
        timer?.cancel(); timer = null; seconds = 0
        runOnUiThread { tvTimer.text = "00:00" } 
    }
    
    private fun loadRecordings() { 
        val dir = File(getExternalFilesDir(null), "VoiceRecorder")
        recordingFiles.clear()
        if (dir.exists()) dir.listFiles()?.filter { it.name.endsWith(".m4a") }?.let { recordingFiles.addAll(it.sortedByDescending { f -> f.lastModified() }) }
        adapter.notifyDataSetChanged() 
    }
    
    private fun requestPermission() { 
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) 
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200) 
    }

    override fun onPlayPause(file: File, seekBar: SeekBar, tvCurrent: TextView, tvTotal: TextView) {
        if (mediaPlayer?.isPlaying == true && currentPlayingFile == file) {
            mediaPlayer?.pause(); adapter.setPlayingState(file, false)
            btnRecord.isEnabled = true; btnRecord.alpha = 1.0f
        } else {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath); prepare(); start()
                setOnCompletionListener {
                    adapter.setPlayingState(file, false); currentPlayingFile = null
                    btnRecord.isEnabled = true; btnRecord.alpha = 1.0f
                }
            }
            currentPlayingFile = file; adapter.setPlayingState(file, true)
            btnRecord.isEnabled = false; btnRecord.alpha = 0.5f
        }
    }
    override fun onSeekTo(file: File, position: Int) { if(currentPlayingFile == file) mediaPlayer?.seekTo(position) }
    override fun getMediaPlayer(): MediaPlayer? = mediaPlayer

    override fun onMenuClick(file: File, view: View) {
        PopupMenu(this, view).apply {
            menu.add("Share"); menu.add("Rename"); menu.add("Delete")
            setOnMenuItemClickListener {
                when(it.title.toString()) { 
                    "Share" -> onShare(file)
                    "Rename" -> onRename(file)
                    "Delete" -> onDelete(file) 
                }; true
            }; show()
        }
    }
    override fun onShare(file: File) { 
        val uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        startActivity(Intent.createChooser(Intent().apply { 
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) 
        }, "Share")) 
    }
    override fun onRename(file: File) { 
        val input = EditText(this); input.setText(file.nameWithoutExtension)
        AlertDialog.Builder(this).setTitle("Rename").setView(input).setPositiveButton("OK") { _, _ -> 
            val newFile = File(file.parent, "${input.text}.m4a"); if (file.renameTo(newFile)) loadRecordings() 
        }.setNegativeButton("Cancel", null).show() 
    }
    override fun onDelete(file: File) {
        if (currentPlayingFile == file) { mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null; currentPlayingFile = null; adapter.setPlayingState(file, false); btnRecord.isEnabled = true; btnRecord.alpha = 1.0f }
        AlertDialog.Builder(this).setTitle("Delete").setMessage("Delete ${file.name}?").setPositiveButton("Delete") { _, _ -> 
            if (file.delete()) { Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show(); loadRecordings() } 
        }.setNegativeButton("Cancel", null).show()
    }
    
    override fun onDestroy() { 
        super.onDestroy()
        stopTimer()
        mediaPlayer?.stop(); mediaPlayer?.release()
        mediaRecorder?.release() 
    }
}
