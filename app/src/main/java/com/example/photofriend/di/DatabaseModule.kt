package com.example.photofriend.di

import android.content.Context
import androidx.room.Room
import com.example.photofriend.data.local.db.PhotofriendDatabase
import com.example.photofriend.data.local.db.dao.CameraModelDao
import com.example.photofriend.data.local.db.dao.CameraSettingDao
import com.example.photofriend.data.local.db.dao.RecipeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhotofriendDatabase =
        Room.databaseBuilder(context, PhotofriendDatabase::class.java, "photofriend.db")
            .build()

    @Provides
    fun provideCameraModelDao(db: PhotofriendDatabase): CameraModelDao = db.cameraModelDao()

    @Provides
    fun provideCameraSettingDao(db: PhotofriendDatabase): CameraSettingDao = db.cameraSettingDao()

    @Provides
    fun provideRecipeDao(db: PhotofriendDatabase): RecipeDao = db.recipeDao()
}
