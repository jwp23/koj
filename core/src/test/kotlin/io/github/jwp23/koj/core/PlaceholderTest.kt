package io.github.jwp23.koj.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceholderTest {
    @Test
    fun appNameIsKoj() {
        assertEquals("koj", Placeholder.appName())
    }
}
