package io.github.jwp23.koj.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwp23.koj.form.DrinkField
import io.github.jwp23.koj.form.DrinkForm
import io.github.jwp23.koj.form.formatDecimal
import io.github.jwp23.koj.form.newDrinkForm
import io.github.jwp23.koj.intake.Amounts
import io.github.jwp23.koj.record.Source
import io.github.jwp23.koj.storage.DrinkStore
import io.github.jwp23.koj.storage.SettingsStore
import io.github.jwp23.koj.storage.SourceStore
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * What the Log a drink screen shows once the [source] and the sip setting are read: the [form], the
 * fields the last log attempt rejected, and what the form would log ([amounts], null while the
 * servings cannot be logged).
 */
data class LogDrinkState(
    val source: Source,
    val sipServings: Double,
    val form: DrinkForm,
    val errors: Set<DrinkField> = emptySet(),
    val amounts: Amounts?,
)

/** Logs a drink of the source with id [sourceId], with an amount and start the user chooses. */
class LogDrinkViewModel(
    private val sources: SourceStore,
    private val drinks: DrinkStore,
    settings: SettingsStore,
    private val clock: Clock,
    sourceId: Long,
) : ViewModel() {
    private val mutableState = MutableStateFlow<LogDrinkState?>(null)

    /** Null until the source and settings are read. */
    val state: StateFlow<LogDrinkState?> = mutableState

    init {
        viewModelScope.launch {
            val source = sources.source(sourceId) ?: return@launch
            val form = newDrinkForm(clock.instant(), clock.zone)
            mutableState.value =
                LogDrinkState(
                    source,
                    settings.settings().first().sipServings,
                    form,
                    amounts = amounts(source, form),
                )
        }
    }

    fun change(form: DrinkForm) {
        val current = mutableState.value ?: return
        mutableState.value = current.copy(form = form, amounts = amounts(current.source, form))
    }

    /** Puts the sip setting in the servings field. */
    fun justASip() {
        val current = mutableState.value ?: return
        change(current.form.copy(servings = formatDecimal(current.sipServings)))
    }

    /** Logs a valid form and then calls [onDone]; otherwise shows what is wrong. */
    fun log(onDone: () -> Unit) {
        val current = mutableState.value ?: return
        val errors = current.form.errors(clock.zone, clock.instant())
        mutableState.value = current.copy(errors = errors)
        if (errors.isEmpty()) {
            viewModelScope.launch {
                val form = current.form
                drinks.save(
                    current.source.logged(form.servingsValue(), form.start(clock.zone), clock.zone),
                )
                onDone()
            }
        }
    }

    private fun amounts(source: Source, form: DrinkForm): Amounts? =
        if (form.servingsError() == null) {
            source.logged(form.servingsValue(), clock.instant(), clock.zone).taken
        } else null
}
