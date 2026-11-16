package io.github.jwp23.koj.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.SourceStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SourcesViewModel(sources: SourceStore) : ViewModel() {
    /** The sources that are not archived, by name. Empty until the first read finishes. */
    val state: StateFlow<List<Source>> =
        sources
            .sources()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
