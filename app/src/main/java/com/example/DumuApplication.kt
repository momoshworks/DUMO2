package com.example

import android.app.Application
import com.example.data.local.DumuDatabase
import com.example.data.local.PreferenceManager
import com.example.data.repository.DumuRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DumuApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { DumuDatabase.getInstance(this) }
    val preferenceManager by lazy { PreferenceManager(this) }
    val repository by lazy { DumuRepository(this, database, preferenceManager) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch(Dispatchers.IO) {
            repository.initializeDefaultDataIfEmpty()
        }
    }
}
