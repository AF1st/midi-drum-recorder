package com.af1st.mididrumrecorder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.af1st.mididrumrecorder.model.DrumHit
import com.af1st.mididrumrecorder.model.DrumType
import com.af1st.mididrumrecorder.model.RecordingState
import com.af1st.mididrumrecorder.viewmodel.DrumViewModel

@Composable
fun DrumRecorderScreen(viewModel: DrumViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { DrumTopBar() },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            RecordingTimer(
                state = uiState.recordingState,
                durationMs = uiState.recordingDurationMs
            )
            PatternVisualizer(
                hits = uiState.hits,
                durationMs = uiState.recordingDurationMs,
                isRecording = uiState.recordingState == RecordingState.RECORDING
            )
            HitStatsRow(hits = uiState.hits)
            DrumPadsRow(
                enabled = uiState.recordingState == RecordingState.RECORDING,
                onHit = { viewModel.onDrumHit(it) }
            )
            TransportControls(
                state = uiState.recordingState,
                hasHits = uiState.hits.isNotEmpty(),
                onStart = { viewModel.startRecording() },
                onStop = { viewModel.stopRecording() },
                onReset = { viewModel.resetRecording() },
                onExport = { viewModel.exportMidi(context) }
            )
            AnimatedVisibility(
                visible = uiState.exportMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.exportMessage?.let { msg ->
                    ExportMessageCard(message = msg)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "🥁 MIDI Drum Recorder",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun RecordingTimer(
    state: RecordingState,
    durationMs: Long
) {
    var elapsed by remember { mutableLongStateOf(0L) }
    var startTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state) {
        when (state) {
            RecordingState.RECORDING -> {
                startTime = System.currentTimeMillis()
                while (true) {
                    elapsed = System.currentTimeMillis() - startTime
                    kotlinx.coroutines.delay(50)
                }
            }
            RecordingState.STOPPED -> elapsed = durationMs
            RecordingState.IDLE -> elapsed = 0L
        }
    }

    val displayMs = if (state == RecordingState.RECORDING) elapsed else durationMs
    val seconds = displayMs / 1000
    val millis = (displayMs % 1000) / 10

    val color = when (state) {
        RecordingState.RECORDING -> MaterialTheme.colorScheme.error
        RecordingState.STOPPED -> MaterialTheme.colorScheme.primary
        RecordingState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state == RecordingState.RECORDING) {
            val infiniteTransition = rememberInfiniteTransition(label = "blink")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 0.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500), repeatMode = RepeatMode.Reverse
                ), label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Red.copy(alpha = alpha))
            )
        }
        Text(
            text = "%02d:%02d".format(seconds, millis),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 48.sp
        )
    }
}

@Composable
fun PatternVisualizer(
    hits: List<DrumHit>,
    durationMs: Long,
    isRecording: Boolean
) {
    val displayDuration = maxOf(durationMs, 5000L)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val kickY = size.height * 0.35f
                val snareY = size.height * 0.65f
                val lineColor = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.3f)

                drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, kickY), androidx.compose.ui.geometry.Offset(size.width, kickY), strokeWidth = 1f)
                drawLine(lineColor, androidx.compose.ui.geometry.Offset(0f, snareY), androidx.compose.ui.geometry.Offset(size.width, snareY), strokeWidth = 1f)
                drawLine(lineColor, androidx.compose.ui.geometry.Offset(size.width * 0.5f, 0f), androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height), strokeWidth = 1f)

                hits.forEach { hit ->
                    val x = if (displayDuration > 0) (hit.timestampMs.toFloat() / displayDuration * size.width) else 0f
                    val y = if (hit.type == DrumType.KICK) kickY else snareY
                    val hitColor = if (hit.type == DrumType.KICK)
                        androidx.compose.ui.graphics.Color(0xFF4CAF50)
                    else
                        androidx.compose.ui.graphics.Color(0xFF2196F3)
                    drawCircle(hitColor, radius = 6f, center = androidx.compose.ui.geometry.Offset(x, y))
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("K", fontSize = 10.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                Text("S", fontSize = 10.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HitStatsRow(hits: List<DrumHit>) {
    val kicks = hits.count { it.type == DrumType.KICK }
    val snares = hits.count { it.type == DrumType.SNARE }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatChip(label = "Kick", count = kicks, color = Color(0xFF4CAF50))
        StatChip(label = "Total", count = hits.size, color = MaterialTheme.colorScheme.primary)
        StatChip(label = "Snare", count = snares, color = Color(0xFF2196F3))
    }
}

@Composable
fun StatChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DrumPadsRow(
    enabled: Boolean,
    onHit: (DrumType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrumPadButton(
            label = "KICK",
            subtitle = "Bass Drum (36)",
            enabled = enabled,
            color = Color(0xFF4CAF50),
            onClick = { onHit(DrumType.KICK) }
        )
        DrumPadButton(
            label = "SNARE",
            subtitle = "Snare Drum (38)",
            enabled = enabled,
            color = Color(0xFF2196F3),
            onClick = { onHit(DrumType.SNARE) }
        )
    }
}

@Composable
fun DrumPadButton(
    label: String,
    subtitle: String,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val bgColor = if (enabled) color else color.copy(alpha = 0.35f)

    Button(
        onClick = {
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = Modifier
            .size(140.dp)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        enabled = enabled,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (enabled) 6.dp else 0.dp
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TransportControls(
    state: RecordingState,
    hasHits: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (state) {
                RecordingState.IDLE -> {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .width(160.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.FiberManualRecord, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("REC", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                RecordingState.RECORDING -> {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .width(160.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("STOP", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                RecordingState.STOPPED -> {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                    Button(
                        onClick = onExport,
                        enabled = hasHits,
                        modifier = Modifier.height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export MIDI", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportMessageCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
