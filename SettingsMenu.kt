package com.serhio.homeaccountingapp

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun SettingsMenu(
    onDismiss: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    updateLocale: (Context, String) -> Unit,
    onSaveSettings: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var selectedLanguage by remember { mutableStateOf(sharedPreferences.getString("language", "UK") ?: "UK") }
    var selectedCurrency by remember { mutableStateOf(sharedPreferences.getString("currency", "UAH") ?: "UAH") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = true, onClick = {})
            .zIndex(1f),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Gray.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.8f))
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .widthIn(max = 300.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Налаштування",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Мова:", color = Color.White)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LanguageOption("UK", selectedLanguage) { language ->
                    selectedLanguage = language
                    onLanguageSelected(language)
                }
                LanguageOption("EN", selectedLanguage) { language ->
                    selectedLanguage = language
                    onLanguageSelected(language)
                }
                LanguageOption("RU", selectedLanguage) { language ->
                    selectedLanguage = language
                    onLanguageSelected(language)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.Gray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Валюта:", color = Color.White)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CurrencyOption("UAH", selectedCurrency) { currency ->
                    selectedCurrency = currency
                    onCurrencySelected(currency)
                }
                CurrencyOption("USD", selectedCurrency) { currency ->
                    selectedCurrency = currency
                    onCurrencySelected(currency)
                }
                CurrencyOption("EUR", selectedCurrency) { currency ->
                    selectedCurrency = currency
                    onCurrencySelected(currency)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Закрити", color = Color.Red)
                }
                TextButton(onClick = {
                    saveSettings(sharedPreferences, selectedLanguage, selectedCurrency) // Виклик saveSettings
                    onSaveSettings()
                    updateLocale(context, selectedLanguage) // Виклик updateLocale
                    onDismiss()
                }) {
                    Text("Зберегти", color = Color.Green)
                }
            }
        }
    }
}

@Composable
fun LanguageOption(language: String, selectedLanguage: String, onSelect: (String) -> Unit) {
    Button(
        onClick = { onSelect(language) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (language == selectedLanguage) Color.Gray else Color.Gray.copy(alpha = 0.5f),
            contentColor = Color.White
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
    ) {
        Text(text = language)
    }
}

@Composable
fun CurrencyOption(currency: String, selectedCurrency: String, onSelect: (String) -> Unit) {
    Button(
        onClick = { onSelect(currency) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (currency == selectedCurrency) Color.Blue else Color.Blue.copy(alpha = 0.5f),
            contentColor = Color.White
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
    ) {
        Text(text = currency)
    }
}

fun saveSettings(sharedPreferences: SharedPreferences, language: String, currency: String) {
    with(sharedPreferences.edit()) {
        putString("language", language)
        putString("currency", currency)
        apply()
    }
}
