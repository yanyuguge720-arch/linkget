package com.linkget.ui.parse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkget.data.db.HistoryDao
import com.linkget.data.db.HistoryEntry
import com.linkget.data.download.DownloadRepository
import com.linkget.data.model.MediaResource
import com.linkget.data.model.ParsedWork
import com.linkget.data.parser.LinkText
import com.linkget.data.parser.ParseCoordinator
import com.linkget.data.parser.readableMessage
import com.linkget.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClipboardSuggestion(val text: String, val url: String)

data class ParseUiState(
    val input: String = "",
    val parsing: Boolean = false,
    val error: String? = null,
    val result: ParsedWork? = null,
    val selected: Set<String> = emptySet(),
    val clipboardSuggestion: ClipboardSuggestion? = null,
    val saving: Boolean = false,
    val queuedCount: Int = 0,
    val historySaved: Boolean = false,
    val extractAudio: Boolean = false
)

class ParseViewModel(
    private val parser: ParseCoordinator,
    private val downloads: DownloadRepository,
    private val historyDao: HistoryDao,
    private val settings: SettingsRepository,
    clipboardReads: SharedFlow<String?>
) : ViewModel() {

    private val _state = MutableStateFlow(ParseUiState())
    val uiState: StateFlow<ParseUiState> = _state.asStateFlow()

    val recentHistory = historyDao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    private var parseJob: Job? = null
    private var lastClipboardUrl: String? = null

    init {
        viewModelScope.launch {
            clipboardReads.collect { onClipboardText(it) }
        }
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value, error = null) }
    }

    fun paste(text: String?) {
        if (text.isNullOrBlank()) return
        _state.update { it.copy(input = text.trim(), error = null) }
    }

    fun clearInput() {
        _state.update { it.copy(input = "", error = null) }
    }

    fun parse() {
        val text = _state.value.input.trim()
        if (text.isBlank()) {
            _state.update { it.copy(error = "先粘贴一条分享链接") }
            return
        }
        parseJob?.cancel()
        _state.update {
            it.copy(
                parsing = true,
                error = null,
                result = null,
                selected = emptySet(),
                historySaved = false,
                extractAudio = false
            )
        }
        parseJob = viewModelScope.launch {
            try {
                val work = parser.parse(text)
                val default = defaultVideoKey(work, settings.state.value.preferredQuality)
                _state.update {
                    it.copy(
                        parsing = false,
                        result = work,
                        selected = default?.let { key -> setOf(key) } ?: emptySet()
                    )
                }
                recordHistory(work)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(parsing = false, error = e.readableMessage()) }
            } finally {
                parseJob = null
            }
        }
    }

    fun parseUrl(url: String) {
        _state.update { it.copy(input = url, error = null) }
        parse()
    }

    fun cancelParse() {
        parseJob?.cancel()
        parseJob = null
        _state.update { it.copy(parsing = false, error = null) }
    }

    fun dismissResult() {
        _state.update {
            it.copy(result = null, selected = emptySet(), extractAudio = false)
        }
    }

    fun toggleExtractAudio() {
        _state.update { it.copy(extractAudio = !it.extractAudio) }
    }


    fun toggleResource(resource: MediaResource) {
        _state.update { state ->
            val next = if (resource.key in state.selected) {
                state.selected - resource.key
            } else {
                state.selected + resource.key
            }
            state.copy(selected = next)
        }
    }

    fun selectAll() {
        val work = _state.value.result ?: return
        _state.update { it.copy(selected = work.resources.map { r -> r.key }.toSet()) }
    }

    fun clearSelection() {
        _state.update { it.copy(selected = emptySet()) }
    }

    fun enqueueSelected() {
        val state = _state.value
        val work = state.result ?: return
        if (state.saving) return
        val chosen = work.resources.filter { it.key in state.selected }
        if (chosen.isEmpty()) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val count = downloads.enqueue(work, chosen, extractAudio = state.extractAudio)
                _state.update {
                    it.copy(
                        saving = false,
                        result = null,
                        selected = emptySet(),
                        queuedCount = count,
                        extractAudio = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(saving = false, error = e.message ?: "加入下载队列失败")
                }
            }
        }
    }

    fun consumeQueued() {
        _state.update { it.copy(queuedCount = 0) }
    }

    fun onClipboardText(text: String?) {
        if (!settings.state.value.clipboardEnabled) return
        val value = text?.trim().orEmpty()
        if (value.isEmpty()) return
        val url = LinkText.extractUrl(value) ?: run {
            lastClipboardUrl = null
            return
        }
        if (url == lastClipboardUrl) return
        lastClipboardUrl = url
        if (_state.value.input.trim() == value) return
        _state.update { it.copy(clipboardSuggestion = ClipboardSuggestion(value, url)) }
    }

    fun acceptClipboardSuggestion() {
        val suggestion = _state.value.clipboardSuggestion ?: return
        _state.update { it.copy(clipboardSuggestion = null, input = suggestion.text, error = null) }
    }

    fun dismissClipboardSuggestion() {
        _state.update { it.copy(clipboardSuggestion = null) }
    }

    private fun defaultVideoKey(work: ParsedWork, preferred: String): String? {
        val videos = work.normalVideos
        if (videos.isEmpty()) return null
        if (preferred == "720P") {
            return videos.firstOrNull { it.quality == "720P" }?.key
                ?: videos.firstOrNull { it.quality in setOf("480P", "360P") }?.key
                ?: videos.last().key
        }
        return videos.first().key
    }

    private fun recordHistory(work: ParsedWork) {
        if (work.sourceUrl.isBlank()) return
        viewModelScope.launch {
            runCatching {
                historyDao.insert(
                    HistoryEntry(
                        id = Integer.toHexString(work.sourceUrl.hashCode()),
                        sourceUrl = work.sourceUrl,
                        title = work.title,
                        author = work.author,
                        coverUrl = work.coverUrl,
                        platform = work.platform.name,
                        parsedAt = System.currentTimeMillis()
                    )
                )
                _state.update { it.copy(historySaved = true) }
            }
        }
    }
}