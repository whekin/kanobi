package com.kanobi.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.kanobi.app.settings.AccentMode
import com.kanobi.app.settings.Settings
import com.kanobi.app.theme.BrandPalette
import com.kanobi.app.theme.KanobiTheme
import com.kanobi.app.theme.systemPalette
import com.kanobi.app.ui.KanobiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        val settings = Settings(applicationContext)
        setContent {
            val system = remember { systemPalette(this) }
            KanobiTheme(if (settings.accent == AccentMode.System) system else BrandPalette) { KanobiApp(settings) }
        }
    }
}
