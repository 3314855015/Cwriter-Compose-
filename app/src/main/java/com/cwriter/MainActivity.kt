package com.cwriter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.cwriter.navigation.CWriterNavGraph
import com.cwriter.ui.screen.ThemeMode
import com.cwriter.ui.screen.loadThemeMode
import com.cwriter.ui.screen.saveThemeMode
import com.cwriter.ui.theme.CWriterTheme
import com.cwriter.ui.theme.LocalIsDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = this
            val systemDark = isSystemInDarkTheme()
            var themeMode by remember { mutableStateOf(loadThemeMode(context, systemDark)) }
            val isDark = themeMode == ThemeMode.DARK

            CWriterTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalIsDark provides isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        CWriterNavGraph(
                            navController = navController,
                            isDark = isDark,
                            onToggleTheme = {
                                themeMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                                saveThemeMode(context, themeMode)
                            }
                        )
                    }
                }
            }
        }
    }
}
