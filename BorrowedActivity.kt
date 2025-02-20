package com.serhio.homeaccountingapp;

import android.annotation.SuppressLint
import android.app.Application
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serhio.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class BorrowedActivity : ComponentActivity() {
    private val borrowedViewModel: BorrowedViewModel by viewModels { BorrowedViewModelFactory(application) }

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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedLanguage = getSelectedLanguage(this)
        updateLocale(this, selectedLanguage)

        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var showOverdueMessage by remember { mutableStateOf(false) }
                val selectedCurrency = getSelectedCurrency(LocalContext.current)

                LaunchedEffect(Unit) {
                    borrowedViewModel.loadTransactions(application)
                    if (borrowedViewModel.hasOverdueTransactions()) {
                        delay(500)
                        showOverdueMessage = true
                        delay(3000)
                        showOverdueMessage = false
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@BorrowedActivity, MainActivity::class.java).apply {
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
                                title = { Text(stringResource(id = R.string.borrowed), color = Color.White) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = stringResource(id = R.string.menu), tint = Color.White)
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
                                BorrowedScreen(viewModel = borrowedViewModel, selectedCurrency = selectedCurrency)

                                AnimatedVisibility(
                                    visible = showOverdueMessage,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 100.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF800080).copy(alpha = 0.8f), Color.Transparent)
                                                ),
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Text(stringResource(id = R.string.overdue_borrowed_message), color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BorrowedScreen(
    viewModel: BorrowedViewModel,
    modifier: Modifier = Modifier,
    selectedCurrency: String
) {
    var showAddBorrowedDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<BorrowedTransaction?>(null) }
    val transactions by viewModel.transactions.collectAsState()

    val totalBorrowed = transactions.sumOf { it.amount }

    BoxWithConstraints {
        val screenWidth = maxWidth
        val fontSize = if (screenWidth < 360.dp) 14.sp else 18.sp
        val padding = if (screenWidth < 360.dp) 8.dp else 16.dp

        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(transactions) { borrowedTransaction ->
                        BorrowedTransactionRow(borrowedTransaction, viewModel, onEdit = { transactionToEdit = it }, selectedCurrency = selectedCurrency)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = padding)
                ) {
                    Text(
                        text = stringResource(id = R.string.total_borrowed),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = padding)
                    )
                    Text(
                        text = "${totalBorrowed.formatBorrowedAmount(2)} $selectedCurrency",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            FloatingActionButton(
                onClick = { showAddBorrowedDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(padding),
                containerColor = Color(0xFFFFA500)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_transaction), tint = Color.White)
            }

            if (showAddBorrowedDialog || transactionToEdit != null) {
                AddOrEditBorrowedTransactionDialog(
                    onDismiss = {
                        showAddBorrowedDialog = false
                        transactionToEdit = null
                    },
                    onSave = { newTransaction ->
                        if (transactionToEdit != null) {
                            viewModel.updateBorrowedTransaction(newTransaction)
                        } else {
                            viewModel.addBorrowedTransaction(newTransaction)
                        }
                        transactionToEdit = null
                        showAddBorrowedDialog = false
                    },
                    transactionToEdit = transactionToEdit
                )
            }
        }
    }
}
// Перейменована функція для форматування суми
fun Double.formatBorrowedAmount(digits: Int): String {
    return "%.${digits}f".format(this)
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BorrowedTransactionRow(
    borrowedTransaction: BorrowedTransaction,
    viewModel: BorrowedViewModel,
    onEdit: (BorrowedTransaction) -> Unit,
    selectedCurrency: String
) {
    BoxWithConstraints {
        val screenWidth = maxWidth
        val fontSize = if (screenWidth < 360.dp) 14.sp else 18.sp
        val padding = if (screenWidth < 360.dp) 4.dp else 8.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.DarkGray.copy(alpha = 0.9f), // Темно-сірий зліва
                            Color.DarkGray.copy(alpha = 0.1f)  // Майже прозорий справа
                        )
                    )
                )
                .clickable {
                    // Handle click, if needed
                }
                .padding(padding)
        ) {
            Column {
                Text(
                    text = "${stringResource(id = R.string.amount)}: ${borrowedTransaction.amount.formatBorrowedAmount(2)} $selectedCurrency",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold, color = Color.Yellow),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.borrower_name)}: ${borrowedTransaction.borrowerName}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = Color.Yellow),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.issue_date)}: ${borrowedTransaction.issueDate}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.due_date)}: ${borrowedTransaction.dueDate}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.comment)}: ${borrowedTransaction.comment}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Row(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { onEdit(borrowedTransaction) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit), tint = Color.White)
                }
                IconButton(onClick = { viewModel.removeBorrowedTransaction(borrowedTransaction) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete), tint = Color.White)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditBorrowedTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (BorrowedTransaction) -> Unit,
    transactionToEdit: BorrowedTransaction? = null
) {
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var borrowerName by remember { mutableStateOf(transactionToEdit?.borrowerName ?: "") }
    var issueDate by remember { mutableStateOf(transactionToEdit?.issueDate ?: getCurrentDateForBorrowed()) }
    var dueDate by remember { mutableStateOf(transactionToEdit?.dueDate ?: getCurrentDateForBorrowed()) }
    var comment by remember { mutableStateOf(transactionToEdit?.comment ?: "") }
    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    if (showIssueDatePicker) {
        BorrowedDatePickerDialog(
            onDateSelected = { selectedDate ->
                issueDate = selectedDate
                showIssueDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        BorrowedDatePickerDialog(
            onDateSelected = { selectedDate ->
                dueDate = selectedDate
                showDueDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (transactionToEdit != null) stringResource(id = R.string.edit_transaction) else stringResource(id = R.string.add_new_transaction), style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)) },
        text = {
            Column {
                TextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(id = R.string.loan_amount), style = TextStyle(color = Color.White)) },
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

                Text(
                    text = stringResource(id = R.string.loan_date),
                    style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedButton(
                    onClick = { showIssueDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = issueDate, style = TextStyle(color = Color.White))
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.repayment_date),
                    style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                OutlinedButton(
                    onClick = { showDueDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = dueDate, style = TextStyle(color = Color.White))
                }
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = borrowerName,
                    onValueChange = { borrowerName = it },
                    label = { Text(stringResource(id = R.string.borrower_name_label), style = TextStyle(color = Color.White)) },
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

                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text(stringResource(id = R.string.loan_comment), style = TextStyle(color = Color.White)) },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue != null) {
                        onSave(BorrowedTransaction(amountValue, borrowerName, issueDate, dueDate, comment, transactionToEdit?.id ?: UUID.randomUUID()))
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500)),
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
fun BorrowedDatePickerDialog(onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(formattedDate)
        },
        year, month, day
    )
    LaunchedEffect(Unit) {
        datePickerDialog.show()
    }
}

fun getCurrentDateForBorrowed(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}

class BorrowedViewModel(application: Application) : AndroidViewModel(application) {
    private val _transactions = MutableStateFlow<List<BorrowedTransaction>>(emptyList())
    val transactions: StateFlow<List<BorrowedTransaction>> = _transactions

    init {
        loadTransactions(application)
    }

    fun loadTransactions(context: Context) {
        val sharedPreferences = context.getSharedPreferences("BorrowedPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = sharedPreferences.getString("BorrowedTransactions", "[]")
        val type = object : TypeToken<List<BorrowedTransaction>>() {}.type
        val loadedTransactions: List<BorrowedTransaction> = gson.fromJson(transactionsJson, type)
        _transactions.update { loadedTransactions }
    }

    private fun saveTransactions(context: Context) {
        val sharedPreferences = context.getSharedPreferences("BorrowedPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = gson.toJson(_transactions.value)
        sharedPreferences.edit().putString("BorrowedTransactions", transactionsJson).apply()
    }

    fun addBorrowedTransaction(transaction: BorrowedTransaction) {
        _transactions.update { currentList ->
            currentList + transaction
        }
        saveTransactions(getApplication())
    }

    fun updateBorrowedTransaction(transaction: BorrowedTransaction) {
        _transactions.update { currentList ->
            currentList.map { if (it.id == transaction.id) transaction else it }
        }
        saveTransactions(getApplication())
    }

    fun removeBorrowedTransaction(transaction: BorrowedTransaction) {
        _transactions.update { currentList ->
            currentList - transaction
        }
        saveTransactions(getApplication())
    }

    fun hasOverdueTransactions(): Boolean {
        val currentDate = Date()
        return _transactions.value.any { it.dueDate.toDate().before(currentDate) }
    }
}

fun String.toDate(): Date {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.parse(this) ?: Date()
}

class BorrowedViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BorrowedViewModel::class.java)) {
            return BorrowedViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class BorrowedTransaction(
    val amount: Double,
    val borrowerName: String,
    val issueDate: String,
    val dueDate: String,
    val comment: String,
    val id: UUID = UUID.randomUUID()
)
