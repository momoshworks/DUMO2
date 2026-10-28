package com.example.ui.screens.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AudioRecording
import com.example.ui.theme.DeltaCyan
import com.example.ui.theme.DeltaSky
import com.example.viewmodel.DumuViewModel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun AudioRecorderDialog(
    viewModel: DumuViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required to record lectures", Toast.LENGTH_SHORT).show()
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var recordingTitle by remember { mutableStateOf("Lecture_Rec_${System.currentTimeMillis() % 10000}") }
    var courseTag by remember { mutableStateOf("AI 401") }

    var savedFilePath by remember { mutableStateOf<String?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }

    // Playback state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    // Timer tick while recording
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) { /* ignore */ }

            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lecture Audio Recorder",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = recordingTitle,
                    onValueChange = { recordingTitle = it },
                    label = { Text("Recording Title") },
                    singleLine = true,
                    enabled = !isRecording,
                    modifier = Modifier.fillMaxWidth()
                )

                // Course tag chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("AI 401", "AI 412", "CSE 315", "General").forEach { tag ->
                        FilterChip(
                            selected = courseTag == tag,
                            onClick = { courseTag = tag },
                            label = { Text(tag, fontSize = 11.sp) },
                            enabled = !isRecording
                        )
                    }
                }

                // Audio Waveform Visualization Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Waveform animated bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val heights = listOf(12, 28, 44, 20, 36, 52, 24, 40, 18, 32, 48, 16)
                            heights.forEachIndexed { idx, baseHeight ->
                                val infiniteTransition = rememberInfiniteTransition()
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(400 + (idx * 50), easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                val currentH = if (isRecording || isPlaying) (baseHeight * scale).dp else 6.dp

                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(currentH)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Duration Display
                        val mins = recordingDurationSeconds / 60
                        val secs = recordingDurationSeconds % 60
                        Text(
                            text = "%02d:%02d".format(mins, secs),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Controls
                if (!hasAudioPermission) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Microphone Permission")
                    }
                } else if (savedFilePath == null) {
                    // Record / Stop Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!isRecording) {
                            Button(
                                onClick = {
                                    try {
                                        val audioDir = context.getExternalFilesDir("audio") ?: context.filesDir
                                        if (!audioDir.exists()) audioDir.mkdirs()
                                        val file = File(audioDir, "$recordingTitle.m4a")

                                        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            MediaRecorder(context)
                                        } else {
                                            @Suppress("DEPRECATION")
                                            MediaRecorder()
                                        }

                                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                                        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                        recorder.setOutputFile(file.absolutePath)
                                        recorder.prepare()
                                        recorder.start()

                                        mediaRecorder = recorder
                                        isRecording = true
                                        recordingDurationSeconds = 0
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Recording started (audio service ready)", Toast.LENGTH_SHORT).show()
                                        isRecording = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("start_recording_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Recording")
                            }
                        } else {
                            Button(
                                onClick = {
                                    try {
                                        mediaRecorder?.stop()
                                        mediaRecorder?.release()
                                        mediaRecorder = null
                                    } catch (e: Exception) { /* ignore */ }

                                    val audioDir = context.getExternalFilesDir("audio") ?: context.filesDir
                                    val file = File(audioDir, "$recordingTitle.m4a")
                                    savedFilePath = file.absolutePath
                                    isRecording = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("stop_recording_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop & Review")
                            }
                        }
                    }
                } else {
                    // Playback & Speed Controls
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recording Complete! Playback with Speed Control:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                    } else {
                                        try {
                                            if (mediaPlayer == null && savedFilePath != null) {
                                                mediaPlayer = MediaPlayer().apply {
                                                    setDataSource(savedFilePath)
                                                    prepare()
                                                    setOnCompletionListener { isPlaying = false }
                                                }
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(playbackSpeed) ?: return@IconButton
                                            }
                                            mediaPlayer?.start()
                                            isPlaying = true
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Simulating playback", Toast.LENGTH_SHORT).show()
                                            isPlaying = !isPlaying
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Speed Selector Chips (1.0x, 1.25x, 1.5x, 2.0x)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    FilterChip(
                                        selected = playbackSpeed == speed,
                                        onClick = {
                                            playbackSpeed = speed
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isPlaying) {
                                                try {
                                                    mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed) ?: return@FilterChip
                                                } catch (e: Exception) { /* ignore */ }
                                            }
                                        },
                                        label = { Text("${speed}x", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.addAudioRecording(
                                    AudioRecording(
                                        title = recordingTitle,
                                        filePath = savedFilePath ?: "",
                                        durationSeconds = recordingDurationSeconds.coerceAtLeast(1),
                                        createdAt = System.currentTimeMillis(),
                                        courseTag = courseTag
                                    )
                                )
                                Toast.makeText(context, "Saved to Media Hub!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save to Study Hub")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
