package com.herowalls.app.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herowalls.app.data.local.LibraryRepository
import com.herowalls.app.data.model.FeedPage
import com.herowalls.app.data.model.HeroCategory
import com.herowalls.app.data.model.SortOrder
import com.herowalls.app.data.model.Wallpaper
import com.herowalls.app.data.model.WallpaperDirectory
import com.herowalls.app.data.remote.WikimediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val items: List<Wallpaper> = emptyList(),
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false,
    val sort: SortOrder = SortOrder.RELEVANCE,
    val fromCache: Boolean = false,
    val scanned: Int = 0,
    val fallbackNote: String? = null
)

class FeedViewModel(
    private val category: HeroCategory,
    private val wikimedia: WikimediaRepository,
    private val library: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState(sort = SortOrder.RELEVANCE))
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    val favoriteIds: StateFlow<Set<Long>> = library.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val downloadedIds: StateFlow<Set<Long>> = library.observeDownloadedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val seen = LinkedHashSet<Long>()
    private var nextOffset: Int? = 0
    private var loading = false

    init {
        load(reset = true)
    }

    fun setSort(sort: SortOrder) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        load(reset = true, force = true)
    }

    fun refresh() = load(reset = true, force = true)

    fun retry() = load(reset = _state.value.items.isEmpty(), force = true)

    fun loadMore() {
        val current = _state.value
        if (loading || current.endReached || nextOffset == null) return
        load(reset = false)
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch { library.toggleFavorite(wallpaper) }
    }

    private fun load(reset: Boolean, force: Boolean = false) {
        if (loading) return
        loading = true
        viewModelScope.launch {
            if (reset) {
                seen.clear()
                nextOffset = 0
                _state.update { it.copy(loading = true, error = null, endReached = false) }
            } else {
                _state.update { it.copy(loadingMore = true, error = null) }
            }

            val offset = if (reset) 0 else (nextOffset ?: 0)
            runCatching {
                wikimedia.feed(
                    category = category,
                    offset = offset,
                    sort = _state.value.sort,
                    forceRefresh = force
                )
            }.onSuccess { page: FeedPage ->
                WallpaperDirectory.remember(page.items)
                val merged = if (reset) {
                    page.items.distinctBy { it.pageId }
                } else {
                    (_state.value.items + page.items).distinctBy { it.pageId }
                }
                seen.addAll(merged.map { it.pageId })
                nextOffset = page.nextOffset
                _state.update {
                    it.copy(
                        items = merged,
                        loading = false,
                        loadingMore = false,
                        endReached = page.endReached || page.nextOffset == null,
                        fromCache = page.fromCache,
                        scanned = page.totalScanned,
                        fallbackNote = page.fallbackNote,
                        error = null
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        loadingMore = false,
                        error = error.message ?: "Could not reach Wikimedia Commons"
                    )
                }
            }
            loading = false
        }
    }
}
