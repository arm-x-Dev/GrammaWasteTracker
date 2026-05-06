package com.example.grammawastetracker.utils

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun MapView.setup(context: Context) {
    val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
    val config = Configuration.getInstance()
    
    config.userAgentValue = context.packageName
    // Explicitly use internal storage for cache to avoid external storage permission issues
    val osmdroidBasePath = java.io.File(context.filesDir, "osmdroid")
    val osmdroidTileCache = java.io.File(osmdroidBasePath, "tiles")
    config.osmdroidBasePath = osmdroidBasePath
    config.osmdroidTileCache = osmdroidTileCache
    
    config.load(context, prefs)
    
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(true)
    isVerticalMapRepetitionEnabled = false
    isHorizontalMapRepetitionEnabled = false
    setScrollableAreaLimitLatitude(85.0, -85.0, 0)
    
    controller.setZoom(17.0)
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
}

fun View.startForestGlowAnimation() {
    setLayerType(View.LAYER_TYPE_SOFTWARE, null) // Required for BlurMaskFilter to bleed
    val drawable = ForestGlowDrawable(context)
    background = drawable
    
    ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 8000 // Even slower for a calm, breathing aura
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener { animation ->
            drawable.setOffset(animation.animatedValue as Float)
        }
        start()
    }
}
