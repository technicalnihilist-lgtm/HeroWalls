package com.herowalls.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.herowalls.app.data.download.WallpaperDownloader
import com.herowalls.app.data.local.LibraryRepository
import com.herowalls.app.data.model.Wallpaper
import com.herowalls.app.data.model.WallpaperDirectory
import com.herowalls.app.data.remote.WikimediaRepository
import com.herowalls.app.wallpaper.SetWallpaperResult
import com.herowalls.app.wallpaper.WallpaperSetter
import com.herowalls.app.wallpaper.WallpaperTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class DetailUiState(
    val wallpaper: Wallpaper? = null,
    val loading: Boolean = true,
    val isFavorite: Boolean = false,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedPath: String? = null,
    val applyingTarget: WallpaperTarget? = null,
    val message: String? = null,
    val loadError: String? = null
)

class DetailViewModel(
    private val pageId: Long,
    private val wikimedia: WikimediaRepository,
    private val library: LibraryRepository,
    private val downloader: WallpaperDownloader,
    private val setter: WallpaperSetter
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val wallpaper = WallpaperDirectory.get(pageId)
                ?: library.findDownload(pageId)?.let { runCatching { com.google.gson.Gson().fromJson(it.payload, Wallpaper::class.java) }.getOrNull() }
                ?: wikimedia.byPageId(pageId)

            if (wallpaper == null) {
                _state.update {
                    it.copy(loading = false, loadError = "This wallpaper is no longer available on Wikimedia Commons.")
                }
                return@launch
            }

            WallpaperDirectory.remember(listOf(wallpaper))
            library.recordView(wallpaper)
            val downloaded = downloader.fileFor(wallpaper).takeIf { it.exists() && it.length() > 0 }
            _state.update {
                it.copy(
                    wallpaper = wallpaper,
                    loading = false,
                    isFavorite = library.isFavorite(pageId),
                    downloadedPath = downloaded?.absolutePath
                )
            }
        }

        viewModelScope.launch {
            library.observeIsFavorite(pageId).collect { fav ->
                _state.update { it.copy(isFavorite = fav) }
            }
        }

        viewModelScope.launch {
            library.observeDownload(pageId).collect { row ->
                if (row != null) _state.update { it.copy(downloadedPath = row.filePath) }
            }
        }
    }

    fun localFile(): File? = _state.value.downloadedPath?.let { File(it) }?.takeIf { it.exists() }

    fun download() {
        val wallpaper = _state.value.wallpaper ?: return
        if (_state.value.downloading) return
        viewModelScope.launch {
            _state.update { it.copy(downloading = true, progress = 0f, message = null) }
            val result = downloader.download(wallpaper) { value ->
                _state.update { it.copy(progress = value) }
            }
            result.onSuccess { file ->
                _state.update {
                    it.copy(
                        downloading = false,
                        progress = 1f,
                        downloadedPath = file.absolutePath,
                        message = "Saved for offline use"
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        downloading = false,
                        progress = 0f,
                        message = "Download failed: ${error.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    fun setWallpaper(target: WallpaperTarget) {
        val wallpaper = _state.value.wallpaper ?: return
        if (_state.value.applyingTarget != null) return
        viewModelScope.launch {
            _state.update { it.copy(applyingTarget = target, message = null) }

            val existing = localFile()
            val file = existing ?: run {
                _state.update { it.copy(downloading = true, progress = 0f) }
                val result = downloader.download(wallpaper) { value ->
                    _state.update { s -> s.copy(progress = value) }
                }
                _state.update { it.copy(downloading = false) }
                result.getOrNull()
            }

            if (file == null) {
                _state.update {
                    it.copy(applyingTarget = null, message = "Could not download the full-resolution image.")
                }
                return@launch
            }

            _state.update { it.copy(downloadedPath = file.absolutePath) }
            when (val outcome = setter.set(file, target)) {
                is SetWallpaperResult.Success -> _state.update {
                    it.copy(applyingTarget = null, message = "Set as ${target.label.lowercase()}")
                }
                is SetWallpaperResult.Failure -> _state.update {
                    it.copy(applyingTarget = null, message = outcome.message)
                }
            }
        }
    }

    fun toggleFavorite() {
        val wallpaper = _state.value.wallpaper ?: return
        viewModelScope.launch {
            val added = library.toggleFavorite(wallpaper)
            _state.update {
                it.copy(
                    isFavorite = added,
                    message = if (added) "Added to favourites" else "Removed from favourites"
                )
            }
        }
    }

    fun removeDownload() {
        val wallpaper = _state.value.wallpaper ?: return
        viewModelScope.launch {
            downloader.delete(wallpaper)
            _state.update { it.copy(downloadedPath = null, message = "Offline copy deleted") }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
