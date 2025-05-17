package com.dscorp.ispadmin.presentation.ui.features.dashboard.graphics

import android.graphics.Color
import com.dscorp.ispadmin.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.components.YAxis.YAxisLabelPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet


class DashboardPaymentMethodsBarChart(private val chart: BarChart) {
    private val xAxis = chart.xAxis
    private val yAxis = chart.axisLeft

    init {
        setupChartAppearance()
    }

    private fun setupChartAppearance() {
        chart.apply {
            setDrawValueAboveBar(true)
            legend.isEnabled = false
            axisRight.isEnabled = false
            setDrawBarShadow(false)
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            chart.description.isEnabled = false
            chart.legend.isEnabled = false


        }

        setupXAxis()
        setupYAxis()
    }

    private fun setupXAxis() {
        xAxis.apply {
            setDrawGridLines(false)
            position = XAxisPosition.BOTTOM
        }
    }

    private fun setupYAxis() {
        yAxis.apply {
            valueFormatter = PercentFormatter()
            setPosition(YAxisLabelPosition.OUTSIDE_CHART)
            spaceTop = 15f
            axisMinimum = 0f
            axisMaximum = 100f
            setDrawGridLines(true)
        }
    }

    fun setData(paymentResume: Map<String, Double>) {
        val xAxisLabels = paymentResume.keys.toList()

//        xAxis.mAxisMaximum = xAxisLabels.size.toFloat()
        xAxis.labelCount = xAxisLabels.size
        val barEntries = paymentResume.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val barColors = listOf(Color.RED, Color.GREEN, Color.BLUE)

        xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels)

        val barDataSet: BarDataSet = if (chart.data != null && chart.data.dataSetCount > 0) {
            chart.data.getDataSetByIndex(0) as BarDataSet
        } else {
            createNewBarDataSet(barEntries, barColors)
        }

        barDataSet.values = barEntries
        barDataSet.notifyDataSetChanged()

        if (chart.data == null || chart.data.dataSetCount == 0) {
            val barDataSets = mutableListOf<IBarDataSet>()
            barDataSets.add(barDataSet)
            val barData = BarData(barDataSets)
            barData.setValueTextSize(10f)
            barData.barWidth = 0.9f
            chart.data = barData
        }

        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
    }


    fun setStackedData(barEntries: List<BarEntry>, xAxisLabels: List<String>) {
        xAxis.mAxisMaximum = xAxisLabels.size.toFloat()

        val set1: BarDataSet

        if (chart.data != null &&
            chart.data.dataSetCount > 0
        ) {
            set1 = chart.data.getDataSetByIndex(0) as BarDataSet
            set1.values = barEntries
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
        } else {
            set1 = BarDataSet(barEntries, null)
            set1.setDrawIcons(false)
            val colors = listOf(
                Color.rgb(3, 169, 244),
                Color.rgb(63, 81, 181),
                Color.rgb(255, 87, 34)
            )
            set1.colors = colors


            set1.stackLabels = arrayOf("Descontado", "Recaudado", "Por cobrar")

            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(set1)
            val data = BarData(dataSets)
            data.setValueFormatter(MyAxisValueFormatter())
            data.setValueTextColors(
                listOf(
                    Color.rgb(200, 200, 200)
                )
            )
            data.setValueTextSize(10f)
            chart.data = data
        }

        xAxis.valueFormatter = IndexAxisValueFormatter(xAxisLabels.map { it ->
            it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }})

        yAxis.axisMaximum = 105f
        xAxis.labelCount = xAxisLabels.size

        val l = chart.legend
        l.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        l.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        l.orientation = Legend.LegendOrientation.HORIZONTAL
        l.setDrawInside(false)
        l.formSize = 8f
        l.formToTextSpace = 4f
        l.xEntrySpace = 6f
        l.isEnabled = true
     //add margin top to legend
        chart.setExtraOffsets(0f, 0f, 0f, chart.context.resources.getDimension(R.dimen.barchart_legend_top_margin))
        chart.description.isEnabled = false
        chart.setFitBars(true)
        chart.invalidate()
    }

    private fun createNewBarDataSet(entries: List<BarEntry>, colors: List<Int>): BarDataSet {
        val dataSet = BarDataSet(entries, null)
        dataSet.setDrawIcons(false)
        dataSet.colors = colors
        return dataSet
    }


}
