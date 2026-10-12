package com.example.myapplication3.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication3.components.DarkCard
import com.example.myapplication3.components.OrderBottomSheet
import com.example.myapplication3.components.StockChart
import com.example.myapplication3.data.domain.Stock
import com.example.myapplication3.data.remote.AuthService
import com.example.myapplication3.data.repository.StockRepository
import com.example.myapplication3.data.repository.TxType
import com.example.myapplication3.ui.theme.*
import java.util.Locale

@Composable
fun HomeScreen(
    stocks: List<Stock>,
    repository: StockRepository,
    onSelectStock: (Stock) -> Unit
) {
    val balance by repository.balance.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        item {
            DarkCard(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Total Available Balance", color = TextMuted, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", balance)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            }
            Text("Live Watchlist", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(stocks) { stock ->
            DarkCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onSelectStock(stock) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stock.symbol, fontWeight = FontWeight.Bold, color = TextWhite, fontSize = 16.sp)
                        Text(stock.name, color = TextMuted, fontSize = 13.sp)
                    }
                    Box(modifier = Modifier.size(width = 80.dp, height = 35.dp)) {
                        StockChart(points = stock.chartPoints, isPositive = stock.change >= 0)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("₹${String.format(Locale.US, "%.2f", stock.currentPrice)}", fontWeight = FontWeight.Bold, color = TextWhite)
                        val isPos = stock.change >= 0
                        Text(
                            text = "${if (isPos) "+" else ""}${String.format(Locale.US, "%.2f", stock.percentChange)}%",
                            color = if (isPos) AccentGreen else AccentRed,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StockDetailScreen(
    stock: Stock,
    repository: StockRepository,
    authService: AuthService,
    onBack: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    var orderType by remember { mutableStateOf(TxType.BUY) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(stock.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(stock.symbol, color = TextMuted, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "₹${String.format(Locale.US, "%.2f", stock.currentPrice)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite
        )

        Spacer(modifier = Modifier.height(24.dp))
        DarkCard(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Box(modifier = Modifier.padding(16.dp)) {
                StockChart(points = stock.chartPoints, isPositive = stock.change >= 0)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { orderType = TxType.BUY; showSheet = true },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Buy", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Button(
                onClick = { orderType = TxType.SELL; showSheet = true },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sell", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    if (showSheet) {
        OrderBottomSheet(
            stock = stock,
            type = orderType,
            repository = repository,
            authService = authService,
            onDismiss = { showSheet = false },
            onSuccess = { showSheet = false }
        )
    }
}

@Composable
fun ProfileScreen(
    stocks: List<Stock>,
    repository: StockRepository,
    authService: AuthService,
    onSignOut: () -> Unit = {}
) {
    val holdings by repository.holdings.collectAsState(initial = emptyList())
    val transactions by repository.transactions.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        item {
            Text("Portfolio Holdings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (holdings.isEmpty()) {
            item {
                Text("No holdings yet. Start buying from Home!", color = TextMuted, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            items(holdings) { h ->
                val live = stocks.find { it.symbol == h.symbol }
                val currentP = live?.currentPrice ?: h.avgPrice
                val invested = h.quantity * h.avgPrice
                val currentVal = h.quantity * currentP
                val pnl = currentVal - invested

                DarkCard(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(h.symbol, fontWeight = FontWeight.Bold, color = TextWhite)
                            Text(
                                text = "${h.quantity} shares @ ₹${String.format(Locale.US, "%.2f", h.avgPrice)}",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${String.format(Locale.US, "%.2f", currentVal)}",
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "${if (pnl >= 0) "+" else ""}₹${String.format(Locale.US, "%.2f", pnl)}",
                                color = if (pnl >= 0) AccentGreen else AccentRed,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Recent Transactions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(transactions) { tx ->
            DarkCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${tx.type} ${tx.symbol}",
                            fontWeight = FontWeight.Bold,
                            color = if (tx.type == "BUY") AccentGreen else AccentRed
                        )
                        Text(tx.timestamp, color = TextDim, fontSize = 11.sp)
                    }
                    Text(
                        text = "${tx.shares} @ ₹${String.format(Locale.US, "%.2f", tx.price)}",
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    authService.signOut()
                    onSignOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}