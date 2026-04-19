package com.tjyg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tjgy.feature.sample.SampleScreen
import com.tjyg.ui.theme.TjygTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TjygTheme {
                SampleScreen()
            }
        }
    }
}
