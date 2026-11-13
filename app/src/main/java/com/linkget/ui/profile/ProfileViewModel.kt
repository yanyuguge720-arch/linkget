package com.linkget.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkget.data.library.LibraryRepository
import com.linkget.data.library.LocalMediaFile
import com.linkget.data.model.ResourceKind
import com.linkget.data.settings.AppSettings
import com.linkget.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val files: List<LocalMediaFile> = emptyList(),
    val loading: Boolean = false,
    val tab: ResourceKind = ResourceKind.VIDEO,
    val message: String? = null
)

class ProfileViewModel(
    private val settings: SettingsRepository,
    private val library: LibraryRepository
) : ViewModel() {

    val settingsState: StateFlow<AppSettings> = settings.state

    private val _state = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun setTab(tab: ResourceKind) = _state.update { it.copy(tab = tab) }

    fun refresh() {
        val folder = settings.state.value.saveFolder
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            val files = runCatching { library.load(folder) }.getOrDefault(emptyList())
            _state.update { it.copy(files = files, loading = false) }
        }
    }

    fun setClipboardEnabled(enabled: Boolean) = settings.setClipboardEnabled(enabled)
    fun setSaveFolder(folder: String) {
        settings.setSaveFolder(folder)
        refresh()
    }
    fun setPreferredQuality(quality: String) = settings.setPreferredQuality(quality)
    fun setMaxParallel(value: Int) = settings.setMaxParallel(value)
    fun setWifiOnly(enabled: Boolean) = settings.setWifiOnly(enabled)
    fun setApiBase(url: String) = settings.setApiBase(url)
    fun setBilibiliCookie(cookie: String) = settings.setBilibiliCookie(cookie)
    fun clearBilibiliCookie() = settings.clearBilibiliCookie()
    fun setCloudLiveFallback(enabled: Boolean) = settings.setCloudLiveFallback(enabled)

    fun delete(file: LocalMediaFile) {
        viewModelScope.launch {
            val ok = library.delete(file)
            _state.update {
                it.copy(message = if (ok) "已删除" else "删除失败，可能没有权限")
            }
            refresh()
        }
    }

    fun rename(file: LocalMediaFile, newName: String) {
        viewModelScope.launch {
            val ok = library.rename(file, newName)
            _state.update {
                it.copy(message = if (ok) "已重命名" else "重命名失败")
            }
            refresh()
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
