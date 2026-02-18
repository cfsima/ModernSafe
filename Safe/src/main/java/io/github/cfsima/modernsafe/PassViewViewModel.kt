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

class PassViewViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentPassEntry = MutableStateFlow<PassEntry?>(null)
    val currentPassEntry: StateFlow<PassEntry?> = _currentPassEntry.asStateFlow()

    private val _currentPosition = MutableStateFlow<Int>(-1)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _packageAccess = MutableStateFlow<List<String>>(emptyList())
    val packageAccess: StateFlow<List<String>> = _packageAccess.asStateFlow()

    private var rowIds: LongArray? = null
    private var categoryId: Long? = null

    fun initialize(rowId: Long?, categoryId: Long?, rowIds: LongArray?, listPosition: Int) {
        this.categoryId = categoryId
        this.rowIds = rowIds

        if (listPosition != -1 && rowIds != null && listPosition < rowIds.size) {
             _currentPosition.value = listPosition
             loadPassword(rowIds[listPosition])
        } else if (rowId != null && rowId > 0) {
             loadPassword(rowId)
             if (rowIds != null) {
                 val index = rowIds.indexOf(rowId)
                 if (index != -1) {
                     _currentPosition.value = index
                 }
             }
        }
    }

    private fun loadPassword(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = Passwords.getPassEntry(id, true, false)
            _currentPassEntry.value = entry

            val packages = Passwords.getPackageAccess(id)
            _packageAccess.value = packages ?: emptyList()
        }
    }

    fun loadNext() {
        val currentPos = _currentPosition.value
        rowIds?.let { ids ->
            if (currentPos < ids.size - 1) {
                val newPos = currentPos + 1
                _currentPosition.value = newPos
                loadPassword(ids[newPos])
            }
        }
    }

    fun loadPrevious() {
        val currentPos = _currentPosition.value
        rowIds?.let { ids ->
            if (currentPos > 0) {
                val newPos = currentPos - 1
                _currentPosition.value = newPos
                loadPassword(ids[newPos])
            }
        }
    }

    fun hasNext(): Boolean {
        return rowIds?.let { _currentPosition.value < it.size - 1 } ?: false
    }

    fun hasPrevious(): Boolean {
        return _currentPosition.value > 0
    }

    fun updateLastUsedPassword(password: String) {
        Safe.last_used_password = password
    }

    fun reloadCurrent() {
        val currentEntry = _currentPassEntry.value
        if (currentEntry != null) {
             loadPassword(currentEntry.id)
        }
    }

    fun deleteCurrentEntry() {
         val currentEntry = _currentPassEntry.value ?: return
         viewModelScope.launch(Dispatchers.IO) {
             Passwords.deletePassEntry(currentEntry.id)
         }
    }
}
