package com.dscorp.ispadmin.observability

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object ObsReplayPrivacy {
    fun interface Hold {
        fun release()
    }

    private val holds = AtomicInteger(0)

    fun hold(): Hold {
        holds.incrementAndGet()
        val released = AtomicBoolean(false)
        return Hold {
            if (released.compareAndSet(false, true)) holds.updateAndGet { (it - 1).coerceAtLeast(0) }
        }
    }

    fun isSuppressed(): Boolean = holds.get() > 0

    internal fun reset() = holds.set(0)
}
