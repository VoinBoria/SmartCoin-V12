package com.serhio.homeaccountingapp

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serhio.homeaccountingapp.ui.theme.HomeAccountingAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class TaskActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    private fun <T> navigateToActivity(activityClass: Class<T>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("tasks_prefs", MODE_PRIVATE)

        val viewModel: TaskViewModel by viewModels {
            TaskViewModelFactory(sharedPreferences, gson, this)
        }

        setContent {
            HomeAccountingAppTheme {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var showOverdueMessage by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.loadTasks() // Завантаження задач
                    if (viewModel.hasOverdueTasks()) { // Перевірка прострочених задач
                        delay(500) // Затримка перед показом повідомлення
                        showOverdueMessage = true
                        delay(5000) // Затримка на 5 секунд
                        showOverdueMessage = false
                    }
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        DrawerContent(
                            onNavigateToMainActivity = {
                                val intent = Intent(this@TaskActivity, MainActivity::class.java).apply {
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
                                title = { Text(stringResource(id = R.string.task_list_title), color = Color.White) },
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
                                TaskScreen(viewModel)

                                // Анімоване повідомлення про прострочені завдання
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
                                                    listOf(Color.Yellow.copy(alpha = 0.8f), Color.Transparent)
                                                ),
                                                shape = RoundedCornerShape(16.dp) // додано зглажені кути
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Text(stringResource(id = R.string.overdue_task_message), color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        checkNotificationPermission()
        checkExactAlarmPermission()
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE)
        val language = sharedPreferences.getString("language", "UK") ?: "UK"
        updateLocale(this, language)
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showPermissionDialog(Manifest.permission.POST_NOTIFICATIONS, REQUEST_CODE_NOTIFICATION_PERMISSION)
            }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivityForResult(intent, REQUEST_CODE_EXACT_ALARM_PERMISSION)
            }
        }
    }

    private fun showPermissionDialog(permission: String, requestCode: Int) {
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setPositiveButton(R.string.allow) { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, you can show notifications
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, R.string.notification_permission_denied_message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1
        private const val REQUEST_CODE_EXACT_ALARM_PERMISSION = 2
    }
}

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val startDate: Date,
    val endDate: Date,
    var isCompleted: Boolean = false,
    var reminder: String? = null // New field for reminder
)

class TaskViewModel(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val context: Context
) : ViewModel() {
    private val _tasks = mutableStateListOf<Task>()
    val tasks: List<Task> = _tasks

    @RequiresApi(Build.VERSION_CODES.S)
    fun addTask(task: Task) {
        _tasks.add(task)
        saveTasks()
        scheduleTaskReminders(task) // Запланувати нагадування під час додавання задачі
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun updateTask(updatedTask: Task) {
        val index = _tasks.indexOfFirst { it.id == updatedTask.id }
        if (index != -1) {
            _tasks[index] = updatedTask
            saveTasks()
            scheduleTaskReminders(updatedTask) // Запланувати нагадування після оновлення задачі
        }
    }


    fun removeTask(task: Task) {
        _tasks.remove(task)
        saveTasks()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun toggleTaskCompletion(task: Task) {
        val index = _tasks.indexOf(task)
        if (index != -1) {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            _tasks[index] = updatedTask
            saveTasks()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun loadTasks() {
        val tasksJson = sharedPreferences.getString("tasks", "[]")
        val type = object : TypeToken<List<Task>>() {}.type
        val loadedTasks: List<Task> = gson.fromJson(tasksJson, type)
        _tasks.clear()
        _tasks.addAll(loadedTasks)

        // Schedule reminders for all loaded tasks
        _tasks.forEach { scheduleTaskReminders(it) }
    }

    private fun saveTasks() {
        val editor = sharedPreferences.edit()
        val tasksJson = gson.toJson(_tasks)
        editor.putString("tasks", tasksJson)
        editor.apply()
    }

    fun hasOverdueTasks(): Boolean {
        val currentDate = Date()
        return _tasks.any { !it.isCompleted && it.endDate.before(currentDate) }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleTaskReminders(task: Task) {
        val workManager = WorkManager.getInstance(context)

        // Schedule reminder for task start time
        val startReminderData = workDataOf(
            "TASK_TITLE" to task.title,
            "REMINDER_TIME" to "на початку"
        )
        val startReminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(task.startDate.time - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .setInputData(startReminderData)
            .build()

        workManager.enqueue(startReminderRequest)

        // Schedule advance reminder if a reminder time is selected
        if (task.reminder != context.getString(R.string.reminder_none)) {
            val reminderTime = when (task.reminder) {
                context.getString(R.string.reminder_10_minutes) -> task.startDate.time - 10 * 60 * 1000
                context.getString(R.string.reminder_30_minutes) -> task.startDate.time - 30 * 60 * 1000
                context.getString(R.string.reminder_1_hour) -> task.startDate.time - 60 * 60 * 1000
                context.getString(R.string.reminder_1_day) -> task.startDate.time - 24 * 60 * 60 * 1000
                context.getString(R.string.reminder_1_week) -> task.startDate.time - 7 * 24 * 60 * 60 * 1000
                else -> task.startDate.time
            }

            if (reminderTime > System.currentTimeMillis()) {
                val advanceReminderData = workDataOf(
                    "TASK_TITLE" to task.title,
                    "REMINDER_TIME" to task.reminder
                )
                val advanceReminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(reminderTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(advanceReminderData)
                    .build()

                workManager.enqueue(advanceReminderRequest)
            }
        }
    }
}
class TaskViewModelFactory(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(sharedPreferences, gson, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var showSaveMessage by remember { mutableStateOf(false) }
    var showReminderMessage by remember { mutableStateOf(false) }
    var reminderTaskTitle by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = Color(0xFF228B22),
            ) {
                Text("+", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .padding(bottom = 72.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TaskList(viewModel.tasks, viewModel::toggleTaskCompletion, viewModel::removeTask, onEditTask = { task ->
                        editingTask = task
                        showAddTaskDialog = true
                    })
                }

                if (showAddTaskDialog) {
                    AddTaskDialog(
                        taskToEdit = editingTask,
                        onDismiss = {
                            showAddTaskDialog = false
                            editingTask = null
                        },
                        onSave = { task ->
                            if (editingTask != null) {
                                viewModel.updateTask(task)
                            } else {
                                viewModel.addTask(task)
                            }

                            showAddTaskDialog = false
                            editingTask = null
                            showSaveMessage = true

                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                            // Schedule reminder for task start time
                            scheduleReminder(alarmManager, context, task.startDate.time, task.title, "START", task.id.hashCode(), "на початку")
                        }
                    )
                }

                if (showSaveMessage) {
                    LaunchedEffect(Unit) {
                        delay(3000)
                        showSaveMessage = false
                    }

                    AnimatedVisibility(
                        visible = showSaveMessage,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(500)),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(500)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFF004d00).copy(alpha = 0.8f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(stringResource(id = R.string.task_saved_message), color = Color.White)
                        }
                    }
                }

                if (showReminderMessage) {
                    LaunchedEffect(Unit) {
                        delay(3000)
                        showReminderMessage = false
                    }

                    AnimatedVisibility(
                        visible = showReminderMessage,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(500)),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(500)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFFFFFF00).copy(alpha = 0.8f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(stringResource(id = R.string.task_reminder_message, reminderTaskTitle), color = Color.Black)
                        }
                    }
                }
            }
        }
    )
}
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    taskToEdit: Task? = null,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit
) {
    val context = LocalContext.current
    val reminderOptions = listOf(
        R.string.reminder_none,
        R.string.reminder_10_minutes,
        R.string.reminder_30_minutes,
        R.string.reminder_1_hour,
        R.string.reminder_1_day,
        R.string.reminder_1_week
    )
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var startDate by remember { mutableStateOf(taskToEdit?.startDate ?: Date()) }
    var startTime by remember { mutableStateOf(taskToEdit?.startDate ?: Date()) }
    var endDate by remember { mutableStateOf(taskToEdit?.endDate ?: Date()) }
    var endTime by remember { mutableStateOf(taskToEdit?.endDate ?: Date()) }
    var reminder by remember { mutableStateOf(taskToEdit?.reminder ?: context.getString(R.string.reminder_10_minutes)) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }

    LaunchedEffect(showStartDatePicker) {
        if (!showStartDatePicker) {
            showStartTimePicker = false
        }
    }

    LaunchedEffect(showStartTimePicker) {
        if (!showStartTimePicker) {
            showStartDatePicker = false
        }
    }

    LaunchedEffect(showEndDatePicker) {
        if (!showEndDatePicker) {
            showEndTimePicker = false
        }
    }

    LaunchedEffect(showEndTimePicker) {
        if (!showEndTimePicker) {
            showEndDatePicker = false
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                startDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                showStartDatePicker = false
                showStartTimePicker = true
            },
            startDate.year + 1900,
            startDate.month,
            startDate.date
        ).show()
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startTime = Calendar.getInstance().apply {
                    time = startDate
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }.time
                showStartTimePicker = false
            },
            startTime.hours,
            startTime.minutes,
            true
        ).show()
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                endDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                showEndDatePicker = false
                showEndTimePicker = true
            },
            endDate.year + 1900,
            endDate.month,
            endDate.date
        ).show()
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                endTime = Calendar.getInstance().apply {
                    time = endDate
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }.time
                showEndTimePicker = false
            },
            endTime.hours,
            endTime.minutes,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(if (taskToEdit == null) stringResource(id = R.string.add_task) else stringResource(id = R.string.edit_task), color = Color.White)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.task_title), color = Color.White) },
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.task_description), color = Color.White) },
                    textStyle = TextStyle(color = Color.White),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.White,
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    singleLine = false,
                    maxLines = 10
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFF4CAF50)), shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = stringResource(id = R.string.start_date) + ": " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(startTime),
                        color = Color(0xFF4CAF50),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showReminderMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color.Yellow), shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text(
                        text = stringResource(id = R.string.reminder) + ": " + reminder,
                        color = Color.Yellow,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                DropdownMenu(
                    expanded = showReminderMenu,
                    onDismissRequest = { showReminderMenu = false },
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    reminderOptions.forEach { resId ->
                        DropdownMenuItem(
                            onClick = {
                                reminder = context.getString(resId)
                                showReminderMenu = false
                            },
                            text = { Text(stringResource(id = resId), color = Color.White) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        val task = Task(
                            taskToEdit?.id ?: UUID.randomUUID().toString(),
                            title,
                            description.ifEmpty { null },
                            startTime,
                            endTime,
                            reminder = reminder
                        )
                        onSave(task)
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                        // Schedule reminder based on selected option
                        val reminderTime = when (reminder) {
                            context.getString(R.string.reminder_10_minutes) -> task.startDate.time - 10 * 60 * 1000
                            context.getString(R.string.reminder_30_minutes) -> task.startDate.time - 30 * 60 * 1000
                            context.getString(R.string.reminder_1_hour) -> task.startDate.time - 60 * 60 * 1000
                            context.getString(R.string.reminder_1_day) -> task.startDate.time - 24 * 60 * 60 * 1000
                            context.getString(R.string.reminder_1_week) -> task.startDate.time - 7 * 24 * 60 * 60 * 1000
                            else -> task.startDate.time
                        }
                        scheduleReminder(alarmManager, context, reminderTime, task.title, "REMINDER", task.id.hashCode(), reminder)
                    } else {
                        // Show error message
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text(stringResource(id = R.string.save), color = Color.Green)
            }
        },
        dismissButton = {
            Button(
                onClick = { onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text(stringResource(id = R.string.cancel), color = Color.Red)
            }
        },
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Gray.copy(alpha = 0.8f), Color.Black.copy(alpha = 0.8f))
                ),
                shape = MaterialTheme.shapes.medium
            )
            .border(BorderStroke(1.dp, Color.White)),
        containerColor = Color.Transparent,
        textContentColor = Color.White
    )
}
@Composable
fun TaskList(
    tasks: List<Task>,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit // Add this parameter for editing a task
) {
    LazyColumn {
        items(tasks) { task ->
            TaskItem(task, onToggleCompletion, onDeleteTask, onEditTask) // Pass the onEditTask callback
        }
    }
}
@Composable
fun TaskItem(
    task: Task,
    onToggleCompletion: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onEditTask: (Task) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E1E1E).copy(alpha = 0.8f))
            .padding(16.dp)
            .clickable { onEditTask(task) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = Color.White
                )
            )
            task.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = Color.White
                    )
                )
            }
            Text(
                text = stringResource(id = R.string.start_date) + ": " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.startDate),
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
            )
            Text(
                text = stringResource(id = R.string.task_start_time_label) + ": " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(task.startDate),
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
            )

            if (task.isCompleted) {
                Text(
                    text = stringResource(id = R.string.completed),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Green)
                )
            }
        }
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggleCompletion(task) },
            colors = CheckboxDefaults.colors(checkedColor = Color.Green)
        )
        IconButton(onClick = { onDeleteTask(task) }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete), tint = Color.White)
        }
    }
}
@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("ScheduleExactAlarm")
fun scheduleReminder(alarmManager: AlarmManager, context: Context, triggerAtMillis: Long, taskTitle: String, action: String, requestCode: Int, reminderTime: String) {
    if (triggerAtMillis > System.currentTimeMillis()) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("TASK_TITLE", taskTitle)
            putExtra("ACTION", action)
            putExtra("REMINDER_TIME", reminderTime)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra("TASK_TITLE")
        val action = intent.getStringExtra("ACTION")
        val reminderTime = intent.getStringExtra("REMINDER_TIME")

        val message = when (action) {
            "START" -> taskTitle ?: "Задача"
            "REMINDER" -> context.getString(R.string.task_reminder_message, taskTitle ?: "task", reminderTime ?: "")
            else -> context.getString(R.string.task_reminder_message, taskTitle ?: "task", reminderTime ?: "")
        }

        showNotification(context, message)
        vibratePhone(context)
    }

    private fun showNotification(context: Context, message: String?) {
        val channelId = "task_reminder_channel"
        val channelName = "Task Reminder"
        val importance = NotificationManager.IMPORTANCE_HIGH

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Канал для нагадувань про задачі"
            }
            val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, TaskActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder))
            .setContentText(message ?: context.getString(R.string.reminder))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            requestNotificationPermission(context)
        }
    }

    private fun vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun requestNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, R.string.notification_permission_denied_message, Toast.LENGTH_LONG).show()
        }
    }
}
