package com.example.refrigeratordatabase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.refrigeratordatabase.data.local.AppDatabase
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food
import com.example.refrigeratordatabase.ui.theme.RefrigeratorDatabaseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- ここから動作確認用のコード ---
        val db = AppDatabase.getDatabase(applicationContext)
        val categoryDao = db.categoryDao()
        val foodDao = db.foodDao()

        GlobalScope.launch(Dispatchers.IO) {
            // 1. カテゴリを追加
            val category = Category(name = "野菜")
            categoryDao.insertCategory(category)

            // 2. 食材を追加
            val food = Food(
                categoryId = 1, // 先ほど追加したカテゴリのID
                name = "トマト",
                expiryDate = System.currentTimeMillis(), // 今の日時
                remainingPercentage = 100,
                createdAt = System.currentTimeMillis()
            )
            foodDao.insertFood(food)
        }
        // --- ここまで ---


        setContent {
            RefrigeratorDatabaseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RefrigeratorDatabaseTheme {
        Greeting("Android")
    }
}