package com.dscorp.ispadmin.presentation.designsystem.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dscorp.ispadmin.presentation.theme.IspTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.grouped
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.CartesianChart
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape

private val CHART_HEIGHT = 180.dp
private val COLUMN_THICKNESS = 12.dp
private val DIVERGING_THICKNESS = 18.dp
private const val VERTICAL_TICKS = 4
private const val MAX_BOTTOM_LABELS = 12

/** One named series; values are aligned by index with [ChartData.labels]. */
data class ChartSeries(val name: String, val color: Color, val values: List<Double>)

data class ChartData(val labels: List<String>, val series: List<ChartSeries>) {
    val isEmpty: Boolean get() = labels.isEmpty() || series.isEmpty() || series.all { it.values.isEmpty() }
}

/**
 * Grouped column chart themed from the design system. One y-axis, starting at zero, with round ticks
 * ([integers] keeps counts on whole steps). [diverging] stacks the series on one column per label: pass the
 * second series as negative values to draw it below zero; labels show magnitudes.
 */
@Composable
fun IspColumnChart(
    data: ChartData,
    modifier: Modifier = Modifier,
    valueFormat: (Double) -> String = ::compactNumber,
    integers: Boolean = false,
    diverging: Boolean = false,
) {
    val columns = data.series.map { series ->
        rememberLineComponent(
            fill = fill(series.color),
            thickness = if (diverging) DIVERGING_THICKNESS else COLUMN_THICKNESS,
            shape = CorneredShape.rounded(4f, 4f, if (diverging) 4f else 0f, if (diverging) 4f else 0f),
        )
    }
    val range = remember(data, integers, diverging) { columnRange(data, integers, diverging) }
    val layer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(columns),
        mergeMode = { if (diverging) ColumnCartesianLayer.MergeMode.Stacked else ColumnCartesianLayer.MergeMode.grouped(columnSpacing = 2.dp) },
        rangeProvider = CartesianLayerRangeProvider.fixed(minY = range.min, maxY = range.max),
    )
    val model = remember(data) {
        CartesianChartModel(ColumnCartesianLayerModel.build { data.series.forEach { series(it.values) } })
    }
    val format: (Double) -> String = if (diverging) { value -> valueFormat(kotlin.math.abs(value)) } else valueFormat
    ChartHost(rememberCartesianChart(layer, startAxis = startAxis(format, range.step), bottomAxis = bottomAxis(data.labels)), model, modifier)
}

private fun columnRange(data: ChartData, integers: Boolean, diverging: Boolean): DivergingScale {
    val values = data.series.flatMap { it.values }
    if (diverging) {
        return divergingScale(up = values.maxOrNull()?.coerceAtLeast(0.0) ?: 0.0, down = -(values.minOrNull()?.coerceAtMost(0.0) ?: 0.0))
    }
    val scale = niceScale(values.maxOrNull() ?: 0.0, integers)
    return DivergingScale(min = 0.0, max = scale.max, step = scale.step)
}

/**
 * Line chart; a single series gets a faint area fill. The y-axis follows the data range with some headroom, or
 * runs from zero on round ticks when [startAtZero] (amounts whose level matters, not only their trend).
 */
@Composable
fun IspLineChart(
    data: ChartData,
    modifier: Modifier = Modifier,
    valueFormat: (Double) -> String = ::compactNumber,
    startAtZero: Boolean = false,
) {
    val singleSeries = data.series.size == 1
    val lines = data.series.map { series ->
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(fill(series.color)),
            areaFill = if (singleSeries) LineCartesianLayer.AreaFill.single(fill(series.color.copy(alpha = 0.08f))) else null,
        )
    }
    val zeroScale = remember(data, startAtZero) { if (startAtZero) niceScale(data.series.flatMap { it.values }.maxOrNull() ?: 0.0) else null }
    val range = remember(data, zeroScale) { zeroScale?.let { 0.0..it.max } ?: lineRange(data) }
    val layer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(lines),
        rangeProvider = CartesianLayerRangeProvider.fixed(minY = range.start, maxY = range.endInclusive),
    )
    val model = remember(data) {
        CartesianChartModel(LineCartesianLayerModel.build { data.series.forEach { series(it.values) } })
    }
    ChartHost(rememberCartesianChart(layer, startAxis = startAxis(valueFormat, zeroScale?.step), bottomAxis = bottomAxis(data.labels)), model, modifier)
}

@Composable
private fun ChartHost(chart: CartesianChart, model: CartesianChartModel, modifier: Modifier) {
    CartesianChartHost(
        chart = chart,
        model = model,
        modifier = modifier.fillMaxWidth().height(CHART_HEIGHT),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(false, Zoom.Content, Zoom.Content, Zoom.Content),
    )
}

@Composable
private fun axisLabel() = rememberTextComponent(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textSize = 10.sp,
)

/** [step] places ticks on a round scale; without it the axis splits the range in [VERTICAL_TICKS]. */
@Composable
private fun startAxis(valueFormat: (Double) -> String, step: Double? = null): VerticalAxis<Axis.Position.Vertical.Start> =
    VerticalAxis.rememberStart(
        line = null,
        label = axisLabel(),
        valueFormatter = remember(valueFormat) { formatter { y -> valueFormat(y) } },
        tick = null,
        guideline = rememberLineComponent(fill = fill(IspTheme.colors.chartGrid), thickness = 1.dp),
        itemPlacer = remember(step) {
            if (step != null) VerticalAxis.ItemPlacer.step({ step }) else VerticalAxis.ItemPlacer.count({ VERTICAL_TICKS })
        },
    )

@Composable
private fun bottomAxis(labels: List<String>): HorizontalAxis<Axis.Position.Horizontal.Bottom> {
    val spacing = ((labels.size + MAX_BOTTOM_LABELS - 1) / MAX_BOTTOM_LABELS).coerceAtLeast(1)
    return HorizontalAxis.rememberBottom(
        line = rememberLineComponent(fill = fill(MaterialTheme.colorScheme.outlineVariant), thickness = 1.dp),
        label = axisLabel(),
        valueFormatter = remember(labels) { formatter { x -> labels.getOrNull(x.toInt()).orEmpty() } },
        tick = null,
        guideline = null,
        itemPlacer = remember(spacing) { HorizontalAxis.ItemPlacer.aligned(spacing = { spacing }) },
    )
}

private fun formatter(format: (Double) -> String) = object : CartesianValueFormatter {
    override fun format(context: CartesianMeasuringContext, value: Double, verticalAxisPosition: Axis.Position.Vertical?) =
        format(value)
}

private const val LINE_HEADROOM = 0.15

/** Pads the data range so lines show their trend; never goes below zero for non-negative data. */
private fun lineRange(data: ChartData): ClosedFloatingPointRange<Double> {
    val values = data.series.flatMap { it.values }
    val min = values.minOrNull() ?: 0.0
    val max = values.maxOrNull() ?: 0.0
    val padding = ((max - min) * LINE_HEADROOM).takeIf { it > 0 } ?: (kotlin.math.abs(max) * LINE_HEADROOM).coerceAtLeast(1.0)
    val start = (min - padding).let { if (min >= 0 && it < 0) 0.0 else it }
    return start..(max + padding)
}

/** 41870.0 -> "41.9k", 1284.0 -> "1,284", 866.7 -> "867", 1.25 -> "1.3". */
fun compactNumber(value: Double): String {
    val magnitude = kotlin.math.abs(value)
    return when {
        magnitude >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
        magnitude >= 10_000 -> "%.1fk".format(value / 1_000)
        magnitude >= 10 -> "%,d".format(kotlin.math.round(value).toLong())
        value % 1.0 == 0.0 -> value.toLong().toString()
        else -> "%.1f".format(value)
    }
}
