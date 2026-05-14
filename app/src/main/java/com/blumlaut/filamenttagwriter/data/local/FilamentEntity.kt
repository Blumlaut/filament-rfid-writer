package com.blumlaut.filamenttagwriter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filaments")
data class FilamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val manufacturerCode: Int,
    val filamentCode: Short,
    val material: String,
    val materialSubtype: String,
    val colorRgb: Int,
    val diameter: Float,
    val weight: Int,
    val productionYear: Int,
    val productionMonth: Int,
)
