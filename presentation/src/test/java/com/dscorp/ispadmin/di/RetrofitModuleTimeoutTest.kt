package com.dscorp.ispadmin.di

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RetrofitModuleTimeoutTest {

    @Test
    fun provideHttpClient_readTimeout_covers_fiber_olt_tr069() {
        val src = source()
        val block = src.substringAfter("fun provideHttpClient").substringBefore("return httpClient.build()")
        val match = Regex("""readTimeout\((\d+),\s*TimeUnit\.(SECONDS|MINUTES)\)""").find(block)
            ?: error("readTimeout not found in provideHttpClient")
        val value = match.groupValues[1].toLong()
        val seconds = if (match.groupValues[2] == "MINUTES") value * 60 else value
        assertTrue("readTimeout must be >= 180s for FIBER register, was ${seconds}s", seconds >= 180)
    }

    private fun source(): String {
        val fromModule = File("src/main/java/com/dscorp/ispadmin/di/RetrofitModule.kt")
        val fromRoot = File("presentation/src/main/java/com/dscorp/ispadmin/di/RetrofitModule.kt")
        return when {
            fromModule.exists() -> fromModule.readText()
            fromRoot.exists() -> fromRoot.readText()
            else -> error("RetrofitModule.kt not found")
        }
    }
}
