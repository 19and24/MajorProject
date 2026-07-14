package com.example.majorproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.majorproject.food.ui.FoodTrackerApp
import com.example.majorproject.food.ui.theme.FoodTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FoodTrackerTheme {
                FoodTrackerApp()
            }
        }
    }
}
