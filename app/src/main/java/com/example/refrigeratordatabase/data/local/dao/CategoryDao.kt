package com.example.refrigeratordatabase.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.refrigeratordatabase.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<Category>)

    @Query("SELECT * FROM categories ORDER BY category_id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}
