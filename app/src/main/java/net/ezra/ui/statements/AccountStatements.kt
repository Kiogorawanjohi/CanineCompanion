package net.ezra.ui.statements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BottomNavigation
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import net.ezra.navigation.ROUTE_DASHBOARD
import net.ezra.navigation.ROUTE_HOME
import net.ezra.navigation.ROUTE_VIEW_PROD
import java.util.Locale

private val BrandOrange = Color(0xFFF68B1E)
private val ScreenBackground = Color(0xFFF3F4F6)
private val CardWhite = Color(0xFFFFFFFF)
private val TableHeaderGrey = Color(0xFFEFEFEF)
private val ChipIdle = Color(0xFFF0F1F3)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextMuted = Color(0xFF8A9099)
private val Hairline = Color(0xFFE3E5E8)

/** Demo exchange rate used until the payouts service is wired in. */
private const val LOCAL_UNITS_PER_USD = 129.15

private const val LOCAL_CURRENCY_CODE = "KES"
private const val USD_CURRENCY_CODE = "USD"

enum class StatementStatus(val label: String, val indicator: Color) {
    OPEN("OPEN", Color(0xFF9E9E9E)),
    PAID("PAID", Color(0xFFA6E22E)),
    UNPAID("UNPAID", Color(0xFFF2D024))
}

data class AccountStatement(
    val number: String,
    val period: String,
    val status: StatementStatus,
    val payoutLocal: Double
)

/**
 * Seed data for the statements screen. Replace with the Firestore-backed payouts
 * collection once vendor settlements are live.
 */
val demoAccountStatements = listOf(
    AccountStatement("SOA-KE-260901", "01 Sep - 15 Sep 2026", StatementStatus.OPEN, 72_340.92),
    AccountStatement("SOA-KE-260816", "16 Aug - 31 Aug 2026", StatementStatus.PAID, 58_115.40),
    AccountStatement("SOA-KE-260801", "01 Aug - 15 Aug 2026", StatementStatus.PAID, 43_902.75),
    AccountStatement("SOA-KE-260716", "16 Jul - 31 Jul 2026", StatementStatus.PAID, 39_480.10),
    AccountStatement("SOA-KE-260701", "01 Jul - 15 Jul 2026", StatementStatus.UNPAID, 21_654.30)
)

private enum class StatusFilter(val label: String, val status: StatementStatus?) {
    ALL("ALL", null),
    OPEN("OPEN", StatementStatus.OPEN),
    PAID("PAID", StatementStatus.PAID),
    UNPAID("UNPAID", StatementStatus.UNPAID);

    fun matches(other: StatementStatus): Boolean = status == null || status == other
}

private fun formatMoney(amount: Double): String = String.format(Locale.US, "%,.2f", amount)

private fun AccountStatement.payoutIn(showLocal: Boolean): Double =
    if (showLocal) payoutLocal else payoutLocal / LOCAL_UNITS_PER_USD

private fun currencyCode(showLocal: Boolean): String =
    if (showLocal) LOCAL_CURRENCY_CODE else USD_CURRENCY_CODE

@Composable
fun AccountStatementsScreen(
    navController: NavHostController,
    statements: List<AccountStatement> = demoAccountStatements
) {
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    var showLocalCurrency by remember { mutableStateOf(true) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val visibleStatements = remember(statusFilter, statements) {
        statements.filter { statusFilter.matches(it.status) }
    }
    val selected = statements.getOrNull(selectedIndex)

    Scaffold(
        containerColor = ScreenBackground,
        bottomBar = { StatementsBottomBar(navController) }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            item {
                StatementsHeader()
            }

            item {
                BalanceCard(
                    statement = selected,
                    showLocalCurrency = showLocalCurrency,
                    onPrevious = { if (selectedIndex > 0) selectedIndex-- },
                    onNext = { if (selectedIndex < statements.lastIndex) selectedIndex++ }
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .height(1.dp)
                        .background(Hairline)
                )
            }

            item {
                FilterCard(
                    statusFilter = statusFilter,
                    onStatusSelected = { statusFilter = it },
                    showLocalCurrency = showLocalCurrency,
                    onCurrencySelected = { showLocalCurrency = it }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                TableHeader()
            }

            if (visibleStatements.isEmpty()) {
                item { EmptyStatements() }
            } else {
                items(visibleStatements) { statement ->
                    StatementRow(statement = statement, showLocalCurrency = showLocalCurrency)
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatementsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Account Statements",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Button(
            onClick = { },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandOrange,
                contentColor = Color.White
            )
        ) {
            Text(text = "Export Transactions", fontSize = 14.sp)
        }
    }
}

@Composable
private fun BalanceCard(
    statement: AccountStatement?,
    showLocalCurrency: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Previous statement",
                    tint = TextPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${formatMoney(statement?.payoutIn(showLocalCurrency) ?: 0.0)} " +
                        currencyCode(showLocalCurrency),
                    color = TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when (statement?.status) {
                        StatementStatus.PAID -> "Paid Statement"
                        StatementStatus.UNPAID -> "Unpaid Statement"
                        else -> "Open Statement"
                    },
                    color = TextMuted,
                    fontSize = 16.sp
                )
                if (statement != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = statement.period, color = TextMuted, fontSize = 12.sp)
                }
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next statement",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun FilterCard(
    statusFilter: StatusFilter,
    onStatusSelected: (StatusFilter) -> Unit,
    showLocalCurrency: Boolean,
    onCurrencySelected: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(BrandOrange)
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "STATUS:", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusFilter.values().forEach { filter ->
                    StatusChip(
                        filter = filter,
                        selected = filter == statusFilter,
                        onClick = { onStatusSelected(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(text = "CURRENCY:", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                CurrencyOption(
                    label = USD_CURRENCY_CODE,
                    selected = !showLocalCurrency,
                    onClick = { onCurrencySelected(false) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                CurrencyOption(
                    label = "LOCAL",
                    selected = showLocalCurrency,
                    onClick = { onCurrencySelected(true) }
                )
            }
        }
    }
}

@Composable
private fun StatusChip(filter: StatusFilter, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandOrange else ChipIdle)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filter.status?.let { status ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(status.indicator)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = filter.label,
            color = if (selected) Color.White else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CurrencyOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = BrandOrange,
                unselectedColor = TextPrimary
            )
        )
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 15.sp,
            modifier = Modifier.clickable(onClick = onClick)
        )
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(TableHeaderGrey)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Period / Number / Status", color = TextMuted, fontSize = 15.sp)
        Text(text = "Payout", color = TextMuted, fontSize = 15.sp)
    }
}

@Composable
private fun StatementRow(statement: AccountStatement, showLocalCurrency: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(CardWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = statement.period, color = TextPrimary, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = statement.number, color = TextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statement.status.indicator)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statement.status.label,
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = "${formatMoney(statement.payoutIn(showLocalCurrency))} " +
                    currencyCode(showLocalCurrency),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Hairline)
        )
    }
}

@Composable
private fun EmptyStatements() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(CardWhite)
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "No Account Statements to list.", color = TextPrimary, fontSize = 16.sp)
    }
}

@Composable
private fun StatementsBottomBar(navController: NavHostController) {
    BottomNavigation(
        elevation = 10.dp,
        backgroundColor = Color.White
    ) {
        BottomNavigationItem(
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "", tint = TextPrimary) },
            label = { Text(text = "Home", color = TextPrimary) },
            selected = false,
            onClick = { navController.navigate(ROUTE_HOME) }
        )

        BottomNavigationItem(
            icon = { Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "", tint = TextPrimary) },
            label = { Text(text = "Orders", color = TextPrimary) },
            selected = false,
            onClick = { navController.navigate(ROUTE_VIEW_PROD) }
        )

        BottomNavigationItem(
            icon = { Icon(imageVector = Icons.Default.List, contentDescription = "", tint = TextPrimary) },
            label = { Text(text = "Manage Products", color = TextPrimary) },
            selected = false,
            onClick = { navController.navigate(ROUTE_VIEW_PROD) }
        )

        BottomNavigationItem(
            icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "", tint = TextPrimary) },
            label = { Text(text = "Menu", color = TextPrimary) },
            selected = true,
            onClick = { navController.navigate(ROUTE_DASHBOARD) }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountStatementsScreenPreview() {
    AccountStatementsScreen(navController = rememberNavController())
}
