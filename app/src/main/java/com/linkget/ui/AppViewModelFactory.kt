package com.linkget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.linkget.data.library.LibraryRepository
import com.linkget.di.AppContainer
import com.linkget.ui.downloads.DownloadsViewModel
import com.linkget.ui.history.HistoryViewModel
import com.linkget.ui.parse.ParseViewModel
import com.linkget.ui.profile.ProfileViewModel

@Suppress("UNCHECKED_CAST")
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ParseViewModel::class.java) -> ParseViewModel(
                parser = container.parser,
                downloads = container.downloads,
                historyDao = container.historyDao,
                settings = container.settings,
                clipboardReads = container.clipboardReads
            ) as T
            modelClass.isAssignableFrom(DownloadsViewModel::class.java) -> DownloadsViewModel(
                dao = container.dao,
                downloads = container.downloads
            ) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(
                dao = container.historyDao
            ) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(
                settings = container.settings,
                library = LibraryRepository(container.appContext)
            ) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: " + modelClass.name)
        }
    }
}
