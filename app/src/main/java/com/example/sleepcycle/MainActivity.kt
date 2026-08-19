package com.example.sleepcycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.sleepcycle.ui.SleepScreen
import com.example.sleepcycle.ui.SleepViewModel
import com.example.sleepcycle.ui.theme.SleepCycleTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SleepViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SleepCycleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SleepScreen(viewModel = viewModel)
                }
            }
        }
    }
}
