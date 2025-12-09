package com.example.refrigeratordatabase.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.ui.theme.RefrigeratorDatabaseTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AddFoodDialog - 食材追加ダイアログ
 *
 * PHPでいう「追加フォーム (add_form.php)」に相当。
 * モーダルダイアログで食材名、ジャンル、期限を入力する。
 *
 * Figma node: 145-180
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodDialog(
    categories: List<Category>,
    foodName: String,
    selectedCategoryId: Int?,
    expiryDate: Long?,
    onFoodNameChange: (String) -> Unit,
    onCategorySelect: (Int) -> Unit,
    onExpiryDateSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // Figmaデザインの色定義
    val primaryDark = Color(0xFF030213)
    val grayBackground = Color(0xFFF3F3F5)
    val grayText = Color(0xFF717182)

    var showDatePicker by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.categoryId == selectedCategoryId }
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // ヘッダー
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "食材を追加",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = grayText.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 食材名入力
                Text(
                    text = "食材名",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = foodName,
                    onValueChange = onFoodNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "例: トマト",
                            color = grayText,
                            fontSize = 16.sp
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

                Spacer(modifier = Modifier.height(16.dp))

                // ジャンル選択
                Text(
                    text = "ジャンル",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(grayBackground)
                            .clickable { categoryExpanded = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory?.name ?: "選択してください",
                            fontSize = 16.sp,
                            color = if (selectedCategory != null) Color.Black else grayText
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "展開",
                            modifier = Modifier.size(16.dp),
                            tint = grayText
                        )
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onCategorySelect(category.categoryId)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 期限入力
                Text(
                    text = "賞味・消費期限",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(grayBackground)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expiryDate?.let { dateFormat.format(Date(it)) } ?: "日付を選択",
                        fontSize = 16.sp,
                        color = if (expiryDate != null) Color.Black else grayText
                    )
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "カレンダー",
                        modifier = Modifier.size(16.dp),
                        tint = grayText
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "キャンセル",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryDark),
                        enabled = foodName.isNotBlank() && selectedCategoryId != null && expiryDate != null
                    ) {
                        Text(
                            text = "追加",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiryDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onExpiryDateSelect(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddFoodDialogPreview() {
    val sampleCategories = listOf(
        Category(categoryId = 1, name = "野菜"),
        Category(categoryId = 2, name = "乳製品"),
        Category(categoryId = 3, name = "肉類")
    )

    RefrigeratorDatabaseTheme {
        AddFoodDialog(
            categories = sampleCategories,
            foodName = "",
            selectedCategoryId = null,
            expiryDate = null,
            onFoodNameChange = {},
            onCategorySelect = {},
            onExpiryDateSelect = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}
