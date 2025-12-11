package com.example.refrigeratordatabase.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food
import com.example.refrigeratordatabase.data.local.relation.FoodWithCategory
import com.example.refrigeratordatabase.ui.theme.RefrigeratorDatabaseTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * CalendarScreen - カレンダー表示画面
 *
 * PHPでいう「カレンダーページ (calendar.php)」に相当。
 * 月カレンダーを表示し、期限日が近い食材をマーカーで表示する。
 * 日付をタップすると、その日が期限の食材リストを表示する。
 * Googleカレンダーの予定も統合して表示できる。
 *
 * Figma node: 122-954
 */
@Composable
fun CalendarScreen(
    foods: List<FoodWithCategory>,
    selectedDate: Long?,
    onDateSelect: (Long) -> Unit,
    onAddFoodClick: () -> Unit,
    onFoodClick: (FoodWithCategory) -> Unit,
    selectedTabIndex: Int = 1,
    onTabSelect: (Int) -> Unit = {},
    // Google Calendar 連携用パラメータ（初回起動時に自動連携）
    googleEventDates: Set<Long> = emptySet(),
    onMonthChange: (year: Int, month: Int) -> Unit = { _, _ -> }
) {
    // Figmaデザインの色定義
    val primaryDark = Color(0xFF1A2B2E)
    val grayText = Color(0xFF717182)
    val selectedBackground = Color(0xFFE9EBEF)
    val darkText = Color(0xFF030213)
    val tabBackground = Color(0xFFECECF0)

    // カレンダーの状態
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var internalSelectedDate by remember { mutableLongStateOf(selectedDate ?: System.currentTimeMillis()) }

    // 月が変更されたらコールバックを呼び出し（Googleカレンダーのイベント取得用）
    LaunchedEffect(currentMonth) {
        val year = currentMonth.get(Calendar.YEAR)
        val month = currentMonth.get(Calendar.MONTH) + 1
        onMonthChange(year, month)
    }

    // 選択日の食材をフィルタリング
    val foodsOnSelectedDate = run {
        val selectedCal = Calendar.getInstance().apply { timeInMillis = internalSelectedDate }
        foods.filter { foodWithCategory ->
            val expiryCal = Calendar.getInstance().apply {
                timeInMillis = foodWithCategory.food.expiryDate
            }
            selectedCal.get(Calendar.YEAR) == expiryCal.get(Calendar.YEAR) &&
                    selectedCal.get(Calendar.MONTH) == expiryCal.get(Calendar.MONTH) &&
                    selectedCal.get(Calendar.DAY_OF_MONTH) == expiryCal.get(Calendar.DAY_OF_MONTH)
        }
    }

    // 期限がある日のセット（ドットマーカー表示用）
    val datesWithExpiry = foods.map { it.food.expiryDate }
        .map { expiryDate ->
            Calendar.getInstance().apply {
                timeInMillis = expiryDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSet()

    // カレンダーに表示するドットマーカーは食材の期限日のみ
    // ※ Googleカレンダーの予定は表示しない（ユーザー要望）
    val allDatesWithMarker = datesWithExpiry

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

        Spacer(modifier = Modifier.height(12.dp))

        // タブ
        FoodListTabs(
            selectedTabIndex = selectedTabIndex,
            onTabSelect = onTabSelect,
            tabBackground = tabBackground,
            darkText = darkText
        )

        Spacer(modifier = Modifier.height(32.dp))

        // カレンダーカード
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 月ナビゲーション
                CalendarHeader(
                    currentMonth = currentMonth,
                    onPreviousMonth = {
                        currentMonth = (currentMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    },
                    onNextMonth = {
                        currentMonth = (currentMonth.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    },
                    grayText = grayText
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 曜日ヘッダー
                WeekdayHeader(grayText = grayText)

                Spacer(modifier = Modifier.height(8.dp))

                // カレンダーグリッド（食材期限日 + Googleカレンダー予定日を表示）
                CalendarGrid(
                    currentMonth = currentMonth,
                    selectedDate = internalSelectedDate,
                    datesWithExpiry = allDatesWithMarker,
                    onDateSelect = { date ->
                        internalSelectedDate = date
                        onDateSelect(date)
                    },
                    grayText = grayText,
                    selectedBackground = selectedBackground,
                    darkText = darkText
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 選択日の食材リスト
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
        val selectedDateText = dateFormat.format(Date(internalSelectedDate))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = selectedDateText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (foodsOnSelectedDate.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "この日に期限を迎える食材はありません",
                            fontSize = 14.sp,
                            color = grayText
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(foodsOnSelectedDate) { foodWithCategory ->
                            FoodExpiryItem(
                                foodWithCategory = foodWithCategory,
                                onClick = { onFoodClick(foodWithCategory) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CalendarHeader - カレンダーのヘッダー（月ナビゲーション）
 */
@Composable
private fun CalendarHeader(
    currentMonth: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    grayText: Color
) {
    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "前月",
                tint = grayText.copy(alpha = 0.5f)
            )
        }

        Text(
            text = dateFormat.format(currentMonth.time),
            fontSize = 14.sp,
            color = Color.Black
        )

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "翌月",
                tint = grayText.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * WeekdayHeader - 曜日ヘッダー
 */
@Composable
private fun WeekdayHeader(grayText: Color) {
    val weekdays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        weekdays.forEach { day ->
            Text(
                text = day,
                fontSize = 12.sp,
                color = grayText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * CalendarGrid - カレンダーの日付グリッド
 */
@Composable
private fun CalendarGrid(
    currentMonth: Calendar,
    selectedDate: Long?,
    datesWithExpiry: Set<Long>,
    onDateSelect: (Long) -> Unit,
    grayText: Color,
    selectedBackground: Color,
    darkText: Color
) {
    val days = getMonthDays(currentMonth)
    val today = Calendar.getInstance()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.height(260.dp),
        userScrollEnabled = false
    ) {
        items(days) { day ->
            val isCurrentMonth = day != null &&
                    day.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)

            val dayMillis = day?.let {
                Calendar.getInstance().apply {
                    timeInMillis = it.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }

            val isSelected = selectedDate?.let { selected ->
                val selectedCal = Calendar.getInstance().apply {
                    timeInMillis = selected
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                day?.let {
                    it.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                            it.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                            it.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH)
                } ?: false
            } ?: false

            val hasExpiry = dayMillis?.let { datesWithExpiry.contains(it) } ?: false

            val isToday = day?.let {
                it.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        it.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        it.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
            } ?: false

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) selectedBackground else Color.Transparent)
                    .clickable(enabled = day != null) {
                        dayMillis?.let { onDateSelect(it) }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (day != null) {
                        Text(
                            text = day.get(Calendar.DAY_OF_MONTH).toString(),
                            fontSize = 14.sp,
                            fontWeight = if (hasExpiry) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                !isCurrentMonth -> grayText
                                isToday -> darkText
                                else -> Color.Black
                            }
                        )

                        // 期限ドットマーカー
                        if (hasExpiry && isCurrentMonth) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD4183D))
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * FoodExpiryItem - 選択日の食材アイテム
 */
@Composable
private fun FoodExpiryItem(
    foodWithCategory: FoodWithCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${foodWithCategory.food.name}期限切れ",
                fontSize = 16.sp,
                color = Color.Black
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = foodWithCategory.category.name,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * 指定月の日付リストを取得（前月・翌月の日付も含む）
 */
private fun getMonthDays(month: Calendar): List<Calendar?> {
    val days = mutableListOf<Calendar?>()

    val firstDayOfMonth = (month.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    // 月の最初の曜日（0 = 日曜日）
    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1

    // 前月の日付を追加
    val prevMonth = (month.clone() as Calendar).apply {
        add(Calendar.MONTH, -1)
    }
    val prevMonthLastDay = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

    for (i in firstDayOfWeek - 1 downTo 0) {
        days.add((prevMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, prevMonthLastDay - i)
        })
    }

    // 今月の日付を追加
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (day in 1..daysInMonth) {
        days.add((month.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, day)
        })
    }

    // 翌月の日付を追加（6行 x 7列 = 42日分になるように）
    val nextMonth = (month.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
    }
    var nextDay = 1
    while (days.size < 42) {
        days.add((nextMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, nextDay++)
        })
    }

    return days
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenPreview() {
    val sampleCategories = listOf(
        Category(categoryId = 1, name = "野菜"),
        Category(categoryId = 2, name = "乳製品")
    )

    val sampleFoods = listOf(
        FoodWithCategory(
            food = Food(
                foodId = 1,
                categoryId = 1,
                name = "レタス",
                expiryDate = System.currentTimeMillis() + 86400000 * 3,
                remainingPercentage = 100,
                createdAt = System.currentTimeMillis()
            ),
            category = sampleCategories[0]
        )
    )

    RefrigeratorDatabaseTheme {
        CalendarScreen(
            foods = sampleFoods,
            selectedDate = System.currentTimeMillis() + 86400000 * 3,
            onDateSelect = {},
            onAddFoodClick = {},
            onFoodClick = {},
            selectedTabIndex = 1,
            onTabSelect = {}
        )
    }
}
