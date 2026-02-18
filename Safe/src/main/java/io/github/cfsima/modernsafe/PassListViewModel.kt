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

data class PassListUiState(
    val entries: List<PassEntry> = emptyList(),
    val categoryName: String = "",
    val isLoading: Boolean = false
)

class PassListViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PassListUiState(isLoading = true))
    val uiState: StateFlow<PassListUiState> = _uiState.asStateFlow()

    private var currentCategoryId: Long = -1

    fun load(categoryId: Long) {
        currentCategoryId = categoryId
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val categoryEntry = Passwords.getCategoryEntry(categoryId)
            val categoryName = categoryEntry?.plainName ?: ""

            // getPassEntries(categoryId, decrypt=true, descriptionOnly=true)
            val entries = Passwords.getPassEntries(categoryId, true, true) ?: ArrayList()

            _uiState.value = PassListUiState(
                entries = entries,
                categoryName = categoryName,
                isLoading = false
            )
        }
    }

    fun reload() {
        if (currentCategoryId != -1L) {
            load(currentCategoryId)
        }
    }

    fun deleteEntry(entry: PassEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            Passwords.deletePassEntry(entry.id)
            reload()
        }
    }

    fun moveEntry(entry: PassEntry, newCategoryId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            Passwords.updatePassCategory(entry.id, newCategoryId)
            reload()
        }
    }

    fun getAvailableCategories(): Map<String, Long> {
        val allCategories = Passwords.getCategoryNameToId() ?: HashMap()
        val currentName = _uiState.value.categoryName
        if (currentName.isNotEmpty()) {
            allCategories.remove(currentName)
        }
        return allCategories
    }
}
