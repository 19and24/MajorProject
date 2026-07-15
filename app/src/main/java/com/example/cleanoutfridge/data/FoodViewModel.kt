package com.example.cleanoutfridge.data

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.addDays

private fun initialItems() = listOf(
    FoodItem(id = "1", name = "닭가슴살", category = "육류", expiryDate = addDays(-1), emoji = "🍗", addedDate = addDays(-4)),
    FoodItem(id = "2", name = "스트링 치즈", category = "유제품", expiryDate = addDays(0), emoji = "🧀", addedDate = addDays(-6)),
    FoodItem(id = "3", name = "계란", category = "계란", expiryDate = addDays(1), emoji = "🥚", addedDate = addDays(-5)),
    FoodItem(id = "4", name = "우유", category = "유제품", expiryDate = addDays(3), emoji = "🥛", addedDate = addDays(-3)),
    FoodItem(id = "5", name = "두부", category = "두부/콩류", expiryDate = addDays(5), emoji = "🫘", addedDate = addDays(-1)),
    FoodItem(id = "6", name = "샐러드", category = "채소", expiryDate = addDays(7), emoji = "🥗", addedDate = addDays(-1)),
    FoodItem(id = "7", name = "사과", category = "과일", expiryDate = addDays(9), emoji = "🍎", addedDate = addDays(-2)),
    FoodItem(id = "8", name = "당근", category = "채소", expiryDate = addDays(12), emoji = "🥕", addedDate = addDays(-2)),
    FoodItem(id = "9", name = "버터", category = "유제품", expiryDate = addDays(18), emoji = "🧈", addedDate = addDays(-8)),
    FoodItem(id = "10", name = "냉동 만두", category = "냉동식품", expiryDate = addDays(30), emoji = "🥟", addedDate = addDays(-10)),
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
