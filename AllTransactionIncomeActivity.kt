package com.serhio.homeaccountingapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.serhio.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class AllTransactionIncomeActivity : ComponentActivity() {
    private val viewModel: IncomeViewModel by viewModels { IncomeViewModelFactory(application) }
    private lateinit var updateReceiver: BroadcastReceiver
    private lateinit var localeReceiver: BroadcastReceiver

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    private fun updateLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    private fun getSelectedLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("language", "UK") ?: "UK"
    }

    private fun getSelectedCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("currency", "UAH") ?: "UAH"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedLanguage = getSelectedLanguage(this)
        updateLocale(this, selectedLanguage)

        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val selectedCurrency = getSelectedCurrency(LocalContext.current)

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@AllTransactionIncomeActivity, MainActivity::class.java).apply {
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
                                title = {
                                    Text(stringResource(id = R.string.all_transactions_income), color = White)
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu), tint = White)
                                    }
                                },
                                actions = {
                                    SortMenu(viewModel)
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
                                AllTransactionIncomeScreen(
                                    viewModel = viewModel,
                                    onDeleteTransaction = { transaction -> deleteTransaction(transaction) },
                                    onUpdateTransaction = { transaction -> updateTransaction(transaction) },
                                    selectedCurrency = selectedCurrency
                                )
                                PeriodButton(viewModel, Modifier.align(Alignment.BottomStart).padding(16.dp))
                            }
                        }
                    )
                }
            }
        }

        updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.homeaccountingapp.UPDATE_INCOME") {
                    viewModel.loadDataIncome()
                }
            }
        }
        val filter = IntentFilter("com.example.homeaccountingapp.UPDATE_INCOME")
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter)

        localeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.homeaccountingapp.UPDATE_LOCALE") {
                    val language = intent.getStringExtra("language") ?: return
                    updateLocale(context, language)
                    recreate()
                }
            }
        }
        val localeFilter = IntentFilter("com.example.homeaccountingapp.UPDATE_LOCALE")
        LocalBroadcastManager.getInstance(this).registerReceiver(localeReceiver, localeFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localeReceiver)
    }

    private fun sendUpdateBroadcast() {
        val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_INCOME")
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent)
    }

    private fun deleteTransaction(transaction: IncomeTransaction) {
        viewModel.deleteTransaction(transaction)
        sendUpdateBroadcast() // Відправка повідомлення про оновлення
    }

    private fun updateTransaction(updatedTransaction: IncomeTransaction) {
        viewModel.updateTransaction(updatedTransaction)
        sendUpdateBroadcast() // Відправка повідомлення про оновлення
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PeriodButton(viewModel: IncomeViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dialogState = remember { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val startDate = remember { mutableStateOf(LocalDate.now()) }
    val endDate = remember { mutableStateOf(LocalDate.now()) }

    OutlinedButton(
        onClick = { dialogState.value = true },
        modifier = modifier.size(100.dp, 40.dp),
        border = BorderStroke(1.dp, Color.Gray),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White,
            containerColor = Color.Transparent // Прозорий фон
        )
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
                    DatePickerField(
                        label = stringResource(id = R.string.start_date),
                        date = startDate.value,
                        onDateSelected = { date -> startDate.value = date }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DatePickerField(
                        label = stringResource(id = R.string.end_date),
                        date = endDate.value,
                        onDateSelected = { date -> endDate.value = date }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.filterByPeriodForAllTransactions(startDate.value, endDate.value)
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
fun DatePickerField(label: String, date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        showDatePickerDialog(context, date, onDateSelected)
    }) {
        Text(text = "$label: ${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}", style = TextStyle(color = White, fontWeight = FontWeight.Bold))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun showDatePickerDialog(context: Context, initialDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
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

@Composable
fun SortMenu(viewModel: IncomeViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = stringResource(id = R.string.sort_by), tint = White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Black) // Зміна фону меню на чорний
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.sort_by_date), color = White) },
                onClick = {
                    viewModel.sortTransactions(SortType.DATE)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.sort_by_amount), color = White) },
                onClick = {
                    viewModel.sortTransactions(SortType.AMOUNT)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.sort_by_category), color = White) },
                onClick = {
                    viewModel.sortTransactions(SortType.CATEGORY)
                    expanded = false
                }
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AllTransactionIncomeScreen(
    viewModel: IncomeViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onDeleteTransaction: (IncomeTransaction) -> Unit,
    onUpdateTransaction: (IncomeTransaction) -> Unit,
    selectedCurrency: String
) {
    var selectedTransaction by remember { mutableStateOf<IncomeTransaction?>(null) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    BoxWithConstraints {
        val screenWidth = maxWidth
        val fontSize = if (screenWidth < 360.dp) 14.sp else 18.sp
        val padding = if (screenWidth < 360.dp) 8.dp else 16.dp

        val incomeTransactions by viewModel.sortedTransactions.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(start = padding, end = padding, top = padding, bottom = 70.dp) // Зменшена висота транзакцій
                    .background(Color.Transparent) // Зробити фон прозорим
            ) {
                when (viewModel.sortType) {
                    SortType.DATE -> {
                        val groupedTransactions = incomeTransactions.groupBy { it.date }
                        groupedTransactions.toSortedMap(compareByDescending { it }).forEach { (date, transactions) ->
                            items(transactions) { transaction ->
                                AllIncomeTransactionItem(
                                    transaction = transaction,
                                    onClick = {
                                        selectedTransaction = transaction
                                        showMenuDialog = true
                                    },
                                    selectedCurrency = selectedCurrency
                                )
                            }
                            item {
                                val totalGroupIncome = transactions.sumOf { it.amount }
                                Text(
                                    text = "${stringResource(id = R.string.total_amount_for_date)} $date: $totalGroupIncome $selectedCurrency",
                                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = White),
                                    modifier = Modifier.padding(padding)
                                )
                            }
                        }
                    }
                    SortType.AMOUNT -> {
                        val sortedTransactions = incomeTransactions.sortedByDescending { it.amount }
                        items(sortedTransactions) { transaction ->
                            AllIncomeTransactionItem(
                                transaction = transaction,
                                onClick = {
                                    selectedTransaction = transaction
                                    showMenuDialog = true
                                },
                                selectedCurrency = selectedCurrency
                            )
                        }
                    }
                    SortType.CATEGORY -> {
                        val groupedTransactions = incomeTransactions.groupBy { it.category }
                        groupedTransactions.forEach { (_, transactions) ->
                            items(transactions) { transaction ->
                                AllIncomeTransactionItem(
                                    transaction = transaction,
                                    onClick = {
                                        selectedTransaction = transaction
                                        showMenuDialog = true
                                    },
                                    selectedCurrency = selectedCurrency
                                )
                            }
                            item {
                                val totalGroupIncome = transactions.sumOf { it.amount }
                                Text(
                                    text = "${stringResource(id = R.string.total_income_for_category)}: $totalGroupIncome $selectedCurrency",
                                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = White),
                                    modifier = Modifier.padding(padding)
                                )
                            }
                        }
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
                        onDeleteTransaction(selectedTransaction!!) // Виклик onDeleteTransaction
                        showMenuDialog = false
                    }
                )
            }
            if (showEditDialog && selectedTransaction != null) {
                IncomeEditTransactionDialog(
                    transaction = selectedTransaction!!,
                    onDismiss = { showEditDialog = false },
                    onSave = { updatedTransaction ->
                        onUpdateTransaction(updatedTransaction) // Виклик onUpdateTransaction
                        showEditDialog = false
                    },
                    categories = viewModel.categories // Передаємо список категорій
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun AllIncomeTransactionItem(
    transaction: IncomeTransaction,
    onClick: () -> Unit,
    selectedCurrency: String
) {
    BoxWithConstraints {
        val screenWidth = maxWidth
        val fontSize = if (screenWidth < 360.dp) 14.sp else 18.sp
        val padding = if (screenWidth < 360.dp) 4.dp else 8.dp // Зменшення відступу

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.DarkGray.copy(alpha = 0.9f), // Темно-сірий зліва
                            Color.DarkGray.copy(alpha = 0.1f)  // Майже прозорий справа
                        ),
                        startX = 0.0f,
                        endX = screenWidth.value / 2 // Градієнт до середини
                    )
                )
                .clickable { onClick() } // Виклик onClick у контексті click
                .padding(padding)
        ) {
            Column {
                Text(
                    text = "${stringResource(id = R.string.category)}: ${transaction.category}", // Додавання назви категорії
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.Green, fontSize = fontSize),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.amount)}: ${transaction.amount} $selectedCurrency",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.Green, fontSize = fontSize),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.date)}: ${transaction.date}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Green, fontSize = fontSize),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (!transaction.comments.isNullOrEmpty()) {
                    Text(
                        text = "${stringResource(id = R.string.comment)}: ${transaction.comments}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Green, fontSize = fontSize)
                    )
                }
            }
        }
    }
}
