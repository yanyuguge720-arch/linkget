package com.linkget.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkget.data.db.HistoryDao
import com.linkget.data.db.HistoryEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val dao: HistoryDao) : ViewModel() {

    val entries: StateFlow<List<HistoryEntry>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { dao.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { dao.clear() }
    }
}
