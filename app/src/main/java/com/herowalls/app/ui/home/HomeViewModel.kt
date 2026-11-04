package com.herowalls.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herowalls.app.data.local.LibraryRepository
import com.herowalls.app.data.model.Catalog
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Section(
    val items: List<Wallpaper> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

data class HomeUiState(
    val latest: Section = Section(),
    val marvel: Section = Section(),
    val dc: Section = Section(),
    val villains: Section = Section(),
    val daily: Wallpaper? = null,
    val dailyLoading: Boolean = true,
    val featured: Map<String, Wallpaper> = emptyMap(),
    val newIds: Set<Long> = emptySet(),
    val lastLiveUpdate: Long = 0L,
    val live: Boolean = true,
    val message: String? = null
)

private enum class SectionKind { LATEST, MARVEL, DC, VILLAINS }

class HomeViewModel(
    private val wikimedia: WikimediaRepository,
    private val library: LibraryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val favoriteIds: StateFlow<Set<Long>> = library.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val downloadedIds: StateFlow<Set<Long>> = library.observeDownloadedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val recentlyViewed: StateFlow<List<Wallpaper>> = library.observeRecentlyViewed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val seenLatest = LinkedHashSet<Long>()
    private var liveJob: Job? = null

    init {
        loadEverything()
        startLiveUpdates()
    }

    fun loadEverything(force: Boolean = false) {
        loadSection(SectionKind.LATEST, latestCategory(), SortOrder.NEWEST, delayMs = 0, force = force)
        loadSection(SectionKind.MARVEL, groupCategory("marvel-all", "Marvel Universe", "Marvel Comics", CollectionGroup.MARVEL), SortOrder.RELEVANCE, 350, force)
        loadSection(SectionKind.DC, groupCategory("dc-all", "DC Universe", "DC Comics", CollectionGroup.DC), SortOrder.RELEVANCE, 700, force)
        loadSection(SectionKind.VILLAINS, groupCategory("villains-all", "Popular Villains", "comic book villain", CollectionGroup.VILLAINS), SortOrder.RELEVANCE, 1_050, force)
        loadDaily(force)
        loadFeatured(force)
    }

    fun refresh() {
        _state.update { it.copy(message = null) }
        loadEverything(force = true)
    }

    fun dismissMessage() = _state.update { it.copy(message = null) }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            val added = library.toggleFavorite(wallpaper)
            _state.update {
                it.copy(message = if (added) "Added to favourites" else "Removed from favourites")
            }
        }
    }

    private fun loadSection(
        kind: SectionKind,
        category: HeroCategory,
        sort: SortOrder,
        delayMs: Long,
        force: Boolean,
        live: Boolean = false
    ) {
        viewModelScope.launch {
            if (delayMs > 0) delay(delayMs)
            updateSection(kind) { it.copy(loading = it.items.isEmpty(), error = null) }
            runCatching {
                wikimedia.feed(
                    category = category,
                    offset = 0,
                    limit = if (live) 24 else 12,
                    sort = sort,
                    forceRefresh = force
                )
            }.onSuccess { page ->
                WallpaperDirectory.remember(page.items)
                if (live) mergeLive(page.items) else updateSection(kind) {
                    it.copy(items = page.items, loading = false, error = null)
                }
            }.onFailure { error ->
                updateSection(kind) {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Could not load this collection"
                    )
                }
            }
        }
    }

    /** Merge a fresh live page on top of what is already on screen, flagging what is new. */
    private fun mergeLive(items: List<Wallpaper>) {
        val fresh = items.filter { seenLatest.add(it.pageId) }
        _state.update { current ->
            val combined = (fresh + current.latest.items)
                .distinctBy { it.pageId }
                .take(48)
            current.copy(
                latest = Section(items = combined, loading = false, error = null),
                newIds = if (current.latest.items.isEmpty()) emptySet() else fresh.map { it.pageId }.toSet(),
                lastLiveUpdate = System.currentTimeMillis()
            )
        }
    }

    private fun loadDaily(force: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(dailyLoading = true) }
            runCatching { wikimedia.dailyWallpaper() }
                .onSuccess { wallpaper ->
                    wallpaper?.let { WallpaperDirectory.remember(listOf(it)) }
                    _state.update { it.copy(daily = wallpaper, dailyLoading = false) }
                }
                .onFailure { _state.update { it.copy(dailyLoading = false) } }
        }
    }

    /**
     * Featured characters: one small query per character, loaded one after another
     * so the home screen paints immediately and Commons is never hit with a burst.
     */
    private fun loadFeatured(force: Boolean) {
        viewModelScope.launch {
            Catalog.featured().forEach { category ->
                val cached = _state.value.featured[category.id]
                if (cached != null && !force) return@forEach
                runCatching {
                    wikimedia.feed(
                        category = category,
                        offset = 0,
                        limit = 4,
                        sort = SortOrder.RELEVANCE,
                        minSide = 600,
                        forceRefresh = force
                    )
                }.onSuccess { page ->
                    val first = page.items.firstOrNull() ?: return@onSuccess
                    WallpaperDirectory.remember(page.items)
                    _state.update { it.copy(featured = it.featured + (category.id to first)) }
                }
                delay(220)
            }
        }
    }

    private fun startLiveUpdates() {
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            while (isActive) {
                delay(LIVE_INTERVAL_MS)
                if (_state.value.live) {
                    loadSection(
                        SectionKind.LATEST,
                        latestCategory(),
                        SortOrder.NEWEST,
                        delayMs = 0,
                        force = true,
                        live = true
                    )
                }
            }
        }
    }

    fun setLive(enabled: Boolean) {
        _state.update { it.copy(live = enabled) }
    }

    private fun updateSection(kind: SectionKind, transform: (Section) -> Section) {
        _state.update { current ->
            when (kind) {
                SectionKind.LATEST -> current.copy(latest = transform(current.latest))
                SectionKind.MARVEL -> current.copy(marvel = transform(current.marvel))
                SectionKind.DC -> current.copy(dc = transform(current.dc))
                SectionKind.VILLAINS -> current.copy(villains = transform(current.villains))
            }
        }
    }

    private fun latestCategory() = HeroCategory(
        id = "latest",
        name = "Latest wallpapers",
        searchTerm = Catalog.LIVE_FEED_QUERY,
        group = CollectionGroup.COLLECTIONS
    )

    private fun groupCategory(id: String, name: String, term: String, group: CollectionGroup) =
        HeroCategory(id = id, name = name, searchTerm = term, group = group)

    companion object {
        const val LIVE_INTERVAL_MS = 45_000L
    }
}
