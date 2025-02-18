package com.serhio.homeaccountingapp

import android.content.Context

object StandardCategories {
    fun getStandardExpenseCategories(): List<String> {
        return listOf(
            "Аренда",
            "Комунальні послуги",
            "Транспорт",
            "Розваги",
            "Бакалія",
            "Одяг",
            "Здоров'я",
            "Освіта",
            "Подарунки",
            "Хоббі",
            "Благпппподійність",
            "Спорт",
            "Електроніка"
        )
    }

    fun getStandardIncomeCategories(): List<String> {
        return listOf(
            "Заіірплата",
            "Бонуси",
            "Подарунки",
            "Пасивний дохід"
        )
    }

    fun getCategoriesHash(categories: List<String>): String {
        return categories.joinToString().hashCode().toString()
    }
}