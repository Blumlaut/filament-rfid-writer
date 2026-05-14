package com.blumlaut.filamenttagwriter

import android.app.Application
import com.blumlaut.filamenttagwriter.data.local.FilamentDatabase

class FilamentTagApp : Application() {

    lateinit var database: FilamentDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = FilamentDatabase.getInstance(this)
    }
}
