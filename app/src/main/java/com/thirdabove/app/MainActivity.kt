package com.thirdabove.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.thirdabove.app.ui.screens.FunSplashScreen
import com.thirdabove.app.ui.screens.HarmonyTrainerScreen
import com.thirdabove.app.ui.theme.ThirdAboveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThirdAboveTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    FunSplashScreen(
                        onContinue = { showSplash = false }
                    )
                } else {
                    HarmonyTrainerScreen()
                }
            }
        }
    }
}
