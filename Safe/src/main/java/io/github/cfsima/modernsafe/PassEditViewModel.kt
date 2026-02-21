package io.github.cfsima.modernsafe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cfsima.modernsafe.model.PassEntry
import io.github.cfsima.modernsafe.model.Passwords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PassEditUiState(
    val id: Long = -1,
    val categoryId: Long = -1,
    val description: String = "",
    val website: String = "",
    val username: String = "",
    val password: String = "",
    val note: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val isModified: Boolean = false,
    val error: String? = null
)

class PassEditViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PassEditUiState(isLoading = true))
    val uiState: StateFlow<PassEditUiState> = _uiState.asStateFlow()

    private var initialEntry: PassEntry? = null

    fun load(id: Long, categoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _uiState.value
            _uiState.value = currentState.copy(isLoading = true, id = id, categoryId = categoryId)

            if (id != -1L && id != 0L) {
                // Load existing entry
                val entry = Passwords.getPassEntry(id, true, false)
                if (entry != null) {
                    initialEntry = entry
                    _uiState.value = _uiState.value.copy(
                        description = entry.plainDescription ?: "",
                        website = entry.plainWebsite ?: "",
                        username = entry.plainUsername ?: "",
                        password = entry.plainPassword ?: "",
                        note = entry.plainNote ?: "",
                        isLoading = false,
                        isModified = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Entry not found"
                    )
                }
            } else {
                // New entry
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isModified = false // Not modified initially
                )
            }
        }
    }

    private fun updateState(update: (PassEditUiState) -> PassEditUiState) {
        val newState = update(_uiState.value)
        val isModified = checkIfModified(newState)
        _uiState.value = newState.copy(isModified = isModified)
    }

    private fun checkIfModified(state: PassEditUiState): Boolean {
        if (initialEntry == null) {
            // New entry: modified if any field is not empty
            return state.description.isNotEmpty() ||
                   state.website.isNotEmpty() ||
                   state.username.isNotEmpty() ||
                   state.password.isNotEmpty() ||
                   state.note.isNotEmpty()
        } else {
            val init = initialEntry!!
            return state.description != (init.plainDescription ?: "") ||
                   state.website != (init.plainWebsite ?: "") ||
                   state.username != (init.plainUsername ?: "") ||
                   state.password != (init.plainPassword ?: "") ||
                   state.note != (init.plainNote ?: "")
        }
    }

    fun updateDescription(value: String) {
        updateState { it.copy(description = value) }
    }

    fun updateWebsite(value: String) {
        updateState { it.copy(website = value) }
    }

    fun updateUsername(value: String) {
        updateState { it.copy(username = value) }
    }

    fun updatePassword(value: String) {
        updateState { it.copy(password = value) }
    }

    fun updateNote(value: String) {
        updateState { it.copy(note = value) }
    }

    fun save() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value
            val entry = PassEntry()

            entry.category = current.categoryId
            entry.plainDescription = current.description
            entry.plainWebsite = current.website
            entry.plainUsername = current.username
            entry.plainPassword = current.password
            entry.plainNote = current.note

            var success = false
            var newId = current.id

            if (current.id == -1L || current.id == 0L) {
                entry.id = 0
                newId = Passwords.putPassEntry(entry)
                if (newId != -1L) success = true
            } else {
                entry.id = current.id
                initialEntry?.let {
                    entry.uniqueName = it.uniqueName
                }
                val result = Passwords.putPassEntry(entry)
                if (result != -1L) success = true
            }

            if (success) {
                _uiState.value = current.copy(isSaved = true, id = if(newId != -1L) newId else current.id)
            } else {
                _uiState.value = current.copy(error = "Failed to save entry")
            }
        }
    }

    fun delete() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value
            if (current.id != -1L && current.id != 0L) {
                Passwords.deletePassEntry(current.id)
            }
            _uiState.value = current.copy(isDeleted = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun consumeSaveEvent() {
         _uiState.value = _uiState.value.copy(isSaved = false)
    }

    fun consumeDeleteEvent() {
         _uiState.value = _uiState.value.copy(isDeleted = false)
    }
}
