package com.example.refrigeratordatabase.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.refrigeratordatabase.data.local.dao.CategoryDao
import com.example.refrigeratordatabase.data.local.dao.FoodDao
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppDatabase - Roomデータベースのメインクラス
 *
 * PHPでいうMySQLの「データベース接続」に相当。
 * Room.databaseBuilder() = new mysqli() のような接続作成。
 */
@Database(entities = [Food::class, Category::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Figmaデザインに表示されている初期カテゴリ
         * PHPでいう「初期データINSERT」のシード処理
         */
        private val INITIAL_CATEGORIES = listOf(
            Category(categoryId = 1, name = "野菜"),
            Category(categoryId = 2, name = "果物"),
            Category(categoryId = 3, name = "肉類"),
            Category(categoryId = 4, name = "魚介類"),
            Category(categoryId = 5, name = "乳製品"),
            Category(categoryId = 6, name = "調味料"),
            Category(categoryId = 7, name = "飲料"),
            Category(categoryId = 8, name = "その他")
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "refrigerator_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * RoomDatabase.Callback - DB作成時に初期データを投入
         *
         * PHPでいう「マイグレーション後のシーダー実行」に相当。
         * onCreate() = DBが初めて作成されたときだけ実行される。
         */
        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // コルーチンで非同期実行（PHPでは同期だが、Androidではメインスレッドでの
                // DB操作はNGなので非同期で行う）
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        val categoryDao = database.categoryDao()
                        INITIAL_CATEGORIES.forEach { category ->
                            categoryDao.insertCategory(category)
                        }
                    }
                }
            }
        }
    }
}
