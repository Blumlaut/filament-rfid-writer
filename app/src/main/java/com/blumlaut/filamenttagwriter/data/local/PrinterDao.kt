package com.blumlaut.filamenttagwriter.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterDao {

    @Query("SELECT * FROM printers")
    fun getAll(): Flow<List<PrinterEntity>>

    @Query("SELECT * FROM printers WHERE id = :id")
    suspend fun getById(id: String): PrinterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(printer: PrinterEntity)

    @Delete
    suspend fun delete(printer: PrinterEntity)
}
