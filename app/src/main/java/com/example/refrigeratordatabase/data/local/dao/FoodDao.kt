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

    // 食材名で検索（PHPの SELECT * FROM foods WHERE name LIKE '%検索語%' と同じ）
    @Transaction
    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY expiry_date ASC")
    fun searchFoods(query: String): Flow<List<FoodWithCategory>>

    // カテゴリで絞り込み（PHPの SELECT * FROM foods WHERE category_id = ? と同じ）
    @Transaction
    @Query("SELECT * FROM foods WHERE category_id = :categoryId ORDER BY expiry_date ASC")
    fun getFoodsByCategory(categoryId: Int): Flow<List<FoodWithCategory>>

    // ID指定で1件取得（PHPの SELECT * FROM foods WHERE food_id = ? LIMIT 1 と同じ）
    @Query("SELECT * FROM foods WHERE food_id = :foodId")
    suspend fun getFoodById(foodId: Int): Food?

    // IDで削除（更新ダイアログの削除ボタン用）
    @Query("DELETE FROM foods WHERE food_id = :foodId")
    suspend fun deleteFoodById(foodId: Int)
}
