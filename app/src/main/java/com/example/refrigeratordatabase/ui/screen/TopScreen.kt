package com.example.refrigeratordatabase.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.refrigeratordatabase.R
import com.example.refrigeratordatabase.ui.theme.RefrigeratorDatabaseTheme

/**
 * TopScreen - スタート画面
 *
 * PHPでいう「トップページ (index.php)」に相当。
 * ユーザーが最初に見る画面で、「スタート」ボタンを押すとリスト画面へ遷移する。
 *
 * Figma node: 134-2046
 */
@Composable
fun TopScreen(
    onStartClick: () -> Unit
) {
    // Figmaデザインの色定義
    val primaryDark = Color(0xFF1A2B2E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 冷蔵庫アイコン（カスタムPNG画像）
            Image(
                painter = painterResource(id = R.drawable.refrigerator),
                contentDescription = "冷蔵庫アイコン",
                modifier = Modifier.size(120.dp),
                colorFilter = ColorFilter.tint(primaryDark)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // アプリタイトル
            Text(
                text = "冷蔵庫データベース",
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(48.dp))

            // スタートボタン
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryDark
                )
            ) {
                Text(
                    text = "スタート",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TopScreenPreview() {
    RefrigeratorDatabaseTheme {
        TopScreen(onStartClick = {})
    }
}
