package com.example.refrigeratordatabase.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "foods",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Food(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "food_id")
    val foodId: Int = 0,

    @ColumnInfo(name = "category_id", index = true)
    val categoryId: Int,

    val name: String,

    @ColumnInfo(name = "expiry_date")
    val expiryDate: Long,

    @ColumnInfo(name = "remaining_percentage")
    val remainingPercentage: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
