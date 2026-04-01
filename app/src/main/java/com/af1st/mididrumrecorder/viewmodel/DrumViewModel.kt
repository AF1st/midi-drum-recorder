package com.af1st.mididrumrecorder.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.af1st.mididrumrecorder.midi.MidiExporter
import com.af1st.mididrumrecorder.model.DrumHit
import com.af1st.mididrumrecorder.model.DrumType
import com.af1st.mididrumrecorder.model.RecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DrumUiState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val hits: List<DrumHit> = emptyList(),
    val recordingDurationMs: Long = 0L,
    val exportMessage: String? = null,
    val lastHitType: DrumType? = null
)

class DrumViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DrumUiState())
    val uiState: StateFlow<DrumUiState> = _uiState.asStateFlow()

    private var recordingStartMs: Long = 0L

    fun startRecording() {
        recordingStartMs = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                recordingState = RecordingState.RECORDING,
                hits = emptyList(),
                recordingDurationMs = 0L,
                exportMessage = null,
                lastHitType = null
            )
        }
    }

    fun stopRecording() {
        val duration = System.currentTimeMillis() - recordingStartMs
        _uiState.update {
            it.copy(
                recordingState = RecordingState.STOPPED,
                recordingDurationMs = duration
            )
        }
    }

    fun resetRecording() {
        _uiState.update {
            DrumUiState()
        }
    }

    fun onDrumHit(type: DrumType) {
        if (_uiState.value.recordingState != RecordingState.RECORDING) return
        val relativeTime = System.currentTimeMillis() - recordingStartMs
        val hit = DrumHit(
            type = type,
            timestampMs = relativeTime
        )
        _uiState.update {
            it.copy(
                hits = it.hits + hit,
                lastHitType = type
            )
        }
    }

    fun exportMidi(context: Context) {
        val hits = _uiState.value.hits
        val duration = _uiState.value.recordingDurationMs
        if (hits.isEmpty()) {
            _uiState.update { it.copy(exportMessage = "Нет ударов для экспорта") }
            return
        }
        viewModelScope.launch {
            val result = MidiExporter.export(context, hits, duration)
            _uiState.update {
                it.copy(exportMessage = result)
            }
        }
    }
}
