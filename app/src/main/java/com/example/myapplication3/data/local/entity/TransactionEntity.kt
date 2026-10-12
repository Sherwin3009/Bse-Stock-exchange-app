package com.example.myapplication3.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "BUY" or "SELL"
    val symbol: String,
    val shares: Int,
    val price: Double,
    val timestamp: String
)