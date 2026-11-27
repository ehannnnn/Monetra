
package app.project.monetra

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

class VoiceVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Urutan warna dari kiri ke kanan
    private val barColors = intArrayOf(
        R.color.monetra_black,
        R.color.monetra_purple,
        R.color.monetra_green,
        R.color.monetra_red,
        R.color.monetra_black,
        R.color.monetra_yellow,
        R.color.monetra_red,
        R.color.monetra_green,
        R.color.monetra_purple,
        R.color.monetra_black,
        R.color.monetra_red,
        R.color.monetra_yellow,
        R.color.monetra_green,
        R.color.monetra_black
    )

    // Tinggi dasar setiap batang
    private val baseHeights = floatArrayOf(
        0.30f, 0.48f, 0.68f, 0.42f,
        0.25f, 0.58f, 0.82f, 0.48f,
        0.72f, 0.35f, 0.62f, 0.88f,
        0.52f, 0.30f
    )

    private val colors = barColors.map {
        ContextCompat.getColor(context, it)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        color = ContextCompat.getColor(
            context,
            R.color.monetra_black
        )
    }

    private val cornerRadius = dp(3f)
    private val barWidth = dp(7f)
    private val barGap = dp(7f)

    private var targetLevel = 0f
    private var audioLevel = 0f

    // Menyimpan intensitas gelombang pada setiap batang
    private val waveLevels = FloatArray(14)

    fun updateAudioLevel(rms: Float) {
        targetLevel = min(
            1f,
            max(0f, (rms + 2f) / 12f)
        )

        // Gelombang bergerak dari kiri ke kanan
        // Gelombang bergerak dari kiri ke kanan
        for (i in waveLevels.size - 1 downTo 1) {
            waveLevels[i] = waveLevels[i - 1]
        }

        waveLevels[0] = targetLevel

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val totalWidth = 14 * barWidth + 13 * barGap
        val startX = (width - totalWidth) / 2f
        val centerY = height / 2f

        // Transisi intensitas lebih halus
        audioLevel += (targetLevel - audioLevel) * 0.25f

        for (i in 0 until 14) {

            val wave = waveLevels[i]

            val dynamicHeight = min(
                0.95f,
                baseHeights[i] + wave * 0.65f
            )

            val barHeight = height * dynamicHeight

            val left = startX + i * (barWidth + barGap)
            val top = centerY - barHeight / 2f
            val right = left + barWidth
            val bottom = centerY + barHeight / 2f

            val rect = RectF(left, top, right, bottom)

            fillPaint.color = colors[i]

            canvas.drawRoundRect(
                rect,
                cornerRadius,
                cornerRadius,
                fillPaint
            )

            canvas.drawRoundRect(
                rect,
                cornerRadius,
                cornerRadius,
                borderPaint
            )
        }

        // Animasi transisi ketika intensitas berubah
        if (absLevelDifference() > 0.01f) {
            postInvalidateOnAnimation()
        }
    }

    private fun absLevelDifference(): Float {
        return kotlin.math.abs(targetLevel - audioLevel)
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}
