package com.herowalls.app.ui.search

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.herowalls.app.data.model.Catalog
import com.herowalls.app.ui.components.EmptyState
import com.herowalls.app.ui.components.WallpaperGrid
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.SurfaceMid
import com.herowalls.app.ui.theme.TextPrimary
import com.herowalls.app.ui.theme.TextSecondary
import com.herowalls.app.ui.theme.TextTertiary

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onOpenDetail: (Long) -> Unit,
    onOpenCategory: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Scaffold(containerColor = Color.Black) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = padding.calculateTopPadding())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .clip(RoundedCornerShape(14.dp)),
                    placeholder = {
                        Text("Search heroes, villains, comics", color = TextTertiary)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, null, tint = TextTertiary, modifier = Modifier.size(19.dp))
                    },
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.clearQuery() }) {
                                Icon(Icons.Rounded.Close, "Clear", tint = TextTertiary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        viewModel.submit()
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceMid,
                        unfocusedContainerColor = SurfaceMid,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Crimson
                    )
                )
            }

            if (!state.hasSearched) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    if (recent.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Recent searches",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Clear",
                                style = MaterialTheme.typography.labelLarge,
                                color = Crimson,
                                modifier = Modifier.clickable { viewModel.clearHistory() }
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        ChipWrap(items = recent, onClick = { viewModel.submit(it) })
                        Spacer(Modifier.height(26.dp))
                    }

                    Text(
                        "Browse every collection",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Catalog.all.size) { index ->
                            val category = Catalog.all[index]
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .clickable { onOpenCategory(category.id) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(30.dp))
                    EmptyState(
                        title = "Search the Commons archive",
                        message = "Type at least three characters. Results are filtered to freely licensed artwork only."
                    )
                }
            } else {
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
                        Text(
                            text = "${state.items.size} result${if (state.items.size == 1) "" else "s"} for \"${state.submitted}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    },
                    empty = {
                        EmptyState(
                            title = "No reusable results",
                            message = "Nothing on Wikimedia Commons matched \"${state.submitted}\" with a free licence. " +
                                "Try another character or a broader term.",
                            actionLabel = "Retry",
                            onAction = { viewModel.retry() }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ChipWrap(items: List<String>, onClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items.size) { index ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable { onClick(items[index]) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Search, null, tint = TextTertiary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(items[index], style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                }
            }
        }
    }
}
