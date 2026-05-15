package com.blumlaut.filamenttagwriter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "printers")
data class PrinterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 3030,
    val lastSeen: Long = 0,
)
