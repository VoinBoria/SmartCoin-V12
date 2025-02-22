package com.serhio.homeaccountingapp;
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.serhio.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.compose.ui.zIndex
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Locale


class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var updateReceiver: BroadcastReceiver

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізація локалі
        val sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("language", "UK") ?: "UK"
        updateLocale(this, selectedLanguage)

        // Перевірка та оновлення категорій при запуску
        updateCategoriesIfNeeded()

        setContent {
            HomeAccountingAppTheme {
                MainContent()
            }
        }

        updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.homeaccountingapp.UPDATE_EXPENSES" ||
                    intent.action == "com.example.homeaccountingapp.UPDATE_INCOME") {
                    viewModel.refreshExpenses()
                    viewModel.refreshIncomes()
                    viewModel.refreshCategories()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("com.example.homeaccountingapp.UPDATE_EXPENSES")
            addAction("com.example.homeaccountingapp.UPDATE_INCOME")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(updateReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun MainContent() {
        val context = LocalContext.current
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        var selectedCurrency by remember { mutableStateOf(getSelectedCurrency(sharedPreferences)) }
        var selectedLanguage by remember { mutableStateOf(getSelectedLanguage(sharedPreferences)) }
        var tempSelectedCurrency by remember { mutableStateOf(selectedCurrency) }
        var tempSelectedLanguage by remember { mutableStateOf(selectedLanguage) }

        var showSplashScreen by remember { mutableStateOf(false) }
        var showSettingsMenu by remember { mutableStateOf(sharedPreferences.getBoolean("first_launch", true)) } // Show settings menu only on first launch

        LaunchedEffect(key1 = selectedLanguage) {
            refreshUI()
        }

        val appTitle = stringResource(id = R.string.main_screen_title)

        if (showSplashScreen) {
            SplashScreen(onTimeout = {
                showSplashScreen = false
            })
        } else {
            MainScreen(
                onNavigateToMainActivity = {
                    val intent = Intent(context, MainActivity::class.java).apply {
                        putExtra("SHOW_SPLASH_SCREEN", false)
                    }
                    context.startActivity(intent)
                },
                onNavigateToIncomes = {
                    val intent = Intent(context, IncomeActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToExpenses = {
                    val intent = Intent(context, ExpenseActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToIssuedOnLoan = {
                    val intent = Intent(context, IssuedOnLoanActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToBorrowed = {
                    val intent = Intent(context, BorrowedActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToAllTransactionIncome = {
                    val intent = Intent(context, AllTransactionIncomeActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToAllTransactionExpense = {
                    val intent = Intent(context, AllTransactionExpenseActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToBudgetPlanning = {
                    val intent = Intent(context, BudgetPlanningActivity::class.java)
                    context.startActivity(intent)
                },
                onNavigateToTaskActivity = {
                    val intent = Intent(context, TaskActivity::class.java)
                    context.startActivity(intent)
                },
                viewModel = viewModel(),
                onIncomeCategoryClick = { category ->
                    val intent = Intent(context, IncomeTransactionActivity::class.java).apply {
                        putExtra("categoryName", category)
                    }
                    context.startActivity(intent)
                },
                onExpenseCategoryClick = { category ->
                    val intent = Intent(context, ExpenseTransactionActivity::class.java).apply {
                        putExtra("categoryName", category)
                    }
                    context.startActivity(intent)
                },
                selectedCurrency = tempSelectedCurrency,
                onCurrencySelected = { currency ->
                    tempSelectedCurrency = currency
                },
                selectedLanguage = tempSelectedLanguage,
                onLanguageSelected = { language ->
                    tempSelectedLanguage = language
                },
                onSaveSettings = {
                    selectedCurrency = tempSelectedCurrency
                    selectedLanguage = tempSelectedLanguage
                    saveSettings(sharedPreferences, selectedLanguage, selectedCurrency)
                    updateLocale(context, selectedLanguage)
                    updateCategories()
                    refreshUI()
                    sharedPreferences.edit().putBoolean("first_launch", false).apply() // Update first launch status
                    showSettingsMenu = false // Close SettingsMenu after saving settings
                },
                updateLocale = ::updateLocale,
                currency = selectedCurrency,
                refreshUI = ::refreshUI,
                appTitle = appTitle,
                showSettingsMenu = showSettingsMenu // Ensure this parameter is passed
            )
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateUI() {
        // Викликайте цю функцію для оновлення всіх необхідних елементів UI після зміни мови
        setContent {
            HomeAccountingAppTheme {
                MainContent()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshUI() {
        viewModel.refreshExpenses()
        viewModel.refreshIncomes()
        viewModel.refreshCategories()
        setContent {
            HomeAccountingAppTheme {
                MainContent()
            }
        }
    }
    private fun getSelectedCurrency(sharedPreferences: SharedPreferences): String {
        return sharedPreferences.getString("currency", "UAH") ?: "UAH"
    }

    private fun getSelectedLanguage(sharedPreferences: SharedPreferences): String {
        return sharedPreferences.getString("language", "UK") ?: "UK"
    }

    private fun updateLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun saveSettings(sharedPreferences: SharedPreferences, language: String, currency: String) {
        with(sharedPreferences.edit()) {
            putString("language", language)
            putString("currency", currency)
            apply()
        }
    }

    private fun updateCategories() {
        val sharedPreferencesExpense = getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val sharedPreferencesIncome = getSharedPreferences("IncomePrefs", Context.MODE_PRIVATE)

        val expenseCategories = StandardCategories.getStandardExpenseCategories(this)
        val incomeCategories = StandardCategories.getStandardIncomeCategories(this)

        saveCategories(sharedPreferencesExpense, expenseCategories)
        saveCategories(sharedPreferencesIncome, incomeCategories)

        viewModel.refreshCategories()
    }

    private fun saveCategories(sharedPreferences: SharedPreferences, categories: List<String>) {
        val editor = sharedPreferences.edit()
        val categoriesJson = Gson().toJson(categories)
        editor.putString("categories", categoriesJson)
        editor.apply()
    }

    private fun updateCategoriesIfNeeded() {
        val sharedPreferencesExpense = getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val sharedPreferencesIncome = getSharedPreferences("IncomePrefs", Context.MODE_PRIVATE)
        val currentExpenseHash = StandardCategories.getCategoriesHash(StandardCategories.getStandardExpenseCategories(this))
        val currentIncomeHash = StandardCategories.getCategoriesHash(StandardCategories.getStandardIncomeCategories(this))

        val savedExpenseHash = sharedPreferencesExpense.getString("categories_hash", "")
        val savedIncomeHash = sharedPreferencesIncome.getString("categories_hash", "")

        if (currentExpenseHash != savedExpenseHash) {
            val expenseCategories = StandardCategories.getStandardExpenseCategories(this)
            saveCategories(sharedPreferencesExpense, expenseCategories)
            sharedPreferencesExpense.edit().putString("categories_hash", currentExpenseHash).apply()
        }

        if (currentIncomeHash != savedIncomeHash) {
            val incomeCategories = StandardCategories.getStandardIncomeCategories(this)
            saveCategories(sharedPreferencesIncome, incomeCategories)
            sharedPreferencesIncome.edit().putString("categories_hash", currentIncomeHash).apply()
        }

        viewModel.refreshCategories()
    }
}
// Функція Splash Screen
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_imagee), // Переконайтеся, що цей ресурс існує
            contentDescription = null,
            contentScale = ContentScale.Crop, // Додайте цей рядок для розтягування зображення
            modifier = Modifier.fillMaxSize() // Додайте цей рядок для розтягування зображення
        )
    }

    LaunchedEffect(Unit) {
        delay(2000) // Затримка для відображення SplashScreen
        currentOnTimeout()
    }
}
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferencesExpense = application.getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
    private val sharedPreferencesIncome = application.getSharedPreferences("IncomePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _expenses = MutableLiveData<Map<String, Double>>()
    val expenses: LiveData<Map<String, Double>> = _expenses

    private val _incomes = MutableLiveData<Map<String, Double>>()
    val incomes: LiveData<Map<String, Double>> = _incomes

    private val _expenseCategories = MutableLiveData<List<String>>()
    val expenseCategories: LiveData<List<String>> = _expenseCategories

    private val _incomeCategories = MutableLiveData<List<String>>()
    val incomeCategories: LiveData<List<String>> = _incomeCategories

    init {
        loadStandardCategories()
    }

    // Метод для завантаження стандартних категорій
    fun loadStandardCategories() {
        val expenseCategories = loadCategories(sharedPreferencesExpense, StandardCategories.getStandardExpenseCategories(getApplication()))
        val incomeCategories = loadCategories(sharedPreferencesIncome, StandardCategories.getStandardIncomeCategories(getApplication()))

        _expenseCategories.value = expenseCategories
        _incomeCategories.value = incomeCategories

        // Завантаження доходів та витрат
        loadExpensesFromSharedPreferences()
        loadIncomesFromSharedPreferences()
    }

    // Загальний метод для завантаження категорій
    private fun loadCategories(sharedPreferences: SharedPreferences, defaultCategories: List<String>): List<String> {
        val categoriesJson = sharedPreferences.getString("categories", null)
        return if (categoriesJson != null) {
            gson.fromJson(categoriesJson, object : TypeToken<List<String>>() {}.type)
        } else {
            saveCategories(sharedPreferences, defaultCategories)
            defaultCategories
        }
    }

    // Загальний метод для збереження категорій
    private fun saveCategories(sharedPreferences: SharedPreferences, categories: List<String>) {
        val editor = sharedPreferences.edit()
        val categoriesJson = gson.toJson(categories)
        editor.putString("categories", categoriesJson)
        editor.apply()
    }

    fun loadExpensesFromSharedPreferences() {
        val expensesJson = sharedPreferencesExpense.getString("expenses", null)
        val expenseMap: Map<String, Double> = if (expensesJson != null) {
            gson.fromJson(expensesJson, object : TypeToken<Map<String, Double>>() {}.type)
        } else {
            emptyMap()
        }
        Log.d("MainViewModel", "Expenses loaded: $expensesJson")
        _expenses.postValue(expenseMap)
    }

    fun addIncomeCategory(newCategory: String) {
        val currentCategories = _incomeCategories.value ?: emptyList()
        if (newCategory !in currentCategories) {
            val updatedCategories = currentCategories + newCategory
            _incomeCategories.value = updatedCategories
            saveCategories(sharedPreferencesIncome, updatedCategories)
        }
    }

    fun saveExpensesToSharedPreferences(expenses: Map<String, Double>) {
        val editor = sharedPreferencesExpense.edit()
        val expensesJson = gson.toJson(expenses)
        editor.putString("expenses", expensesJson).apply()
        Log.d("MainViewModel", "Expenses saved: $expensesJson")
        _expenses.postValue(expenses) // Негайне оновлення LiveData
    }

    fun loadIncomesFromSharedPreferences() {
        val incomesJson = sharedPreferencesIncome.getString("incomes", null)
        val incomeMap: Map<String, Double> = if (incomesJson != null) {
            gson.fromJson(incomesJson, object : TypeToken<Map<String, Double>>() {}.type)
        } else {
            emptyMap()
        }
        Log.d("MainViewModel", "Incomes loaded: $incomesJson")
        _incomes.postValue(incomeMap)
    }

    fun saveIncomesToSharedPreferences(incomes: Map<String, Double>) {
        val editor = sharedPreferencesIncome.edit()
        val incomesJson = gson.toJson(incomes)
        editor.putString("incomes", incomesJson).apply()
        Log.d("MainViewModel", "Incomes saved: $incomesJson")
        _incomes.postValue(incomes) // Негайне оновлення LiveData
    }

    fun saveExpenseTransaction(context: Context, transaction: Transaction) {
        val existingTransactionsJson = sharedPreferencesExpense.getString("transactions", "[]") ?: "[]"
        val type = object : TypeToken<List<Transaction>>() {}.type
        val existingTransactions: MutableList<Transaction> = gson.fromJson(existingTransactionsJson, type)

        val formattedDate = DateUtils.formatDate(transaction.date, "dd/MM/yyyy", "yyyy-MM-dd")
        existingTransactions.add(transaction.copy(amount = -transaction.amount, date = formattedDate))
        val updatedJson = gson.toJson(existingTransactions)

        sharedPreferencesExpense.edit().putString("transactions", updatedJson).apply()
        Log.d("MainViewModel", "Expense transactions saved: $updatedJson")

        refreshExpenses()

        val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_EXPENSES")
        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
    }

    fun saveIncomeTransaction(context: Context, transaction: IncomeTransaction) {
        val existingTransactionsJson = sharedPreferencesIncome.getString("IncomeTransactions", "[]") ?: "[]"
        val type = object : TypeToken<List<IncomeTransaction>>() {}.type
        val existingTransactions: MutableList<IncomeTransaction> = gson.fromJson(existingTransactionsJson, type)

        val formattedDate = DateUtils.formatDate(transaction.date, "dd/MM/yyyy", "yyyy-MM-dd")
        existingTransactions.add(transaction.copy(date = formattedDate))
        val updatedJson = gson.toJson(existingTransactions)

        sharedPreferencesIncome.edit().putString("IncomeTransactions", updatedJson).apply()
        Log.d("MainViewModel", "Income transactions saved: $updatedJson")

        refreshIncomes()

        val updateIntent = Intent("com.example.homeaccountingapp.UPDATE_INCOME")
        LocalBroadcastManager.getInstance(context).sendBroadcast(updateIntent)
    }

    fun refreshExpenses() {
        val transactionsJson = sharedPreferencesExpense.getString("transactions", "[]") ?: "[]"
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions: List<Transaction> = gson.fromJson(transactionsJson, type)

        // Перерахунок витрат
        val updatedExpenses = calculateExpenses(transactions)

        // Додавання порожніх категорій
        val expenseCategories = _expenseCategories.value ?: emptyList()
        val completeExpenses = expenseCategories.associateWith { updatedExpenses[it] ?: 0.0 }

        _expenses.postValue(completeExpenses)
        saveExpensesToSharedPreferences(completeExpenses)
    }

    fun addExpenseCategory(newCategory: String) {
        val currentCategories = _expenseCategories.value ?: emptyList()
        if (newCategory !in currentCategories) {
            val updatedCategories = currentCategories + newCategory
            _expenseCategories.value = updatedCategories
            saveCategories(sharedPreferencesExpense, updatedCategories)
        }
    }

    fun refreshIncomes() {
        val transactionsJson = sharedPreferencesIncome.getString("IncomeTransactions", "[]") ?: "[]"
        val type = object : TypeToken<List<IncomeTransaction>>() {}.type
        val transactions: List<IncomeTransaction> = gson.fromJson(transactionsJson, type)

        // Перерахунок доходів
        val updatedIncomes = calculateIncomes(transactions)

        // Додавання порожніх категорій
        val incomeCategories = _incomeCategories.value ?: emptyList()
        val completeIncomes = incomeCategories.associateWith { updatedIncomes[it] ?: 0.0 }

        _incomes.postValue(completeIncomes)
        saveIncomesToSharedPreferences(completeIncomes)
    }

    // Допоміжний метод для перерахунку витрат за категоріями
    private fun calculateExpenses(transactions: List<Transaction>): Map<String, Double> {
        return transactions.groupBy { it.category }.mapValues { (_, transactions) ->
            transactions.sumOf { it.amount }
        }
    }

    // Допоміжний метод для перерахунку доходів за категоріями
    private fun calculateIncomes(transactions: List<IncomeTransaction>): Map<String, Double> {
        return transactions.groupBy { it.category }.mapValues { (_, transactions) ->
            transactions.sumOf { it.amount }
        }
    }

    fun refreshCategories() {
        loadStandardCategories()
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToMainActivity: () -> Unit,
    onNavigateToIncomes: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIssuedOnLoan: () -> Unit,
    onNavigateToBorrowed: () -> Unit,
    onNavigateToAllTransactionIncome: () -> Unit,
    onNavigateToAllTransactionExpense: () -> Unit,
    onNavigateToBudgetPlanning: () -> Unit,
    onNavigateToTaskActivity: () -> Unit,
    viewModel: MainViewModel = viewModel(),
    onIncomeCategoryClick: (String) -> Unit,
    onExpenseCategoryClick: (String) -> Unit,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    updateLocale: (Context, String) -> Unit,
    onSaveSettings: () -> Unit,
    currency: String,
    refreshUI: () -> Unit,
    appTitle: String,
    showSettingsMenu: Boolean
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showExpenses by remember { mutableStateOf(false) }
    var showIncomes by remember { mutableStateOf(false) }
    var showAddExpenseTransactionDialog by remember { mutableStateOf(false) }
    var showAddIncomeTransactionDialog by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf(false) }
    var settingsMenuVisible by remember { mutableStateOf(showSettingsMenu) } // Use the passed parameter to control visibility

    LaunchedEffect(Unit) {
        showExpenses = false
        showIncomes = false
    }

    val expenses by viewModel.expenses.observeAsState(initial = emptyMap())
    val incomes by viewModel.incomes.observeAsState(initial = emptyMap())
    val expenseCategories by viewModel.expenseCategories.observeAsState(initial = emptyList())
    val incomeCategories by viewModel.incomeCategories.observeAsState(initial = emptyList())

    val totalExpenses = expenses.values.sum()
    val totalIncomes = incomes.values.sum()
    val balance = totalIncomes + totalExpenses

    val showWarning = balance < 0
    val showSuccess = balance > 0

    var messagePhase by remember { mutableStateOf(0) }

    LaunchedEffect(balance) {
        showMessage = showWarning || showSuccess
        if (showMessage) {
            messagePhase = 1
            delay(3000)
            messagePhase = 2
            delay(1000)
            messagePhase = 3
            delay(2000)
            showMessage = false
            messagePhase = 0
        }
    }

    val context = LocalContext.current
    val pagerState = rememberPagerState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onNavigateToMainActivity = { scope.launch { drawerState.close(); onNavigateToMainActivity() } },
                onNavigateToIncomes = { scope.launch { drawerState.close(); onNavigateToIncomes() } },
                onNavigateToExpenses = { scope.launch { drawerState.close(); onNavigateToExpenses() } },
                onNavigateToIssuedOnLoan = { scope.launch { drawerState.close(); onNavigateToIssuedOnLoan() } },
                onNavigateToBorrowed = { scope.launch { drawerState.close(); onNavigateToBorrowed() } },
                onNavigateToAllTransactionIncome = { scope.launch { drawerState.close(); onNavigateToAllTransactionIncome() } },
                onNavigateToAllTransactionExpense = { scope.launch { drawerState.close(); onNavigateToAllTransactionExpense() } },
                onNavigateToBudgetPlanning = { scope.launch { drawerState.close(); onNavigateToBudgetPlanning() } },
                onNavigateToTaskActivity = { scope.launch { drawerState.close(); onNavigateToTaskActivity() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(appTitle, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Меню",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { settingsMenuVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(id = R.string.settings),
                                tint = Color.White
                            )
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
                    BoxWithConstraints {
                        val isWideScreen = maxWidth > 600.dp

                        if (isWideScreen) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .widthIn(max = 400.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 400.dp)
                                            .padding(vertical = 8.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
                                    ) {
                                        ExpandableButtonWithAmount(
                                            text = stringResource(id = R.string.incomes),
                                            amount = totalIncomes,
                                            gradientColors = listOf(
                                                Color.Transparent,
                                                Color.Transparent
                                            ),
                                            isExpanded = showIncomes,
                                            onClick = { showIncomes = !showIncomes },
                                            textColor = Color(0xFF00FF00),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            currency = currency
                                        )
                                    }

                                    if (showIncomes) {
                                        Column(
                                            modifier = Modifier
                                                .heightIn(max = 200.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            IncomeList(incomes = incomes, onCategoryClick = onIncomeCategoryClick, currency = currency)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 400.dp)
                                            .padding(vertical = 8.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
                                    ) {
                                        ExpandableButtonWithAmount(
                                            text = stringResource(id = R.string.expenses),
                                            amount = totalExpenses,
                                            gradientColors = listOf(
                                                Color.Transparent,
                                                Color.Transparent
                                            ),
                                            isExpanded = showExpenses,
                                            onClick = { showExpenses = !showExpenses },
                                            textColor = Color(0xFFFF0000),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            currency = currency
                                        )
                                    }

                                    if (showExpenses) {
                                        Column(
                                            modifier = Modifier
                                                .heightIn(max = 200.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            ExpensesList(expenses = expenses, onCategoryClick = onExpenseCategoryClick, currency = currency)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 400.dp)
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Transparent, RoundedCornerShape(10.dp))
                                        .border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
                                ) {
                                    IncomeExpenseChart(
                                        incomes = incomes,
                                        expenses = expenses,
                                        totalIncomes = totalIncomes,
                                        totalExpenses = totalExpenses,
                                        currency = currency
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 600.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 400.dp)
                                            .padding(vertical = 8.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
                                    ) {
                                        ExpandableButtonWithAmount(
                                            text = stringResource(id = R.string.incomes),
                                            amount = totalIncomes,
                                            gradientColors = listOf(
                                                Color.Transparent,
                                                Color.Transparent
                                            ),
                                            isExpanded = showIncomes,
                                            onClick = { showIncomes = !showIncomes },
                                            textColor = Color(0xFF00FF00),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            currency = currency
                                        )
                                    }

                                    if (showIncomes) {
                                        Column(
                                            modifier = Modifier
                                                .heightIn(max = 200.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            IncomeList(incomes = incomes, onCategoryClick = onIncomeCategoryClick, currency = currency)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 400.dp)
                                            .padding(vertical = 8.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
                                    ) {
                                        ExpandableButtonWithAmount(
                                            text = stringResource(id = R.string.expenses),
                                            amount = totalExpenses,
                                            gradientColors = listOf(
                                                Color.Transparent,
                                                Color.Transparent
                                            ),
                                            isExpanded = showExpenses,
                                            onClick = { showExpenses = !showExpenses },
                                            textColor = Color(0xFFFF0000),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            currency = currency
                                        )
                                    }

                                    if (showExpenses) {
                                        Column(
                                            modifier = Modifier
                                                .heightIn(max = 200.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            ExpensesList(expenses = expenses, onCategoryClick = onExpenseCategoryClick, currency = currency)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(32.dp))

                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 400.dp)
                                            .padding(vertical = 8.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                            .border(2.dp, Color.Gray, RoundedCornerShape(10.dp))
                                    ) {
                                        IncomeExpenseChart(
                                            incomes = incomes,
                                            expenses = expenses,
                                            totalIncomes = totalIncomes,
                                            totalExpenses = totalExpenses,
                                            currency = currency
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, bottom = 15.dp)
                            .zIndex(0f),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        BalanceDisplay(balance = balance, currency = currency)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(0f)
                    ) {
                    }

                    if (showAddExpenseTransactionDialog) {
                        AddTransactionDialog(
                            categories = expenseCategories,
                            onDismiss = { showAddExpenseTransactionDialog = false },
                            onSave = { transaction: Transaction ->
                                val negativeTransaction = transaction.copy(amount = -transaction.amount) // Додаємо знак мінус до суми витрат
                                viewModel.saveExpenseTransaction(context, negativeTransaction)
                                viewModel.refreshExpenses()
                                showAddExpenseTransactionDialog = false
                            },
                            onAddCategory = { newCategory ->
                                viewModel.addExpenseCategory(newCategory)
                            }
                        )
                    }

                    if (showAddIncomeTransactionDialog) {
                        IncomeAddIncomeTransactionDialog(
                            categories = incomeCategories,
                            onDismiss = { showAddIncomeTransactionDialog = false },
                            onSave = { incomeTransaction ->
                                viewModel.saveIncomeTransaction(context, incomeTransaction)
                                viewModel.refreshIncomes()
                                showAddIncomeTransactionDialog = false
                            },
                            onAddCategory = { newCategory ->
                                viewModel.addIncomeCategory(newCategory)
                            }
                        )
                    }

                    if (settingsMenuVisible) {
                        SettingsMenu(
                            refreshUI = refreshUI,
                            onDismiss = { settingsMenuVisible = false },
                            onCurrencySelected = onCurrencySelected,
                            onLanguageSelected = onLanguageSelected,
                            updateLocale = updateLocale,
                            onSaveSettings = onSaveSettings
                        )
                    }

                    // Повідомлення
                    AnimatedVisibility(
                        visible = showMessage,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight }
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight }
                        ),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(
                                    if (showWarning) Color(0x80B22222) else Color(0x8000B22A),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = if (showWarning) stringResource(id = R.string.need_to_spend_less) else stringResource(id = R.string.on_the_right_track),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .offset(y = (-16).dp)
                    ) {
                        FloatingActionButton(
                            onClick = { showAddIncomeTransactionDialog = true },
                            containerColor = Color(0xFF228B22), // Slightly darker green color
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text("+", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        }
                        FloatingActionButton(
                            onClick = { showAddExpenseTransactionDialog = true },
                            containerColor = Color(0xFFe6194B)
                        ) {
                            Text("+", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        )
    }
}
private fun Modifier.backgroundWithImage(
    painter: androidx.compose.ui.graphics.painter.Painter,
    contentScale: ContentScale
): Modifier {
    return this.then(
        Modifier.paint(
            painter = painter,
            contentScale = contentScale
        )
    )
}
