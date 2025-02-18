package com.serhio.homeaccountingapp;

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDate(date: String, inputFormat: String, outputFormat: String): String {
        return try {
            val inputDateFormat = SimpleDateFormat(inputFormat, Locale.getDefault())
            val outputDateFormat = SimpleDateFormat(outputFormat, Locale.getDefault())
            val parsedDate = inputDateFormat.parse(date)
            outputDateFormat.format(parsedDate)
        } catch (e: ParseException) {
            e.printStackTrace()
            date // Повертаємо оригінальну дату у випадку помилки
        }
    }

    fun getCurrentDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }
}