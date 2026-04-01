package com.af1st.mididrumrecorder.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.af1st.mididrumrecorder.viewmodel.DrumViewModel

@Composable
fun DrumRecorderApp() {
    val viewModel: DrumViewModel = viewModel()
    DrumRecorderScreen(viewModel = viewModel)
}
