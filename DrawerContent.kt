// DrawerContent.kt
package com.serhio.homeaccountingapp

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DrawerContent(
    onNavigateToMainActivity: () -> Unit,
    onNavigateToIncomes: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIssuedOnLoan: () -> Unit,
    onNavigateToBorrowed: () -> Unit,
    onNavigateToAllTransactionIncome: () -> Unit,
    onNavigateToAllTransactionExpense: () -> Unit,
    onNavigateToBudgetPlanning: () -> Unit,
    onNavigateToTaskActivity: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val iconSize = when {
        screenWidthDp < 360.dp -> 20.dp  // Small screens
        screenWidthDp < 600.dp -> 24.dp  // Normal screens
        else -> 28.dp  // Large screens
    }
    val textSize = if (screenWidthDp < 360.dp) 14.sp else 18.sp  // Adjust text size for small screens
    val paddingSize = if (screenWidthDp < 360.dp) 8.dp else 16.dp  // Adjust padding for small screens

    val drawerWidth = when {
        screenWidthDp < 600.dp -> screenWidthDp * 0.8f
        else -> 240.dp  // Set a fixed width for larger screens
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(drawerWidth)
            .background(Color(0xFF1E1E1E).copy(alpha = 0.9f))
            .padding(paddingSize)
            .verticalScroll(rememberScrollState())  // Enable vertical scrolling
    ) {
        Text(
            text = stringResource(id = R.string.menu),
            style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontSize = textSize)
        )
        Spacer(modifier = Modifier.height(20.dp))  // Adjust space for small screens
        Column(modifier = Modifier.fillMaxWidth()) {
            CategoryItem(
                text = stringResource(id = R.string.main_menu),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = stringResource(id = R.string.main_menu_icon),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToMainActivity,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = stringResource(id = R.string.incomes),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_income),
                        contentDescription = stringResource(id = R.string.incomes_icon),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToIncomes,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = stringResource(id = R.string.expenses),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_expense),
                        contentDescription = stringResource(id = R.string.expenses_icon),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToExpenses,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = stringResource(id = R.string.all_income_transactions),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_all_income_transactions),
                        contentDescription = stringResource(id = R.string.all_income_transactions),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToAllTransactionIncome,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = stringResource(id = R.string.all_expense_transactions),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_all_expense_transactions),
                        contentDescription = stringResource(id = R.string.all_expense_transactions),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToAllTransactionExpense,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = stringResource(id = R.string.issued_on_loan),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_loan_issued),
                        contentDescription = stringResource(id = R.string.issued_on_loan_icon),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToIssuedOnLoan,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = stringResource(id = R.string.borrowed),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_loan_borrowed),
                        contentDescription = stringResource(id = R.string.borrowed_icon),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToBorrowed,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = stringResource(id = R.string.budget_planning),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_budget_planning),
                        contentDescription = stringResource(id = R.string.budget_planning_icon),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToBudgetPlanning,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryItem(
                text = stringResource(id = R.string.task_manager),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_task),
                        contentDescription = stringResource(id = R.string.task_manager),
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                },
                onClick = onNavigateToTaskActivity,
                gradientColors = listOf(
                    Color(0xFF000000).copy(alpha = 0.7f),
                    Color(0xFF2E2E2E).copy(alpha = 0.7f)
                )
            )
        }
    }
}
