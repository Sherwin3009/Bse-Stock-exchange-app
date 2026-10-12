package com.example.myapplication3.data.repository

import com.example.myapplication3.data.domain.Stock
import com.example.myapplication3.data.local.AppDatabase
import com.example.myapplication3.data.local.entity.HoldingEntity
import com.example.myapplication3.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class TxType { BUY, SELL }

class StockRepository(private val database: AppDatabase) {

    private val _balance = MutableStateFlow(980763.06)
    val balance = _balance.asStateFlow()

    val holdings: Flow<List<HoldingEntity>> = database.holdingDao().getAllHoldings()
    val transactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactions()

    private val baseStocks = listOf(
        Stock("TECHM", "Tech Mahindra", 1674.30, 1710.70, 1694.00, 1660.70, "1.5M", listOf(35f, 32f, 40f, 38f, 55f, 50f, 65f)),
        Stock("TITAN", "Titan Company", 3499.70, 3467.80, 3520.80, 3467.80, "4.4L", listOf(20f, 30f, 45f, 40f, 65f, 75f, 90f)),
        Stock("KOTAKBANK", "Kotak Bank", 2151.10, 2132.30, 2154.70, 2135.10, "11.0L", listOf(40f, 45f, 42f, 55f, 50f, 65f, 70f)),
        Stock("TCS", "Tata Consultancy", 3425.00, 3451.40, 3460.00, 3410.00, "2.1M", listOf(70f, 65f, 58f, 60f, 52f, 48f, 50f)),
        Stock("TATASTEEL", "Tata Steel", 150.83, 153.20, 154.00, 149.50, "18.2M", listOf(50f, 52f, 48f, 47f, 45f, 46f, 44f))
    )

    private val _stockFeed = MutableStateFlow(baseStocks)
    val stockFeed: Flow<List<Stock>> = _stockFeed.asStateFlow()

    suspend fun startStockFeedLoop() {
        var currentList = baseStocks
        while (true) {
            _stockFeed.value = currentList
            delay(2000)
            currentList = currentList.map { s ->
                val delta = (Random.nextDouble(-0.35, 0.40) / 100.0) * s.currentPrice
                val newPrice = String.format(Locale.US, "%.2f", s.currentPrice + delta).toDouble()
                s.copy(currentPrice = newPrice)
            }
        }
    }

    suspend fun executeOrder(type: TxType, symbol: String, quantity: Int, price: Double): Boolean = withContext(Dispatchers.IO) {
        val totalCost = quantity * price
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateString = dateFormat.format(Date())

        if (type == TxType.BUY) {
            if (_balance.value < totalCost) return@withContext false
            _balance.value -= totalCost
            val existing = database.holdingDao().getHoldingBySymbol(symbol)
            if (existing != null) {
                val newQty = existing.quantity + quantity
                val newAvg = ((existing.quantity * existing.avgPrice) + totalCost) / newQty
                database.holdingDao().insertOrUpdateHolding(existing.copy(quantity = newQty, avgPrice = newAvg))
            } else {
                database.holdingDao().insertOrUpdateHolding(HoldingEntity(symbol, quantity, price, dateString))
            }
        } else {
            val existing = database.holdingDao().getHoldingBySymbol(symbol) ?: return@withContext false
            if (existing.quantity < quantity) return@withContext false
            _balance.value += totalCost
            if (existing.quantity == quantity) {
                database.holdingDao().deleteHolding(existing)
            } else {
                database.holdingDao().insertOrUpdateHolding(existing.copy(quantity = existing.quantity - quantity))
            }
        }

        database.transactionDao().insertTransaction(
            TransactionEntity(
                type = type.name,
                symbol = symbol,
                shares = quantity,
                price = price,
                timestamp = dateString
            )
        )
        return@withContext true
    }
}