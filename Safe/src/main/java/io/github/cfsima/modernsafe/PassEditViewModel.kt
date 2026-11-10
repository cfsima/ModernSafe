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

import kotlinx.coroutines.withContext

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
    val error: String? = null,
)

class PassEditViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PassEditUiState(isLoading = true))
    val uiState: StateFlow<PassEditUiState> = _uiState.asStateFlow()

    private var initialEntry: PassEntry? = null
    private var loadedId: Long? = null
    private var loadedCategoryId: Long? = null

    fun load(id: Long, categoryId: Long) {
        if (loadedId == id && loadedCategoryId == categoryId) {
            return
        }
        loadedId = id
        loadedCategoryId = categoryId

        val isExisting = (id != -1L) && (id != 0L)
        if (!isExisting) {
            // New entry: no background database query needed
            initialEntry = null
            _uiState.value = PassEditUiState(
                id = id,
                categoryId = categoryId,
                description = "",
                website = "",
                username = "",
                password = "",
                note = "",
                isLoading = false,
                isModified = false,
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, id = id, categoryId = categoryId)
        viewModelScope.launch {
            val entry = withContext(Dispatchers.IO) {
                Passwords.getPassEntry(id, decrypt = true, descriptionOnly = false)
            }
            if (entry != null) {
                initialEntry = entry
                _uiState.value = _uiState.value.copy(
                    description = entry.plainDescription,
                    website = entry.plainWebsite ?: "",
                    username = entry.plainUsername ?: "",
                    password = entry.plainPassword ?: "",
                    note = entry.plainNote ?: "",
                    isLoading = false,
                    isModified = false,
                )
            } else {
                loadedId = null
                loadedCategoryId = null
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Entry not found",
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
        val init = initialEntry
        return if (init == null) {
            // New entry: modified if any field is not empty
            state.description.isNotEmpty() ||
                    state.website.isNotEmpty() ||
                    state.username.isNotEmpty() ||
                    state.password.isNotEmpty() ||
                    state.note.isNotEmpty()
        } else {
            state.description != init.plainDescription ||
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

    fun onDescriptionChange(value: String) = updateDescription(value)
    fun onWebsiteChange(value: String) = updateWebsite(value)
    fun onUsernameChange(value: String) = updateUsername(value)
    fun onPasswordChange(value: String) = updatePassword(value)
    fun onNoteChange(value: String) = updateNote(value)

    fun save() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value
            val entry = PassEntry()

            entry.category = current.categoryId
            entry.plainDescription = current.description
            entry.plainWebsite = current.website
            entry.plainUsername = current.username
            entry.plainPassword = current.password
            entry.plainNote = current.note

            val isNew = (current.id == -1L) || (current.id == 0L)
            entry.id = if (isNew) 0L else current.id
            if (!isNew) {
                initialEntry?.let {
                    entry.uniqueName = it.uniqueName
                }
            }

            val resultId = Passwords.putPassEntry(entry)
            if (resultId != -1L) {
                _uiState.value = current.copy(
                    isLoading = false,
                    isSaved = true,
                    id = resultId,
                    isModified = false,
                )
            } else {
                _uiState.value = current.copy(
                    isLoading = false,
                    error = "Failed to save entry",
                )
            }
        }
    }

    fun delete() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value
            if ((current.id != -1L) && (current.id != 0L)) {
                Passwords.deletePassEntry(current.id)
            }
            _uiState.value = current.copy(
                isLoading = false,
                isDeleted = true,
                isModified = false,
            )
        }
    }

    @Suppress("unused")
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun consumeSaveEvent() {
        _uiState.value = _uiState.value.copy(isSaved = false, isModified = false)
    }

    fun consumeDeleteEvent() {
        _uiState.value = _uiState.value.copy(isDeleted = false, isModified = false)
    }
}
