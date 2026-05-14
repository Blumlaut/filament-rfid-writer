package com.blumlaut.filamenttagwriter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filaments")
data class FilamentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val manufacturerCode: Int,
    val material: String,
    val subtypeCode: Short,
    val subtype: String,
    val colorRgb: Int,
    val colorModifier: Byte,
    val minTemp: Short,
    val maxTemp: Short,
    val diameter: Float,
    val weight: Int,
    val productionDateRaw: Short,
)
