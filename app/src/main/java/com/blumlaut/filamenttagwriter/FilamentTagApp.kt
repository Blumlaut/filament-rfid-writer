package com.blumlaut.filamenttagwriter

import android.app.Application
import com.blumlaut.filamenttagwriter.data.local.FilamentDatabase
import com.blumlaut.filamenttagwriter.data.model.SpoolmanLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FilamentTagApp : Application() {

    lateinit var database: FilamentDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = FilamentDatabase.getInstance(this)

        // Load SpoolmanDB in background (non-blocking)
        runBlocking {
            launch(Dispatchers.IO) {
                SpoolmanLoader.load(this@FilamentTagApp)
            }
        }
    }
}
