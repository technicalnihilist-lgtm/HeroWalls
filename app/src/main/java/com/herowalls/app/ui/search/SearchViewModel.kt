package com.herowalls.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herowalls.app.data.local.LibraryRepository
import com.herowalls.app.data.model.CollectionGroup
import com.herowalls.app.data.model.HeroCategory
import com.herowalls.app.data.model.SortOrder
import com.herowalls.app.data.model.Wallpaper
import com.herowalls.app.data.model.WallpaperDirectory
import com.herowalls.app.data.remote.WikimediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val submitted: String = "",
    val items: List<Wallpaper> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false,
    val hasSearched: Boolean = false
)

class SearchViewModel(
    private val wikimedia: WikimediaRepository,
    private val library: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recentSearches: StateFlow<List<String>> = library.observeRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteIds: StateFlow<Set<Long>> = library.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val downloadedIds: StateFlow<Set<Long>> = library.observeDownloadedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var debounceJob: Job? = null
    private var nextOffset: Int? = 0
    private var loading = false

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        debounceJob?.cancel()
        if (value.trim().length < 3) {
            if (value.isBlank()) {
                _state.update {
                    it.copy(items = emptyList(), hasSearched = false, submitted = "", error = null)
                }
            }
            return
        }
        debounceJob = viewModelScope.launch {
            delay(450)
            search(value.trim())
        }
    }

    fun submit(query: String = _state.value.query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        _state.update { it.copy(query = trimmed) }
        viewModelScope.launch { library.recordSearch(trimmed) }
        search(trimmed)
    }

    fun clearQuery() {
        debounceJob?.cancel()
        _state.update {
            it.copy(query = "", submitted = "", items = emptyList(), hasSearched = false, error = null)
        }
    }

    fun clearHistory() {
        viewModelScope.launch { library.clearSearches() }
    }

    fun loadMore() {
        val s = _state.value
        if (loading || !s.hasSearched || s.endReached || nextOffset == null) return
        run(s.submitted, nextOffset ?: 0, reset = false)
    }

    fun retry() {
        if (_state.value.submitted.isNotBlank()) search(_state.value.submitted)
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch { library.toggleFavorite(wallpaper) }
    }

    private fun search(query: String) {
        if (query.equals(_state.value.submitted, ignoreCase = true) && _state.value.items.isNotEmpty()) return
        nextOffset = 0
        run(query, 0, reset = true)
    }

    private fun run(query: String, offset: Int, reset: Boolean) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            _state.update {
                it.copy(
                    submitted = query,
                    loading = reset,
                    loadingMore = !reset,
                    hasSearched = true,
                    error = null,
                    items = if (reset) emptyList() else it.items
                )
            }
            val category = HeroCategory(
                id = "search",
                name = query,
                searchTerm = query,
                group = CollectionGroup.COLLECTIONS
            )
            runCatching {
                wikimedia.feed(category = category, offset = offset, sort = SortOrder.RELEVANCE)
            }.onSuccess { page ->
                WallpaperDirectory.remember(page.items)
                nextOffset = page.nextOffset
                _state.update {
                    val merged = if (reset) page.items else (it.items + page.items).distinctBy { w -> w.pageId }
                    it.copy(
                        items = merged,
                        loading = false,
                        loadingMore = false,
                        endReached = page.endReached || page.nextOffset == null,
                        error = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        error = error.message ?: "Search failed"
                    )
                }
            }
            loading = false
        }
    }
}
