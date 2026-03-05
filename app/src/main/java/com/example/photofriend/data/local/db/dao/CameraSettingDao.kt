package com.example.photofriend.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photofriend.data.local.db.entity.CameraSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CameraSettingDao {
    @Query("SELECT * FROM camera_settings WHERE cameraId = :cameraId ORDER BY category, name")
    fun getByCameraId(cameraId: String): Flow<List<CameraSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(settings: List<CameraSettingEntity>)

    @Query("SELECT COUNT(*) FROM camera_settings WHERE cameraId = :cameraId")
    suspend fun countForCamera(cameraId: String): Int
}
