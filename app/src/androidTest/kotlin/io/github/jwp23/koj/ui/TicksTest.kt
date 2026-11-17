package io.github.jwp23.koj.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TicksTest {
    // currentTime, the virtual time runTest has advanced, is experimental.
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun ticksAtOnceAndThenOnceAMinute() = runTest {
        val tickTimes = mutableListOf<Long>()
        minuteTicks().take(3).collect { tickTimes += currentTime }
        assertEquals(listOf(0L, 60_000L, 120_000L), tickTimes)
    }
}
