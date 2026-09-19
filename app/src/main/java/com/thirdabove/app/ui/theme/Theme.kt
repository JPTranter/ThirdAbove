package com.thirdabove.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ThirdAboveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ThirdAboveColorScheme,
        content = content
    )
}
