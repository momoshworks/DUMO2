package com.example.ui.screens.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ScheduleItem
import com.example.ui.theme.CategoryLab
import com.example.ui.theme.CategoryLecture
import com.example.ui.theme.DeltaAccentAmber
import com.example.ui.theme.DeltaCyan
import com.example.ui.theme.DeltaNavy
import com.example.ui.theme.DeltaSky
import com.example.viewmodel.DumuViewModel
import java.util.Calendar

data class DayTab(val dayOfWeek: Int, val shortName: String, val fullName: String)

val EgyptianWeekDays = listOf(
    DayTab(Calendar.SATURDAY, "SAT", "Saturday"),
    DayTab(Calendar.SUNDAY, "SUN", "Sunday"),
    DayTab(Calendar.MONDAY, "MON", "Monday"),
    DayTab(Calendar.TUESDAY, "TUE", "Tuesday"),
    DayTab(Calendar.WEDNESDAY, "WED", "Wednesday"),
    DayTab(Calendar.THURSDAY, "THU", "Thursday"),
    DayTab(Calendar.FRIDAY, "FRI", "Friday")
)

@Composable
fun ScheduleScreen(
    viewModel: DumuViewModel,
    onOpenImportExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSchedule by viewModel.allSchedule.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedSubgroup by viewModel.selectedSubgroupFilter.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ScheduleItem?>(null) }
    var selectedHallDetails by remember { mutableStateOf<ScheduleItem?>(null) }

    // Filter items for selected day and subgroup
    val dayItems = remember(allSchedule, selectedDay, selectedSubgroup) {
        allSchedule.filter { it.dayOfWeek == selectedDay }
            .filter { selectedSubgroup == "All" || it.subgroup == "All" || it.subgroup.equals(selectedSubgroup, ignoreCase = true) }
            .sortedBy { it.startHour * 60 + it.startMinute }
    }

    // Conflict detection
    val conflicts = remember(dayItems) {
        val detected = mutableListOf<Pair<ScheduleItem, ScheduleItem>>()
        for (i in 0 until dayItems.size) {
            for (j in i + 1 until dayItems.size) {
                val a = dayItems[i]
                val b = dayItems[j]
                val aStart = a.startHour * 60 + a.startMinute
                val aEnd = a.endHour * 60 + a.endMinute
                val bStart = b.startHour * 60 + b.startMinute
                val bEnd = b.endHour * 60 + b.endMinute

                if (maxOf(aStart, bStart) < minOf(aEnd, bEnd)) {
                    detected.add(a to b)
                }
            }
        }
        detected
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.testTag("add_schedule_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Class")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("schedule_screen")
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Delta University Timeline",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Import / Export JSON & QR Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenImportExport)
                        .testTag("import_export_schedule_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Import/Export",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "JSON / QR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Horizontal Day Selector Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(EgyptianWeekDays) { tab ->
                    val isSelected = tab.dayOfWeek == selectedDay
                    val isToday = tab.dayOfWeek == Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else if (isToday) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.selectDay(tab.dayOfWeek) }
                            .testTag("day_tab_${tab.shortName}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tab.shortName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (isToday) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else MaterialTheme.colorScheme.secondary)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subgroup Filter Chips (All, Section 1, Section 2, Lab A, Lab B)
            val subgroups = listOf("All", "Section 1", "Section 2", "Lab A", "Lab B")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subgroups) { subgroup ->
                    val isSelected = subgroup == selectedSubgroup
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setSubgroupFilter(subgroup) },
                        label = { Text(subgroup, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Conflict Warning Banner if detected
            if (conflicts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Conflict",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Conflict detected between ${conflicts[0].first.courseCode} and ${conflicts[0].second.courseCode} at the same time!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vertical Timeline of Classes
            if (dayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Classes Scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the '+' button below or import a schedule to populate this day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(dayItems, key = { it.id }) { item ->
                        TimelineClassCard(
                            item = item,
                            onHallClick = { selectedHallDetails = item },
                            onEdit = { itemToEdit = item },
                            onDelete = { viewModel.deleteScheduleItem(item.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || itemToEdit != null) {
        AddEditScheduleDialog(
            item = itemToEdit,
            defaultDay = selectedDay,
            onDismiss = {
                showAddDialog = false
                itemToEdit = null
            },
            onSave = { newItem ->
                if (itemToEdit != null) {
                    viewModel.updateScheduleItem(newItem)
                } else {
                    viewModel.addScheduleItem(newItem)
                }
                showAddDialog = false
                itemToEdit = null
            }
        )
    }

    // Hall Location Dialog
    selectedHallDetails?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedHallDetails = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("${item.hall} - ${item.building}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Campus Location Guide:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${item.building} is located next to the Central Library plaza.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• ${item.hall} is equipped with interactive smart projectors and lab workstations.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Instructor: ${item.instructor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Time Slot: ${item.formatTimeRange()} (${item.durationMinutes()} minutes)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedHallDetails = null }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun TimelineClassCard(
    item: ScheduleItem,
    onHallClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLecture = item.type.contains("Lecture", ignoreCase = true)
    val sidebarColor = if (isLecture) CategoryLecture else CategoryLab

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_item_${item.id}"),
        verticalAlignment = Alignment.Top
    ) {
        // Time Column on Left
        Column(
            modifier = Modifier.width(62.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "%02d:%02d".format(item.startHour, item.startMinute),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "%02d:%02d".format(item.endHour, item.endMinute),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Vertical Timeline Dot and Connector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(sidebarColor)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(96.dp)
                    .background(sidebarColor.copy(alpha = 0.3f))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Main Card with color-coded sidebar
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.weight(1f)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Colored Sidebar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(110.dp)
                        .background(sidebarColor)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = sidebarColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = item.type.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = sidebarColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Edit / Delete Actions
                        Row {
                            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.courseName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${item.courseCode} • ${item.instructor}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // One-Tap Hall Location Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onHallClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.hall}, ${item.building}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditScheduleDialog(
    item: ScheduleItem?,
    defaultDay: Int,
    onDismiss: () -> Unit,
    onSave: (ScheduleItem) -> Unit
) {
    var courseCode by remember { mutableStateOf(item?.courseCode ?: "") }
    var courseName by remember { mutableStateOf(item?.courseName ?: "") }
    var type by remember { mutableStateOf(item?.type ?: "Lecture") }
    var hall by remember { mutableStateOf(item?.hall ?: "Hall 101") }
    var building by remember { mutableStateOf(item?.building ?: "Building A") }
    var instructor by remember { mutableStateOf(item?.instructor ?: "") }
    var subgroup by remember { mutableStateOf(item?.subgroup ?: "All") }
    var startHour by remember { mutableStateOf(item?.startHour ?: 9) }
    var startMinute by remember { mutableStateOf(item?.startMinute ?: 0) }
    var endHour by remember { mutableStateOf(item?.endHour ?: 10) }
    var endMinute by remember { mutableStateOf(item?.endMinute ?: 30) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (item != null) "Edit Class Session" else "Add New Class")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = courseCode,
                    onValueChange = { courseCode = it },
                    label = { Text("Course Code (e.g. AI 401)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                androidx.compose.material3.OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("Course Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Lecture", "Lab", "Section").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = hall,
                        onValueChange = { hall = it },
                        label = { Text("Hall / Lab") },
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = building,
                        onValueChange = { building = it },
                        label = { Text("Building") },
                        modifier = Modifier.weight(1f)
                    )
                }

                androidx.compose.material3.OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("Instructor / TA Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = "%02d:%02d".format(startHour, startMinute),
                        onValueChange = {
                            val parts = it.split(":")
                            if (parts.size == 2) {
                                startHour = parts[0].toIntOrNull() ?: startHour
                                startMinute = parts[1].toIntOrNull() ?: startMinute
                            }
                        },
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f)
                    )

                    androidx.compose.material3.OutlinedTextField(
                        value = "%02d:%02d".format(endHour, endMinute),
                        onValueChange = {
                            val parts = it.split(":")
                            if (parts.size == 2) {
                                endHour = parts[0].toIntOrNull() ?: endHour
                                endMinute = parts[1].toIntOrNull() ?: endMinute
                            }
                        },
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (courseCode.isNotBlank() && courseName.isNotBlank()) {
                        val newItem = ScheduleItem(
                            id = item?.id ?: 0,
                            courseCode = courseCode,
                            courseName = courseName,
                            type = type,
                            dayOfWeek = item?.dayOfWeek ?: defaultDay,
                            startHour = startHour,
                            startMinute = startMinute,
                            endHour = endHour,
                            endMinute = endMinute,
                            hall = hall,
                            building = building,
                            instructor = instructor.ifBlank { "Faculty Staff" },
                            subgroup = subgroup
                        )
                        onSave(newItem)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
