package com.herowalls.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.herowalls.app.data.model.Wallpaper
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.SurfaceMid
import com.herowalls.app.ui.theme.TextTertiary

/** Horizontal artwork row used by every home section. */
@Composable
fun WallpaperRow(
    items: List<Wallpaper>,
    onOpen: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier,
    favoriteIds: Set<Long> = emptySet(),
    downloadedIds: Set<Long> = emptySet(),
    newIds: Set<Long> = emptySet(),
    onFavoriteToggle: ((Wallpaper) -> Unit)? = null,
    cardWidth: Dp = 168.dp
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items.size) { index ->
            val wallpaper = items[index]
            Box {
                WallpaperCard(
                    wallpaper = wallpaper,
                    onClick = { onOpen(wallpaper) },
                    isFavorite = wallpaper.pageId in favoriteIds,
                    isDownloaded = wallpaper.pageId in downloadedIds,
                    onFavoriteToggle = onFavoriteToggle?.let { { it(wallpaper) } },
                    cardWidth = cardWidth
                )
                if (wallpaper.pageId in newIds) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Crimson)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}

/** Two-column paginated grid with loading, error and empty states. */
@Composable
fun WallpaperGrid(
    items: List<Wallpaper>,
    onOpen: (Wallpaper) -> Unit,
    modifier: Modifier = Modifier,
    favoriteIds: Set<Long> = emptySet(),
    downloadedIds: Set<Long> = emptySet(),
    onFavoriteToggle: ((Wallpaper) -> Unit)? = null,
    loading: Boolean = false,
    loadingMore: Boolean = false,
    endReached: Boolean = false,
    onLoadMore: () -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    empty: (@Composable () -> Unit)? = null,
    gridState: LazyGridState = rememberLazyGridState()
) {
    if (loading && items.isEmpty()) {
        ShimmerGrid()
        return
    }

    if (!loading && items.isEmpty()) {
        Column(modifier.fillMaxWidth()) {
            header?.invoke()
            empty?.invoke()
        }
        return
    }

    LaunchedEffect(gridState, items.size, endReached) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (!endReached && items.isNotEmpty() && lastVisible >= items.size - 4) {
                    onLoadMore()
                }
            }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        items(items, key = { it.pageId }) { wallpaper ->
            WallpaperCard(
                wallpaper = wallpaper,
                onClick = { onOpen(wallpaper) },
                isFavorite = wallpaper.pageId in favoriteIds,
                isDownloaded = wallpaper.pageId in downloadedIds,
                onFavoriteToggle = onFavoriteToggle?.let { { it(wallpaper) } }
            )
        }
        if (loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Crimson,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (endReached && items.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "You have reached the end of this collection",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                )
            }
        }
    }
}

@Composable
fun ShimmerGrid(columns: Int = 2, rows: Int = 4) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(columns) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(230.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SurfaceMid)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
fun ShimmerRow(count: Int = 3, width: Dp = 168.dp) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(238.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceMid)
            )
        }
    }
}
