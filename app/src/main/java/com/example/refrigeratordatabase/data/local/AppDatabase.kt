package com.example.refrigeratordatabase.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.refrigeratordatabase.data.local.dao.CategoryDao
import com.example.refrigeratordatabase.data.local.dao.FoodDao
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food

/**
 * AppDatabase - Roomデータベースのメインクラス
 *
 * PHPでいうMySQLの「データベース接続」に相当。
 * Room.databaseBuilder() = new mysqli() のような接続作成。
 */
@Database(entities = [Food::class, Category::class], version = 4, exportSchema = false)
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
            "野菜",
            "果物",
            "肉類",
            "魚介類",
            "乳製品",
            "調味料",
            "飲料",
            "その他"
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "refrigerator_database"
                )
                    .fallbackToDestructiveMigration() // バージョンアップ時にDBを再作成
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
         *
         * 注意: この時点ではINSTANCEがnullなので、直接SQLを実行する
         */
        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d("AppDatabase", "=== onCreate called! Inserting initial categories ===")
                // 直接SQLでカテゴリを挿入（PHPの mysqli_query() と同じ）
                // INSTANCEがまだnullなので、SupportSQLiteDatabaseを直接使用
                INITIAL_CATEGORIES.forEachIndexed { index, categoryName ->
                    Log.d("AppDatabase", "Inserting category: $categoryName")
                    db.execSQL(
                        "INSERT INTO categories (category_id, name) VALUES (${index + 1}, '$categoryName')"
                    )
                }
                Log.d("AppDatabase", "=== All categories inserted! ===")
            }
        }
    }
}
