package com.herowalls.app.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.herowalls.app.data.local.DownloadedWallpaper
import com.herowalls.app.ui.components.EmptyState
import com.herowalls.app.ui.components.WallpaperGrid
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.SurfaceMid
import com.herowalls.app.ui.theme.TextPrimary
import com.herowalls.app.ui.theme.TextSecondary
import com.herowalls.app.ui.theme.TextTertiary
import com.herowalls.app.util.ShareHelper
import com.herowalls.app.util.Text
import java.io.File

@Composable
fun FavoritesScreen(
    viewModel: LibraryViewModel,
    onOpenDetail: (Long) -> Unit,
    onBrowse: () -> Unit
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val downloadedIds = downloads.map { it.wallpaper.pageId }.toSet()

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Favourites", "${favorites.size} saved wallpaper${if (favorites.size == 1) "" else "s"}")
        WallpaperGrid(
            items = favorites,
            onOpen = { onOpenDetail(it.pageId) },
            favoriteIds = favorites.map { it.pageId }.toSet(),
            downloadedIds = downloadedIds,
            onFavoriteToggle = viewModel::toggleFavorite,
            loading = false,
            empty = {
                EmptyState(
                    title = "No favourites yet",
                    message = "Tap the heart on any wallpaper to keep it here. Favourites stay on your device and work offline.",
                    icon = Icons.Rounded.FavoriteBorder,
                    actionLabel = "Browse collections",
                    onAction = onBrowse
                )
            }
        )
    }
}

@Composable
fun DownloadsScreen(
    viewModel: LibraryViewModel,
    onOpenDetail: (Long) -> Unit,
    onBrowse: () -> Unit
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        ScreenTitle(
            "Offline downloads",
            if (downloads.isEmpty()) "Nothing saved yet" else
                "${downloads.size} file${if (downloads.size == 1) "" else "s"} · ${Text.humanBytes(downloads.sumOf { it.bytes })}"
        )
        if (downloads.isEmpty()) {
            EmptyState(
                title = "No offline wallpapers",
                message = "Downloaded wallpapers live in app storage, so you can set them without a connection.",
                icon = Icons.Rounded.DeleteOutline,
                actionLabel = "Find wallpapers",
                onAction = onBrowse
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                downloads.forEach { item ->
                    DownloadRow(
                        item = item,
                        onOpen = { onOpenDetail(item.wallpaper.pageId) },
                        onShare = { ShareHelper.share(context, item.wallpaper, File(item.filePath)) },
                        onDelete = { viewModel.removeDownload(item.wallpaper) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    item: DownloadedWallpaper,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(item.wallpaper.thumbUrl).crossfade(true).build(),
            contentDescription = item.wallpaper.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceMid)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.wallpaper.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${item.wallpaper.resolutionLabel} · ${Text.humanBytes(item.bytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
            Text(
                "Downloaded ${Text.relativeTime(item.downloadedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
        IconAction(Icons.Rounded.Wallpaper, "Set wallpaper", Crimson, onOpen)
        IconAction(Icons.Rounded.Share, "Share", TextSecondary, onShare)
        IconAction(Icons.Rounded.DeleteOutline, "Delete", TextSecondary, onDelete)
    }
}

@Composable
private fun IconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun ScreenTitle(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
        Spacer(Modifier.height(3.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
    }
}
