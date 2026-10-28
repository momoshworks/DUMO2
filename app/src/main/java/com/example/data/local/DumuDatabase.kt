package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AudioRecording
import com.example.data.model.DulmsTask
import com.example.data.model.PomodoroSession
import com.example.data.model.ScheduleItem
import com.example.data.model.StudyFile

@Database(
    entities = [
        ScheduleItem::class,
        DulmsTask::class,
        StudyFile::class,
        AudioRecording::class,
        PomodoroSession::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DumuDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun dulmsTaskDao(): DulmsTaskDao
    abstract fun studyFileDao(): StudyFileDao
    abstract fun audioRecordingDao(): AudioRecordingDao
    abstract fun pomodoroDao(): PomodoroDao

    companion object {
        @Volatile
        private var INSTANCE: DumuDatabase? = null

        fun getInstance(context: Context): DumuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DumuDatabase::class.java,
                    "dumu_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
