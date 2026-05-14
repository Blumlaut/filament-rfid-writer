package com.blumlaut.filamenttagwriter.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FilamentDao {

    @Query("SELECT * FROM filaments")
    fun getAll(): Flow<List<FilamentEntity>>

    @Query("SELECT * FROM filaments WHERE id = :id")
    suspend fun getById(id: String): FilamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filament: FilamentEntity)

    @Delete
    suspend fun delete(filament: FilamentEntity)
}
