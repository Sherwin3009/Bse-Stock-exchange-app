package com.example.myapplication3.data.domain

data class Stock(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val previousClose: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val volume: String,
    val chartPoints: List<Float>
) {
    val change: Double get() = currentPrice - previousClose
    val percentChange: Double get() = (change / previousClose) * 100
}