package com.radityodwiki.maptrack.ui.tripdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.radityodwiki.maptrack.ui.format.formatDuration

/** Speed over time drawn with Canvas (PRD §21); expects at least two samples. */
@Composable
fun SpeedChart(samples: List<SpeedSample>, modifier: Modifier = Modifier) {
    val maxKmh = chartMaxKmh(samples)
    val maxOffset = samples.last().offsetMs.coerceAtLeast(1)
    val lineColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${maxKmh.toInt()}", style = labelStyle, color = labelColor)
                Text("0", style = labelStyle, color = labelColor)
            }
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
                drawLine(axisColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
                drawLine(axisColor, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 2f)
                val path = Path()
                samples.forEachIndexed { index, sample ->
                    val x = size.width * sample.offsetMs / maxOffset
                    val y = size.height * (1f - (sample.kmh / maxKmh).toFloat())
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = Stroke(width = 4f))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0 menit", style = labelStyle, color = labelColor)
            Text(formatDuration(maxOffset), style = labelStyle, color = labelColor)
        }
    }
}
