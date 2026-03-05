package com.example.photofriend.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photofriend.data.local.db.entity.CameraModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraModelDao {
    @Query("SELECT * FROM camera_models ORDER BY brand, name")
    fun getAll(): Flow<List<CameraModelEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(models: List<CameraModelEntity>)

    @Query("SELECT COUNT(*) FROM camera_models")
    suspend fun count(): Int
}
