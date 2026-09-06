package com.example.sleepcycle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.sleepcycle.ui.SleepScreen
import com.example.sleepcycle.ui.SleepViewModel
import com.example.sleepcycle.ui.theme.SleepCycleTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SleepViewModel by viewModels { SleepViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("ui_prefs", MODE_PRIVATE)
        setContent {
            // 深色覆盖：用户从未切换过则跟随系统；切换后持久化覆盖
            var darkOverride by remember {
                mutableStateOf(if (prefs.contains("dark_theme")) prefs.getBoolean("dark_theme", false) else null)
            }
            val isDark = darkOverride ?: isSystemInDarkTheme()
            SleepCycleTheme(darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SleepScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDark,
                        onToggleTheme = {
                            val new = !isDark
                            darkOverride = new
                            prefs.edit().putBoolean("dark_theme", new).apply()
                        }
                    )
                }
            }
        }
    }
}
