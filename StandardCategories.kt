package com.serhio.homeaccountingapp

import android.content.Context

object StandardCategories {
    fun getStandardExpenseCategories(context: Context): List<String> {
        return listOf(
            context.getString(R.string.rent),
            context.getString(R.string.utilities),
            context.getString(R.string.transport),
            context.getString(R.string.entertainment),
            context.getString(R.string.groceries),
            context.getString(R.string.clothing),
            context.getString(R.string.health),
            context.getString(R.string.education),
            context.getString(R.string.gifts),
            context.getString(R.string.hobbies),
            context.getString(R.string.charity),
            context.getString(R.string.sports),
            context.getString(R.string.electronics),
            context.getString(R.string.other)
        )
    }

    fun getStandardIncomeCategories(context: Context): List<String> {
        return listOf(
            context.getString(R.string.salary),
            context.getString(R.string.bonus),
            context.getString(R.string.passive_income)
        )
    }

    fun getCategoriesHash(categories: List<String>): String {
        return categories.joinToString().hashCode().toString()
    }
}
