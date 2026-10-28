package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_items")
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseCode: String,
    val courseName: String,
    val type: String, // "Lecture", "Lab", "Section"
    val dayOfWeek: Int, // 1 = Sunday, 2 = Monday, ..., 7 = Saturday (Java Calendar format)
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val hall: String,
    val building: String,
    val instructor: String,
    val subgroup: String = "All"
) {
    fun formatTimeRange(): String {
        return "%02d:%02d - %02d:%02d".format(startHour, startMinute, endHour, endMinute)
    }

    fun durationMinutes(): Int {
        val start = startHour * 60 + startMinute
        val end = endHour * 60 + endMinute
        return (end - start).coerceAtLeast(1)
    }
}

@Entity(tableName = "dulms_tasks")
data class DulmsTask(
    @PrimaryKey val id: String,
    val title: String,
    val courseName: String,
    val type: String, // "Assignment", "Quiz", "Announcement", "Request"
    val dueDateMillis: Long,
    val status: String, // "Pending", "Submitted", "Overdue", "Graded"
    val isCompleted: Boolean = false,
    val details: String = "",
    val alarmScheduled: Boolean = false
)

@Entity(tableName = "study_files")
data class StudyFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: String, // "PDF", "Word", "PowerPoint", "Excel", "Other"
    val courseTag: String = "General"
)

@Entity(tableName = "audio_recordings")
data class AudioRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationSeconds: Int,
    val createdAt: Long,
    val courseTag: String = "General"
)

@Entity(tableName = "pomodoro_sessions")
data class PomodoroSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseTag: String,
    val durationMinutes: Int,
    val completedAt: Long
)
