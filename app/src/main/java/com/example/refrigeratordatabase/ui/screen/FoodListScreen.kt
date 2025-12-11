package com.example.refrigeratordatabase.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food
import com.example.refrigeratordatabase.data.local.relation.FoodWithCategory
import com.example.refrigeratordatabase.ui.theme.RefrigeratorDatabaseTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * FoodListScreen - メインリスト画面
 *
 * PHPでいう「一覧ページ (list.php)」に相当。
 * 食材のリスト表示、検索、カテゴリ絞り込みを行う。
 * タブで「リスト表示」と「カレンダー表示」を切り替える。
 *
 * Figma node: 133-1329
 */
@Composable
fun FoodListScreen(
    foods: List<FoodWithCategory>,
    categories: List<Category>,
    searchQuery: String,
    selectedCategoryId: Int?,
    selectedTabIndex: Int,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (Int?) -> Unit,
    onTabSelect: (Int) -> Unit,
    onAddFoodClick: () -> Unit,
    onEditFoodClick: (FoodWithCategory) -> Unit,
    // Google Calendar 連携用パラメータ（初回起動時に自動連携）
    googleEventDates: Set<Long> = emptySet(),
    onMonthChange: (year: Int, month: Int) -> Unit = { _, _ -> }
) {
    // Figmaデザインの色定義
    val primaryDark = Color(0xFF1A2B2E)
    val grayBackground = Color(0xFFF3F3F5)
    val tabBackground = Color(0xFFECECF0)
    val grayText = Color(0xFF717182)
    val redBadge = Color(0xFFD4183D)
    val darkText = Color(0xFF030213)

    // フィルタリング処理（PHPの WHERE句に相当）
    val filteredFoods = foods.filter { foodWithCategory ->
        val matchesSearch = searchQuery.isEmpty() ||
                foodWithCategory.food.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategoryId == null ||
                foodWithCategory.food.categoryId == selectedCategoryId
        matchesSearch && matchesCategory
    }

    // リスト表示 or カレンダー表示
    when (selectedTabIndex) {
        0 -> {
            // リスト表示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .statusBarsPadding()  // ステータスバーとの重なりを防ぐ
                    .padding(16.dp)
            ) {
                // 「+食材を追加」ボタン
                Button(
                    onClick = onAddFoodClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "食材を追加",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 検索・フィルターカード
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 検索バー
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = "食材名で検索...",
                                    color = grayText,
                                    fontSize = 16.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "検索",
                                    tint = grayText,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = grayBackground,
                                unfocusedContainerColor = grayBackground,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // カテゴリドロップダウン
                        CategoryDropdown(
                            categories = categories,
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelect = onCategorySelect,
                            grayBackground = grayBackground,
                            grayText = grayText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // タブ
                FoodListTabs(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelect = onTabSelect,
                    tabBackground = tabBackground,
                    darkText = darkText
                )

                Spacer(modifier = Modifier.height(32.dp))

                // リスト表示
                if (filteredFoods.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "食材が登録されていません",
                            color = grayText,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFoods) { foodWithCategory ->
                            FoodCard(
                                foodWithCategory = foodWithCategory,
                                onEditClick = { onEditFoodClick(foodWithCategory) },
                                grayText = grayText,
                                redBadge = redBadge,
                                darkText = darkText
                            )
                        }
                    }
                }
            }
        }
        1 -> {
            // カレンダー表示
            CalendarScreen(
                foods = foods,
                selectedDate = null,
                onDateSelect = {},
                onAddFoodClick = onAddFoodClick,
                onFoodClick = onEditFoodClick,
                selectedTabIndex = selectedTabIndex,
                onTabSelect = onTabSelect,
                // Google Calendar 連携用パラメータ（初回起動時に自動連携）
                googleEventDates = googleEventDates,
                onMonthChange = onMonthChange
            )
        }
    }
}

/**
 * FoodListTabs - タブコンポーネント
 *
 * Figmaデザイン (node: 122-969) に基づいたカスタムタブ
 * 選択されたタブは白背景、非選択は透明背景
 */
@Composable
fun FoodListTabs(
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    tabBackground: Color,
    darkText: Color
) {
    // Figmaのタブリスト背景色: #ECECF0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tabBackground)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // リスト表示タブ
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selectedTabIndex == 0) Color.White else Color.Transparent)
                .clickable { onTabSelect(0) }
                .padding(vertical = 5.dp, horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "リスト表示",
                fontSize = 14.sp,
                color = darkText
            )
        }

        // カレンダー表示タブ
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selectedTabIndex == 1) Color.White else Color.Transparent)
                .clickable { onTabSelect(1) }
                .padding(vertical = 5.dp, horizontal = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "カレンダー表示",
                fontSize = 14.sp,
                color = darkText
            )
        }
    }
}

/**
 * CategoryDropdown - カテゴリ選択ドロップダウン
 */
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelect: (Int?) -> Unit,
    grayBackground: Color,
    grayText: Color
) {
    var expanded by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.categoryId == selectedCategoryId }
    val displayText = selectedCategory?.name ?: "全て"

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(grayBackground)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                fontSize = 14.sp,
                color = Color.Black
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "展開",
                modifier = Modifier.size(16.dp),
                tint = grayText
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 「全て」オプション
            DropdownMenuItem(
                text = { Text("全て") },
                onClick = {
                    onCategorySelect(null)
                    expanded = false
                }
            )
            // カテゴリリスト
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelect(category.categoryId)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * FoodCard - 食材カード
 *
 * PHPでいう「一覧の1行分 (foreach の中身)」に相当。
 */
@Composable
private fun FoodCard(
    foodWithCategory: FoodWithCategory,
    onEditClick: () -> Unit,
    grayText: Color,
    redBadge: Color,
    darkText: Color
) {
    val food = foodWithCategory.food
    val category = foodWithCategory.category

    // 期限までの日数を計算
    val daysUntilExpiry = calculateDaysUntilExpiry(food.expiryDate)
    val expiryBadgeText = when {
        daysUntilExpiry < 0 -> "期限切れ"
        daysUntilExpiry == 0L -> "今日まで"
        daysUntilExpiry <= 3 -> "残り${daysUntilExpiry}日"
        else -> "残り${daysUntilExpiry}日"
    }
    val isUrgent = daysUntilExpiry <= 3

    // 日付フォーマット
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
    val expiryDateText = dateFormat.format(Date(food.expiryDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 食材名とバッジ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = food.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = darkText
                    )

                    // カテゴリバッジ
                    Badge(
                        text = category.name,
                        backgroundColor = Color.Transparent,
                        textColor = darkText,
                        hasBorder = true
                    )

                    // 期限バッジ
                    Badge(
                        text = expiryBadgeText,
                        backgroundColor = if (isUrgent) redBadge else Color(0xFFECEEF2),
                        textColor = if (isUrgent) Color.White else darkText,
                        hasBorder = false
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 期限日と残量
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "期限: $expiryDateText",
                        fontSize = 16.sp,
                        color = grayText
                    )
                    Text(
                        text = "残り:${food.remainingPercentage}%",
                        fontSize = 16.sp,
                        color = grayText
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // プログレスバー（Figmaデザインに合わせてシンプルなバーに）
                LinearProgressIndicator(
                    progress = { food.remainingPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = darkText,
                    trackColor = Color(0x33030213),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
            }

            // 編集ボタン
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "編集",
                    modifier = Modifier.size(16.dp),
                    tint = grayText
                )
            }
        }
    }
}

/**
 * Badge - バッジコンポーネント
 */
@Composable
private fun Badge(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    hasBorder: Boolean
) {
    val borderModifier = if (hasBorder) {
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(1.dp)
    } else {
        Modifier
    }

    Box(
        modifier = borderModifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(
                if (hasBorder) Modifier.background(Color.Transparent) else Modifier
            )
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

/**
 * 期限までの日数を計算
 */
private fun calculateDaysUntilExpiry(expiryDate: Long): Long {
    val now = System.currentTimeMillis()
    val diff = expiryDate - now
    return TimeUnit.MILLISECONDS.toDays(diff)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun FoodListScreenPreview() {
    val sampleCategories = listOf(
        Category(categoryId = 1, name = "野菜"),
        Category(categoryId = 2, name = "乳製品"),
        Category(categoryId = 3, name = "肉類")
    )

    val sampleFoods = listOf(
        FoodWithCategory(
            food = Food(
                foodId = 1,
                categoryId = 2,
                name = "牛乳",
                expiryDate = System.currentTimeMillis() - 86400000, // 1日前
                remainingPercentage = 50,
                createdAt = System.currentTimeMillis()
            ),
            category = sampleCategories[1]
        ),
        FoodWithCategory(
            food = Food(
                foodId = 2,
                categoryId = 1,
                name = "レタス",
                expiryDate = System.currentTimeMillis() + 259200000, // 3日後
                remainingPercentage = 100,
                createdAt = System.currentTimeMillis()
            ),
            category = sampleCategories[0]
        )
    )

    RefrigeratorDatabaseTheme {
        FoodListScreen(
            foods = sampleFoods,
            categories = sampleCategories,
            searchQuery = "",
            selectedCategoryId = null,
            selectedTabIndex = 0,
            onSearchQueryChange = {},
            onCategorySelect = {},
            onTabSelect = {},
            onAddFoodClick = {},
            onEditFoodClick = {}
        )
    }
}
