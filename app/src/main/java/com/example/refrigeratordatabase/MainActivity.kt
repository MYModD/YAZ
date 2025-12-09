package com.example.refrigeratordatabase

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.refrigeratordatabase.data.local.AppDatabase
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // データベースとRepositoryの初期化
        // PHPでいう「DBへの接続とModelクラスのインスタンス化」に相当
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = FoodRepository(db.foodDao(), db.categoryDao())

        setContent {
            RefrigeratorDatabaseTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // NavControllerの作成（PHPのルーター状態管理に相当）
                    val navController = rememberNavController()

                    // ViewModelの作成（PHPのControllerに相当）
                    val viewModel: FoodViewModel = viewModel(
                        factory = FoodViewModel.Factory(repository)
                    )

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

                    // 編集ダイアログの状態
                    val editRemainingPercentage by viewModel.editRemainingPercentage.collectAsState()

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
                        onEditFoodClick = viewModel::showEditDialog
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
                            onConfirm = viewModel::addFood
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
}
