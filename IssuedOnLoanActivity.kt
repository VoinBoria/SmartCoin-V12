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

class IssuedOnLoanActivity : ComponentActivity() {
    private val loanViewModel: LoanViewModel by viewModels { LoanViewModelFactory(application) }
    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }
    private fun getSelectedCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return sharedPreferences.getString("currency", "UAH") ?: "UAH"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var showOverdueMessage by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    loanViewModel.loadTransactions(application)
                    if (loanViewModel.hasOverdueTransactions()) {
                        delay(500)
                        showOverdueMessage = true
                        delay(3000)
                        showOverdueMessage = false
                    }
                }

                val selectedCurrency = getSelectedCurrency(this)

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@IssuedOnLoanActivity, MainActivity::class.java).apply {
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
                                title = { Text(stringResource(id = R.string.issued_on_loan), color = Color.White) },
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
                                IssuedOnLoanScreen(viewModel = loanViewModel, selectedCurrency = selectedCurrency)

                                // Анімоване повідомлення про прострочені позичання
                                AnimatedVisibility(
                                    visible = showOverdueMessage,
                                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 100.dp) // збільшено значення відступу
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f) // встановлено ширину на 90% від ширини екрану
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color.Red.copy(alpha = 0.8f), Color.Transparent)
                                                ),
                                                shape = RoundedCornerShape(16.dp) // додано зглажені кути
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Text(stringResource(id = R.string.overdue_loan_message), color = Color.White)
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
fun IssuedOnLoanScreen(
    viewModel: LoanViewModel,
    selectedCurrency: String,
    modifier: Modifier = Modifier
) {
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<LoanTransaction?>(null) }
    val transactions by viewModel.transactions.collectAsState()

    // Обчислюємо загальну суму позичань
    val totalLoaned = transactions.sumOf { it.amount }

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
                    items(transactions) { loanTransaction ->
                        LoanTransactionRow(loanTransaction, viewModel, selectedCurrency, onEdit = { transactionToEdit = it })
                    }
                }

                // Додаємо підсумок знизу екрана
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = padding)
                ) {
                    Text(
                        text = stringResource(id = R.string.total_issued_on_loan),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = padding)
                    )
                    Text(
                        text = "${totalLoaned.formatLoanAmount(2)} $selectedCurrency",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            FloatingActionButton(
                onClick = { showAddLoanDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(padding),
                containerColor = Color(0xFFDC143C)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_transaction), tint = Color.White)
            }

            if (showAddLoanDialog || transactionToEdit != null) {
                AddOrEditLoanTransactionDialog(
                    onDismiss = {
                        showAddLoanDialog = false
                        transactionToEdit = null
                    },
                    onSave = { newTransaction ->
                        if (transactionToEdit != null) {
                            viewModel.updateLoanTransaction(newTransaction)
                        } else {
                            viewModel.addLoanTransaction(newTransaction)
                        }
                        transactionToEdit = null
                        showAddLoanDialog = false
                    },
                    transactionToEdit = transactionToEdit
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LoanTransactionRow(loanTransaction: LoanTransaction, viewModel: LoanViewModel, selectedCurrency: String, onEdit: (LoanTransaction) -> Unit) {
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
                    text = "${stringResource(id = R.string.amount)}: ${loanTransaction.amount.formatLoanAmount(2)} $selectedCurrency",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Bold, color = Color.Red),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.borrower_name)}: ${loanTransaction.borrowerName}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = Color.Red),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.issue_date)}: ${loanTransaction.issueDate}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.due_date)}: ${loanTransaction.dueDate}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "${stringResource(id = R.string.comment)}: ${loanTransaction.comment}",
                    style = TextStyle(fontSize = fontSize, fontWeight = FontWeight.Normal, color = Color.LightGray),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Row(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { onEdit(loanTransaction) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit), tint = Color.White)
                }
                IconButton(onClick = { viewModel.removeLoanTransaction(loanTransaction) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete), tint = Color.White)
                }
            }
        }
    }
}
fun Double.formatLoanAmount(digits: Int): String {
    return "%.${digits}f".format(this)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditLoanTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (LoanTransaction) -> Unit,
    transactionToEdit: LoanTransaction? = null
) {
    var amount by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var borrowerName by remember { mutableStateOf(transactionToEdit?.borrowerName ?: "") }
    var issueDate by remember { mutableStateOf(transactionToEdit?.issueDate ?: getCurrentDateForLoan()) }
    var dueDate by remember { mutableStateOf(transactionToEdit?.dueDate ?: getCurrentDateForLoan()) }
    var comment by remember { mutableStateOf(transactionToEdit?.comment ?: "") }
    var showIssueDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    if (showIssueDatePicker) {
        LoanDatePickerDialog(
            onDateSelected = { selectedDate ->
                issueDate = selectedDate
                showIssueDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        LoanDatePickerDialog(
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
                        onSave(
                            LoanTransaction(
                                amount = amountValue,
                                borrowerName = borrowerName,
                                issueDate = issueDate,
                                dueDate = dueDate,
                                comment = comment,
                                id = transactionToEdit?.id ?: UUID.randomUUID()
                            )
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC143C)),
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
fun LoanDatePickerDialog(onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "${selectedYear}-${selectedMonth + 1}-${selectedDay}"
            onDateSelected(formattedDate)
        },
        year, month, day
    )
    LaunchedEffect(Unit) {
        datePickerDialog.show()
    }
}

fun getCurrentDateForLoan(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}

class LoanViewModel(application: Application) : AndroidViewModel(application) {
    private val _transactions = MutableStateFlow<List<LoanTransaction>>(emptyList())
    val transactions: StateFlow<List<LoanTransaction>> = _transactions

    init {
        loadTransactions(application)
    }

    fun loadTransactions(context: Context) { // Зміна видимості на публічну
        val sharedPreferences = context.getSharedPreferences("LoanPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = sharedPreferences.getString("LoanTransactions", "[]")
        val type = object : TypeToken<List<LoanTransaction>>() {}.type
        val loadedTransactions: List<LoanTransaction> = gson.fromJson(transactionsJson, type)
        _transactions.update { loadedTransactions }
    }

    private fun saveTransactions(context: Context) {
        val sharedPreferences = context.getSharedPreferences("LoanPrefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val transactionsJson = gson.toJson(_transactions.value)
        sharedPreferences.edit().putString("LoanTransactions", transactionsJson).apply()
    }

    fun addLoanTransaction(transaction: LoanTransaction) {
        _transactions.update { currentList ->
            currentList + transaction
        }
        saveTransactions(getApplication())
    }

    fun updateLoanTransaction(transaction: LoanTransaction) {
        _transactions.update { currentList ->
            currentList.map { if (it.id == transaction.id) transaction else it }
        }
        saveTransactions(getApplication())
    }

    fun removeLoanTransaction(transaction: LoanTransaction) {
        _transactions.update { currentList ->
            currentList - transaction
        }
        saveTransactions(getApplication())
    }

    fun hasOverdueTransactions(): Boolean {
        val currentDate = Date()
        return _transactions.value.any { it.dueDate.toLoanDate().before(currentDate) }
    }
}

fun String.toLoanDate(): Date {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.parse(this) ?: Date()
}
class LoanViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoanViewModel::class.java)) {
            return LoanViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class LoanTransaction(
    val amount: Double,
    val borrowerName: String,
    val issueDate: String,
    val dueDate: String,
    val comment: String,
    val id: UUID = UUID.randomUUID()
)
