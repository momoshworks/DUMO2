package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DumuDatabase
import com.example.data.local.PreferenceManager
import com.example.data.model.AudioRecording
import com.example.data.model.DulmsTask
import com.example.data.model.PomodoroSession
import com.example.data.model.ScheduleItem
import com.example.data.model.StudyFile
import com.example.network.DulmsSyncEngine
import com.example.util.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class DumuRepository(
    private val context: Context,
    private val database: DumuDatabase,
    private val preferenceManager: PreferenceManager
) {
    private val scheduleDao = database.scheduleDao()
    private val dulmsTaskDao = database.dulmsTaskDao()
    private val studyFileDao = database.studyFileDao()
    private val audioRecordingDao = database.audioRecordingDao()
    private val pomodoroDao = database.pomodoroDao()
    private val syncEngine = DulmsSyncEngine()

    val allSchedule: Flow<List<ScheduleItem>> = scheduleDao.getAllScheduleItems()
    val allTasks: Flow<List<DulmsTask>> = dulmsTaskDao.getAllTasks()
    val allFiles: Flow<List<StudyFile>> = studyFileDao.getAllFiles()
    val allRecordings: Flow<List<AudioRecording>> = audioRecordingDao.getAllRecordings()
    val allPomodoroSessions: Flow<List<PomodoroSession>> = pomodoroDao.getAllSessions()

    fun getScheduleForDay(dayOfWeek: Int): Flow<List<ScheduleItem>> {
        return scheduleDao.getScheduleForDay(dayOfWeek)
    }

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val todaySchedule = scheduleDao.getScheduleForDayList(Calendar.WEDNESDAY)
        if (todaySchedule.isEmpty()) {
            val defaultItems = createDefaultSchedule()
            scheduleDao.insertItems(defaultItems)
            Log.d("DumuRepository", "Populated ${defaultItems.size} default schedule items")
        }

        // Initialize default DULMS tasks if empty
        val defaultTasks = syncEngine.generateDefaultDeltaTasks()
        dulmsTaskDao.insertTasks(defaultTasks)

        // Schedule alarms for tasks if enabled
        if (preferenceManager.autoAlarmEnabled) {
            defaultTasks.forEach { task ->
                AlarmScheduler.scheduleTaskAlarms(context, task)
            }
        }
    }

    suspend fun insertScheduleItem(item: ScheduleItem) = withContext(Dispatchers.IO) {
        scheduleDao.insertItem(item)
    }

    suspend fun updateScheduleItem(item: ScheduleItem) = withContext(Dispatchers.IO) {
        scheduleDao.updateItem(item)
    }

    suspend fun deleteScheduleItem(id: Long) = withContext(Dispatchers.IO) {
        scheduleDao.deleteById(id)
    }

    suspend fun toggleTaskCompleted(taskId: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        dulmsTaskDao.setTaskCompleted(taskId, isCompleted)
    }

    suspend fun syncDulmsTasks(): DulmsSyncEngine.SyncResult = withContext(Dispatchers.IO) {
        val username = preferenceManager.lmsUsername
        val password = preferenceManager.lmsPassword
        val result = syncEngine.syncFromDulms(username, password)

        if (result is DulmsSyncEngine.SyncResult.Success) {
            dulmsTaskDao.insertTasks(result.tasks)
            preferenceManager.lastSyncTime = System.currentTimeMillis()

            if (preferenceManager.autoAlarmEnabled) {
                result.tasks.forEach { task ->
                    AlarmScheduler.scheduleTaskAlarms(context, task)
                    dulmsTaskDao.setAlarmScheduled(task.id, true)
                }
            }
        }
        result
    }

    suspend fun addStudyFile(file: StudyFile) = withContext(Dispatchers.IO) {
        studyFileDao.insertFile(file)
    }

    suspend fun deleteStudyFile(id: Long) = withContext(Dispatchers.IO) {
        studyFileDao.deleteById(id)
    }

    suspend fun addAudioRecording(recording: AudioRecording) = withContext(Dispatchers.IO) {
        audioRecordingDao.insertRecording(recording)
    }

    suspend fun deleteAudioRecording(id: Long) = withContext(Dispatchers.IO) {
        audioRecordingDao.deleteById(id)
    }

    suspend fun logPomodoroSession(session: PomodoroSession) = withContext(Dispatchers.IO) {
        pomodoroDao.insertSession(session)
    }

    // Schedule JSON Export
    suspend fun exportScheduleToJson(items: List<ScheduleItem>): String = withContext(Dispatchers.Default) {
        val root = JSONObject()
        root.put("app", "DUMU")
        root.put("version", "2.0")
        root.put("exportedAt", System.currentTimeMillis())

        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("courseCode", item.courseCode)
                put("courseName", item.courseName)
                put("type", item.type)
                put("dayOfWeek", item.dayOfWeek)
                put("startHour", item.startHour)
                put("startMinute", item.startMinute)
                put("endHour", item.endHour)
                put("endMinute", item.endMinute)
                put("hall", item.hall)
                put("building", item.building)
                put("instructor", item.instructor)
                put("subgroup", item.subgroup)
            }
            array.put(obj)
        }
        root.put("schedule", array)
        root.toString(2)
    }

    // Schedule JSON Import
    suspend fun importScheduleFromJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val array = root.getJSONArray("schedule")
            val importedList = mutableListOf<ScheduleItem>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val item = ScheduleItem(
                    courseCode = obj.optString("courseCode", "CSE 101"),
                    courseName = obj.optString("courseName", "General Course"),
                    type = obj.optString("type", "Lecture"),
                    dayOfWeek = obj.optInt("dayOfWeek", Calendar.SATURDAY),
                    startHour = obj.optInt("startHour", 9),
                    startMinute = obj.optInt("startMinute", 0),
                    endHour = obj.optInt("endHour", 10),
                    endMinute = obj.optInt("endMinute", 30),
                    hall = obj.optString("hall", "Hall 101"),
                    building = obj.optString("building", "Building A"),
                    instructor = obj.optString("instructor", "Faculty Staff"),
                    subgroup = obj.optString("subgroup", "All")
                )
                importedList.add(item)
            }

            if (importedList.isNotEmpty()) {
                scheduleDao.insertItems(importedList)
            }
            importedList.size
        } catch (e: Exception) {
            Log.e("DumuRepository", "Error parsing schedule JSON", e)
            throw IllegalArgumentException("Invalid schedule JSON format: ${e.message}")
        }
    }

    private fun createDefaultSchedule(): List<ScheduleItem> {
        return listOf(
            // Saturday (7)
            ScheduleItem(
                courseCode = "AI 401",
                courseName = "Machine Learning",
                type = "Lecture",
                dayOfWeek = Calendar.SATURDAY,
                startHour = 9,
                startMinute = 0,
                endHour = 10,
                endMinute = 30,
                hall = "Hall 204",
                building = "Building A",
                instructor = "Prof. Dr. Khaled",
                subgroup = "All"
            ),
            ScheduleItem(
                courseCode = "AI 401",
                courseName = "Machine Learning Lab",
                type = "Lab",
                dayOfWeek = Calendar.SATURDAY,
                startHour = 10,
                startMinute = 45,
                endHour = 12,
                endMinute = 15,
                hall = "Lab 3",
                building = "Building B",
                instructor = "Eng. Sarah",
                subgroup = "Section 1"
            ),
            ScheduleItem(
                courseCode = "CSE 315",
                courseName = "Operating Systems",
                type = "Lecture",
                dayOfWeek = Calendar.SATURDAY,
                startHour = 12,
                startMinute = 45,
                endHour = 14,
                endMinute = 15,
                hall = "Hall 101",
                building = "Building A",
                instructor = "Dr. Tamer",
                subgroup = "All"
            ),

            // Sunday (1)
            ScheduleItem(
                courseCode = "AI 412",
                courseName = "Computer Vision",
                type = "Lecture",
                dayOfWeek = Calendar.SUNDAY,
                startHour = 8,
                startMinute = 45,
                endHour = 10,
                endMinute = 15,
                hall = "Auditorium 1",
                building = "Building C",
                instructor = "Dr. Hesham",
                subgroup = "All"
            ),
            ScheduleItem(
                courseCode = "AI 412",
                courseName = "Computer Vision Section",
                type = "Section",
                dayOfWeek = Calendar.SUNDAY,
                startHour = 10,
                startMinute = 30,
                endHour = 12,
                endMinute = 0,
                hall = "Room 208",
                building = "Building C",
                instructor = "Eng. Omar",
                subgroup = "Section 1"
            ),
            ScheduleItem(
                courseCode = "MAT 204",
                courseName = "Probability & Statistics",
                type = "Lecture",
                dayOfWeek = Calendar.SUNDAY,
                startHour = 12,
                startMinute = 30,
                endHour = 14,
                endMinute = 0,
                hall = "Hall 302",
                building = "Building A",
                instructor = "Dr. Noha",
                subgroup = "All"
            ),

            // Monday (2)
            ScheduleItem(
                courseCode = "GP 499",
                courseName = "Capstone Graduation Project",
                type = "Lab",
                dayOfWeek = Calendar.MONDAY,
                startHour = 9,
                startMinute = 0,
                endHour = 11,
                endMinute = 0,
                hall = "AI Lab 1",
                building = "Building B",
                instructor = "Project Committee",
                subgroup = "Section 1"
            ),
            ScheduleItem(
                courseCode = "AI 423",
                courseName = "Autonomous Robotics",
                type = "Lecture",
                dayOfWeek = Calendar.MONDAY,
                startHour = 11,
                startMinute = 30,
                endHour = 13,
                endMinute = 0,
                hall = "Hall 205",
                building = "Building A",
                instructor = "Dr. Mostafa",
                subgroup = "All"
            ),

            // Tuesday (3)
            ScheduleItem(
                courseCode = "AI 405",
                courseName = "Natural Language Processing",
                type = "Lecture",
                dayOfWeek = Calendar.TUESDAY,
                startHour = 9,
                startMinute = 0,
                endHour = 10,
                endMinute = 30,
                hall = "Hall 102",
                building = "Building A",
                instructor = "Dr. Mona",
                subgroup = "All"
            ),
            ScheduleItem(
                courseCode = "AI 405",
                courseName = "NLP Practical Lab",
                type = "Lab",
                dayOfWeek = Calendar.TUESDAY,
                startHour = 10,
                startMinute = 45,
                endHour = 12,
                endMinute = 15,
                hall = "Lab 5",
                building = "Building B",
                instructor = "Eng. Ahmed",
                subgroup = "Section 1"
            ),
            ScheduleItem(
                courseCode = "HUM 102",
                courseName = "Technical Communication",
                type = "Lecture",
                dayOfWeek = Calendar.TUESDAY,
                startHour = 13,
                startMinute = 0,
                endHour = 14,
                endMinute = 30,
                hall = "Hall 304",
                building = "Building C",
                instructor = "Dr. Salma",
                subgroup = "All"
            ),

            // Wednesday (4)
            ScheduleItem(
                courseCode = "AI 430",
                courseName = "Cloud Computing for AI",
                type = "Lecture",
                dayOfWeek = Calendar.WEDNESDAY,
                startHour = 8,
                startMinute = 45,
                endHour = 10,
                endMinute = 15,
                hall = "Lab 4",
                building = "Building B",
                instructor = "Dr. Yasser",
                subgroup = "All"
            ),
            ScheduleItem(
                courseCode = "CSE 420",
                courseName = "Distributed Systems",
                type = "Lecture",
                dayOfWeek = Calendar.WEDNESDAY,
                startHour = 10,
                startMinute = 45,
                endHour = 12,
                endMinute = 15,
                hall = "Hall 202",
                building = "Building A",
                instructor = "Dr. Sherif",
                subgroup = "All"
            ),
            ScheduleItem(
                courseCode = "AI 430",
                courseName = "Cloud Infrastructure Lab",
                type = "Lab",
                dayOfWeek = Calendar.WEDNESDAY,
                startHour = 12,
                startMinute = 45,
                endHour = 14,
                endMinute = 15,
                hall = "Lab 2",
                building = "Building B",
                instructor = "Eng. Mahmoud",
                subgroup = "Section 1"
            ),

            // Thursday (5)
            ScheduleItem(
                courseCode = "SEM 401",
                courseName = "AI Innovation Seminar",
                type = "Lecture",
                dayOfWeek = Calendar.THURSDAY,
                startHour = 9,
                startMinute = 30,
                endHour = 11,
                endMinute = 30,
                hall = "Auditorium 2",
                building = "Building C",
                instructor = "Guest Speakers",
                subgroup = "All"
            ),
            ScheduleItem(
                courseCode = "ACT 101",
                courseName = "Student Activity & AI Club",
                type = "Section",
                dayOfWeek = Calendar.THURSDAY,
                startHour = 12,
                startMinute = 0,
                endHour = 14,
                endMinute = 0,
                hall = "Innovation Hub",
                building = "Building D",
                instructor = "Student Council",
                subgroup = "All"
            )
        )
    }
}
