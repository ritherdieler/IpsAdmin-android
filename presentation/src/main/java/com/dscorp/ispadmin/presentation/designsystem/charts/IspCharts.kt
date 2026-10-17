package com.dscorp.ispadmin.presentation.designsystem.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.patrykandpatrick.vico.compose.common.insets
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.common.Position
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.DashedShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.common.shape.MarkerCorneredShape
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
    markerFormat: (Double) -> String = ::exactNumber,
    showAverage: Boolean = false,
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
    val marker = rememberValueMarker(data, if (diverging) { value -> markerFormat(kotlin.math.abs(value)) } else markerFormat)
    val selection = rememberMarkerSelection(data)
    ChartHost(rememberCartesianChart(layer, startAxis = startAxis(format, range.step), bottomAxis = bottomAxis(data.labels), marker = marker,
        markerVisibilityListener = selection.listener, persistentMarkers = selection.persistent(marker),
        decorations = averageLine(data, showAverage)), model, modifier)
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
    markerFormat: (Double) -> String = ::exactNumber,
    showAverage: Boolean = false,
) {
    val singleSeries = data.series.size == 1
    val lastIndex = data.labels.lastIndex
    val lines = data.series.map { series ->
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(fill(series.color)),
            areaFill = if (singleSeries) LineCartesianLayer.AreaFill.single(fill(series.color.copy(alpha = 0.08f))) else null,
            pointProvider = rememberMonthPoints(series.color, lastIndex),
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
    val marker = rememberValueMarker(data, markerFormat)
    val selection = rememberMarkerSelection(data)
    ChartHost(rememberCartesianChart(layer, startAxis = startAxis(valueFormat, zeroScale?.step), bottomAxis = bottomAxis(data.labels), marker = marker,
        markerVisibilityListener = selection.listener, persistentMarkers = selection.persistent(marker),
        decorations = averageLine(data, showAverage)), model, modifier)
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

/**
 * Shown when a month is touched: a guideline and a label with the month and the exact value of each series
 * ("sep\nNeto: S/ 49,000.00\nCobrado: S/ 45,000.00"). Values come from [data] by month, so they match the bars.
 */
@Composable
private fun rememberValueMarker(data: ChartData, format: (Double) -> String): CartesianMarker {
    val label = rememberTextComponent(
        color = MaterialTheme.colorScheme.inverseOnSurface,
        textSize = 12.sp,
        lineCount = data.series.size + 1,
        padding = insets(MARKER_PADDING_H, MARKER_PADDING_V),
        background = rememberShapeComponent(
            fill(MaterialTheme.colorScheme.inverseSurface),
            MarkerCorneredShape(CorneredShape.rounded(MARKER_RADIUS, MARKER_RADIUS, MARKER_RADIUS, MARKER_RADIUS), MARKER_TAIL),
        ),
    )
    val formatter = remember(data, format) {
        DefaultCartesianMarker.ValueFormatter { _, targets -> markerText(data, targets.firstOrNull()?.x?.toInt(), format) }
    }
    return rememberDefaultCartesianMarker(
        label = label,
        labelPosition = DefaultCartesianMarker.LabelPosition.AbovePoint,
        valueFormatter = formatter,
        guideline = rememberLineComponent(fill(MaterialTheme.colorScheme.outline), thickness = 1.dp),
    )
}

/**
 * One point per month with a ring in the card color, so each month reads on its own; the latest month is larger
 * because it is the one the screen talks about.
 */
@Composable
private fun rememberMonthPoints(color: Color, lastIndex: Int): LineCartesianLayer.PointProvider {
    val ring = IspTheme.colors.card
    val regular = LineCartesianLayer.point(
        rememberShapeComponent(fill(color), CorneredShape.Pill, strokeFill = fill(ring), strokeThickness = POINT_RING), POINT_SIZE)
    val latest = LineCartesianLayer.point(
        rememberShapeComponent(fill(color), CorneredShape.Pill, strokeFill = fill(ring), strokeThickness = POINT_RING), LATEST_POINT_SIZE)
    return remember(regular, latest, lastIndex) {
        object : LineCartesianLayer.PointProvider {
            override fun getPoint(entry: LineCartesianLayerModel.Entry, seriesIndex: Int, extraStore: ExtraStore) =
                if (entry.x.toInt() == lastIndex) latest else regular

            override fun getLargestPoint(extraStore: ExtraStore) = latest
        }
    }
}

/**
 * Dashed average of a single series, to read each month as above or below usual. Only for monthly flows
 * ([showAverage]); levels such as subscribers and multi-series charts skip it. The value goes in the card
 * subtitle (see [averageCaption]) so it never covers a bar or the line.
 */
@Composable
private fun averageLine(data: ChartData, showAverage: Boolean): List<Decoration> {
    val values = data.series.singleOrNull()?.values?.takeIf { it.size > 1 }
    val line = rememberLineComponent(fill(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AVERAGE_ALPHA)), thickness = 1.dp,
        shape = DashedShape(Shape.Rectangle, DASH_LENGTH, DASH_GAP))
    if (!showAverage || values == null) return emptyList()
    val average = values.average()
    return remember(average, line) { listOf(HorizontalLine(y = { average }, line = line)) }
}

/** "prom. 19.7" for the subtitle of a card whose chart draws the dashed average; null when it draws none. */
fun averageCaption(values: List<Double>, format: (Double) -> String): String? =
    values.takeIf { it.size > 1 }?.let { "prom. ${format(it.average())}" }

private val POINT_SIZE = 8.dp
private val LATEST_POINT_SIZE = 11.dp
private val POINT_RING = 2.dp
private const val AVERAGE_ALPHA = 0.7f
private const val DASH_LENGTH = 4f
private const val DASH_GAP = 3f

/** Month last touched; its value stays visible after the finger lifts, until another month is touched. */
private class MarkerSelection(private val selected: androidx.compose.runtime.MutableState<Double?>) {
    val listener = object : CartesianMarkerVisibilityListener {
        override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) = select(targets)
        override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) = select(targets)
        override fun onHidden(marker: CartesianMarker) = Unit
    }

    private fun select(targets: List<CartesianMarker.Target>) {
        targets.firstOrNull()?.let { selected.value = it.x }
    }

    fun persistent(marker: CartesianMarker): (CartesianChart.PersistentMarkerScope.(com.patrykandpatrick.vico.core.common.data.ExtraStore) -> Unit)? {
        val x = selected.value ?: return null
        return { marker at x }
    }
}

@Composable
private fun rememberMarkerSelection(data: ChartData): MarkerSelection {
    val selected = remember(data) { androidx.compose.runtime.mutableStateOf<Double?>(null) }
    return remember(selected) { MarkerSelection(selected) }
}

/** Detail bubble text: the month in bold, then each value; with several series, a dot in the series color and its name. */
internal fun markerText(data: ChartData, index: Int?, format: (Double) -> String): CharSequence {
    index ?: return ""
    val text = android.text.SpannableStringBuilder()
    val month = data.labels.getOrNull(index).orEmpty()
    text.append(month)
    text.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, month.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    data.series.forEach { series ->
        val value = series.values.getOrNull(index) ?: return@forEach
        text.append("\n")
        if (data.series.size == 1) {
            text.append(format(value))
        } else {
            val dotStart = text.length
            text.append("●")
            text.setSpan(android.text.style.ForegroundColorSpan(series.color.toArgb()), dotStart, text.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.append(" ${series.name}  ${format(value)}")
        }
    }
    return text
}

private val MARKER_PADDING_H = 12.dp
private val MARKER_PADDING_V = 8.dp
/** Same bubble as the design system «Burbuja de detalle»: 12 dp corners and an 8 dp tail to the touched point. */
private const val MARKER_RADIUS = 12f
private const val MARKER_TAIL = 8f

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

/** Full value for the touch label: 41870.4 -> "41,870.4", 1284.0 -> "1,284", 1.25 -> "1.25". */
fun exactNumber(value: Double): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("es-PE")).apply { maximumFractionDigits = 2 }.format(value)

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
