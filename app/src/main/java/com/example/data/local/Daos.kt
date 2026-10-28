package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AudioRecording
import com.example.data.model.DulmsTask
import com.example.data.model.PomodoroSession
import com.example.data.model.ScheduleItem
import com.example.data.model.StudyFile
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getAllScheduleItems(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE dayOfWeek = :dayOfWeek ORDER BY startHour ASC, startMinute ASC")
    fun getScheduleForDay(dayOfWeek: Int): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE dayOfWeek = :dayOfWeek ORDER BY startHour ASC, startMinute ASC")
    suspend fun getScheduleForDayList(dayOfWeek: Int): List<ScheduleItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ScheduleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ScheduleItem>)

    @Update
    suspend fun updateItem(item: ScheduleItem)

    @Delete
    suspend fun deleteItem(item: ScheduleItem)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM schedule_items")
    suspend fun clearAll()
}

@Dao
interface DulmsTaskDao {
    @Query("SELECT * FROM dulms_tasks ORDER BY dueDateMillis ASC")
    fun getAllTasks(): Flow<List<DulmsTask>>

    @Query("SELECT * FROM dulms_tasks WHERE dueDateMillis <= :dueThreshold ORDER BY dueDateMillis ASC")
    fun getTasksDueBefore(dueThreshold: Long): Flow<List<DulmsTask>>

    @Query("SELECT * FROM dulms_tasks WHERE type = :type ORDER BY dueDateMillis ASC")
    fun getTasksByType(type: String): Flow<List<DulmsTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DulmsTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<DulmsTask>)

    @Update
    suspend fun updateTask(task: DulmsTask)

    @Query("UPDATE dulms_tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: String, completed: Boolean)

    @Query("UPDATE dulms_tasks SET alarmScheduled = :scheduled WHERE id = :id")
    suspend fun setAlarmScheduled(id: String, scheduled: Boolean)

    @Query("DELETE FROM dulms_tasks WHERE id = :id")
    suspend fun deleteTask(id: String)
}

@Dao
interface StudyFileDao {
    @Query("SELECT * FROM study_files ORDER BY lastModified DESC")
    fun getAllFiles(): Flow<List<StudyFile>>

    @Query("SELECT * FROM study_files WHERE category = :category ORDER BY lastModified DESC")
    fun getFilesByCategory(category: String): Flow<List<StudyFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: StudyFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<StudyFile>)

    @Delete
    suspend fun deleteFile(file: StudyFile)

    @Query("DELETE FROM study_files WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AudioRecordingDao {
    @Query("SELECT * FROM audio_recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<AudioRecording>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: AudioRecording): Long

    @Delete
    suspend fun deleteRecording(recording: AudioRecording)

    @Query("DELETE FROM audio_recordings WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_sessions ORDER BY completedAt DESC")
    fun getAllSessions(): Flow<List<PomodoroSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: PomodoroSession): Long
}
