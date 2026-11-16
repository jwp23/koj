package io.github.jwp23.koj.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.jwp23.koj.AppContainer

@Composable
fun KojApp(container: AppContainer) {
    val backStack = rememberNavBackStack(Today)
    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    for (destination in barDestinations) {
                        NavigationBarItem(
                            selected = backStack.last() == destination.key,
                            onClick = { backStack.chooseBarDestination(destination.key) },
                            icon = {},
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { padding ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(padding),
                onBack = { backStack.removeLastOrNull() },
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                entryProvider =
                    entryProvider {
                        entry<Today> { Placeholder("Today") }
                        entry<History> { Placeholder("History") }
                        entry<Sources> { Placeholder("Sources") }
                        entry<AppSettings> { Placeholder("Settings") }
                    },
            )
        }
    }
}

/** Leaves Today alone on the stack, then adds the chosen destination unless it is Today. */
private fun NavBackStack<NavKey>.chooseBarDestination(key: NavKey) {
    while (size > 1) removeLast()
    if (key != Today) add(key)
}

@Composable
private fun Placeholder(name: String) {
    Text(
        name,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
}
