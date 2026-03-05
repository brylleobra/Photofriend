package com.example.photofriend.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.photofriend.data.local.db.entity.RecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes WHERE isBuiltIn = 1 ORDER BY name")
    fun getBuiltIn(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE isUserSaved = 1 ORDER BY name")
    fun getSaved(): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity)

    @Query("DELETE FROM recipes WHERE id = :id AND isBuiltIn = 0")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM recipes WHERE isBuiltIn = 1")
    suspend fun countBuiltIn(): Int
}
