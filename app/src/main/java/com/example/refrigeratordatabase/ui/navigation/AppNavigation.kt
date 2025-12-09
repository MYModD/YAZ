package com.example.refrigeratordatabase.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.refrigeratordatabase.ui.screen.TopScreen
import com.example.refrigeratordatabase.ui.screen.FoodListScreen
import com.example.refrigeratordatabase.data.local.relation.FoodWithCategory
import com.example.refrigeratordatabase.data.local.entity.Category

/**
 * Screen - 画面のルート定義
 *
 * PHPでいう「URLルーティング定義」に相当。
 * switch($_GET['page']) { case 'top': ... case 'list': ... } のような分岐に近い。
 */
sealed class Screen(val route: String) {
    object Top : Screen("top")
    object FoodList : Screen("foodList")
}

/**
 * AppNavigation - アプリ全体のナビゲーション定義
 *
 * PHPでいう「index.php のルーター部分」に相当。
 * NavHost = PHPの switch($_GET['page']) による画面振り分け。
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    foods: List<FoodWithCategory>,
    categories: List<Category>,
    searchQuery: String,
    selectedCategoryId: Int?,
    selectedTabIndex: Int,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (Int?) -> Unit,
    onTabSelect: (Int) -> Unit,
    onAddFoodClick: () -> Unit,
    onEditFoodClick: (FoodWithCategory) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Top.route
    ) {
        // トップ画面（スタート画面）
        composable(Screen.Top.route) {
            TopScreen(
                onStartClick = {
                    navController.navigate(Screen.FoodList.route) {
                        // トップ画面をバックスタックから削除（戻るボタンで戻らないように）
                        popUpTo(Screen.Top.route) { inclusive = true }
                    }
                }
            )
        }

        // 食材リスト画面（メイン画面）
        composable(Screen.FoodList.route) {
            FoodListScreen(
                foods = foods,
                categories = categories,
                searchQuery = searchQuery,
                selectedCategoryId = selectedCategoryId,
                selectedTabIndex = selectedTabIndex,
                onSearchQueryChange = onSearchQueryChange,
                onCategorySelect = onCategorySelect,
                onTabSelect = onTabSelect,
                onAddFoodClick = onAddFoodClick,
                onEditFoodClick = onEditFoodClick
            )
        }
    }
}
