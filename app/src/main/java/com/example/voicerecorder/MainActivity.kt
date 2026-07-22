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
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), RecordingAdapter.OnItemClickListener {

    private lateinit var btnRecord: Button
    private lateinit var rvRecordings: RecyclerView
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentFilePath: String? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private lateinit var adapter: RecordingAdapter
    private val recordingFiles = ArrayList<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        rvRecordings = findViewById(R.id.rvRecordings)

        adapter = RecordingAdapter(recordingFiles, this)
        rvRecordings.layoutManager = LinearLayoutManager(this)
        rvRecordings.adapter = adapter

        loadRecordings()
        requestPermission()

        btnRecord.setOnClickListener {
            if (mediaRecorder == null) {
                startRecording()
                btnRecord.text = "Stop Recording"
            } else {
                stopRecording()
                btnRecord.text = "Start Recording"
                loadRecordings()
            }
        }
    }

    private fun startRecording() {
        val dir = File(getExternalFilesDir(null), "VoiceRecorder")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        currentFilePath = "${dir.absolutePath}/REC_${System.currentTimeMillis()}.m4a"
        
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(currentFilePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        
        // Reflection use பண்ணி audioSessionId எடுக்குறோம். Compile error வராது
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val method = mediaRecorder?.javaClass?.getMethod("getAudioSessionId")
                val sessionId = method?.invoke(mediaRecorder) as? Int ?: 0
                
                if (sessionId != 0) {
                    if (NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = NoiseSuppressor.create(sessionId)
                        noiseSuppressor?.enabled = true
                    }
                    if (AcousticEchoCanceler.isAvailable()) {
                        echoCanceler = AcousticEchoCanceler.create(sessionId)
                        echoCanceler?.enabled = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } // இங்க தான் function முடியுது

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        mediaRecorder?.release()
        mediaRecorder = null
        
        noiseSuppressor?.release()
        noiseSuppressor = null
        
        echoCanceler?.release()
        echoCanceler = null
    }

    private fun loadRecordings() {
        val dir = File(getExternalFilesDir(null), "VoiceRecorder")
        recordingFiles.clear()
        if (dir.exists()) {
            dir.listFiles()?.filter { it.isFile && it.name.endsWith(".m4a") }?.let {
                recordingFiles.addAll(it.sortedByDescending { f -> f.lastModified() })
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onPlay(file: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            start()
            setOnCompletionListener { release() }
        }
        Toast.makeText(this, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onShare(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Recording"))
    }

    override fun onDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Delete ${file.name}?")
            .setPositiveButton("Yes") { _, _ ->
                file.delete()
                loadRecordings()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onRename(file: File) {
        val editText = EditText(this)
        editText.setText(file.nameWithoutExtension)
        AlertDialog.Builder(this)
            .setTitle("Rename File")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString() + ".m4a"
                val newFile = File(file.parent, newName)
                if (file.renameTo(newFile)) {
                    loadRecordings()
                    Toast.makeText(this, "Renamed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaRecorder?.release()
    }
} // Class இங்க முடியுது
