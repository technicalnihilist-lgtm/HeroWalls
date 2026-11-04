package com.herowalls.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.herowalls.app.ui.navigation.HeroWallsNavHost
import com.herowalls.app.ui.rememberContainer
import com.herowalls.app.ui.theme.HeroWallsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HeroWallsTheme {
                val container = rememberContainer()
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    HeroWallsNavHost(container = container)
                }
            }
        }
    }
}
