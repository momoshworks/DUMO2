package com.example.ui.screens.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StudyFile
import com.example.ui.theme.CategoryAssignment
import com.example.ui.theme.CategoryLab
import com.example.ui.theme.CategoryLecture
import com.example.ui.theme.CategoryQuiz
import com.example.ui.theme.DeltaAccentAmber
import com.example.ui.theme.DeltaCyan
import com.example.ui.theme.DeltaSky
import com.example.viewmodel.DumuViewModel
import java.io.File

@Composable
fun MediaHubScreen(
    viewModel: DumuViewModel,
    onNavigateToPdfConverter: () -> Unit,
    onNavigateToPomodoro: () -> Unit,
    onOpenAudioRecorder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allFiles by viewModel.allFiles.collectAsStateWithLifecycle()
    val allRecordings by viewModel.allRecordings.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    var groupByCourse by remember { mutableStateOf(false) }

    // Seed sample files if database files empty to give student immediate richness
    LaunchedEffect(allFiles) {
        if (allFiles.isEmpty()) {
            val sampleFiles = listOf(
                StudyFile(
                    name = "AI_401_Lecture01_Supervised_Learning.pdf",
                    filePath = "/sample/AI_401_Lec01.pdf",
                    sizeBytes = 4_250_000,
                    lastModified = System.currentTimeMillis() - 86400000L,
                    category = "PDF",
                    courseTag = "AI 401 - Machine Learning"
                ),
                StudyFile(
                    name = "Computer_Vision_Lab02_Convolution.docx",
                    filePath = "/sample/CV_Lab02.docx",
                    sizeBytes = 1_850_000,
                    lastModified = System.currentTimeMillis() - 172800000L,
                    category = "Word",
                    courseTag = "AI 412 - Computer Vision"
                ),
                StudyFile(
                    name = "Robotics_ROS2_Architecture_Slides.pptx",
                    filePath = "/sample/Robotics_ROS2.pptx",
                    sizeBytes = 8_900_000,
                    lastModified = System.currentTimeMillis() - 259200000L,
                    category = "PowerPoint",
                    courseTag = "AI 423 - Autonomous Robotics"
                ),
                StudyFile(
                    name = "Attendance_And_Grades_Section1.xlsx",
                    filePath = "/sample/Attendance_S1.xlsx",
                    sizeBytes = 720_000,
                    lastModified = System.currentTimeMillis() - 345600000L,
                    category = "Excel",
                    courseTag = "General"
                )
            )
            sampleFiles.forEach { viewModel.addStudyFile(it) }
        }
    }

    // Filter files
    val filteredFiles = remember(allFiles, selectedCategory) {
        if (selectedCategory == "All") allFiles
        else allFiles.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    // Category counters
    val pdfCount = remember(allFiles) { allFiles.count { it.category.equals("PDF", true) } }
    val wordCount = remember(allFiles) { allFiles.count { it.category.equals("Word", true) } }
    val pptCount = remember(allFiles) { allFiles.count { it.category.equals("PowerPoint", true) } }
    val excelCount = remember(allFiles) { allFiles.count { it.category.equals("Excel", true) } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("media_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Title
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Media & Study Hub",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Documents, Lecture Recordings & Study Tools",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Tools Row (Image to PDF, Pomodoro, Audio Record, Academic Search)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Study Tools",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudyToolCard(
                        title = "Images to PDF",
                        subtitle = "Scan & compress",
                        icon = Icons.Default.PictureAsPdf,
                        color = DeltaAccentAmber,
                        onClick = onNavigateToPdfConverter,
                        modifier = Modifier.weight(1f)
                    )

                    StudyToolCard(
                        title = "Pomodoro Timer",
                        subtitle = "Course focused blocks",
                        icon = Icons.Default.Timer,
                        color = CategoryLecture,
                        onClick = onNavigateToPomodoro,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudyToolCard(
                        title = "Lecture Audio",
                        subtitle = "Record & speed controls",
                        icon = Icons.Default.Mic,
                        color = DeltaCyan,
                        onClick = onOpenAudioRecorder,
                        modifier = Modifier.weight(1f)
                    )

                    StudyToolCard(
                        title = "Academic Search",
                        subtitle = "Delta Library & Scholar",
                        icon = Icons.Default.Search,
                        color = DeltaSky,
                        onClick = {
                            val scholarIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://scholar.google.com"))
                            context.startActivity(scholarIntent)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Audio Recordings section
        if (allRecordings.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Recent Audio Recordings (${allRecordings.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(allRecordings) { recording ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                modifier = Modifier.width(220.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DeltaCyan.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = recording.courseTag,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeltaCyan,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteAudioRecording(recording.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = recording.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val mins = recording.durationSeconds / 60
                                    val secs = recording.durationSeconds % 60
                                    Text(
                                        text = "Duration: %02d:%02d".format(mins, secs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // File Explorer Section Header & Group Toggle
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Document Explorer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Group by Course",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = groupByCourse,
                            onCheckedChange = { groupByCourse = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal Filter Tabs with Counters: All, PDF, Word, PowerPoint, Excel
                val filterCategories = listOf(
                    "All" to allFiles.size,
                    "PDF" to pdfCount,
                    "Word" to wordCount,
                    "PowerPoint" to pptCount,
                    "Excel" to excelCount
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterCategories) { (cat, count) ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text("$cat ($count)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // File items list
        if (filteredFiles.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No $selectedCategory files found", fontWeight = FontWeight.Bold)
                        Text(
                            "Use 'Images to PDF' above or download files from DULMS to view them here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredFiles, key = { it.id }) { file ->
                StudyFileItemRow(
                    file = file,
                    onOpen = {
                        val fileObj = File(file.filePath)
                        if (fileObj.exists()) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    fileObj
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, getMimeType(file.category))
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Sample preview: ${file.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, file.name)
                            putExtra(Intent.EXTRA_TEXT, "Delta University study material: ${file.name} for ${file.courseTag}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Study File"))
                    },
                    onDelete = {
                        viewModel.deleteStudyFile(file.id)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StudyToolCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudyFileItemRow(
    file: StudyFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (file.category) {
        "PDF" -> Icons.Default.PictureAsPdf to CategoryQuiz
        "Word" -> Icons.Default.Description to DeltaSky
        "PowerPoint" -> Icons.Default.Description to DeltaAccentAmber
        "Excel" -> Icons.Default.TableChart to CategoryLecture
        else -> Icons.Default.Description to MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val sizeMb = file.sizeBytes / (1024.0 * 1024.0)
                Text(
                    text = "${file.courseTag} • %.1f MB".format(sizeMb),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun getMimeType(category: String): String {
    return when (category) {
        "PDF" -> "application/pdf"
        "Word" -> "application/msword"
        "PowerPoint" -> "application/vnd.ms-powerpoint"
        "Excel" -> "application/vnd.ms-excel"
        else -> "*/*"
    }
}
