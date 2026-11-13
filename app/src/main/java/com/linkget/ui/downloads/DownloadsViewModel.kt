package com.linkget.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkget.data.db.DownloadDao
import com.linkget.data.db.DownloadTaskEntity
import com.linkget.data.download.DownloadRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val dao: DownloadDao,
    private val downloads: DownloadRepository
) : ViewModel() {

    val tasks: StateFlow<List<DownloadTaskEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pause(id: String) = downloads.pause(id)
    fun resume(id: String) = downloads.resume(id)
    fun delete(id: String) = downloads.delete(id)

    fun retryAll() {
        viewModelScope.launch { downloads.retryAllFailed() }
    }

    fun resumeAll() {
        viewModelScope.launch { downloads.resumeAllPaused() }
    }

    fun clearFinished() {
        viewModelScope.launch { downloads.clearFinished() }
    }
}
