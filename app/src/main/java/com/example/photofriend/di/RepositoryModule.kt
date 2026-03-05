package com.example.photofriend.di

import com.example.photofriend.data.repository.AIRepositoryImpl
import com.example.photofriend.data.repository.CameraRepositoryImpl
import com.example.photofriend.domain.repository.AIRepository
import com.example.photofriend.domain.repository.CameraRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCameraRepository(impl: CameraRepositoryImpl): CameraRepository

    @Binds
    @Singleton
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository
}
