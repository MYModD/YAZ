package com.example.refrigeratordatabase.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.refrigeratordatabase.data.local.entity.Food
import com.example.refrigeratordatabase.data.local.relation.FoodWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: Food)

    @Update
    suspend fun updateFood(food: Food)

    @Delete
    suspend fun deleteFood(food: Food)

    @Transaction
    @Query("SELECT * FROM foods ORDER BY expiry_date ASC")
    fun getAllFoodsWithCategory(): Flow<List<FoodWithCategory>>
}
