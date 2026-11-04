package com.herowalls.app.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.herowalls.app.data.model.HeroCategory
import com.herowalls.app.data.model.SortOrder
import com.herowalls.app.ui.components.EmptyState
import com.herowalls.app.ui.components.WallpaperGrid
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.GlassStroke
import com.herowalls.app.ui.theme.TextPrimary
import com.herowalls.app.ui.theme.TextSecondary
import com.herowalls.app.ui.theme.TextTertiary

@Composable
fun BrowseScreen(
    category: HeroCategory,
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = padding.calculateTopPadding())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack, "Back",
                        tint = TextPrimary, modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = category.blurb.ifBlank { "Live from Wikimedia Commons" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        Icons.Rounded.Refresh, "Refresh",
                        tint = TextSecondary, modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SortChips(
                selected = state.sort,
                onSelect = { viewModel.setSort(it) }
            )
            Spacer(Modifier.height(6.dp))

            WallpaperGrid(
                items = state.items,
                onOpen = { onOpenDetail(it.pageId) },
                favoriteIds = favoriteIds,
                downloadedIds = downloadedIds,
                onFavoriteToggle = viewModel::toggleFavorite,
                loading = state.loading,
                loadingMore = state.loadingMore,
                endReached = state.endReached,
                onLoadMore = { viewModel.loadMore() },
                header = {
                    val note = state.fallbackNote
                        ?: if (state.fromCache) "Showing cached results - refresh when you are back online" else null
                    if (note != null) {
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                },
                empty = {
                    EmptyState(
                        title = "No reusable artwork yet",
                        message = "Wikimedia Commons did not answer for ${category.name} right now. " +
                            "HeroWalls never fills this space with unlicensed artwork - check your connection and retry.",
                        actionLabel = "Retry",
                        onAction = { viewModel.retry() }
                    )
                }
            )
        }
    }
}

@Composable
private fun SortChips(selected: SortOrder, onSelect: (SortOrder) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SortOrder.entries.size) { index ->
            val option = SortOrder.entries[index]
            val active = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Crimson.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) Crimson else TextSecondary
                )
            }
        }
    }
}
