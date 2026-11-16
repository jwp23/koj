package io.github.jwp23.koj.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwp23.koj.form.CheckInField
import io.github.jwp23.koj.form.CheckInForm
import io.github.jwp23.koj.form.toForm
import io.github.jwp23.koj.record.CheckIn
import io.github.jwp23.koj.storage.CheckInStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the Edit check-in screen shows. */
sealed interface EditCheckInState {
    /** The check-in is still being read. */
    data object Loading : EditCheckInState

    /** No check-in has the id the screen was opened with. */
    data object NotFound : EditCheckInState

    /**
     * The stored [checkIn], the [form] being edited, and the fields the last save attempt rejected.
     */
    data class Editing(
        val checkIn: CheckIn,
        val form: CheckInForm,
        val errors: Set<CheckInField> = emptySet(),
    ) : EditCheckInState
}

/** Corrects or deletes the check-in with id [checkInId]. */
class EditCheckInViewModel(private val checkIns: CheckInStore, checkInId: Long) : ViewModel() {
    private val mutableState = MutableStateFlow<EditCheckInState>(EditCheckInState.Loading)

    val state: StateFlow<EditCheckInState> = mutableState

    init {
        viewModelScope.launch {
            mutableState.value =
                checkIns.checkIn(checkInId)?.let { EditCheckInState.Editing(it, it.toForm()) }
                    ?: EditCheckInState.NotFound
        }
    }

    fun change(form: CheckInForm) {
        val current = mutableState.value as? EditCheckInState.Editing ?: return
        mutableState.value = current.copy(form = form)
    }

    /** Saves a valid form and then calls [onDone]; otherwise shows what is wrong. */
    fun save(onDone: () -> Unit) {
        val current = mutableState.value as? EditCheckInState.Editing ?: return
        val errors = current.form.errors()
        mutableState.value = current.copy(errors = errors)
        if (errors.isEmpty()) {
            viewModelScope.launch {
                checkIns.save(current.form.applyTo(current.checkIn))
                onDone()
            }
        }
    }

    /** Deletes the check-in, then calls [onDone]. */
    fun delete(onDone: () -> Unit) {
        val current = mutableState.value as? EditCheckInState.Editing ?: return
        viewModelScope.launch {
            checkIns.delete(current.checkIn)
            onDone()
        }
    }
}
