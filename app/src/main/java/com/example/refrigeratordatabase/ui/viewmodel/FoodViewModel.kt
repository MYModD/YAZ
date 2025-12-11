package com.example.refrigeratordatabase.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.refrigeratordatabase.data.auth.GoogleAuthService
import com.example.refrigeratordatabase.data.auth.GoogleSignInResult
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food
import com.example.refrigeratordatabase.data.local.relation.FoodWithCategory
import com.example.refrigeratordatabase.data.model.CalendarEvent
import com.example.refrigeratordatabase.data.network.GoogleCalendarService
import com.example.refrigeratordatabase.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

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
    private val repository: FoodRepository,
    private val googleAuthService: GoogleAuthService? = null,
    private val googleCalendarService: GoogleCalendarService? = null
) : ViewModel() {

    companion object {
        private const val TAG = "FoodViewModel"
    }

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
    // Google Calendar 連携状態
    // PHPでいう $_SESSION['google_connected'] に相当
    // ========================================

    /** Googleカレンダー連携状態 */
    private val _isGoogleConnected = MutableStateFlow(false)
    val isGoogleConnected: StateFlow<Boolean> = _isGoogleConnected.asStateFlow()

    /** Googleカレンダー連携中の状態（ローディング） */
    private val _isGoogleConnecting = MutableStateFlow(false)
    val isGoogleConnecting: StateFlow<Boolean> = _isGoogleConnecting.asStateFlow()

    /** Googleカレンダーのイベント（予定がある日のタイムスタンプセット） */
    private val _googleEventDates = MutableStateFlow<Set<Long>>(emptySet())
    val googleEventDates: StateFlow<Set<Long>> = _googleEventDates.asStateFlow()

    /** 選択日のGoogleカレンダーイベント */
    private val _googleEventsForSelectedDate = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val googleEventsForSelectedDate: StateFlow<List<CalendarEvent>> = _googleEventsForSelectedDate.asStateFlow()

    /** エラーメッセージ */
    private val _googleErrorMessage = MutableStateFlow<String?>(null)
    val googleErrorMessage: StateFlow<String?> = _googleErrorMessage.asStateFlow()

    /** 現在表示中のカレンダー年月 */
    private var currentCalendarYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentCalendarMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

    init {
        // 初期化時にGoogle接続状態を確認
        checkGoogleConnectionStatus()
    }

    /**
     * Google接続状態を確認
     * PHPでいう: isset($_SESSION['google_access_token'])
     */
    private fun checkGoogleConnectionStatus() {
        _isGoogleConnected.value = googleAuthService?.isSignedIn() == true
        
        // 接続済みなら現在の月のイベントを取得
        if (_isGoogleConnected.value) {
            fetchGoogleEventsForMonth(currentCalendarYear, currentCalendarMonth)
        }
    }

    // ========================================
    // 追加ダイアログの入力状態
    // ========================================

    private val _addFoodName = MutableStateFlow("")
    val addFoodName: StateFlow<String> = _addFoodName.asStateFlow()

    private val _addCategoryId = MutableStateFlow<Int?>(null)
    val addCategoryId: StateFlow<Int?> = _addCategoryId.asStateFlow()

    private val _addExpiryDate = MutableStateFlow<Long?>(null)
    val addExpiryDate: StateFlow<Long?> = _addExpiryDate.asStateFlow()

    /** 食材追加中フラグ（連打防止用） */
    private val _isAddingFood = MutableStateFlow(false)
    val isAddingFood: StateFlow<Boolean> = _isAddingFood.asStateFlow()

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
     * Googleカレンダー連携中の場合は、期限日をカレンダーにも追加する
     * 
     * 連打防止: 追加中は処理をスキップする
     */
    fun addFood() {
        // 連打防止: すでに追加中なら何もしない
        if (_isAddingFood.value) {
            Log.d(TAG, "Already adding food, ignoring duplicate request")
            return
        }

        val name = _addFoodName.value
        val categoryId = _addCategoryId.value
        val expiryDate = _addExpiryDate.value

        if (name.isBlank() || categoryId == null || expiryDate == null) {
            return // バリデーションエラー
        }

        // 追加中フラグをON
        _isAddingFood.value = true

        viewModelScope.launch {
            try {
                val food = Food(
                    categoryId = categoryId,
                    name = name,
                    expiryDate = expiryDate,
                    remainingPercentage = 100, // 新規追加時は100%
                    createdAt = System.currentTimeMillis()
                )
                repository.insertFood(food)
                
                // Googleカレンダー連携中なら、期限日をカレンダーにも追加
                if (_isGoogleConnected.value && googleCalendarService != null) {
                    try {
                        val result = googleCalendarService.addFoodExpiryEvent(name, expiryDate)
                        result.onSuccess { eventId ->
                            Log.d(TAG, "Successfully added to Google Calendar: $eventId")
                            // カレンダーのイベント日付を更新
                            fetchGoogleEventsForMonth(currentCalendarYear, currentCalendarMonth)
                        }.onFailure { e ->
                            Log.e(TAG, "Failed to add to Google Calendar", e)
                            // Googleカレンダー追加に失敗してもローカルDBには追加済み
                            // ユーザーへのエラー通知は任意
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception adding to Google Calendar", e)
                    }
                }
                
                hideAddDialog()
            } finally {
                // 処理完了後にフラグをOFF
                _isAddingFood.value = false
            }
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
    // Google Calendar 連携アクション
    // PHPでいう OAuthフロー処理に相当
    // ========================================

    /**
     * Googleカレンダーと連携開始（サイレントサインインを試行）
     * インタラクティブなサインインが必要な場合は、MainActivityから直接呼び出す
     */
    fun connectGoogleCalendar() {
        if (googleAuthService == null) {
            Log.w(TAG, "GoogleAuthService is not initialized")
            _googleErrorMessage.value = "Google認証サービスが初期化されていません"
            return
        }

        viewModelScope.launch {
            _isGoogleConnecting.value = true
            _googleErrorMessage.value = null

            try {
                // まずサイレントサインインを試行
                val result = googleAuthService.trySilentSignIn()
                handleGoogleSignInResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in exception", e)
                _googleErrorMessage.value = e.message ?: "不明なエラーが発生しました"
                _isGoogleConnecting.value = false
            }
        }
    }

    /**
     * Googleサインイン結果を処理
     * MainActivityからコールバックで呼び出される
     */
    fun handleGoogleSignInResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Success -> {
                Log.d(TAG, "Google sign-in successful: ${result.email}")
                _isGoogleConnected.value = true
                _googleErrorMessage.value = null
                // 連携成功後、現在の月のイベントを取得
                fetchGoogleEventsForMonth(currentCalendarYear, currentCalendarMonth)
            }
            is GoogleSignInResult.Error -> {
                Log.e(TAG, "Google sign-in failed: ${result.message}")
                _googleErrorMessage.value = result.message
            }
            is GoogleSignInResult.Cancelled -> {
                Log.d(TAG, "Google sign-in cancelled")
                _googleErrorMessage.value = "ログインがキャンセルされました"
            }
            is GoogleSignInResult.NeedInteractiveSignIn -> {
                // インタラクティブサインインが必要
                // MainActivityで直接処理するので、ここではエラーメッセージを設定
                Log.d(TAG, "Need interactive sign-in")
                _googleErrorMessage.value = "サインイン画面を表示します..."
            }
        }
        _isGoogleConnecting.value = false
    }

    /**
     * Google連携中状態を設定（MainActivityから呼び出し）
     */
    fun setGoogleConnecting(connecting: Boolean) {
        _isGoogleConnecting.value = connecting
        if (connecting) {
            _googleErrorMessage.value = null
        }
    }

    /**
     * Googleカレンダーとの連携を解除
     * PHPでいう: unset($_SESSION['google_access_token'])
     */
    fun disconnectGoogleCalendar() {
        viewModelScope.launch {
            googleAuthService?.signOut()
            _isGoogleConnected.value = false
            _googleEventDates.value = emptySet()
            _googleEventsForSelectedDate.value = emptyList()
            Log.d(TAG, "Google calendar disconnected")
        }
    }

    /**
     * 指定年月のGoogleカレンダーイベントを取得
     * PHPでいう: curl()でCalendar APIを叩く処理
     *
     * @param year 年
     * @param month 月（1-12）
     */
    fun fetchGoogleEventsForMonth(year: Int, month: Int) {
        if (googleCalendarService == null || !_isGoogleConnected.value) {
            return
        }

        currentCalendarYear = year
        currentCalendarMonth = month

        viewModelScope.launch {
            try {
                val result = googleCalendarService.getEventDates(year, month)
                result.onSuccess { dates ->
                    _googleEventDates.value = dates
                    Log.d(TAG, "Fetched ${dates.size} event dates for $year/$month")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch event dates", e)
                    // エラーでも空セットを設定（UIがクラッシュしないように）
                    _googleEventDates.value = emptySet()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching event dates", e)
            }
        }
    }

    /**
     * 選択日のGoogleカレンダーイベントを取得
     *
     * @param timestamp 選択日のタイムスタンプ
     */
    fun fetchGoogleEventsForDate(timestamp: Long) {
        if (googleCalendarService == null || !_isGoogleConnected.value) {
            _googleEventsForSelectedDate.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val result = googleCalendarService.getEventsForDay(timestamp)
                result.onSuccess { events ->
                    _googleEventsForSelectedDate.value = events
                    Log.d(TAG, "Fetched ${events.size} events for selected date")
                }.onFailure { e ->
                    Log.e(TAG, "Failed to fetch events for date", e)
                    _googleEventsForSelectedDate.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching events for date", e)
            }
        }
    }

    /**
     * エラーメッセージをクリア
     */
    fun clearGoogleError() {
        _googleErrorMessage.value = null
    }

    /**
     * Google Sign-In Clientを取得（インタラクティブサインイン用）
     */
    fun getSignInClient() = googleAuthService?.getSignInClient()

    // ========================================
    // ViewModelFactory
    // ========================================

    /**
     * ViewModelFactory - ViewModelのインスタンス生成
     *
     * PHPでいう「Dependency Injection (依存性注入)」に相当。
     * ViewModelに必要なRepository（データベース接続）と
     * Google認証サービスを渡す。
     */
    class Factory(
        private val repository: FoodRepository,
        private val googleAuthService: GoogleAuthService? = null,
        private val googleCalendarService: GoogleCalendarService? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FoodViewModel::class.java)) {
                return FoodViewModel(repository, googleAuthService, googleCalendarService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

