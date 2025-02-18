package com.serhio.homeaccountingapp

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.pager.ExperimentalPagerApi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun IncomeExpenseChart(
    incomes: Map<String, Double>,
    expenses: Map<String, Double>,
    totalIncomes: Double,
    totalExpenses: Double,
    currency: String // Додаємо параметр для валюти
) {
    val emptyIncomeChartColor = Color(0x8032CD32).copy(alpha = 0.5f) // Прозоро зелений колір для доходів
    val emptyExpenseChartColor = Color(0x80FF0000).copy(alpha = 0.5f) // Прозоро червоний колір для витрат
    val separatorColor = Color.DarkGray

    BoxWithConstraints {
        val screenWidth = maxWidth
        val isSmallScreen = screenWidth < 360.dp
        val chartSize = if (isSmallScreen) 100.dp else 176.dp
        val strokeWidth = if (isSmallScreen) 16f else 50f
        val padding = if (isSmallScreen) 8.dp else 16.dp

        val scope = rememberCoroutineScope()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!isSmallScreen) Modifier.widthIn(max = 400.dp) else Modifier)
        ) {
            val incomeColors = generateDistinctColors(incomes.size.takeIf { it > 0 } ?: 1, excludeRed = true)
            val expenseColors = generateDistinctColors(expenses.size.takeIf { it > 0 } ?: 1, excludeGreen = true)

            val pagerState = rememberPagerState()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (pagerState.currentPage == 0) Color.Gray else Color.Transparent
                        )
                    ) {
                        Text(stringResource(id = R.string.incomes), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (pagerState.currentPage == 1) Color.Gray else Color.Transparent
                        )
                    ) {
                        Text(stringResource(id = R.string.expenses), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                HorizontalPager(state = pagerState, count = 2, modifier = Modifier.fillMaxWidth()) { page ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (page == 0) {
                            LegendColumn(
                                items = incomes.keys.toList(),
                                colors = incomeColors,
                                maxHeight = chartSize,
                                modifier = Modifier.padding(end = padding)
                            )
                            GradientDonutChart(
                                values = incomes.values.toList(),
                                maxAmount = totalIncomes,
                                chartSize = chartSize,
                                colors = incomeColors,
                                strokeWidth = strokeWidth,
                                emptyChartColor = emptyIncomeChartColor, // Передаємо прозоро зелений колір
                                separatorColor = separatorColor,
                                modifier = Modifier.padding(start = padding)
                            )
                        } else {
                            LegendColumn(
                                items = expenses.keys.toList(),
                                colors = expenseColors,
                                maxHeight = chartSize,
                                modifier = Modifier.padding(end = padding)
                            )
                            GradientDonutChart(
                                values = expenses.values.toList(),
                                maxAmount = totalExpenses,
                                chartSize = chartSize,
                                colors = expenseColors,
                                strokeWidth = strokeWidth,
                                emptyChartColor = emptyExpenseChartColor, // Передаємо прозоро червоний колір
                                separatorColor = separatorColor,
                                modifier = Modifier.padding(start = padding)
                            )
                        }
                    }
                }
            }

            HorizontalPagerIndicator(
                pagerState = pagerState,
                activeColor = Color.White,
                inactiveColor = Color.Gray,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
        }
    }
}
@Composable
fun LegendColumn(
    items: List<String>,
    colors: List<Color>,
    maxHeight: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(maxHeight)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        items.forEachIndexed { index, category ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = colors[index],
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 2,
                    softWrap = true, // Додаємо параметр для переносу тексту
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(modifier = Modifier.height(16.dp)) // Додаємо відступ знизу, щоб уникнути обрізання кольорової крапки
    }
}
@Composable
fun GradientDonutChart(
    values: List<Double>,
    maxAmount: Double,
    chartSize: Dp,
    colors: List<Color>,
    strokeWidth: Float,
    emptyChartColor: Color,
    separatorColor: Color = Color.DarkGray, // Темніший колір для розділової лінії
    modifier: Modifier = Modifier,
    gapSize: Float = 2f // Розмір розділової лінії
) {
    Box(
        modifier = modifier
            .size(chartSize)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.size(chartSize)) {
            val sweepAngles = values.map { value ->
                (value / maxAmount * 360).toFloat()
            }
            var startAngle = -90f
            for (i in values.indices) {
                // Розділова лінія
                drawArc(
                    color = separatorColor,
                    startAngle = startAngle,
                    sweepAngle = gapSize,
                    useCenter = false,
                    style = Stroke(strokeWidth)
                )
                // Сегмент
                drawArc(
                    color = colors[i],
                    startAngle = startAngle + gapSize,
                    sweepAngle = sweepAngles[i] - gapSize,
                    useCenter = false,
                    style = Stroke(strokeWidth)
                )
                startAngle += sweepAngles[i]
            }
            if (values.isEmpty()) {
                drawArc(
                    color = emptyChartColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(strokeWidth)
                )
            }
        }
    }
}


fun generateDistinctColors(count: Int, excludeRed: Boolean = false, excludeGreen: Boolean = false): List<Color> {
    val predefinedColors = listOf(
        Color(0xFFe6194B), // red
        Color(0xFF3cb44b), // green
        Color(0xFFffe119), // yellow
        Color(0xFF4363d8), // blue
        Color(0xFFf58231), // orange
        Color(0xFF911eb4), // purple
        Color(0xFF42d4f4), // cyan
        Color(0xFFf032e6), // magenta
        Color(0xFFbfef45), // lime
        Color(0xFFfabebe), // pink
        Color(0xFF469990), // teal
        Color(0xFFe6beff), // lavender
        Color(0xFF9A6324), // brown
        Color(0xFFfffac8), // beige
        Color(0xFF800000), // maroon
        Color(0xFFaaffc3), // mint
        Color(0xFF808000), // olive
        Color(0xFFffd8b1), // coral
        Color(0xFF000075), // navy
        Color(0xFFa9a9a9)  // gray
    )

    val filteredColors = predefinedColors.filter {
        (!excludeRed || it != Color(0xFFe6194B)) && (!excludeGreen || it != Color(0xFF3cb44b))
    }

    return List(count) { filteredColors[it % filteredColors.size] }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ExpandableButtonWithAmount(
    text: String,
    amount: Double,
    gradientColors: List<Color>,
    isExpanded: Boolean,
    onClick: () -> Unit,
    currency: String, // Додаємо параметр для валюти
    textColor: Color = Color.White, // Додаємо параметр для кольору тексту
    fontWeight: FontWeight = FontWeight.Bold, // Додаємо параметр для жирності шрифту
    fontSize: TextUnit = 24.sp // Збільшення шрифту
) {
    BoxWithConstraints {
        val screenWidth = maxWidth
        val padding = if (screenWidth < 360.dp) 8.dp else 16.dp

        val gradient = Brush.horizontalGradient(
            colors = listOf(
                gradientColors[0],
                gradientColors[1]
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp) // Обмежуємо максимальну ширину кнопки
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(gradient)
                .clickable(onClick = onClick)
                .padding(horizontal = padding) // Додаємо відступи з боків
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontWeight = fontWeight,
                    fontSize = fontSize
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${"%.2f".format(amount)} $currency", // Додаємо валюту
                        color = textColor,
                        fontWeight = fontWeight,
                        fontSize = fontSize
                    )
                    Spacer(modifier = Modifier.width(4.dp)) // Мінімальний відступ від тексту до іконки
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = textColor
                    )
                }
            }
        }
    }
}
@Composable
fun IncomeList(
    incomes: Map<String, Double>,
    currency: String, // Додаємо параметр для валюти
    onCategoryClick: (String) -> Unit // Додаємо параметр для обробки переходу
) {
    // Сортування категорій за сумою у спадному порядку
    val sortedIncomes = incomes.toList().sortedByDescending { (_, amount) -> amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp) // Обмеження висоти списку
    ) {
        items(sortedIncomes) { (category, amount) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .clickable { onCategoryClick(category) } // Додаємо обробку натискання
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                    Text(
                        text = "${"%.2f".format(amount)} $currency", // Додаємо валюту
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpensesList(
    expenses: Map<String, Double>,
    currency: String, // Додаємо параметр для валюти
    onCategoryClick: (String) -> Unit // Додаємо параметр для обробки переходу
) {
    // Сортування категорій за сумою у зростаючому порядку
    val sortedExpenses = expenses.toList().sortedBy { (_, amount) -> amount }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp) // Обмеження висоти списку
    ) {
        items(sortedExpenses) { (category, amount) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .clickable { onCategoryClick(category) } // Додаємо обробку натискання
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                    Text(
                        text = "${"%.2f".format(amount)} $currency", // Додаємо валюту
                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White)
                    )
                }
            }
        }
    }
}


@Composable
fun CategoryItem(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    gradientColors: List<Color>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = gradientColors,
                    startX = 0f,
                    endX = Float.POSITIVE_INFINITY
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            icon() // Іконка зліва
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White
                )
            )
        }
    }
}
@Composable
fun BalanceDisplay(balance: Double, currency: String, modifier: Modifier = Modifier) { // Додаємо параметр для валюти
    val formattedBalance = "%,.2f".format(balance).replace(",", " ")
    val balanceColor = when {
        balance < 0 -> Color.Red
        balance > 0 -> Color.Green
        else -> Color.White
    }

    Column(
        modifier = modifier
            .padding(start = 16.dp, bottom = 25.dp)
            .width(IntrinsicSize.Max)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.height(56.dp) // Встановлюємо висоту контейнера для вирівнювання з кнопками
        ) {
            Text(
                text = stringResource(id = R.string.balance),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 24.sp, // Збільшуємо шрифт для заголовка
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 4.dp) // Налаштовуємо відступ для вирівнювання
            )
            Text(
                text = "$formattedBalance $currency", // Додаємо валюту
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 22.sp, // Збільшуємо шрифт для суми
                    color = balanceColor, // Колір тексту залежить від суми
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.Start) // Вирівнюємо текст по лівому краю
            )
        }
    }
}