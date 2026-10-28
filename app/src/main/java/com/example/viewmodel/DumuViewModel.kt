package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DumuApplication
import com.example.data.model.AudioRecording
import com.example.data.model.DulmsTask
import com.example.data.model.PomodoroSession
import com.example.data.model.ScheduleItem
import com.example.data.model.StudyFile
import com.example.network.DulmsSyncEngine
import com.example.network.GitHubUpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class LiveLectureState(
    val liveItem: ScheduleItem? = null,
    val upNextItem: ScheduleItem? = null,
    val isLiveNow: Boolean = false,
    val progress: Float = 0f,
    val remainingMinutes: Int = 0,
    val minutesUntilNext: Int = 0,
    val totalTodayLectures: Int = 0
)

class DumuViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as DumuApplication
    private val repository = app.repository
    private val prefs = app.preferenceManager
    private val updateChecker = GitHubUpdateChecker(application)

    // Current real-time clock ticker
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis = _currentTimeMillis.asStateFlow()

    // Selected Day in Schedule view (1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat)
    private val _selectedDay = MutableStateFlow(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
    val selectedDay = _selectedDay.asStateFlow()

    // Subgroup filter
    private val _selectedSubgroupFilter = MutableStateFlow("All")
    val selectedSubgroupFilter = _selectedSubgroupFilter.asStateFlow()

    val allSchedule: StateFlow<List<ScheduleItem>> = repository.allSchedule
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<DulmsTask>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFiles: StateFlow<List<StudyFile>> = repository.allFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecordings: StateFlow<List<AudioRecording>> = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPomodoroSessions: StateFlow<List<PomodoroSession>> = repository.allPomodoroSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live lecture calculation
    val liveLectureState: StateFlow<LiveLectureState> = combine(
        allSchedule,
        _currentTimeMillis,
        _selectedSubgroupFilter
    ) { schedule, nowMillis, subgroup ->
        computeLiveLectureState(schedule, nowMillis, subgroup)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LiveLectureState())

    // Tasks due within 48 hours
    val upcomingTasks: StateFlow<List<DulmsTask>> = combine(allTasks, _currentTimeMillis) { tasks, now ->
        val threshold48h = now + (48 * 3600 * 1000L)
        tasks.filter { !it.isCompleted && it.dueDateMillis in (now - 3600 * 1000L)..threshold48h }
            .sortedBy { it.dueDateMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    // Profile state
    private val _studentName = MutableStateFlow(prefs.studentName)
    val studentName = _studentName.asStateFlow()

    private val _studentId = MutableStateFlow(prefs.studentId)
    val studentId = _studentId.asStateFlow()

    private val _faculty = MutableStateFlow(prefs.faculty)
    val faculty = _faculty.asStateFlow()

    private val _level = MutableStateFlow(prefs.level)
    val level = _level.asStateFlow()

    private val _userSubgroup = MutableStateFlow(prefs.subgroup)
    val userSubgroup = _userSubgroup.asStateFlow()

    private val _lmsUsername = MutableStateFlow(prefs.lmsUsername)
    val lmsUsername = _lmsUsername.asStateFlow()

    private val _lmsPassword = MutableStateFlow(prefs.lmsPassword)
    val lmsPassword = _lmsPassword.asStateFlow()

    // OTA Update state
    private val _updateInfo = MutableStateFlow<GitHubUpdateChecker.ReleaseInfo?>(null)
    val updateInfo = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate = _isCheckingUpdate.asStateFlow()

    init {
        // Ticker to refresh minute countdowns & live lecture card
        viewModelScope.launch {
            while (true) {
                _currentTimeMillis.value = System.currentTimeMillis()
                delay(15000) // update every 15s
            }
        }
    }

    fun selectDay(dayOfWeek: Int) {
        _selectedDay.value = dayOfWeek
    }

    fun setSubgroupFilter(subgroup: String) {
        _selectedSubgroupFilter.value = subgroup
    }

    fun toggleTask(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(taskId, completed)
        }
    }

    fun syncDulms() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Authenticating with DULMS portal..."
            val result = repository.syncDulmsTasks()
            _isSyncing.value = false
            _syncMessage.value = when (result) {
                is DulmsSyncEngine.SyncResult.Success -> result.message
                is DulmsSyncEngine.SyncResult.Error -> result.error
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun addScheduleItem(item: ScheduleItem) {
        viewModelScope.launch {
            repository.insertScheduleItem(item)
        }
    }

    fun updateScheduleItem(item: ScheduleItem) {
        viewModelScope.launch {
            repository.updateScheduleItem(item)
        }
    }

    fun deleteScheduleItem(id: Long) {
        viewModelScope.launch {
            repository.deleteScheduleItem(id)
        }
    }

    suspend fun exportScheduleJson(): String {
        return repository.exportScheduleToJson(allSchedule.value)
    }

    suspend fun importScheduleJson(json: String): Result<Int> {
        return try {
            val count = repository.importScheduleFromJson(json)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateProfile(
        name: String,
        id: String,
        facultyVal: String,
        levelVal: String,
        subgroupVal: String,
        lmsUser: String,
        lmsPass: String
    ) {
        prefs.studentName = name
        prefs.studentId = id
        prefs.faculty = facultyVal
        prefs.level = levelVal
        prefs.subgroup = subgroupVal
        prefs.lmsUsername = lmsUser
        prefs.lmsPassword = lmsPass

        _studentName.value = name
        _studentId.value = id
        _faculty.value = facultyVal
        _level.value = levelVal
        _userSubgroup.value = subgroupVal
        _lmsUsername.value = lmsUser
        _lmsPassword.value = lmsPass
    }

    fun addStudyFile(file: StudyFile) {
        viewModelScope.launch {
            repository.addStudyFile(file)
        }
    }

    fun deleteStudyFile(id: Long) {
        viewModelScope.launch {
            repository.deleteStudyFile(id)
        }
    }

    fun addAudioRecording(recording: AudioRecording) {
        viewModelScope.launch {
            repository.addAudioRecording(recording)
        }
    }

    fun deleteAudioRecording(id: Long) {
        viewModelScope.launch {
            repository.deleteAudioRecording(id)
        }
    }

    fun logPomodoro(courseTag: String, durationMinutes: Int) {
        viewModelScope.launch {
            repository.logPomodoroSession(
                PomodoroSession(
                    courseTag = courseTag,
                    durationMinutes = durationMinutes,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun checkForAppUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val info = updateChecker.checkForUpdates(prefs.githubRepo, "1.0")
            _updateInfo.value = info
            _isCheckingUpdate.value = false
        }
    }

    private fun computeLiveLectureState(
        all: List<ScheduleItem>,
        nowMillis: Long,
        subgroupFilter: String
    ): LiveLectureState {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        val currentMinuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val todayLectures = all.filter { it.dayOfWeek == currentDay }
            .filter { subgroupFilter == "All" || it.subgroup == "All" || it.subgroup.equals(subgroupFilter, ignoreCase = true) }
            .sortedBy { it.startHour * 60 + it.startMinute }

        if (todayLectures.isEmpty()) {
            return LiveLectureState(totalTodayLectures = 0)
        }

        // Check if one is live now
        val liveNow = todayLectures.firstOrNull { item ->
            val start = item.startHour * 60 + item.startMinute
            val end = item.endHour * 60 + item.endMinute
            currentMinuteOfDay in start until end
        }

        if (liveNow != null) {
            val start = liveNow.startHour * 60 + liveNow.startMinute
            val end = liveNow.endHour * 60 + liveNow.endMinute
            val elapsed = currentMinuteOfDay - start
            val total = end - start
            val progress = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            val remaining = (end - currentMinuteOfDay).coerceAtLeast(0)

            return LiveLectureState(
                liveItem = liveNow,
                isLiveNow = true,
                progress = progress,
                remainingMinutes = remaining,
                totalTodayLectures = todayLectures.size
            )
        }

        // Check for upcoming later today
        val upNext = todayLectures.firstOrNull { item ->
            val start = item.startHour * 60 + item.startMinute
            start > currentMinuteOfDay
        }

        if (upNext != null) {
            val start = upNext.startHour * 60 + upNext.startMinute
            val minutesUntil = start - currentMinuteOfDay

            return LiveLectureState(
                upNextItem = upNext,
                isLiveNow = false,
                minutesUntilNext = minutesUntil,
                totalTodayLectures = todayLectures.size
            )
        }

        return LiveLectureState(
            totalTodayLectures = todayLectures.size
        )
    }
}
