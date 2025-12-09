package com.example.refrigeratordatabase.data.repository

import com.example.refrigeratordatabase.data.local.dao.CategoryDao
import com.example.refrigeratordatabase.data.local.dao.FoodDao
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food
import com.example.refrigeratordatabase.data.local.relation.FoodWithCategory
import kotlinx.coroutines.flow.Flow

/**
 * FoodRepository - データアクセスを抽象化するRepository層
 *
 * PHPでいうModelクラスに相当。
 * DAOを直接使わず、このRepositoryを経由することで：
 * - ViewModelとデータベースの間に抽象化レイヤーを作る
 * - 将来的にAPIからのデータ取得などを追加しやすくなる
 * - テスト時にモック(ダミー)に差し替えやすくなる
 */
class FoodRepository(
    private val foodDao: FoodDao,
    private val categoryDao: CategoryDao
) {
    // ========================================
    // 食材関連（PHPの foods テーブル操作に相当）
    // ========================================

    /**
     * 全食材をカテゴリ情報付きで取得
     * Flow = PHPでいう「リアルタイム監視できるResultSet」
     * DBが更新されると自動的に最新データが流れてくる
     */
    val allFoodsWithCategory: Flow<List<FoodWithCategory>> =
        foodDao.getAllFoodsWithCategory()

    /**
     * 食材名で検索
     * PHPの: SELECT * FROM foods WHERE name LIKE '%$query%'
     */
    fun searchFoods(query: String): Flow<List<FoodWithCategory>> =
        foodDao.searchFoods(query)

    /**
     * カテゴリで絞り込み
     * PHPの: SELECT * FROM foods WHERE category_id = $categoryId
     */
    fun getFoodsByCategory(categoryId: Int): Flow<List<FoodWithCategory>> =
        foodDao.getFoodsByCategory(categoryId)

    /**
     * ID指定で1件取得
     * PHPの: SELECT * FROM foods WHERE food_id = $foodId LIMIT 1
     */
    suspend fun getFoodById(foodId: Int): Food? =
        foodDao.getFoodById(foodId)

    /**
     * 食材を追加
     * PHPの: INSERT INTO foods (...)
     */
    suspend fun insertFood(food: Food) =
        foodDao.insertFood(food)

    /**
     * 食材を更新（残量更新など）
     * PHPの: UPDATE foods SET ... WHERE food_id = $id
     */
    suspend fun updateFood(food: Food) =
        foodDao.updateFood(food)

    /**
     * 食材を削除
     * PHPの: DELETE FROM foods WHERE food_id = $id
     */
    suspend fun deleteFood(food: Food) =
        foodDao.deleteFood(food)

    /**
     * IDで食材を削除（Food オブジェクトがない場合用）
     */
    suspend fun deleteFoodById(foodId: Int) =
        foodDao.deleteFoodById(foodId)

    // ========================================
    // カテゴリ関連（PHPの categories テーブル操作に相当）
    // ========================================

    /**
     * 全カテゴリを取得（ドロップダウン用）
     */
    val allCategories: Flow<List<Category>> =
        categoryDao.getAllCategories()

    /**
     * カテゴリを追加（初期データ投入用）
     */
    suspend fun insertCategory(category: Category) =
        categoryDao.insertCategory(category)

    /**
     * 初期カテゴリを確認して投入
     * PHPでいう: IF NOT EXISTS ... INSERT
     * カテゴリが0件なら初期データを投入する
     */
    suspend fun ensureInitialCategories() {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            val initialCategories = listOf(
                Category(categoryId = 1, name = "野菜"),
                Category(categoryId = 2, name = "果物"),
                Category(categoryId = 3, name = "肉類"),
                Category(categoryId = 4, name = "魚介類"),
                Category(categoryId = 5, name = "乳製品"),
                Category(categoryId = 6, name = "調味料"),
                Category(categoryId = 7, name = "飲料"),
                Category(categoryId = 8, name = "その他")
            )
            categoryDao.insertCategories(initialCategories)
        }
    }
}

