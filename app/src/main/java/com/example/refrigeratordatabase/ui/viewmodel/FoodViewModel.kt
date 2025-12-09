package com.example.refrigeratordatabase.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food
import com.example.refrigeratordatabase.data.local.relation.FoodWithCategory
import com.example.refrigeratordatabase.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * FoodViewModel - 食材リスト画面のビジネスロジック
 *
 * PHPでいう「Controller層」に相当。
 * ユーザーのアクション（検索、追加、削除など）を受け取り、
 * Repositoryを通じてデータを取得・更新し、UIに反映する。
 *
 * ViewModel = PHPの $_SESSION + Controller の組み合わせに近い。
 * - 画面の状態を保持（回転しても消えない）
 * - ビジネスロジックを実行
 */
class FoodViewModel(
    private val repository: FoodRepository
) : ViewModel() {

    // ========================================
    // UI状態（PHPでいう $_SESSION 変数に相当）
    // ========================================

    /** 検索クエリ */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 選択中のカテゴリID（null = 全て） */
    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId: StateFlow<Int?> = _selectedCategoryId.asStateFlow()

    /** 選択中のタブインデックス（0 = リスト、1 = カレンダー） */
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    /** 追加ダイアログの表示状態 */
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    /** 編集中の食材（null = ダイアログ非表示） */
    private val _editingFood = MutableStateFlow<FoodWithCategory?>(null)
    val editingFood: StateFlow<FoodWithCategory?> = _editingFood.asStateFlow()

    // ========================================
    // 追加ダイアログの入力状態
    // ========================================

    private val _addFoodName = MutableStateFlow("")
    val addFoodName: StateFlow<String> = _addFoodName.asStateFlow()

    private val _addCategoryId = MutableStateFlow<Int?>(null)
    val addCategoryId: StateFlow<Int?> = _addCategoryId.asStateFlow()

    private val _addExpiryDate = MutableStateFlow<Long?>(null)
    val addExpiryDate: StateFlow<Long?> = _addExpiryDate.asStateFlow()

    // ========================================
    // 編集ダイアログの入力状態
    // ========================================

    private val _editRemainingPercentage = MutableStateFlow(100)
    val editRemainingPercentage: StateFlow<Int> = _editRemainingPercentage.asStateFlow()

    // ========================================
    // データベースからのデータ（Flow）
    // ========================================

    /** 全カテゴリ一覧 */
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** フィルタリング済み食材一覧 */
    val filteredFoods: StateFlow<List<FoodWithCategory>> = combine(
        repository.allFoodsWithCategory,
        _searchQuery,
        _selectedCategoryId
    ) { foods, query, categoryId ->
        // PHPでいう WHERE句の条件分岐に相当
        foods.filter { foodWithCategory ->
            val matchesSearch = query.isEmpty() ||
                    foodWithCategory.food.name.contains(query, ignoreCase = true)
            val matchesCategory = categoryId == null ||
                    foodWithCategory.food.categoryId == categoryId
            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ========================================
    // UIアクション（PHPでいう POST リクエスト処理に相当）
    // ========================================

    /** 検索クエリを更新 */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** カテゴリ選択を更新 */
    fun onCategorySelect(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    /** タブ選択を更新 */
    fun onTabSelect(index: Int) {
        _selectedTabIndex.value = index
    }

    /** 追加ダイアログを表示 */
    fun showAddDialog() {
        _addFoodName.value = ""
        _addCategoryId.value = null
        _addExpiryDate.value = null
        _showAddDialog.value = true
    }

    /** 追加ダイアログを閉じる */
    fun hideAddDialog() {
        _showAddDialog.value = false
    }

    /** 追加ダイアログ: 食材名を更新 */
    fun onAddFoodNameChange(name: String) {
        _addFoodName.value = name
    }

    /** 追加ダイアログ: カテゴリを更新 */
    fun onAddCategorySelect(categoryId: Int) {
        _addCategoryId.value = categoryId
    }

    /** 追加ダイアログ: 期限日を更新 */
    fun onAddExpiryDateSelect(date: Long) {
        _addExpiryDate.value = date
    }

    /**
     * 食材を追加
     * PHPでいう: INSERT INTO foods (...) VALUES (...)
     */
    fun addFood() {
        val name = _addFoodName.value
        val categoryId = _addCategoryId.value
        val expiryDate = _addExpiryDate.value

        if (name.isBlank() || categoryId == null || expiryDate == null) {
            return // バリデーションエラー
        }

        viewModelScope.launch {
            val food = Food(
                categoryId = categoryId,
                name = name,
                expiryDate = expiryDate,
                remainingPercentage = 100, // 新規追加時は100%
                createdAt = System.currentTimeMillis()
            )
            repository.insertFood(food)
            hideAddDialog()
        }
    }

    /** 編集ダイアログを表示 */
    fun showEditDialog(foodWithCategory: FoodWithCategory) {
        _editingFood.value = foodWithCategory
        _editRemainingPercentage.value = foodWithCategory.food.remainingPercentage
    }

    /** 編集ダイアログを閉じる */
    fun hideEditDialog() {
        _editingFood.value = null
    }

    /** 編集ダイアログ: 残量を更新 */
    fun onEditRemainingPercentageChange(percentage: Int) {
        _editRemainingPercentage.value = percentage
    }

    /**
     * 食材の残量を更新
     * PHPでいう: UPDATE foods SET remaining_percentage = ? WHERE food_id = ?
     */
    fun updateFood() {
        val food = _editingFood.value?.food ?: return

        viewModelScope.launch {
            val updatedFood = food.copy(
                remainingPercentage = _editRemainingPercentage.value
            )
            repository.updateFood(updatedFood)
            hideEditDialog()
        }
    }

    /**
     * 食材を削除
     * PHPでいう: DELETE FROM foods WHERE food_id = ?
     */
    fun deleteFood() {
        val food = _editingFood.value?.food ?: return

        viewModelScope.launch {
            repository.deleteFood(food)
            hideEditDialog()
        }
    }

    // ========================================
    // ViewModelFactory
    // ========================================

    /**
     * ViewModelFactory - ViewModelのインスタンス生成
     *
     * PHPでいう「Dependency Injection (依存性注入)」に相当。
     * ViewModelに必要なRepository（データベース接続）を渡す。
     */
    class Factory(
        private val repository: FoodRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
                return FoodViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

