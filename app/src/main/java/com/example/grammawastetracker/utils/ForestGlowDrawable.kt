package com.example.grammawastetracker.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.grammawastetracker.R

class ForestGlowDrawable(context: Context) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private var gradientScale = 0f
    private val padding = 60f 

    private val colors = intArrayOf(
        Color.TRANSPARENT,
        Color.argb(180, 0, 109, 91),   // Forest Green (Translucent)
        Color.argb(180, 255, 215, 0), // Forest Gold
        Color.argb(180, 139, 69, 19),  // Earth Brown
        Color.argb(180, 34, 139, 34),  // Forest Green Light
        Color.TRANSPARENT
    )

    init {
        // Super-soft mist diffusion
        paint.maskFilter = BlurMaskFilter(100f, BlurMaskFilter.Blur.NORMAL)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        updateGradient()
    }

    fun setOffset(value: Float) {
        gradientScale = value
        updateGradient()
        invalidateSelf()
    }

    private fun updateGradient() {
        val bounds = bounds
        if (bounds.isEmpty) return

        val matrix = Matrix()
        matrix.setTranslate(bounds.width() * gradientScale, 0f)
        
        val shader = LinearGradient(
            0f, 0f, bounds.width().toFloat() / 2f, 0f,
            colors,
            null,
            Shader.TileMode.MIRROR
        )
        shader.setLocalMatrix(matrix)
        paint.shader = shader
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return

        // Perfect pill shape matching the 28dp radius
        val cornerRadius = (bounds.height() - 2 * padding) / 2f
        canvas.drawRoundRect(
            padding, padding, bounds.width() - padding, bounds.height() - padding,
            cornerRadius, cornerRadius,
            paint
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
