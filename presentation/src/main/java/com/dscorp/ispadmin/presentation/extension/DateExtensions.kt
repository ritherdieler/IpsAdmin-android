package com.dscorp.ispadmin.presentation.extension

import java.util.Calendar


fun Calendar.firstDayFromCurrentMonth(): Long {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.MINUTE, 0)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return this.timeInMillis
}


fun Calendar.lastDayFromCurrentMonth(): Long {
    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
    return this.timeInMillis
}

