package com.example.refrigeratordatabase.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food
import com.example.refrigeratordatabase.data.local.relation.FoodWithCategory
import com.example.refrigeratordatabase.ui.theme.RefrigeratorDatabaseTheme

/**
 * EditFoodDialog - 残量更新ダイアログ
 *
 * PHPでいう「編集フォーム (edit_form.php)」に相当。
 * モーダルダイアログで食材の残量をスライダーで更新、または削除する。
 *
 * Figma node: 137-2358
 */
@Composable
fun EditFoodDialog(
    foodWithCategory: FoodWithCategory,
    remainingPercentage: Int,
    onRemainingPercentageChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    // Figmaデザインの色定義
    val primaryDark = Color(0xFF030213)
    val redButton = Color(0xFFD4183D)
    val grayText = Color(0xFF717182)
    val grayTrack = Color(0xFFECECF0)

    val food = foodWithCategory.food

    // 削除確認ダイアログの表示状態
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                // ヘッダー（閉じるボタン）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
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

                // タイトル
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${food.name}の残り状況",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "残りの割合を選択してください",
                        fontSize = 14.sp,
                        color = grayText,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 残量ラベルと値
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "残量",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "${remainingPercentage}%",
                        fontSize = 16.sp,
                        color = grayText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // スライダー
                Slider(
                    value = remainingPercentage / 100f,
                    onValueChange = { onRemainingPercentageChange((it * 100).toInt()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = primaryDark,
                        inactiveTrackColor = grayTrack
                    )
                )

                // スライダーのラベル
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0%",
                        fontSize = 16.sp,
                        color = grayText
                    )
                    Text(
                        text = "50%",
                        fontSize = 16.sp,
                        color = grayText
                    )
                    Text(
                        text = "100%",
                        fontSize = 16.sp,
                        color = grayText
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ボタン（Figma node: 137-2432）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 削除ボタン
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = redButton)
                    ) {
                        Text(
                            text = "削除",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    // キャンセルボタン
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "キャンセル",
                            fontSize = 14.sp
                        )
                    }

                    // 更新ボタン
                    Button(
                        onClick = {
                            if (remainingPercentage == 0) {
                                showDeleteConfirmDialog = true
                            } else {
                                onUpdate()
                            }
                        },
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryDark)
                    ) {
                        Text(
                            text = "更新",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // 削除確認ダイアログ
    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            foodName = food.name,
            onConfirm = {
                showDeleteConfirmDialog = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

/**
 * DeleteConfirmDialog - 削除確認ダイアログ
 *
 * PHPでいう JavaScript の confirm("削除しますか？") に相当。
 */
@Composable
private fun DeleteConfirmDialog(
    foodName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val redButton = Color(0xFFD4183D)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "削除の確認",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "「${foodName}」を削除しますか？\nこの操作は取り消せません。"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = redButton)
            ) {
                Text(
                    text = "削除する",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "キャンセル")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun EditFoodDialogPreview() {
    val sampleFood = FoodWithCategory(
        food = Food(
            foodId = 1,
            categoryId = 3,
            name = "にんじん",
            expiryDate = System.currentTimeMillis() + 86400000 * 23,
            remainingPercentage = 100,
            createdAt = System.currentTimeMillis()
        ),
        category = Category(categoryId = 3, name = "肉類")
    )

    RefrigeratorDatabaseTheme {
        EditFoodDialog(
            foodWithCategory = sampleFood,
            remainingPercentage = 50,
            onRemainingPercentageChange = {},
            onDismiss = {},
            onUpdate = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeleteConfirmDialogPreview() {
    RefrigeratorDatabaseTheme {
        DeleteConfirmDialog(
            foodName = "にんじん",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
