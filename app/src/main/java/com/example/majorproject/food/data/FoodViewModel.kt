package com.example.majorproject.food.data

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.majorproject.food.model.FoodItem
import com.example.majorproject.food.model.addDays

private fun initialItems() = listOf(
    FoodItem(id = "1", name = "우유", category = "유제품", expiryDate = addDays(1), emoji = "🥛", addedDate = addDays(-3)),
    FoodItem(id = "2", name = "계란", category = "계란", expiryDate = addDays(2), emoji = "🥚", addedDate = addDays(-5)),
    FoodItem(id = "3", name = "닭가슴살", category = "육류", expiryDate = addDays(-1), emoji = "🥩", addedDate = addDays(-4)),
    FoodItem(id = "4", name = "두부", category = "두부/콩류", expiryDate = addDays(4), emoji = "🫘", addedDate = addDays(-1)),
    FoodItem(id = "5", name = "시금치", category = "채소", expiryDate = addDays(3), emoji = "🥬", addedDate = addDays(-2)),
    FoodItem(id = "6", name = "사과", category = "과일", expiryDate = addDays(6), emoji = "🍎", addedDate = addDays(-1)),
    FoodItem(id = "7", name = "냉동만두", category = "냉동식품", expiryDate = addDays(45), emoji = "🧊", addedDate = addDays(-7)),
    FoodItem(id = "8", name = "김치", category = "기타", expiryDate = addDays(25), emoji = "🥬", addedDate = addDays(-10)),
    FoodItem(id = "9", name = "된장", category = "조미료", expiryDate = addDays(90), emoji = "🫙", addedDate = addDays(-20)),
    FoodItem(id = "10", name = "스트링 치즈", category = "유제품", expiryDate = addDays(0), emoji = "🧀", addedDate = addDays(-6)),
)

class FoodViewModel : ViewModel() {
    val items = mutableStateListOf<FoodItem>().apply { addAll(initialItems()) }

    fun addItem(item: FoodItem) {
        items.add(0, item)
    }

    fun deleteItem(id: String) {
        items.removeAll { it.id == id }
    }
}
