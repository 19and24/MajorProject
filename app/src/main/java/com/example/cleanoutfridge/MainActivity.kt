package com.example.cleanoutfridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.cleanoutfridge.ui.CleanOutFridgeApp
import com.example.cleanoutfridge.ui.theme.CleanOutFridgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CleanOutFridgeTheme {
                CleanOutFridgeApp()
            }
        }
    }
}
