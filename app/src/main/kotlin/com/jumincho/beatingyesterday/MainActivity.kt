package com.jumincho.beatingyesterday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jumincho.beatingyesterday.ui.BeatingYesterdayApp
import com.jumincho.beatingyesterday.ui.theme.BeatingYesterdayTheme

/** The single activity; everything else is Compose. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BeatingYesterdayTheme {
                BeatingYesterdayApp()
            }
        }
    }
}
