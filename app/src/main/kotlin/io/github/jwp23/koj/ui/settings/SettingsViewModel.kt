package io.github.jwp23.koj.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwp23.koj.form.SettingsField
import io.github.jwp23.koj.form.SettingsForm
import io.github.jwp23.koj.form.toForm
import io.github.jwp23.koj.storage.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * What the settings screen shows: the [form], the fields the last save attempt rejected, and
 * whether the form as it stands has been [saved].
 */
data class SettingsState(
    val form: SettingsForm,
    val errors: Set<SettingsField> = emptySet(),
    val saved: Boolean = false,
)

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {
    private val mutableState = MutableStateFlow<SettingsState?>(null)
    private var latestSave = 0

    /** Null until the stored settings have been read. */
    val state: StateFlow<SettingsState?> = mutableState

    init {
        viewModelScope.launch {
            mutableState.value = SettingsState(settings.settings().first().toForm())
        }
    }

    fun change(form: SettingsForm) {
        mutableState.value = mutableState.value?.copy(form = form, saved = false)
    }

    /** Stores a valid form and then reports it saved; otherwise shows what is wrong. */
    fun save() {
        val current = mutableState.value ?: return
        val errors = current.form.errors()
        mutableState.value = current.copy(errors = errors, saved = false)
        if (errors.isEmpty()) {
            val attempt = ++latestSave
            viewModelScope.launch {
                settings.save(current.form.toSettings())
                // An earlier save finishing late must not mark a later attempt's form saved.
                if (attempt == latestSave) {
                    mutableState.value =
                        mutableState.value?.takeIf { it.form == current.form }?.copy(saved = true)
                            ?: mutableState.value
                }
            }
        }
    }
}
