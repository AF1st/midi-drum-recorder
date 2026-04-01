package com.af1st.mididrumrecorder.model

enum class DrumType(val midiNote: Int, val label: String, val channel: Int = 9) {
    KICK(36, "KICK"),
    SNARE(38, "SNARE")
}

data class DrumHit(
    val type: DrumType,
    val timestampMs: Long,
    val velocity: Int = 100
)
