package com.example.myapplication3.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication3.data.domain.Stock
import com.example.myapplication3.data.remote.AuthService
import com.example.myapplication3.data.repository.StockRepository
import com.example.myapplication3.data.repository.TxType
import com.example.myapplication3.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderBottomSheet(
    stock: Stock,
    type: TxType,
    repository: StockRepository,
    authService: AuthService,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    val qty = quantityText.toIntOrNull() ?: 0
    val totalCost = qty * stock.currentPrice
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (type == TxType.BUY) "Buy" else "Sell"} ${stock.symbol}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", stock.currentPrice)}",
                    fontSize = 16.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it.filter { char -> char.isDigit() } },
                label = { Text("Quantity", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedLabelColor = PrimaryPurple,
                    unfocusedLabelColor = TextMuted
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total ${if (type == TxType.BUY) "Cost" else "Credit"}:", color = TextMuted)
                Text(
                    text = "₹${String.format(Locale.US, "%.2f", totalCost)}",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            errorMsg?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = AccentRed, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (qty <= 0) {
                        errorMsg = "Enter valid quantity"
                        return@Button
                    }
                    coroutineScope.launch {
                        isProcessing = true
                        errorMsg = null

                        if (type == TxType.BUY) {
                            val firestoreResult = authService.buyStock(
                                symbol = stock.symbol,
                                companyName = stock.name,
                                quantity = qty,
                                pricePerShare = stock.currentPrice
                            )

                            firestoreResult.onSuccess {
                                val localSuccess = repository.executeOrder(type, stock.symbol, qty, stock.currentPrice)
                                isProcessing = false
                                if (localSuccess) {
                                    onSuccess()
                                    onDismiss()
                                } else {
                                    errorMsg = "Local database update failed"
                                }
                            }.onFailure { ex ->
                                isProcessing = false
                                errorMsg = ex.message ?: "Firestore order failed"
                            }
                        } else {
                            // Sell flow with Firestore sync
                            val firestoreResult = authService.sellStock(
                                symbol = stock.symbol,
                                quantity = qty,
                                pricePerShare = stock.currentPrice
                            )

                            firestoreResult.onSuccess {
                                val localSuccess = repository.executeOrder(type, stock.symbol, qty, stock.currentPrice)
                                isProcessing = false
                                if (localSuccess) {
                                    onSuccess()
                                    onDismiss()
                                } else {
                                    errorMsg = "Local database update failed"
                                }
                            }.onFailure { ex ->
                                isProcessing = false
                                errorMsg = ex.message ?: "Sell transaction failed"
                            }
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TxType.BUY) AccentGreen else AccentRed
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (type == TxType.BUY) "Confirm Buy" else "Confirm Sell",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}