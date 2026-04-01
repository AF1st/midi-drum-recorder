package com.af1st.mididrumrecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.af1st.mididrumrecorder.ui.DrumRecorderApp
import com.af1st.mididrumrecorder.ui.theme.MidiDrumRecorderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MidiDrumRecorderTheme {
                DrumRecorderApp()
            }
        }
    }
}
