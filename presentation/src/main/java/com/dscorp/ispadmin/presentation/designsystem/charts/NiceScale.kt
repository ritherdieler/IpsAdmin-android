package com.dscorp.ispadmin.presentation.designsystem.charts

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private const val DEFAULT_TICKS = 4
private val STEP_FACTORS = listOf(1.0, 2.0, 5.0, 10.0)

/** Axis from zero to [max] with ticks every [step]. */
data class NiceScale(val max: Double, val step: Double)

/** Axis from [min] to [max] around zero, ticks every [step]. */
data class DivergingScale(val min: Double, val max: Double, val step: Double)

/** Round axis covering [maxValue] in about [DEFAULT_TICKS] steps of 1, 2 or 5 × 10ⁿ; counts never get fractional steps. */
fun niceScale(maxValue: Double, integers: Boolean = false): NiceScale {
    val top = maxValue.coerceAtLeast(0.0)
    if (top == 0.0) return NiceScale(max = 1.0, step = 1.0)
    val raw = top / DEFAULT_TICKS
    val magnitude = 10.0.pow(floor(log10(raw)))
    val step = STEP_FACTORS.map { it * magnitude }.first { it >= raw }.let { if (integers) it.coerceAtLeast(1.0) else it }
    return NiceScale(max = ceil(top / step) * step, step = step)
}

/** Same round scale above and below zero, so up and down bars compare at a glance. */
fun divergingScale(up: Double, down: Double): DivergingScale {
    val scale = niceScale(maxOf(up, down), integers = true)
    return DivergingScale(min = -scale.max, max = scale.max, step = scale.step)
}
