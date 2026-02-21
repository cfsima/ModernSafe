package io.github.cfsima.modernsafe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.cfsima.modernsafe.model.CategoryEntry
import io.github.cfsima.modernsafe.model.PassEntry
import io.github.cfsima.modernsafe.model.Passwords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchResult(
    val id: Long,
    val name: String,
    val categoryName: String,
    val categoryId: Long
)

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val resultIds: LongArray = LongArray(0),
    val isLoading: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        // Ensure Passwords helper is initialized with context
        Passwords.Initialize(application)
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }

        if (newQuery.isBlank()) {
            _uiState.update {
                it.copy(
                    results = emptyList(),
                    resultIds = LongArray(0),
                    isLoading = false
                )
            }
        } else {
            performSearch(newQuery)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }

            val foundResults = mutableListOf<SearchResult>()
            val trimmedQuery = query.trim().lowercase()

            val categories = Passwords.getCategoryEntries()
            for (cat in categories) {
                // Fetch and decrypt all password entries for this category
                // decrypt=true, descriptionOnly=false (search all fields)
                val passwords = Passwords.getPassEntries(cat.id, true, false)

                for (pass in passwords) {
                    val description = pass.plainDescription?.lowercase() ?: ""
                    val website = pass.plainWebsite?.lowercase() ?: ""
                    val username = pass.plainUsername?.lowercase() ?: ""
                    val password = pass.plainPassword?.lowercase() ?: ""
                    val note = pass.plainNote?.lowercase() ?: ""

                    if (description.contains(trimmedQuery) ||
                        website.contains(trimmedQuery) ||
                        username.contains(trimmedQuery) ||
                        password.contains(trimmedQuery) ||
                        note.contains(trimmedQuery)) {

                        foundResults.add(
                            SearchResult(
                                id = pass.id,
                                name = pass.plainDescription ?: "",
                                categoryName = cat.plainName ?: "",
                                categoryId = cat.id
                            )
                        )
                    }
                }
            }

            // Sort results by name (case-insensitive) to match legacy behavior
            foundResults.sortBy { it.name.lowercase() }

            val resultIds = foundResults.map { it.id }.toLongArray()

            _uiState.update {
                it.copy(
                    results = foundResults,
                    resultIds = resultIds,
                    isLoading = false
                )
            }
        }
    }
}
