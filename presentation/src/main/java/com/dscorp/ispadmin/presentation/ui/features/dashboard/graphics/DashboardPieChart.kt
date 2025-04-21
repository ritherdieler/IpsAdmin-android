package com.dscorp.ispadmin.presentation.ui.features.dashboard.graphics

import android.graphics.Color
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class DashboardPieChart(private val chart: PieChart) {

    init {
        configureChartAppearance()
    }

    private fun configureChartAppearance() {
        chart.apply {
            setUsePercentValues(true)
            centerText = chart.context.resources.getString(R.string.monthly_receipts_resume)
            setDrawCenterText(true)
            setDrawEntryLabels(false)
            legend.isEnabled = true
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            description.isEnabled = false
        }
    }

    fun setData(response: DashBoardDataResponse) {

        val entries = ArrayList<PieEntry>().apply {
            add(PieEntry(response.economicResume.totalDiscount.toFloat(), chart.context.getString(R.string.discount)))
            add(PieEntry(response.economicResume.totalRaised.toFloat(), chart.context.getString(R.string.raised)))
            add(PieEntry(response.economicResume.totalToCollect.toFloat(), chart.context.getString(R.string.pending)))
        }

        val dataSet = createPieDataSet(entries)
        configurePieDataSet(dataSet)

        val data = createPieData(dataSet)
        configureChartData(data)

        updateChartAppearance()
    }

    private fun createPieDataSet(entries: ArrayList<PieEntry>): PieDataSet {
        val dataSet = PieDataSet(entries, null)
        dataSet.sliceSpace = 1f

        val colors = listOf(
            Color.rgb(3, 169, 244), // SKY BLUE
            Color.rgb(63, 81, 181), // BLUE
            Color.rgb(255, 87, 34)   // ORANGE
        )
        dataSet.colors = colors

        return dataSet
    }

    private fun configurePieDataSet(dataSet: PieDataSet) {
        dataSet.selectionShift = 0f
        dataSet.valueLinePart1OffsetPercentage = 80f
        dataSet.valueLinePart1Length = 0.2f
        dataSet.valueLinePart2Length = 0.4f
        dataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
    }

    private fun createPieData(dataSet: PieDataSet): PieData {
        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(chart))
        data.setValueTextSize(13f)
        data.setValueTextColors(
            listOf(
                Color.rgb(255, 255, 255)
            ))
        return data
    }

    private fun configureChartData(data: PieData) {
        chart.data = data
    }

    private fun updateChartAppearance() {
        chart.apply {
            highlightValues(null)
            invalidate()
        }
    }
}
