package com.example.myapplication3.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun StockChart(
    points: List<Float>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val strokeColor = if (isPositive) Color(0xFF00D09C) else Color(0xFFFF5252)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val minPoint = points.minOrNull() ?: 0f
        val maxPoint = points.maxOrNull() ?: 100f
        val range = (maxPoint - minPoint).coerceAtLeast(1f)

        val path = Path()
        val stepX = width / (points.size - 1).coerceAtLeast(1)

        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minPoint) / range) * height

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevY = height - ((points[index - 1] - minPoint) / range) * height
                val midX = (prevX + x) / 2
                path.cubicTo(midX, prevY, midX, y, x, y)
            }
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.5f)
        )
    }
}