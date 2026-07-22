package com.example.voicerecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
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

        // RecyclerView setup
        adapter = RecordingAdapter(recordingFiles, this)
        rvRecordings.layoutManager = LinearLayoutManager(this)
        rvRecordings.adapter = adapter

        loadRecordings()

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

        requestPermission()
    }

    private fun startRecording() {
        currentFilePath = "${getExternalFilesDir("VoiceRecorder")}/REC_${System.currentTimeMillis()}.m4a"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(currentFilePath)
            prepare()
            start()
        }
        val sessionId = mediaRecorder?.audioSessionId ?: 0
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(sessionId)
            noiseSuppressor?.enabled = true
        }
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(sessionId)
            echoCanceler?.enabled = true
        }
    }

    private fun stopRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        noiseSuppressor?.release()
        echoCanceler?.release()
    }

    private fun loadRecordings() {
        val dir = getExternalFilesDir("VoiceRecorder")
        recordingFiles.clear()
        dir?.listFiles()?.filter { it.isFile && it.name.endsWith(".m4a") }?.let {
            recordingFiles.addAll(it.sortedByDescending { f -> f.lastModified() })
        }
        adapter.notifyDataSetChanged()
    }

    // 4 Button Click Functions
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
        val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
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
}
