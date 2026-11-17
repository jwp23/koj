package io.github.jwp23.koj.ui

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoveIfOnTopTest {
    @Test
    fun removesTheKeyWhileItIsOnTop() {
        val backStack = NavBackStack<NavKey>(Today, Sources, EditSource(null))
        backStack.removeIfOnTop(EditSource(null))
        assertEquals(listOf<NavKey>(Today, Sources), backStack.toList())
    }

    @Test
    fun leavesTheStackAloneOnceTheKeyIsNoLongerOnTop() {
        val backStack = NavBackStack<NavKey>(Today, Sources)
        backStack.removeIfOnTop(EditSource(null))
        assertEquals(listOf<NavKey>(Today, Sources), backStack.toList())
    }
}
