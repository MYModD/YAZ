package com.example.refrigeratordatabase

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.refrigeratordatabase.data.auth.GoogleAuthService
import com.example.refrigeratordatabase.data.auth.GoogleSignInResult
import com.example.refrigeratordatabase.data.local.AppDatabase
import com.example.refrigeratordatabase.data.network.GoogleCalendarService
import com.example.refrigeratordatabase.data.repository.FoodRepository
import com.example.refrigeratordatabase.ui.components.AddFoodDialog
import com.example.refrigeratordatabase.ui.components.EditFoodDialog
import com.example.refrigeratordatabase.ui.navigation.AppNavigation
import com.example.refrigeratordatabase.ui.theme.RefrigeratorDatabaseTheme
import com.example.refrigeratordatabase.ui.viewmodel.FoodViewModel

/**
 * MainActivity - アプリのエントリーポイント
 *
 * PHPでいう「index.php」に相当。
 * すべてのリクエスト(画面遷移)はここを起点に処理される。
 */
class MainActivity : ComponentActivity() {
    
    // Google Calendar API連携サービス
    private lateinit var googleAuthService: GoogleAuthService
    private lateinit var googleCalendarService: GoogleCalendarService
    
    // ViewModelへの参照（サインイン結果を渡すため）
    private var foodViewModel: FoodViewModel? = null
    
    // Google Sign-In用のActivityResultLauncher
    // PHPでいう「OAuthコールバック処理」に相当
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("MainActivity", "Sign-in result received: ${result.resultCode}")
        val signInResult = googleAuthService.handleSignInResult(result.data)
        foodViewModel?.handleGoogleSignInResult(signInResult)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // データベースとRepositoryの初期化
        // PHPでいう「DBへの接続とModelクラスのインスタンス化」に相当
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = FoodRepository(db.foodDao(), db.categoryDao())

        // Google Calendar API連携サービスの初期化
        // PHPでいう「OAuth用のクライアント初期化」に相当
        googleAuthService = GoogleAuthService(applicationContext)
        googleCalendarService = GoogleCalendarService(googleAuthService, applicationContext)

        setContent {
            RefrigeratorDatabaseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // NavControllerの作成（PHPのルーター状態管理に相当）
                    val navController = rememberNavController()

                    // ViewModelの作成（PHPのControllerに相当）
                    // Google Calendar連携サービスも渡す
                    val viewModel: FoodViewModel = viewModel(
                        factory = FoodViewModel.Factory(
                            repository = repository,
                            googleAuthService = googleAuthService,
                            googleCalendarService = googleCalendarService
                        )
                    )
                    
                    // ViewModelへの参照を保持（サインイン結果を渡すため）
                    foodViewModel = viewModel

                    // アプリ起動時にカテゴリが空なら初期データを投入
                    // PHPでいう「IF NOT EXISTS ... INSERT」パターン
                    LaunchedEffect(Unit) {
                        Log.d("MainActivity", "=== Checking initial categories ===")
                        repository.ensureInitialCategories()
                        Log.d("MainActivity", "=== Initial categories check done ===")
                    }

                    // ViewModelからUI状態を取得
                    val foods by viewModel.filteredFoods.collectAsState()
                    val categories by viewModel.categories.collectAsState()
                    val searchQuery by viewModel.searchQuery.collectAsState()

                    // デバッグ: カテゴリの数をログ出力
                    LaunchedEffect(categories) {
                        Log.d("MainActivity", "Categories count: ${categories.size}")
                        categories.forEach { cat ->
                            Log.d("MainActivity", "  - ${cat.categoryId}: ${cat.name}")
                        }
                    }
                    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
                    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
                    val showAddDialog by viewModel.showAddDialog.collectAsState()
                    val editingFood by viewModel.editingFood.collectAsState()

                    // 追加ダイアログの状態
                    val addFoodName by viewModel.addFoodName.collectAsState()
                    val addCategoryId by viewModel.addCategoryId.collectAsState()
                    val addExpiryDate by viewModel.addExpiryDate.collectAsState()
                    val isAddingFood by viewModel.isAddingFood.collectAsState()

                    // 編集ダイアログの状態
                    val editRemainingPercentage by viewModel.editRemainingPercentage.collectAsState()

                    // Google Calendar 連携の状態
                    val googleEventDates by viewModel.googleEventDates.collectAsState()

                    // ナビゲーション
                    AppNavigation(
                        navController = navController,
                        foods = foods,
                        categories = categories,
                        searchQuery = searchQuery,
                        selectedCategoryId = selectedCategoryId,
                        selectedTabIndex = selectedTabIndex,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onCategorySelect = viewModel::onCategorySelect,
                        onTabSelect = viewModel::onTabSelect,
                        onAddFoodClick = viewModel::showAddDialog,
                        onEditFoodClick = viewModel::showEditDialog,
                        // Google Calendar 連携（初回起動時に自動連携）
                        googleEventDates = googleEventDates,
                        onMonthChange = viewModel::fetchGoogleEventsForMonth,
                        // スタートボタン押下時にGoogle連携を開始
                        onStartWithGoogleConnect = {
                            launchGoogleSignIn()
                        }
                    )

                    // 追加ダイアログ
                    if (showAddDialog) {
                        AddFoodDialog(
                            categories = categories,
                            foodName = addFoodName,
                            selectedCategoryId = addCategoryId,
                            expiryDate = addExpiryDate,
                            onFoodNameChange = viewModel::onAddFoodNameChange,
                            onCategorySelect = viewModel::onAddCategorySelect,
                            onExpiryDateSelect = viewModel::onAddExpiryDateSelect,
                            onDismiss = viewModel::hideAddDialog,
                            onConfirm = viewModel::addFood,
                            isAdding = isAddingFood  // 連打防止用フラグ
                        )
                    }

                    // 編集ダイアログ
                    editingFood?.let { food ->
                        EditFoodDialog(
                            foodWithCategory = food,
                            remainingPercentage = editRemainingPercentage,
                            onRemainingPercentageChange = viewModel::onEditRemainingPercentageChange,
                            onDismiss = viewModel::hideEditDialog,
                            onUpdate = viewModel::updateFood,
                            onDelete = viewModel::deleteFood
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Googleサインインを開始
     * PHPでいう: header("Location: " . $authUrl)
     * 
     * すでに連携済みの場合はスキップし、未連携の場合のみサインイン画面を表示
     */
    private fun launchGoogleSignIn() {
        // すでに連携済みならスキップ
        if (googleAuthService.isSignedIn()) {
            Log.d("MainActivity", "Already signed in, skipping Google Sign-In")
            return
        }
        
        Log.d("MainActivity", "Launching Google Sign-In...")
        foodViewModel?.setGoogleConnecting(true)
        val signInIntent = googleAuthService.getSignInIntent()
        signInLauncher.launch(signInIntent)
    }
}
