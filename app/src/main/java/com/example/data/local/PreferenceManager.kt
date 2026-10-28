package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "dumu_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("PreferenceManager", "EncryptedSharedPreferences error, falling back", e)
        context.getSharedPreferences("dumu_fallback_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_STUDENT_NAME = "student_name"
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_FACULTY = "faculty"
        private const val KEY_LEVEL = "level"
        private const val KEY_SUBGROUP = "subgroup"
        private const val KEY_LMS_USERNAME = "lms_username"
        private const val KEY_LMS_PASSWORD = "lms_password"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_AUTO_ALARM_ENABLED = "auto_alarm_enabled"
        private const val KEY_GITHUB_REPO = "github_repo"
    }

    var studentName: String
        get() = prefs.getString(KEY_STUDENT_NAME, "Mohamed Shaban") ?: "Mohamed Shaban"
        set(value) = prefs.edit().putString(KEY_STUDENT_NAME, value).apply()

    var studentId: String
        get() = prefs.getString(KEY_STUDENT_ID, "202100412") ?: "202100412"
        set(value) = prefs.edit().putString(KEY_STUDENT_ID, value).apply()

    var faculty: String
        get() = prefs.getString(KEY_FACULTY, "Faculty of Artificial Intelligence") ?: "Faculty of Artificial Intelligence"
        set(value) = prefs.edit().putString(KEY_FACULTY, value).apply()

    var level: String
        get() = prefs.getString(KEY_LEVEL, "Level 4 - Senior") ?: "Level 4 - Senior"
        set(value) = prefs.edit().putString(KEY_LEVEL, value).apply()

    var subgroup: String
        get() = prefs.getString(KEY_SUBGROUP, "Section 1") ?: "Section 1"
        set(value) = prefs.edit().putString(KEY_SUBGROUP, value).apply()

    var lmsUsername: String
        get() = prefs.getString(KEY_LMS_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LMS_USERNAME, value).apply()

    var lmsPassword: String
        get() = prefs.getString(KEY_LMS_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LMS_PASSWORD, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var autoAlarmEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_ALARM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_ALARM_ENABLED, value).apply()

    var githubRepo: String
        get() = prefs.getString(KEY_GITHUB_REPO, "momoshworks/DUMU") ?: "momoshworks/DUMU"
        set(value) = prefs.edit().putString(KEY_GITHUB_REPO, value).apply()
}
