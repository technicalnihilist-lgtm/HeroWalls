package com.herowalls.app.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.herowalls.app.data.model.Catalog
import com.herowalls.app.data.model.HeroCategory
import com.herowalls.app.data.model.Wallpaper
import com.herowalls.app.ui.components.CharacterTile
import com.herowalls.app.ui.components.EmptyState
import com.herowalls.app.ui.components.GlassSurface
import com.herowalls.app.ui.components.HeroArtworkCard
import com.herowalls.app.ui.components.HeroWallsWordmark
import com.herowalls.app.ui.components.LiveDot
import com.herowalls.app.ui.components.SectionHeader
import com.herowalls.app.ui.components.ShimmerRow
import com.herowalls.app.ui.components.WallpaperRow
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.SurfaceMid
import com.herowalls.app.ui.theme.TextPrimary
import com.herowalls.app.ui.theme.TextSecondary
import com.herowalls.app.ui.theme.TextTertiary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenCategory: (String) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCredits: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
    val recent by viewModel.recentlyViewed.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                HomeTopBar(
                    onOpenSearch = onOpenSearch,
                    onRefresh = { viewModel.refresh() },
                    live = state.live
                )
            }

            item {
                SearchEntry(onClick = onOpenSearch)
            }

            item {
                Spacer(Modifier.height(18.dp))
                SectionHeader(
                    title = "Daily superhero wallpaper",
                    subtitle = "A fresh hero every day, straight from Commons",
                    actionLabel = null
                )
                Spacer(Modifier.height(8.dp))
                when {
                    state.dailyLoading && state.daily == null -> Box(
                        Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceMid)
                    )
                    state.daily != null -> Box(Modifier.padding(horizontal = 20.dp)) {
                        HeroArtworkCard(
                            wallpaper = state.daily!!,
                            onClick = { onOpenDetail(state.daily!!.pageId) },
                            badge = "Wallpaper of the day"
                        )
                    }
                    else -> GlassSurface(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                    ) {
                        EmptyState(
                            title = "No daily pick today",
                            message = "Wikimedia Commons did not return a reusable image for today's pick. Try refreshing in a moment."
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(26.dp))
                SectionHeader(
                    title = "Featured superheroes",
                    subtitle = "Hand-picked characters",
                    actionLabel = null
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(Catalog.featured().size) { index ->
                        val category = Catalog.featured()[index]
                        CharacterTile(
                            name = category.name,
                            wallpaper = state.featured[category.id],
                            onClick = { onOpenCategory(category.id) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(26.dp))
                SectionHeader(
                    title = "Trending characters",
                    subtitle = "What everyone is setting right now",
                    actionLabel = null
                )
                Spacer(Modifier.height(10.dp))
                TrendingChips(onOpenCategory = onOpenCategory)
            }

            sectionBlock(
                title = "Marvel collection",
                subtitle = "Earth's mightiest artwork",
                section = state.marvel,
                onSeeAll = { onOpenCategory("marvel-all") },
                onOpenDetail = onOpenDetail,
                favoriteIds = favoriteIds,
                downloadedIds = downloadedIds,
                onFavorite = viewModel::toggleFavorite
            )

            sectionBlock(
                title = "DC collection",
                subtitle = "Guardians of the DC multiverse",
                section = state.dc,
                onSeeAll = { onOpenCategory("dc-all") },
                onOpenDetail = onOpenDetail,
                favoriteIds = favoriteIds,
                downloadedIds = downloadedIds,
                onFavorite = viewModel::toggleFavorite
            )

            sectionBlock(
                title = "Popular villains",
                subtitle = "The darker side of the panel",
                section = state.villains,
                onSeeAll = { onOpenCategory("villains-all") },
                onOpenDetail = onOpenDetail,
                favoriteIds = favoriteIds,
                downloadedIds = downloadedIds,
                onFavorite = viewModel::toggleFavorite
            )

            item {
                Spacer(Modifier.height(26.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveDot(active = state.live)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Latest wallpapers",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            "Updates automatically as new images are uploaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                    Text(
                        text = if (state.live) "LIVE" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.live) Crimson else TextTertiary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.setLive(!state.live) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                when {
                    state.latest.items.isEmpty() && state.latest.loading -> ShimmerRow()
                    state.latest.items.isEmpty() -> GlassSurface(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                    ) {
                        EmptyState(
                            title = "Nothing reusable yet",
                            message = state.latest.error
                                ?: "No freely licensed superhero images were found for the live feed. HeroWalls will never show unlicensed artwork."
                        )
                    }
                    else -> WallpaperRow(
                        items = state.latest.items,
                        onOpen = { onOpenDetail(it.pageId) },
                        favoriteIds = favoriteIds,
                        downloadedIds = downloadedIds,
                        newIds = state.newIds,
                        onFavoriteToggle = viewModel::toggleFavorite
                    )
                }
            }

            if (recent.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(26.dp))
                    SectionHeader(
                        title = "Recently viewed",
                        subtitle = "Pick up where you left off",
                        actionLabel = null
                    )
                    Spacer(Modifier.height(10.dp))
                    WallpaperRow(
                        items = recent.take(12),
                        onOpen = { onOpenDetail(it.pageId) },
                        favoriteIds = favoriteIds,
                        downloadedIds = downloadedIds,
                        onFavoriteToggle = viewModel::toggleFavorite
                    )
                }
            }

            item {
                Spacer(Modifier.height(30.dp))
                FooterCredits(onOpenCredits = onOpenCredits)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.sectionBlock(
    title: String,
    subtitle: String,
    section: Section,
    onSeeAll: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    favoriteIds: Set<Long>,
    downloadedIds: Set<Long>,
    onFavorite: (Wallpaper) -> Unit
) {
    item {
        Spacer(Modifier.height(26.dp))
        SectionHeader(
            title = title,
            subtitle = subtitle,
            actionLabel = "See all",
            onAction = onSeeAll
        )
        Spacer(Modifier.height(10.dp))
        when {
            section.items.isEmpty() && section.loading -> ShimmerRow()
            section.items.isEmpty() -> GlassSurface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                EmptyState(
                    title = "No reusable artwork found",
                    message = section.error
                        ?: "Commons has no freely licensed images for this collection right now. HeroWalls shows an empty shelf rather than unlicensed artwork."
                )
            }
            else -> WallpaperRow(
                items = section.items,
                onOpen = { onOpenDetail(it.pageId) },
                favoriteIds = favoriteIds,
                downloadedIds = downloadedIds,
                onFavoriteToggle = onFavorite
            )
        }
    }
}

@Composable
private fun HomeTopBar(onOpenSearch: () -> Unit, onRefresh: () -> Unit, live: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeroWallsWordmark(modifier = Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(Icons.Rounded.Refresh, "Refresh", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.Rounded.Search, "Search", tint = TextPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SearchEntry(onClick: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                "Search heroes, villains or comics",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun TrendingChips(onOpenCategory: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(Catalog.trending().size) { index ->
            val category: HeroCategory = Catalog.trending()[index]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Crimson.copy(alpha = 0.22f), Color.White.copy(alpha = 0.06f))
                        )
                    )
                    .clickable { onOpenCategory(category.id) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun FooterCredits(onOpenCredits: () -> Unit) {
    GlassSurface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clickable(onClick = onOpenCredits),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Artwork and credits",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Every wallpaper comes from Wikimedia Commons under a free licence (Public Domain, CC0, CC BY or CC BY-SA). " +
                    "Creator, licence and source are shown on each wallpaper and listed in the credits screen.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Open full credits and licence policy",
                style = MaterialTheme.typography.labelLarge,
                color = Crimson
            )
        }
    }
}
