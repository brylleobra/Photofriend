package com.example.photofriend.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.photofriend.data.local.db.dao.CameraModelDao
import com.example.photofriend.data.local.db.dao.CameraSettingDao
import com.example.photofriend.data.local.db.dao.RecipeDao
import com.example.photofriend.data.local.db.entity.CameraModelEntity
import com.example.photofriend.data.local.db.entity.CameraSettingEntity
import com.example.photofriend.data.local.db.entity.RecipeEntity
import com.example.photofriend.data.local.seed.CameraSeeds
import com.example.photofriend.data.local.seed.RecipeSeeds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CameraModelEntity::class, CameraSettingEntity::class, RecipeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PhotofriendDatabase : RoomDatabase() {

    abstract fun cameraModelDao(): CameraModelDao
    abstract fun cameraSettingDao(): CameraSettingDao
    abstract fun recipeDao(): RecipeDao

    class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            // Seeding is handled in the repository after DB creation via DAO
        }
    }
}
