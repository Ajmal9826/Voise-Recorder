package com.example.voicerecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply { color = 0xFF00E676.toInt(); strokeWidth = 4f }
    private var amplitudes: MutableList<Float> = mutableListOf()

    fun addAmplitude(amp: Float) {
        amplitudes.add(amp)
        if (amplitudes.size > 100) amplitudes.removeAt(0)
        invalidate()
    }

    fun clear() {
        amplitudes.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val centerY = height / 2
        val barWidth = width / 100

        amplitudes.forEachIndexed { i, amp ->
            val x = i * barWidth
            val barHeight = amp * centerY
            canvas.drawLine(x, centerY - barHeight, x, centerY + barHeight, paint)
        }
    }
}
