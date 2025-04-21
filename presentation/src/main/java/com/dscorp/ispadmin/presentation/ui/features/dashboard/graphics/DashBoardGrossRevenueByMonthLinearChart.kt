package com.dscorp.ispadmin.presentation.ui.features.dashboard.graphics

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.MonthlyGrossRevenueResume
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.Utils
import java.util.Calendar
import java.util.Locale


class DashBoardGrossRevenueByMonthLinearChart(
    private val chart: LineChart,
    val context: Context
) {

    private var xAxis = chart.xAxis
    private var yAxis = chart.axisLeft

    init {
        chart.apply {
            // Configuración general
            legend.isEnabled = false
            setBackgroundColor(Color.WHITE)
            description.isEnabled = false
            setTouchEnabled(false)
            setOnChartValueSelectedListener(null)
            setDrawGridBackground(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)

            // Eje X
            xAxis.position = XAxis.XAxisPosition.BOTTOM

            // Eje Y (axisLeft)
            yAxis.enableGridDashedLine(10f, 10f, 0f)

            // Deshabilitar eje Y derecho
            axisRight.isEnabled = false
        }
    }

    fun setData(data: List<MonthlyGrossRevenueResume>) {
        if (data.isNotEmpty()) {
            val entries = data.mapIndexed { index, entry ->
                Entry(index.toFloat(), entry.totalCharged.toFloat(), null)
            }
            val yAxisLimit = data.maxOf { it.totalCharged }
            configureXAxisLabels(data)

            configureYAxis(yAxisLimit, data)

            setupChart(entries)
        }
    }

    private fun configureXAxisLabels(data: List<MonthlyGrossRevenueResume>) {
        xAxis.labelCount = data.size+5
        val xAxisLabels = data.map {
            (Calendar.getInstance().apply { timeInMillis = it.billingDate }
                .getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))
                ?.replaceFirstChar { it.titlecase(Locale.ROOT) }?.substring(0, 3) + ".")
        }
        xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)
    }

    private fun configureYAxis(yAxisLimit: Double, data: List<MonthlyGrossRevenueResume>) {
        val yAxis = chart.axisLeft
        yAxis.axisMaximum = ((yAxisLimit * 0.05) + yAxisLimit).toFloat()
        yAxis.axisMinimum =
            (data.minOf { it.totalCharged } - (yAxisLimit * 0.10)).toFloat()
    }

    private fun setupChart(entries: List<Entry>) {
        val set1 = if (chart.data != null && chart.data.dataSetCount > 0) {
            chart.data.getDataSetByIndex(0) as LineDataSet
        } else {
            createNewDataSet(entries)
        }

        set1.values = entries
        set1.notifyDataSetChanged()

        if (chart.data == null || chart.data.dataSetCount == 0) {
            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(set1)
            val data = LineData(dataSets)
            chart.data = data
        }

        // Resto de la configuración de estilo específico del gráfico...
    }

    private fun createNewDataSet(entries: List<Entry>): LineDataSet {
        val set1 = LineDataSet(entries, "")
        set1.apply {
            setDrawIcons(false)
            enableDashedLine(10f, 5f, 0f)
            color = Color.BLACK
            setCircleColor(Color.BLACK)
            lineWidth = 1f
            circleRadius = 3f
            setDrawCircleHole(false)
            formLineWidth = 1f
            formSize = 15f
            valueTextSize = 9f
            enableDashedHighlightLine(10f, 5f, 0f)
            setDrawFilled(true)
            fillFormatter = IFillFormatter { dataSet, dataProvider -> chart.axisLeft.axisMinimum }
            if (Utils.getSDKInt() >= 18) {
                val drawable = ContextCompat.getDrawable(context, R.drawable.fade_red)
                fillDrawable = drawable
            } else {
                fillColor = Color.BLACK
            }
        }
        return set1
    }

}