package com.example.refrigeratordatabase.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.refrigeratordatabase.data.local.entity.Category
import com.example.refrigeratordatabase.data.local.entity.Food

data class FoodWithCategory(
    @Embedded val food: Food,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id"
    )
    val category: Category
)
