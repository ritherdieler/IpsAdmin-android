package com.dscorp.ispadmin.presentation.ui.features.dashboard

/**
 * Largest [keep] entries plus one closing [othersLabel] row with the rest. An entry the source already calls
 * [othersLabel] (the Core has an "Otros" ticket category) is not ranked; it is added to that closing row.
 */
internal fun topWithOthers(items: List<Pair<String, Int>>, keep: Int, othersLabel: String): List<Pair<String, Int>> {
    if (items.size <= keep + 1) return items
    val (others, ranked) = items.partition { it.first == othersLabel }
    val sorted = ranked.sortedByDescending { it.second }
    val rest = sorted.drop(keep).sumOf { it.second } + others.sumOf { it.second }
    return sorted.take(keep) + (othersLabel to rest)
}
