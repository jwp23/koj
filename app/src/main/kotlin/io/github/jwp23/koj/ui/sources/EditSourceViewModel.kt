package io.github.jwp23.koj.ui.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwp23.koj.form.SourceField
import io.github.jwp23.koj.form.SourceForm
import io.github.jwp23.koj.form.toForm
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.SourceStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * What the edit screen shows: the [form], the fields the last save attempt rejected, and the
 * [stored] source being edited (null when adding, or until it has been read), whether it was
 * [notFound] when read, and whether a save or delete is [busy] being written.
 */
data class EditSourceState(
    val form: SourceForm = SourceForm(),
    val errors: Set<SourceField> = emptySet(),
    val stored: Source? = null,
    val notFound: Boolean = false,
    val busy: Boolean = false,
)

/** Edits the source with id [sourceId], or a new source when it is null. */
class EditSourceViewModel(private val sources: SourceStore, private val sourceId: Long?) :
    ViewModel() {
    private val mutableState = MutableStateFlow(EditSourceState())

    /** True when adding a source; false when editing, even before the stored source is read. */
    val isNew = sourceId == null

    val state: StateFlow<EditSourceState> = mutableState

    init {
        if (sourceId != null) {
            viewModelScope.launch {
                val source = sources.source(sourceId)
                mutableState.value =
                    if (source == null) EditSourceState(notFound = true)
                    else EditSourceState(source.toForm(), stored = source)
            }
        }
    }

    fun change(form: SourceForm) {
        mutableState.value = mutableState.value.copy(form = form)
    }

    /** Saves a valid form and then calls [onDone]; otherwise shows what is wrong. */
    fun save(onDone: () -> Unit) {
        if (mutableState.value.busy) return
        val form = mutableState.value.form
        val errors = form.errors()
        mutableState.value = mutableState.value.copy(errors = errors)
        if (errors.isEmpty()) {
            write(onDone) { sources.save(form.toSource(sourceId ?: 0)) }
        }
    }

    /** Deletes the stored source, then calls [onDone]. */
    fun delete(onDone: () -> Unit) {
        if (mutableState.value.busy) return
        val stored = mutableState.value.stored ?: return
        write(onDone) { sources.delete(stored) }
    }

    /** Runs [write] and then [onDone], refusing a second save or delete until it finishes. */
    private fun write(onDone: () -> Unit, write: suspend () -> Unit) {
        mutableState.value = mutableState.value.copy(busy = true)
        viewModelScope.launch {
            try {
                write()
            } finally {
                mutableState.value = mutableState.value.copy(busy = false)
            }
            onDone()
        }
    }
}
