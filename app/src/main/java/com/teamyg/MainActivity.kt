package com.teamyg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.teamyg.ui.theme.TeamYGTheme
import com.teamyg.feature.sample.SampleScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TeamYGTheme() {
                SampleScreen()
            }
        }
    }
}
