package com.af1st.mididrumrecorder.midi

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.af1st.mididrumrecorder.model.DrumHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MidiExporter {

    private const val TEMPO_BPM = 120
    private const val TICKS_PER_BEAT = 480
    private const val MICROSECONDS_PER_BEAT = 60_000_000 / TEMPO_BPM // 500000

    suspend fun export(
        context: Context,
        hits: List<DrumHit>,
        durationMs: Long
    ): String = withContext(Dispatchers.IO) {
        try {
            val midiBytes = buildMidiFile(hits, durationMs)
            val fileName = "drum_pattern_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.mid"
            saveToDownloads(context, midiBytes, fileName)
        } catch (e: Exception) {
            "Ошибка экспорта: ${e.message}"
        }
    }

    private fun msToTicks(ms: Long): Long {
        // ticks = ms * (ticks_per_beat) / (ms_per_beat)
        // ms_per_beat = microseconds_per_beat / 1000
        return ms * TICKS_PER_BEAT * 1000L / MICROSECONDS_PER_BEAT
    }

    private fun buildMidiFile(hits: List<DrumHit>, durationMs: Long): ByteArray {
        val out = ByteArrayOutputStream()

        // MIDI Header chunk
        out.write("MThd".toByteArray())
        out.write(intToBytes4(6))           // chunk length
        out.write(shortToBytes(1))          // format: Type 1 (multi-track)
        out.write(shortToBytes(2))          // number of tracks: tempo + drum
        out.write(shortToBytes(TICKS_PER_BEAT)) // ticks per beat

        // Track 0: Tempo
        val tempoTrack = buildTempoTrack()
        out.write("MTrk".toByteArray())
        out.write(intToBytes4(tempoTrack.size))
        out.write(tempoTrack)

        // Track 1: Drum hits on channel 10 (index 9)
        val drumTrack = buildDrumTrack(hits, durationMs)
        out.write("MTrk".toByteArray())
        out.write(intToBytes4(drumTrack.size))
        out.write(drumTrack)

        return out.toByteArray()
    }

    private fun buildTempoTrack(): ByteArray {
        val track = ByteArrayOutputStream()
        // Delta time 0
        track.write(0x00)
        // Tempo meta event: FF 51 03 tt tt tt
        track.write(0xFF)
        track.write(0x51)
        track.write(0x03)
        val tempo = MICROSECONDS_PER_BEAT
        track.write((tempo shr 16) and 0xFF)
        track.write((tempo shr 8) and 0xFF)
        track.write(tempo and 0xFF)
        // Track name
        val name = "Tempo Track".toByteArray()
        track.write(0x00)
        track.write(0xFF)
        track.write(0x03)
        track.write(varLen(name.size.toLong()))
        track.write(name)
        // End of track
        track.write(0x00)
        track.write(0xFF)
        track.write(0x2F)
        track.write(0x00)
        return track.toByteArray()
    }

    private fun buildDrumTrack(hits: List<DrumHit>, durationMs: Long): ByteArray {
        val track = ByteArrayOutputStream()
        val noteDuration = msToTicks(50) // 50ms note duration
        val drumChannel = 0x99 // Note On, channel 10 (9 zero-indexed)
        val drumChannelOff = 0x89 // Note Off, channel 10

        // Track name event
        val name = "Drum Track".toByteArray()
        track.write(0x00) // delta time
        track.write(0xFF)
        track.write(0x03)
        track.write(varLen(name.size.toLong()))
        track.write(name)

        // Sort hits by time
        val sortedHits = hits.sortedBy { it.timestampMs }

        // Build events: pair (tick, isNoteOn, note, velocity)
        data class MidiEvent(val tick: Long, val isNoteOn: Boolean, val note: Int, val vel: Int)

        val events = mutableListOf<MidiEvent>()
        for (hit in sortedHits) {
            val startTick = msToTicks(hit.timestampMs)
            val endTick = startTick + noteDuration
            events.add(MidiEvent(startTick, true, hit.type.midiNote, hit.velocity))
            events.add(MidiEvent(endTick, false, hit.type.midiNote, 0))
        }
        events.sortWith(compareBy({ it.tick }, { if (it.isNoteOn) 1 else 0 }))

        var currentTick = 0L
        for (event in events) {
            val delta = event.tick - currentTick
            currentTick = event.tick
            track.write(varLen(delta))
            track.write(if (event.isNoteOn) drumChannel else drumChannelOff)
            track.write(event.note)
            track.write(event.vel)
        }

        // End of track
        track.write(0x00)
        track.write(0xFF)
        track.write(0x2F)
        track.write(0x00)

        return track.toByteArray()
    }

    // Variable-length encoding for MIDI
    private fun varLen(value: Long): ByteArray {
        val result = mutableListOf<Int>()
        var v = value
        result.add((v and 0x7F).toInt())
        v = v shr 7
        while (v > 0) {
            result.add(0, ((v and 0x7F) or 0x80).toInt())
            v = v shr 7
        }
        return result.map { it.toByte() }.toByteArray()
    }

    private fun intToBytes4(value: Int): ByteArray = byteArrayOf(
        (value shr 24).toByte(),
        (value shr 16).toByte(),
        (value shr 8).toByte(),
        value.toByte()
    )

    private fun shortToBytes(value: Int): ByteArray = byteArrayOf(
        (value shr 8).toByte(),
        value.toByte()
    )

    private fun saveToDownloads(context: Context, data: ByteArray, fileName: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "audio/midi")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MidiDrumRecorder")
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
            ) ?: return "Ошибка: не удалось создать файл"
            context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
            "Сохранено: Downloads/MidiDrumRecorder/$fileName"
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "MidiDrumRecorder"
            )
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(data) }
            "Сохранено: ${file.absolutePath}"
        }
    }
}
