package com.dscorp.ispadmin.presentation.util

/**
 * Staff-facing 12-hour clock, Spanish (Peru): «9:47 a. m.», «12:00 p. m.» (noon), «12:05 a. m.» (midnight).
 * The suffix is built from the hour on purpose: the JDK/Android Spanish AM/PM symbols vary («a. m.», «a.m.», «AM»).
 */
internal fun clock12(hour: Int, minute: Int): String {
    val h = hour % 24
    val suffix = if (h < 12) "a. m." else "p. m."
    return "${if (h % 12 == 0) 12 else h % 12}:${"%02d".format(minute)} $suffix"
}
