// Autor :  Sinelnikov Oleg 2026
// ModelT16 Detect - Главная Activity

package com.modelt16.detect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.modelt16.detect.ui.MainScreen
import com.modelt16.detect.ui.MainViewModel
import com.modelt16.detect.ui.theme.ModelT16DetectTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ModelT16DetectTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
