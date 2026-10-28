package com.example.ui.screens.tools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CategoryLecture
import com.example.ui.theme.DeltaAccentAmber
import com.example.ui.theme.DeltaCyan
import com.example.viewmodel.DumuViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    viewModel: DumuViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pomodoroSessions by viewModel.allPomodoroSessions.collectAsStateWithLifecycle()

    var isWorkMode by remember { mutableStateOf(true) }
    var selectedWorkMinutes by remember { mutableIntStateOf(25) }
    var selectedBreakMinutes by remember { mutableIntStateOf(5) }
    var remainingSeconds by remember { mutableIntStateOf(25 * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var selectedCourse by remember { mutableStateOf("AI 401 - Machine Learning") }

    val totalDurationSeconds = if (isWorkMode) selectedWorkMinutes * 60 else selectedBreakMinutes * 60
    val progress = (1f - (remainingSeconds.toFloat() / totalDurationSeconds.toFloat())).coerceIn(0f, 1f)

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        } else if (isRunning && remainingSeconds == 0) {
            isRunning = false
            if (isWorkMode) {
                viewModel.logPomodoro(selectedCourse, selectedWorkMinutes)
                isWorkMode = false
                remainingSeconds = selectedBreakMinutes * 60
            } else {
                isWorkMode = true
                remainingSeconds = selectedWorkMinutes * 60
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Course Pomodoro Focus", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .testTag("pomodoro_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Course Selector
            Text(
                text = "Target Course Tag:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val courses = listOf("AI 401 - ML", "AI 412 - CV", "CSE 315 - OS", "GP 499 - Capstone")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                courses.forEach { course ->
                    FilterChip(
                        selected = selectedCourse == course,
                        onClick = { selectedCourse = course },
                        label = { Text(course, fontSize = 11.sp) },
                        enabled = !isRunning
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Timer Dial
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 14.dp,
                    color = if (isWorkMode) MaterialTheme.colorScheme.primary else CategoryLecture,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = (if (isWorkMode) MaterialTheme.colorScheme.primary else CategoryLecture).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isWorkMode) "FOCUS SESSION" else "REST BREAK",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isWorkMode) MaterialTheme.colorScheme.primary else CategoryLecture,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    Text(
                        text = "%02d:%02d".format(mins, secs),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 44.sp
                    )

                    Text(
                        text = selectedCourse,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        isRunning = false
                        remainingSeconds = if (isWorkMode) selectedWorkMinutes * 60 else selectedBreakMinutes * 60
                    },
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }

                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .height(54.dp)
                        .width(160.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isWorkMode) MaterialTheme.colorScheme.primary else CategoryLecture
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Pause" else "Start Focus", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Focus History Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val totalMinutes = pomodoroSessions.sumOf { it.durationMinutes }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Focus Activity Log",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Total: $totalMinutes min",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (pomodoroSessions.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Complete your first focus block to record studying history.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(pomodoroSessions) { session ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = CategoryLecture,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = session.courseTag,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "+${session.durationMinutes} mins",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
