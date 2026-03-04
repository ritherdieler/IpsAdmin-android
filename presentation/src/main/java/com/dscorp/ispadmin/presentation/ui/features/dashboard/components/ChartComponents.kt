package com.dscorp.ispadmin.presentation.ui.features.dashboard.components

import android.graphics.Color
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dscorp.ispadmin.domain.model.DashBoardDataResponse
import com.dscorp.ispadmin.domain.model.MonthlyCollectsResume
import com.dscorp.ispadmin.domain.model.MonthlyGrossRevenueResume
import com.dscorp.ispadmin.domain.model.MonthlySubscriptionResume
import com.dscorp.ispadmin.domain.model.SubscriptionLogSummary
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun PieChartContainer(data: DashBoardDataResponse) {
    // Animación para la aparición del gráfico
    var animationProgress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 1000)
    )
    
    LaunchedEffect(key1 = true) {
        animationProgress = 1f
    }
    
    // Calcular total
    val total = data.economicResume.totalDiscount + 
                data.economicResume.totalRaised + 
                data.economicResume.totalToCollect
    
    // Obtener colores del tema actual
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    
    // Colores para los segmentos del gráfico usando los colores del tema
    val discountColor = MaterialTheme.colorScheme.secondary.toArgb() // Naranja para descuentos
    val collectedColor = MaterialTheme.colorScheme.tertiary.toArgb() // Verde para recaudados
    val pendingColor = MaterialTheme.colorScheme.primary.toArgb() // Azul/Primary para pendientes
    
    ChartCard {
        // Columna para organizar verticalmente el gráfico y la leyenda
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gráfico circular
            AndroidView(
                factory = { context ->
                    PieChart(context).apply {
                        setUsePercentValues(true)
                        description.isEnabled = false
                        setExtraOffsets(5f, 10f, 5f, 10f)  // Menos espacio para las etiquetas
                        
                        // Desactivar la leyenda nativa del gráfico
                        legend.isEnabled = false
                        
                        // Estilo del gráfico de pastel
                        holeRadius = 50f
                        setHoleColor(backgroundColor)
                        transparentCircleRadius = 55f
                        setTransparentCircleColor(backgroundColor)
                        setTransparentCircleAlpha(110)
                        
                        setDrawCenterText(true)
                        centerText = "Recaudación\nMensual"
                        setCenterTextSize(14f)
                        setCenterTextColor(onSurfaceColor)
                        

                        // Creando entradas para el gráfico
                        val entries = ArrayList<PieEntry>().apply {
                            add(PieEntry(
                                data.economicResume.totalDiscount.toFloat(),
                                ""  // Sin etiqueta
                            ))
                            add(PieEntry(
                                data.economicResume.totalRaised.toFloat(), 
                                ""  // Sin etiqueta
                            ))
                            add(PieEntry(
                                data.economicResume.totalToCollect.toFloat(), 
                                ""  // Sin etiqueta
                            ))
                        }
                        
                        // Configurando el conjunto de datos
                        val dataSet = PieDataSet(entries, "").apply {
                            colors = listOf(
                                discountColor,  // Color secundario para descuentos
                                collectedColor,  // Color terciario para recaudados
                                pendingColor     // Color primario para pendientes
                            )
                            valueTextSize = 14f
                            valueTextColor = Color.WHITE
                            
                            // No mostrar etiquetas en los segmentos
                            setDrawValues(true)
                            // Formato simple para mostrar solo porcentajes
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    // Simplemente añadir el símbolo % al valor existente
                                    return String.format("%.1f%%", value)
                                }
                            }
                        }
                        
                        // Desactivar etiquetas de entrada
                        setDrawEntryLabels(false)
                        
                        // Aplicando los datos al gráfico
                        val pieData = PieData(dataSet)
                        this.data = pieData
                        
                        // Habilitar interacciones
                        setTouchEnabled(true)
                        isRotationEnabled = true
                        
                        // Configurar animación
                        animateY(1400)
                        invalidate()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
            
            // Mostrar leyenda personalizada debajo del gráfico con valores
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mostrar Total
            val formatter = NumberFormat.getNumberInstance(Locale("es", "PE"))
            Text(
                text = "Total: S/. ${formatter.format(total)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            // Leyenda en fila
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.secondary,
                    label = "Descuento",
                    value = "S/. ${formatter.format(data.economicResume.totalDiscount)}"
                )
                
                LegendItem(
                    color = MaterialTheme.colorScheme.tertiary,
                    label = "Recaudado",
                    value = "S/. ${formatter.format(data.economicResume.totalRaised)}"
                )
                
                LegendItem(
                    color = MaterialTheme.colorScheme.primary,
                    label = "Pendiente",
                    value = "S/. ${formatter.format(data.economicResume.totalToCollect)}"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: ComposeColor,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LineChartContainer(title: String, data: List<Any>) {
    // Obtener colores del tema actual
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()
    
    ChartCard {
        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    setDrawGridBackground(false)
                    
                    // Configurar estilo
                    setDrawBorders(false)
                    setScaleEnabled(true)
                    setPinchZoom(true)
                    axisRight.isEnabled = false
                    
                    // Configurar eje X
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.granularity = 1f
                    xAxis.setDrawGridLines(false)
                    xAxis.textColor = textColor
                    
                    // Configurar eje Y
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = gridLineColor
                    axisLeft.textColor = textColor
                    axisLeft.setDrawZeroLine(true)
                    
                    // Configurar leyenda
                    legend.isEnabled = true
                    legend.textSize = 12f
                    legend.textColor = textColor
                    legend.form = Legend.LegendForm.LINE
                    
                    when (data.firstOrNull()) {
                        is MonthlyGrossRevenueResume -> {
                            val revenueData = data as List<MonthlyGrossRevenueResume>
                            val entries = revenueData.mapIndexed { index, item ->
                                Entry(index.toFloat(), item.totalCharged.toFloat())
                            }
                            
                            val dataSet = LineDataSet(entries, "Ingresos Brutos").apply {
                                color = tertiaryColor  // Usar color terciario del tema
                                valueTextColor = textColor  // Usar color de texto del tema
                                valueTextSize = 10f
                                setDrawCircles(true)
                                setCircleColor(tertiaryColor)
                                circleRadius = 4f
                                setDrawCircleHole(true)
                                circleHoleRadius = 2f
                                setDrawValues(true)  // Mostrar valores
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return NumberFormat.getNumberInstance(Locale("es", "PE")).format(value.toDouble())
                                    }
                                }
                                lineWidth = 2.5f
                                setDrawFilled(true)
                                fillColor = tertiaryColor
                                fillAlpha = 30
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                            }
                            
                            val lineData = LineData(dataSet)
                            this.data = lineData
                            
                            // Configurar etiquetas del eje X
                            val dateFormat = SimpleDateFormat("MMM", Locale("es", "PE"))
                            val labels = revenueData.map { 
                                dateFormat.format(Date(it.billingDate)).capitalize(Locale("es", "PE"))
                            }
                            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        }
                        is MonthlySubscriptionResume -> {
                            val subscriptionData = data as List<MonthlySubscriptionResume>
                            
                            // Limitar a los 8 datos más recientes si hay más
                            val limitedData = if (subscriptionData.size > 8) {
                                subscriptionData.takeLast(8)
                            } else {
                                subscriptionData
                            }
                            
                            // Entradas para suscripciones activas
                            val activeEntries = limitedData.mapIndexed { index, item ->
                                Entry(index.toFloat(), item.totalActiveSubscriptions.toFloat())
                            }
                            
                            val activeDataSet = LineDataSet(activeEntries, "Suscripciones Activas").apply {
                                color = primaryColor  // Usar color primario del tema
                                valueTextColor = textColor  // Usar color de texto del tema
                                valueTextSize = 10f
                                setDrawCircles(true)
                                setCircleColor(primaryColor)
                                circleRadius = 4f
                                setDrawCircleHole(true)
                                circleHoleRadius = 2f
                                setDrawValues(true)  // Mostrar valores
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return value.toInt().toString()
                                    }
                                }
                                lineWidth = 2.5f
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                            }
                            
                            // Solo usar el dataset de suscripciones activas
                            val lineData = LineData(activeDataSet)
                            this.data = lineData
                            
                            // Configurar etiquetas del eje X
                            val dateFormat = SimpleDateFormat("MMM", Locale("es", "PE"))
                            val labels = limitedData.map { 
                                dateFormat.format(it.date).capitalize(Locale("es", "PE"))
                            }
                            
                            // Configuración del eje X para mostrar todos los meses
                            xAxis.apply {
                                valueFormatter = IndexAxisValueFormatter(labels)
                                setLabelCount(8, false)  // Forzar exactamente 8 etiquetas
                                setAvoidFirstLastClipping(true)  // Evitar recorte de primera/última etiqueta
                                granularity = 1f  // Asegurar que las etiquetas se muestren en cada posición
                                position = XAxis.XAxisPosition.BOTTOM
                                labelRotationAngle = 0f  // Evitar rotación de etiquetas
                                
                                // Ajustar los límites del eje X para mostrar todos los puntos
                                axisMinimum = -0.5f
                                axisMaximum = 7.5f  // Exactamente 8 puntos (0-7)
                                
                                // Asegurar que los gridlines coincidan con las etiquetas
                                setDrawGridLines(true)
                                gridColor = gridLineColor
                                gridLineWidth = 0.5f
                            }
                        }
                    }
                    
                    // Configurar animación
                    animateX(1500)
                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
    }
}

@Composable
fun BarChartContainer(
    title: String, 
    data: Any,
    stacked: Boolean = false
) {
    // Obtener colores del tema actual
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()
    
    ChartCard {
        AndroidView(
            factory = { context ->
                BarChart(context).apply {
                    description.isEnabled = false
                    setDrawGridBackground(false)
                    setDrawBarShadow(false)
                    setScaleEnabled(true)
                    setPinchZoom(false)
                    
                    // Configurar apariencia
                    setFitBars(true)
                    
                    // Configurar eje X
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.granularity = 1f
                    xAxis.setDrawGridLines(false)
                    xAxis.textColor = textColor
                    
                    // Configurar eje Y
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = gridLineColor
                    axisLeft.textColor = textColor
                    axisRight.isEnabled = false
                    
                    // Configurar leyenda
                    legend.isEnabled = true
                    legend.textSize = 12f
                    legend.textColor = textColor
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                    
                    when {
                        data is Map<*, *> && !stacked -> {
                            val paymentData = data as Map<String, Double>
                            val entries = paymentData.entries.mapIndexed { index, entry ->
                                BarEntry(index.toFloat(), entry.value.toFloat())
                            }
                            
                            val dataSet = BarDataSet(entries, "").apply {  // Quitar el título de la leyenda
                                // Usar colores del tema
                                colors = listOf(
                                    primaryColor,
                                    secondaryColor,
                                    tertiaryColor
                                )
                                valueTextColor = textColor
                                valueTextSize = 10f
                                setDrawValues(true)
                                valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return "${value.toInt()}%"
                                    }
                                }
                            }
                            
                            val barData = BarData(dataSet)
                            barData.barWidth = 0.6f
                            this.data = barData
                            
                            // Desactivar la leyenda
                            legend.isEnabled = false
                            
                            // Configurar etiquetas del eje X
                            xAxis.valueFormatter = IndexAxisValueFormatter(paymentData.keys.toList())
                        }
                        data is List<*> && stacked -> {
                            val monthlyCollects = data as List<MonthlyCollectsResume>
                            
                            // Crear entradas apiladas para descuentos, recaudados y por cobrar
                            val entries = monthlyCollects.mapIndexed { index, item ->
                                BarEntry(
                                    index.toFloat(),
                                    floatArrayOf(
                                        item.totalDiscount.toFloat(),
                                        item.totalRaised.toFloat(),
                                        item.totalReceivables.toFloat()
                                    )
                                )
                            }
                            
                            val dataSet = BarDataSet(entries, "").apply {
                                colors = listOf(
                                    secondaryColor,  // Descuentos - Secundario
                                    tertiaryColor,   // Recaudados - Terciario
                                    primaryColor     // Por cobrar - Primario
                                )
                                valueTextColor = textColor
                                valueTextSize = 10f
                                setDrawValues(true)  // Mostrar los valores
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        return "${value.toInt()}%"
                                    }
                                }
                                stackLabels = arrayOf("Descuentos", "Recaudados", "Por cobrar")
                            }
                            
                            val barData = BarData(dataSet)
                            barData.barWidth = 0.6f
                            this.data = barData
                            
                            // Configurar etiquetas del eje X
                            val dateFormat = SimpleDateFormat("MMM", Locale("es", "PE"))
                            val labels = monthlyCollects.map { 
                                dateFormat.format(it.date).capitalize(Locale("es", "PE"))
                            }
                            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        }
                    }
                    
                    // Configurar animación
                    animateY(1500)
                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
    }
}

@Composable
fun ChartCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun CancellationChartContainer(data: Map<String, SubscriptionLogSummary>) {
    val cancellationData = data["TOTAL_CANCEL_SUBSCRIPTION"]
    val context = LocalContext.current
    
    if (cancellationData == null) {
        return
    }
    
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val errorColor = MaterialTheme.colorScheme.error.toArgb()
    
    ChartCard {
        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    setDrawGridBackground(false)
                    
                    setDrawBorders(false)
                    setScaleEnabled(true)
                    setPinchZoom(true)
                    axisRight.isEnabled = false
                    
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.granularity = 1f
                    xAxis.setDrawGridLines(false)
                    xAxis.textColor = textColor
                    
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = gridLineColor
                    axisLeft.textColor = textColor
                    axisLeft.setDrawZeroLine(true)
                    
                    legend.isEnabled = true
                    legend.textSize = 12f
                    legend.textColor = textColor
                    legend.form = Legend.LegendForm.LINE
                    
                    val monthlyDetails = cancellationData.monthlyDetails.reversed()
                    val entries = monthlyDetails.mapIndexed { index, item ->
                        Entry(index.toFloat(), item.count.toFloat())
                    }
                    
                    val dataSet = LineDataSet(entries, "Cancelaciones").apply {
                        color = errorColor
                        valueTextColor = textColor
                        valueTextSize = 10f
                        setDrawCircles(true)
                        setCircleColor(errorColor)
                        circleRadius = 4f
                        setDrawCircleHole(true)
                        circleHoleRadius = 2f
                        setDrawValues(true)
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return value.toInt().toString()
                            }
                        }
                        lineWidth = 2.5f
                        setDrawFilled(true)
                        fillColor = errorColor
                        fillAlpha = 30
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }
                    
                    val lineData = LineData(dataSet)
                    this.data = lineData
                    
                    val labels = monthlyDetails.map { item ->
                        val parts = item.period.split("-")
                        val month = when (parts[1]) {
                            "01" -> "Ene"
                            "02" -> "Feb"
                            "03" -> "Mar"
                            "04" -> "Abr"
                            "05" -> "May"
                            "06" -> "Jun"
                            "07" -> "Jul"
                            "08" -> "Ago"
                            "09" -> "Set"
                            "10" -> "Oct"
                            "11" -> "Nov"
                            "12" -> "Dic"
                            else -> parts[1]
                        }
                        "$month ${parts[0].substring(2)}"
                    }
                    
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    
                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            e?.let { entry ->
                                val index = entry.x.toInt()
                                if (index >= 0 && index < monthlyDetails.size) {
                                    val selectedMonth = monthlyDetails[index]
                                    val parts = selectedMonth.period.split("-")
                                    val year = parts[0]
                                    val monthNumber = parts[1]
                                    
                                    val monthName = when (monthNumber) {
                                        "01" -> "Enero"
                                        "02" -> "Febrero"
                                        "03" -> "Marzo"
                                        "04" -> "Abril"
                                        "05" -> "Mayo"
                                        "06" -> "Junio"
                                        "07" -> "Julio"
                                        "08" -> "Agosto"
                                        "09" -> "Septiembre"
                                        "10" -> "Octubre"
                                        "11" -> "Noviembre"
                                        "12" -> "Diciembre"
                                        else -> monthNumber
                                    }
                                    
                                    val message = "$monthName $year: ${selectedMonth.count} cancelaciones"
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        
                        override fun onNothingSelected() {
                            // No hacer nada cuando no hay selección
                        }
                    })
                    
                    animateX(1500)
                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
    }
}

@Composable
fun ReconnectionChartContainer(data: Map<String, SubscriptionLogSummary>) {
    val reconnectionData = data["RECONNECT_CANCELLED_SUBSCRIPTION"]
    val context = LocalContext.current
    
    if (reconnectionData == null) {
        return
    }
    
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val tertiaryColor = MaterialTheme.colorScheme.tertiary.toArgb()
    
    ChartCard {
        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    setDrawGridBackground(false)
                    
                    setDrawBorders(false)
                    setScaleEnabled(true)
                    setPinchZoom(true)
                    axisRight.isEnabled = false
                    
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.granularity = 1f
                    xAxis.setDrawGridLines(false)
                    xAxis.textColor = textColor
                    
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = gridLineColor
                    axisLeft.textColor = textColor
                    axisLeft.setDrawZeroLine(true)
                    
                    legend.isEnabled = true
                    legend.textSize = 12f
                    legend.textColor = textColor
                    legend.form = Legend.LegendForm.LINE
                    
                    val monthlyDetails = reconnectionData.monthlyDetails.reversed()
                    val entries = monthlyDetails.mapIndexed { index, item ->
                        Entry(index.toFloat(), item.count.toFloat())
                    }
                    
                    val dataSet = LineDataSet(entries, "Reconexiones").apply {
                        color = tertiaryColor
                        valueTextColor = textColor
                        valueTextSize = 10f
                        setDrawCircles(true)
                        setCircleColor(tertiaryColor)
                        circleRadius = 4f
                        setDrawCircleHole(true)
                        circleHoleRadius = 2f
                        setDrawValues(true)
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return value.toInt().toString()
                            }
                        }
                        lineWidth = 2.5f
                        setDrawFilled(true)
                        fillColor = tertiaryColor
                        fillAlpha = 30
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }
                    
                    val lineData = LineData(dataSet)
                    this.data = lineData
                    
                    val labels = monthlyDetails.map { item ->
                        val parts = item.period.split("-")
                        val month = when (parts[1]) {
                            "01" -> "Ene"
                            "02" -> "Feb"
                            "03" -> "Mar"
                            "04" -> "Abr"
                            "05" -> "May"
                            "06" -> "Jun"
                            "07" -> "Jul"
                            "08" -> "Ago"
                            "09" -> "Set"
                            "10" -> "Oct"
                            "11" -> "Nov"
                            "12" -> "Dic"
                            else -> parts[1]
                        }
                        "$month ${parts[0].substring(2)}"
                    }
                    
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    
                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            e?.let { entry ->
                                val index = entry.x.toInt()
                                if (index >= 0 && index < monthlyDetails.size) {
                                    val selectedMonth = monthlyDetails[index]
                                    val parts = selectedMonth.period.split("-")
                                    val year = parts[0]
                                    val monthNumber = parts[1]
                                    
                                    val monthName = when (monthNumber) {
                                        "01" -> "Enero"
                                        "02" -> "Febrero"
                                        "03" -> "Marzo"
                                        "04" -> "Abril"
                                        "05" -> "Mayo"
                                        "06" -> "Junio"
                                        "07" -> "Julio"
                                        "08" -> "Agosto"
                                        "09" -> "Septiembre"
                                        "10" -> "Octubre"
                                        "11" -> "Noviembre"
                                        "12" -> "Diciembre"
                                        else -> monthNumber
                                    }
                                    
                                    val message = "$monthName $year: ${selectedMonth.count} reconexiones"
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        
                        override fun onNothingSelected() {
                            // No hacer nada cuando no hay selección
                        }
                    })
                    
                    animateX(1500)
                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
    }
}

@Composable
fun MigrationChartContainer(data: Map<String, SubscriptionLogSummary>) {
    val migrationData = data["MIGRATE_SUBSCRIPTION"]
    val context = LocalContext.current
    
    if (migrationData == null) {
        return
    }
    
    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val secondaryColor = MaterialTheme.colorScheme.secondary.toArgb()
    
    ChartCard {
        AndroidView(
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    setDrawGridBackground(false)
                    
                    setDrawBorders(false)
                    setScaleEnabled(true)
                    setPinchZoom(true)
                    axisRight.isEnabled = false
                    
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.granularity = 1f
                    xAxis.setDrawGridLines(false)
                    xAxis.textColor = textColor
                    
                    axisLeft.setDrawGridLines(true)
                    axisLeft.gridColor = gridLineColor
                    axisLeft.textColor = textColor
                    axisLeft.setDrawZeroLine(true)
                    
                    legend.isEnabled = true
                    legend.textSize = 12f
                    legend.textColor = textColor
                    legend.form = Legend.LegendForm.LINE
                    
                    val monthlyDetails = migrationData.monthlyDetails.reversed()
                    val entries = monthlyDetails.mapIndexed { index, item ->
                        Entry(index.toFloat(), item.count.toFloat())
                    }
                    
                    val dataSet = LineDataSet(entries, "Migraciones").apply {
                        color = secondaryColor
                        valueTextColor = textColor
                        valueTextSize = 10f
                        setDrawCircles(true)
                        setCircleColor(secondaryColor)
                        circleRadius = 4f
                        setDrawCircleHole(true)
                        circleHoleRadius = 2f
                        setDrawValues(true)
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String {
                                return value.toInt().toString()
                            }
                        }
                        lineWidth = 2.5f
                        setDrawFilled(true)
                        fillColor = secondaryColor
                        fillAlpha = 30
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }
                    
                    val lineData = LineData(dataSet)
                    this.data = lineData
                    
                    val labels = monthlyDetails.map { item ->
                        val parts = item.period.split("-")
                        val month = when (parts[1]) {
                            "01" -> "Ene"
                            "02" -> "Feb"
                            "03" -> "Mar"
                            "04" -> "Abr"
                            "05" -> "May"
                            "06" -> "Jun"
                            "07" -> "Jul"
                            "08" -> "Ago"
                            "09" -> "Set"
                            "10" -> "Oct"
                            "11" -> "Nov"
                            "12" -> "Dic"
                            else -> parts[1]
                        }
                        "$month ${parts[0].substring(2)}"
                    }
                    
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    
                    setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                        override fun onValueSelected(e: Entry?, h: Highlight?) {
                            e?.let { entry ->
                                val index = entry.x.toInt()
                                if (index >= 0 && index < monthlyDetails.size) {
                                    val selectedMonth = monthlyDetails[index]
                                    val parts = selectedMonth.period.split("-")
                                    val year = parts[0]
                                    val monthNumber = parts[1]
                                    
                                    val monthName = when (monthNumber) {
                                        "01" -> "Enero"
                                        "02" -> "Febrero"
                                        "03" -> "Marzo"
                                        "04" -> "Abril"
                                        "05" -> "Mayo"
                                        "06" -> "Junio"
                                        "07" -> "Julio"
                                        "08" -> "Agosto"
                                        "09" -> "Septiembre"
                                        "10" -> "Octubre"
                                        "11" -> "Noviembre"
                                        "12" -> "Diciembre"
                                        else -> monthNumber
                                    }
                                    
                                    val message = "$monthName $year: ${selectedMonth.count} migraciones"
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        
                        override fun onNothingSelected() {
                            // No hacer nada cuando no hay selección
                        }
                    })
                    
                    animateX(1500)
                    invalidate()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
    }
}

// Extensión para capitalizar textos
fun String.capitalize(locale: Locale): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase(locale) else it.toString() 
    }
} 