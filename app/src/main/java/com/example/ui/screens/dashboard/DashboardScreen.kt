package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DulmsTask
import com.example.ui.components.DumuHeader
import com.example.ui.components.RealTimeLiveLectureCard
import com.example.ui.components.TaskItemCard
import com.example.ui.theme.DeltaAccentAmber
import com.example.ui.theme.DeltaCyan
import com.example.ui.theme.DeltaSky
import com.example.viewmodel.DumuViewModel

@Composable
fun DashboardScreen(
    viewModel: DumuViewModel,
    onNavigateToSchedule: () -> Unit,
    onNavigateToMedia: () -> Unit,
    onNavigateToPdfConverter: () -> Unit,
    onOpenAudioRecorder: () -> Unit,
    onOpenScheduleImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val studentName by viewModel.studentName.collectAsStateWithLifecycle()
    val studentId by viewModel.studentId.collectAsStateWithLifecycle()
    val liveLectureState by viewModel.liveLectureState.collectAsStateWithLifecycle()
    val upcomingTasks by viewModel.upcomingTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var selectedTaskForDetails by remember { mutableStateOf<DulmsTask?>(null) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Decluttered Compact Header
        item {
            DumuHeader(
                studentName = studentName,
                studentId = studentId,
                unreadNotificationsCount = upcomingTasks.size,
                onNotificationClick = { showNotificationDialog = true }
            )
        }

        // Live / Up Next Lecture Card
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                RealTimeLiveLectureCard(
                    state = liveLectureState,
                    onClassClick = { onNavigateToSchedule() }
                )
            }
        }

        // Quick Actions Grid (Audio Recording, Multi-Image to PDF, JSON/QR Schedule Import, DULMS Sync)
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Record Audio",
                        icon = Icons.Default.Mic,
                        accentColor = DeltaCyan,
                        onClick = onOpenAudioRecorder,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_audio_record"
                    )

                    QuickActionButton(
                        title = "Images to PDF",
                        icon = Icons.Default.PictureAsPdf,
                        accentColor = DeltaAccentAmber,
                        onClick = onNavigateToPdfConverter,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_images_pdf"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "Import Schedule",
                        icon = Icons.Default.QrCodeScanner,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = onOpenScheduleImport,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_schedule_import"
                    )

                    QuickActionButton(
                        title = if (isSyncing) "Syncing..." else "DULMS Sync",
                        icon = Icons.Default.Refresh,
                        accentColor = DeltaSky,
                        isLoading = isSyncing,
                        onClick = { viewModel.syncDulms() },
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_dulms_sync"
                    )
                }
            }
        }

        // Sync Status Banner (if message available)
        if (syncMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = syncMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearSyncMessage() }) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Today's Stats Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Today's Classes",
                    value = "${liveLectureState.totalTodayLectures}",
                    icon = Icons.Default.School,
                    onClick = onNavigateToSchedule,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pending To-Dos",
                    value = "${allTasks.count { !it.isCompleted }}",
                    icon = Icons.Default.Assignment,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Study Files",
                    value = "${allFiles.size}",
                    icon = Icons.Default.Description,
                    onClick = onNavigateToMedia,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // To-Dos & Deadlines (Due within 48 Hours)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Upcoming Deadlines",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Next 48 Hours",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "${upcomingTasks.size} tasks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (upcomingTasks.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Deadlines in the Next 48 Hours!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All quizzes and assignments are caught up. Tap 'DULMS Sync' to check for newly published tasks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(upcomingTasks, key = { it.id }) { task ->
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    TaskItemCard(
                        task = task,
                        onToggle = { completed -> viewModel.toggleTask(task.id, completed) },
                        onClick = { selectedTaskForDetails = task }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Task Details Dialog
    selectedTaskForDetails?.let { task ->
        AlertDialog(
            onDismissRequest = { selectedTaskForDetails = null },
            title = {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Course: ${task.courseName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Type: ${task.type}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (task.details.isNotBlank()) {
                        Text(
                            text = task.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Auto-alarms scheduled for 24h & 2h before deadline.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.toggleTask(task.id, !task.isCompleted)
                    selectedTaskForDetails = null
                }) {
                    Text(if (task.isCompleted) "Mark as Incomplete" else "Mark as Completed")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTaskForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Notifications Dialog
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Active Reminders & Alarms") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DUMU automatically monitors your upcoming Delta University deadlines and fires notifications at 24h and 2h before submission.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Active Deadlines: ${upcomingTasks.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showNotificationDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    testTag: String = ""
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accentColor
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
