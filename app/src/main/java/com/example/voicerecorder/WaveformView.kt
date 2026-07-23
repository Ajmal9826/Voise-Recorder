package com.example.voicerecorder
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class WaveformView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply { color = 0xFF6200EE.toInt(); strokeWidth = 4f }
    private var amplitudes = MutableList(100) { 0f }
    private var isRecording = false
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                amplitudes.removeAt(0)
                amplitudes.add(Random.nextFloat())
                invalidate()
                postDelayed(this, 100)
            }
        }
    }

    fun startRecording() { isRecording = true; post(updateRunnable) }
    fun pauseRecording() { isRecording = false }
    fun resumeRecording() { isRecording = true; post(updateRunnable) }
    fun stopRecording() { isRecording = false; amplitudes = MutableList(100) { 0f }; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val barWidth = width / amplitudes.size
        amplitudes.forEachIndexed { i, amp ->
            val barHeight = amp * height / 2
            canvas.drawLine(i * barWidth, height/2 - barHeight, i * barWidth, height/2 + barHeight, paint)
        }
    }
}
