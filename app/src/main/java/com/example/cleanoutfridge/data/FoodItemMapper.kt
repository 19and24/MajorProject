package com.example.cleanoutfridge.data

import com.example.cleanoutfridge.data.remote.dto.FridgeItemDto
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.categoryById

/** deadline/createdAt are already yyyy-MM-dd (deadline) or ISO instant (createdAt) per §5; fall back to today for missing ones. */
fun FridgeItemDto.toFoodItem(): FoodItem {
    val category = ingredient.category ?: "기타"
    return FoodItem(
        id = id.toString(),
        ingredientId = ingredient.id,
        name = ingredient.name,
        category = category,
        expiryDate = deadline ?: com.example.cleanoutfridge.model.today(),
        emoji = ingredient.emoji ?: categoryById(category)?.emoji ?: "🍱",
        addedDate = createdAt?.take(10) ?: com.example.cleanoutfridge.model.today(),
        quantity = quantity,
        unit = unit,
        starred = starred,
    )
}
