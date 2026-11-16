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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.jwp23.koj.AppContainer
import io.github.jwp23.koj.ui.log.LogDrinkScreen
import io.github.jwp23.koj.ui.log.LogDrinkViewModel
import io.github.jwp23.koj.ui.settings.SettingsScreen
import io.github.jwp23.koj.ui.settings.SettingsViewModel
import io.github.jwp23.koj.ui.sources.EditSourceScreen
import io.github.jwp23.koj.ui.sources.EditSourceViewModel
import io.github.jwp23.koj.ui.sources.SourcesScreen
import io.github.jwp23.koj.ui.sources.SourcesViewModel
import io.github.jwp23.koj.ui.today.TodayScreen
import io.github.jwp23.koj.ui.today.TodayViewModel

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
                        entry<Today> {
                            TodayScreen(
                                viewModel(
                                    factory =
                                        viewModelFactory {
                                            initializer {
                                                TodayViewModel(
                                                    container.sources,
                                                    container.drinks,
                                                    container.checkIns,
                                                    container.settings,
                                                    container.clock,
                                                )
                                            }
                                        },
                                ),
                                onLogWithOptions = { backStack.add(LogDrink(it)) },
                                onOpenSources = { backStack.chooseBarDestination(Sources) },
                            )
                        }
                        entry<History> { Placeholder("History") }
                        entry<Sources> {
                            SourcesScreen(
                                viewModel(
                                    factory =
                                        viewModelFactory {
                                            initializer { SourcesViewModel(container.sources) }
                                        },
                                ),
                                onAdd = { backStack.add(EditSource(null)) },
                                onEdit = { backStack.add(EditSource(it)) },
                            )
                        }
                        entry<EditSource> { key ->
                            EditSourceScreen(
                                viewModel(
                                    factory =
                                        viewModelFactory {
                                            initializer {
                                                EditSourceViewModel(container.sources, key.sourceId)
                                            }
                                        },
                                ),
                                onDone = { backStack.removeIfOnTop(key) },
                            )
                        }
                        entry<LogDrink> { key ->
                            LogDrinkScreen(
                                viewModel(
                                    factory =
                                        viewModelFactory {
                                            initializer {
                                                LogDrinkViewModel(
                                                    container.sources,
                                                    container.drinks,
                                                    container.settings,
                                                    container.clock,
                                                    key.sourceId,
                                                )
                                            }
                                        },
                                ),
                                onDone = { backStack.removeIfOnTop(key) },
                            )
                        }
                        entry<AppSettings> {
                            SettingsScreen(
                                viewModel(
                                    factory =
                                        viewModelFactory {
                                            initializer { SettingsViewModel(container.settings) }
                                        },
                                ),
                            )
                        }
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

/** Removes [key] only while it is the top destination, so a late finish cannot pop another. */
internal fun NavBackStack<NavKey>.removeIfOnTop(key: NavKey) {
    if (lastOrNull() == key) removeLast()
}

@Composable
private fun Placeholder(name: String) {
    Text(
        name,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() },
    )
}
