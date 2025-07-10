package com.golfswing.vro.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import android.view.Gravity
import com.golfswing.vro.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Simple MainActivity for Golf Swing VRO
 * Using traditional Views instead of Compose to avoid dependency issues
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create layout programmatically to avoid XML dependencies
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }
        
        val titleText = TextView(this).apply {
            text = "🏌️ Golf Swing VRO"
            textSize = 24f
            gravity = Gravity.CENTER
        }
        
        val subtitleText = TextView(this).apply {
            text = "AI-Powered Golf Swing Analysis"
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 64)
        }
        
        val startButton = Button(this).apply {
            text = "Start Analysis"
            setPadding(32, 16, 32, 16)
        }
        
        val historyButton = Button(this).apply {
            text = "View History"
            setPadding(32, 16, 32, 16)
        }
        
        layout.addView(titleText)
        layout.addView(subtitleText)
        layout.addView(startButton)
        layout.addView(historyButton)
        
        setContentView(layout)
    }
}