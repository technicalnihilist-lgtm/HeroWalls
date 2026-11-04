package com.herowalls.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herowalls.app.data.download.WallpaperDownloader
import com.herowalls.app.data.local.DownloadedWallpaper
import com.herowalls.app.data.local.LibraryRepository
import com.herowalls.app.data.model.Wallpaper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs the Favourites, Downloads and Credits screens. */
class LibraryViewModel(
    private val library: LibraryRepository,
    private val downloader: WallpaperDownloader
) : ViewModel() {

    val favorites: StateFlow<List<Wallpaper>> = library.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloads: StateFlow<List<DownloadedWallpaper>> = library.observeDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyViewed: StateFlow<List<Wallpaper>> = library.observeRecentlyViewed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Every image the user has seen or saved, de-duplicated - the credits ledger. */
    val creditedImages: StateFlow<List<Wallpaper>> = combine(
        library.observeFavorites(),
        library.observeRecentlyViewed(),
        library.observeDownloads()
    ) { favs, recents, downs ->
        (favs + recents + downs.map { it.wallpaper })
            .distinctBy { it.pageId }
            .sortedBy { it.characterName.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch { library.toggleFavorite(wallpaper) }
    }

    fun removeDownload(wallpaper: Wallpaper) {
        viewModelScope.launch { downloader.delete(wallpaper) }
    }
}
