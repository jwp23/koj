package io.github.jwp23.koj.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwp23.koj.form.DrinkField
import io.github.jwp23.koj.form.DrinkForm
import io.github.jwp23.koj.form.toEditForm
import io.github.jwp23.koj.record.LoggedDrink
import io.github.jwp23.koj.storage.DrinkStore
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What the Edit drink screen shows. */
sealed interface EditDrinkState {
    /** The drink is still being read. */
    data object Loading : EditDrinkState

    /** No drink has the id the screen was opened with. */
    data object NotFound : EditDrinkState

    /**
     * The stored [drink], the [form] being edited, the fields the last save attempt rejected, and
     * whether a save or delete is [busy] being written.
     */
    data class Editing(
        val drink: LoggedDrink,
        val form: DrinkForm,
        val errors: Set<DrinkField> = emptySet(),
        val busy: Boolean = false,
    ) : EditDrinkState
}

/** Corrects or deletes the drink with id [drinkId]. */
class EditDrinkViewModel(
    private val drinks: DrinkStore,
    private val clock: Clock,
    drinkId: Long,
) : ViewModel() {
    private val mutableState = MutableStateFlow<EditDrinkState>(EditDrinkState.Loading)

    val state: StateFlow<EditDrinkState> = mutableState

    init {
        viewModelScope.launch {
            mutableState.value =
                drinks.drink(drinkId)?.let { EditDrinkState.Editing(it, it.toEditForm()) }
                    ?: EditDrinkState.NotFound
        }
    }

    /** Takes [form] as the edit, unless a save or delete is being written and would discard it. */
    fun change(form: DrinkForm) {
        val current = mutableState.value as? EditDrinkState.Editing ?: return
        if (current.busy) return
        mutableState.value = current.copy(form = form)
    }

    /** Saves a valid form and then calls [onDone]; otherwise shows what is wrong. */
    fun save(onDone: () -> Unit) {
        val current = mutableState.value as? EditDrinkState.Editing ?: return
        if (current.busy) return
        val errors = current.form.errors(current.drink.zone, clock.instant())
        mutableState.value = current.copy(errors = errors)
        if (errors.isEmpty())
            write(current, onDone) { drinks.save(current.form.applyTo(current.drink)) }
    }

    /** Deletes the drink, then calls [onDone]. */
    fun delete(onDone: () -> Unit) {
        val current = mutableState.value as? EditDrinkState.Editing ?: return
        if (current.busy) return
        write(current, onDone) { drinks.delete(current.drink) }
    }

    /**
     * Runs [write] and then [onDone], refusing a second save or delete until it finishes. If the
     * drink no longer exists, shows that instead of calling [onDone].
     */
    private fun write(
        current: EditDrinkState.Editing,
        onDone: () -> Unit,
        write: suspend () -> Unit,
    ) {
        mutableState.value = current.copy(busy = true)
        viewModelScope.launch {
            if (entryStillStored(write)) {
                mutableState.value =
                    (mutableState.value as? EditDrinkState.Editing ?: current).copy(busy = false)
                onDone()
            } else {
                mutableState.value = EditDrinkState.NotFound
            }
        }
    }
}
