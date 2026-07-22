package com.example.voicerecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.parseColor("#1E90FF")
        strokeWidth = 4f
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val amplitudes = ArrayList<Float>()
    private var isRecording = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        if (amplitudes.isEmpty()) {
            canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, paint)
            return
        }

        val centerY = height / 2f
        val barWidth = width.toFloat() / amplitudes.size

        for (i in amplitudes.indices) {
            val x = i * barWidth + barWidth / 2
            val amplitude = amplitudes[i] * centerY
            canvas.drawLine(x, centerY - amplitude, x, centerY + amplitude, paint)
        }
    }
  
    fun updateAmplitude(amplitude: Float) {
        if (!isRecording) return
        val normalized = abs(amplitude) / 32767f
        amplitudes.add(normalized)
        if (amplitudes.size > 100) amplitudes.removeAt(0)
        invalidate()
    }

    fun startRecording() {
        isRecording = true
        amplitudes.clear()
    }

    fun stopRecording() {
        isRecording = false
        amplitudes.clear()
        invalidate()
    }
}
