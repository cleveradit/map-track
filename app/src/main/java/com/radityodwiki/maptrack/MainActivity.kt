package com.radityodwiki.maptrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.radityodwiki.maptrack.navigation.MapTrackNavHost
import com.radityodwiki.maptrack.ui.theme.MapTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MapTrackTheme {
                MapTrackNavHost()
            }
        }
    }
}
