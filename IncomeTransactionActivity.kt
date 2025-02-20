package com.serhio.homeaccountingapp;

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.serhio.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.draw.paint
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class IncomeTransactionActivity : ComponentActivity() {
    private val viewModel: IncomeViewModel by viewModels { IncomeViewModelFactory(application) }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryName = intent.getStringExtra("categoryName") ?: getString(R.string.category)

        // Завантажуємо дані для вибраної категорії
        viewModel.filterByCategory(categoryName)

        val selectedCurrency = getSelectedCurrency(this)

        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@IncomeTransactionActivity, MainActivity::class.java).apply {
                                    putExtra("SHOW_SPLASH_SCREEN", false)
                                }
                                startActivity(intent)
                            },
                            onNavigateToIncomes = { navigateToActivity(IncomeActivity::class.java) },
                            onNavigateToExpenses = { navigateToActivity(ExpenseActivity::class.java) },
                            onNavigateToIssuedOnLoan = { navigateToActivity(IssuedOnLoanActivity::class.java) },
                            onNavigateToBorrowed = { navigateToActivity(BorrowedActivity::class.java) },
                            onNavigateToAllTransactionIncome = { navigateToActivity(AllTransactionIncomeActivity::class.java) },
                            onNavigateToAllTransactionExpense = { navigateToActivity(AllTransactionExpenseActivity::class.java) },
                            onNavigateToBudgetPlanning = { navigateToActivity(BudgetPlanningActivity::class.java) },
                            onNavigateToTaskActivity = { navigateToActivity(TaskActivity::class.java) }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(categoryName, color = Color.White) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu_description), tint = Color.White)
                                    }
                                },
                                actions = {
                                    IncomePeriodButton(
                                        viewModel = viewModel,
                                        modifier = Modifier.width(120.dp) // Збільшуємо ширину кнопки
                                    ) { startDate, endDate ->
                                        viewModel.filterByPeriodForCategory(startDate, endDate, categoryName)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                            )
                        },
                        content = { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .paint(
                                        painter = painterResource(id = R.drawable.background_app),
                                        contentScale = ContentScale.Crop
                                    )
                                    .padding(innerPadding)
                            ) {
                                IncomeTransactionScreen(
                                    categoryName = categoryName,
                                    selectedCurrency = selectedCurrency,
                                    onUpdateTransactions = { updatedTransactions ->
                                        saveTransactionsIncome(updatedTransactions, categoryName)
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }
    private fun getSelectedCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("currency", "UAH") ?: "UAH"
    }

    private fun saveTransactionsIncome(updatedTransactions: List<IncomeTransaction>, categoryName: String) {
        val incomesharedPreferences = getSharedPreferences("IncomePrefs", MODE_PRIVATE)
        val gson = Gson()

        val existingTransactions = try {
            val transactionsJson = incomesharedPreferences.getString("IncomeTransactions", "[]") ?: "[]"
            val type = object : TypeToken<List<IncomeTransaction>>() {}.type
            gson.fromJson<List<IncomeTransaction>>(transactionsJson, type)
        } catch (e: Exception) {
            emptyList()
        }

        // Оновлюємо список транзакцій для конкретної категорії
        val updatedList = existingTransactions.filter { it.category != categoryName } + updatedTransactions
        val newTransactionsJson = gson.toJson(updatedList)

        // Зберігаємо транзакції в SharedPreferences
        incomesharedPreferences.edit().putString("IncomeTransactions", newTransactionsJson).apply()

        // Надсилаємо Broadcast для оновлення даних
        val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_INCOME")
        updateIntent.putExtra("categoryName", categoryName)
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent)
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IncomePeriodButton(
    viewModel: IncomeViewModel,
    modifier: Modifier = Modifier,
    onPeriodSelected: (LocalDate, LocalDate) -> Unit
) {
    val context = LocalContext.current
    val dialogState = remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val startDate = remember { mutableStateOf(LocalDate.now()) }
    val endDate = remember { mutableStateOf(LocalDate.now()) }

    OutlinedButton(
        onClick = { dialogState.value = true },
        modifier = modifier.size(120.dp, 40.dp), // Збільшуємо ширину кнопки
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Text(stringResource(id = R.string.period), fontWeight = FontWeight.Bold, color = Color.White)
    }

    if (dialogState.value) {
        AlertDialog(
            onDismissRequest = { dialogState.value = false },
            title = {
                Text(stringResource(id = R.string.period_selection), style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold))
            },
            text = {
                Column {
                    IncomeDatePickerField(
                        label = stringResource(id = R.string.start_date),
                        date = startDate.value,
                        onDateSelected = { date -> startDate.value = date }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    IncomeDatePickerField(
                        label = stringResource(id = R.string.end_date),
                        date = endDate.value,
                        onDateSelected = { date -> endDate.value = date }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPeriodSelected(startDate.value, endDate.value)
                        dialogState.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(id = R.string.save), style = MaterialTheme.typography.bodyLarge)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogState.value = false }) {
                    Text(stringResource(id = R.string.cancel), style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold))
                }
            },
            containerColor = Color.DarkGray
        )
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IncomeDatePickerField(label: String, date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        showIncomeDatePickerDialog(context, date, onDateSelected)
    }) {
        Text(text = "$label: ${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}", style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun showIncomeDatePickerDialog(context: Context, initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val calendar = Calendar.getInstance()
    calendar.set(initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth)
    DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(selectedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IncomeTransactionScreen(
    categoryName: String,
    selectedCurrency: String,
    onUpdateTransactions: (List<IncomeTransaction>) -> Unit,
    viewModel: IncomeViewModel = viewModel()
) {
    val transactions: List<IncomeTransaction> by viewModel.filteredTransactions.collectAsState(initial = emptyList())
    var selectedTransaction by remember { mutableStateOf<IncomeTransaction?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val totalIncomeForFilteredTransactions = transactions.sumOf { it.amount }

    val context = LocalContext.current
    val receiver = rememberUpdatedState(
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val category = intent?.getStringExtra("categoryName")
                if (category == categoryName) {
                    viewModel.filterByCategory(categoryName)
                }
            }
        }
    )

    DisposableEffect(Unit) {
        val filter = IntentFilter("com.example.homeaccountingapp.UPDATE_INCOME")
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver.value, filter)
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver.value)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background_app),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(96.dp)
                .align(Alignment.CenterEnd)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0x00000000),
                            Color(0x99000000)
                        )
                    )
                )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp) // Додавання відступу знизу для уникнення налазання під напис
            ) {
                items(transactions) { transaction ->
                    IncomeTransactionItem(
                        transaction = transaction,
                        selectedCurrency = selectedCurrency,
                        onClick = {
                            selectedTransaction = transaction
                            showMenuDialog = true
                        }
                    )
                }
            }
        }

        if (showMenuDialog && selectedTransaction != null) {
            IncomeEditDeleteDialog(
                transaction = selectedTransaction!!,
                onDismiss = { showMenuDialog = false },
                onEdit = {
                    showMenuDialog = false
                    showEditDialog = true
                },
                onDelete = {
                    viewModel.deleteTransaction(selectedTransaction!!)
                    viewModel.filterByCategory(categoryName)
                    showMenuDialog = false
                    val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_INCOME")
                    LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
                }
            )
        }
        if (showEditDialog && selectedTransaction != null) {
            IncomeEditTransactionDialog(
                transaction = selectedTransaction!!,
                onDismiss = { showEditDialog = false },
                onSave = { updatedTransaction ->
                    viewModel.updateTransaction(updatedTransaction)
                    viewModel.filterByCategory(categoryName)
                    showEditDialog = false
                    val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_INCOME")
                    LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
                },
                categories = viewModel.categories // Передаємо список категорій
            )
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF228B22)
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_transaction), tint = Color.White)
        }
        if (showAddDialog) {
            IncomeAddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onSave = { newTransaction: IncomeTransaction ->
                    viewModel.updateIncomeTransactions(newTransaction)
                    viewModel.filterByCategory(categoryName)
                    showAddDialog = false
                    val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_INCOME")
                    LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
                },
                categoryName = categoryName
            )
        }

        // Додаємо текст "Загальні доходи" та суму доходів
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp)) // Додати відступ між кнопкою та текстом
            Text(
                text = stringResource(id = R.string.total_income),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "${totalIncomeForFilteredTransactions.incomeFormatAmount(2)} $selectedCurrency",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Green // Зелений колір для суми доходів
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeAddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (IncomeTransaction) -> Unit,
    categoryName: String
) {
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(DateUtils.getCurrentDate()) }
    var comment by remember { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    if (showDatePickerDialog) {
        DatePickerDialogComponent(
            onDateSelected = { selectedDate ->
                date = DateUtils.formatDate(selectedDate, "dd/MM/yyyy", "yyyy-MM-dd")
                showDatePickerDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.add_new_transaction),
                style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(id = R.string.amount), style = TextStyle(color = Color.White)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(
                        text = date,
                        style = TextStyle(color = Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(id = R.string.comment), style = TextStyle(color = Color.White)) },
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),  // Білий шрифт для введення коментаря
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null && date.isNotBlank()) {
                        onSave(
                            IncomeTransaction(
                                category = categoryName,
                                amount = amountValue,
                                date = date,
                                comments = comment
                            )
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(id = R.string.save), style = MaterialTheme.typography.bodyLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = Color.White)
            }
        },
        containerColor = Color.DarkGray
    )
}
@Composable
fun IncomeTransactionItem(
    transaction: IncomeTransaction,
    selectedCurrency: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.DarkGray.copy(alpha = 0.9f), // Темно-сірий зліва
                        Color.DarkGray.copy(alpha = 0.1f)  // Майже прозорий справа
                    ),
                    startX = 0f,
                    endX = 300f  // Налаштовано так, щоб градієнт став майже прозорим з половини
                )
            )
            .clickable(onClick = onClick)
            .padding(16.dp) // Внутрішній відступ
    ) {
        Column {
            Text(
                text = "Сума: ${transaction.amount} $selectedCurrency",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.Green),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Дата: ${transaction.date}",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Green),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (!transaction.comments.isNullOrEmpty()) {
                Text(
                    text = "Коментар: ${transaction.comments}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Green)
                )
            }
        }
    }
}
@Composable
fun IncomeEditDeleteDialog(
    transaction: IncomeTransaction,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = true, onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Дії з транзакцією",
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Редагувати", color = Color.White)
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Видалити", color = Color.White)
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdownMenu(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Зменшуємо висоту кнопки
            border = BorderStroke(1.dp, Color.Gray)
        ) {
            Text(text = "Категорія: $selectedCategory", style = TextStyle(color = Color.White))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(200.dp) // Встановлюємо фіксовану ширину для випадаючого меню
                .background(Color.Gray.copy(alpha = 0.8f)) // Сірий прозорий фон
                .border(1.dp, Color.White, RoundedCornerShape(8.dp)) // Рамка з білим кольором і закругленими кутами
                .heightIn(max = 200.dp) // Обмежуємо висоту випадаючого меню
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(text = category, style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)) }, // Зробимо текст білим і жирним
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeEditTransactionDialog(
    transaction: IncomeTransaction,
    onDismiss: () -> Unit,
    onSave: (IncomeTransaction) -> Unit,
    categories: List<String> // Додано список категорій
) {
    var updatedAmount by remember { mutableStateOf(transaction.amount.toString()) }
    var updatedDate by remember { mutableStateOf(transaction.date) }
    var updatedComment by remember { mutableStateOf(transaction.comments ?: "") }
    var updatedCategory by remember { mutableStateOf(transaction.category) } // Додано змінну для категорії
    val datePickerState = remember { mutableStateOf(false) }

    if (datePickerState.value) {
        IncomeDatePickerDialog(
            onDismiss = { datePickerState.value = false },
            onDateSelected = { day, month, year ->
                updatedDate = "$year-${month + 1}-$day"
                datePickerState.value = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.edit_transaction), style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)) },
        text = {
            Column {
                TextField(
                    value = updatedAmount,
                    onValueChange = { updatedAmount = it },
                    label = { Text(stringResource(id = R.string.amount), style = TextStyle(color = Color.White)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { datePickerState.value = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF616161))
                ) {
                    Text(
                        text = if (updatedDate.isBlank()) stringResource(id = R.string.date) else "${stringResource(id = R.string.date)}: $updatedDate",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = updatedComment,
                    onValueChange = { updatedComment = it },
                    label = { Text(stringResource(id = R.string.comment), style = TextStyle(color = Color.White)) },
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.White,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Додаємо випадаючий список для вибору категорії
                CategoryDropdownMenu(
                    categories = categories,
                    selectedCategory = updatedCategory,
                    onCategorySelected = { updatedCategory = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = updatedAmount.toDoubleOrNull()
                    if (amountValue != null) {
                        onSave(transaction.copy(amount = amountValue, date = updatedDate, comments = updatedComment, category = updatedCategory))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(id = R.string.save), style = MaterialTheme.typography.bodyLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel), color = Color.White)
            }
        },
        containerColor = Color.DarkGray
    )
}
@Composable
fun IncomeDatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (day: Int, month: Int, year: Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val IncomeDatePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            onDateSelected(selectedDay, selectedMonth, selectedYear)
        },
        year,
        month,
        day
    )
    LaunchedEffect(Unit) {
        IncomeDatePickerDialog.show()
    }
    DisposableEffect(Unit) {
        onDispose {
            onDismiss()
        }
    }
}
