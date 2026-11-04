package com.herowalls.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.herowalls.app.data.model.Catalog
import com.herowalls.app.ui.HeroViewModelFactory
import com.herowalls.app.ui.browse.BrowseScreen
import com.herowalls.app.ui.browse.FeedViewModel
import com.herowalls.app.ui.credits.CreditsScreen
import com.herowalls.app.ui.detail.DetailScreen
import com.herowalls.app.ui.detail.DetailViewModel
import com.herowalls.app.ui.favorites.FavoritesScreen
import com.herowalls.app.ui.favorites.DownloadsScreen
import com.herowalls.app.ui.favorites.LibraryViewModel
import com.herowalls.app.ui.home.HomeScreen
import com.herowalls.app.ui.home.HomeViewModel
import com.herowalls.app.ui.search.SearchScreen
import com.herowalls.app.ui.search.SearchViewModel
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.GlassStroke
import com.herowalls.app.ui.theme.TextTertiary

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val DOWNLOADS = "downloads"
    const val CREDITS = "credits"
    const val BROWSE = "browse/{categoryId}"
    const val DETAIL = "detail/{pageId}"

    fun browse(categoryId: String) = "browse/$categoryId"
    fun detail(pageId: Long) = "detail/$pageId"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Home", Icons.Rounded.Home),
    TabItem(Routes.SEARCH, "Search", Icons.Rounded.Search),
    TabItem(Routes.FAVORITES, "Saved", Icons.Rounded.Favorite),
    TabItem(Routes.DOWNLOADS, "Offline", Icons.Rounded.Download)
)

@Composable
fun HeroWallsNavHost(
    container: com.herowalls.app.AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.SEARCH, Routes.FAVORITES, Routes.DOWNLOADS)

    Box(Modifier.fillMaxWidth()) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.fillMaxWidth()
        ) {
            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(
                    factory = HeroViewModelFactory { HomeViewModel(container.wikimedia, container.library) }
                )
                HomeScreen(
                    viewModel = vm,
                    onOpenCategory = { navController.navigate(Routes.browse(it)) },
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenCredits = { navController.navigate(Routes.CREDITS) }
                )
            }

            composable(Routes.BROWSE) { entry ->
                val categoryId = entry.arguments?.getString("categoryId").orEmpty()
                val category = Catalog.byId(categoryId) ?: Catalog.groups.first()
                val vm: FeedViewModel = viewModel(
                    key = "feed-$categoryId",
                    factory = HeroViewModelFactory {
                        FeedViewModel(category, container.wikimedia, container.library)
                    }
                )
                BrowseScreen(
                    category = category,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { navController.navigate(Routes.detail(it)) }
                )
            }

            composable(Routes.DETAIL) { entry ->
                val pageId = entry.arguments?.getString("pageId")?.toLongOrNull() ?: 0L
                val vm: DetailViewModel = viewModel(
                    key = "detail-$pageId",
                    factory = HeroViewModelFactory {
                        DetailViewModel(
                            pageId = pageId,
                            wikimedia = container.wikimedia,
                            library = container.library,
                            downloader = container.downloader,
                            setter = container.wallpaperSetter
                        )
                    }
                )
                DetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }

            composable(Routes.SEARCH) {
                val vm: SearchViewModel = viewModel(
                    factory = HeroViewModelFactory { SearchViewModel(container.wikimedia, container.library) }
                )
                SearchScreen(
                    viewModel = vm,
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                    onOpenCategory = { navController.navigate(Routes.browse(it)) }
                )
            }

            composable(Routes.FAVORITES) {
                val vm: LibraryViewModel = viewModel(
                    factory = HeroViewModelFactory { LibraryViewModel(container.library, container.downloader) }
                )
                FavoritesScreen(
                    viewModel = vm,
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                    onBrowse = { navController.navigate(Routes.HOME) }
                )
            }

            composable(Routes.DOWNLOADS) {
                val vm: LibraryViewModel = viewModel(
                    factory = HeroViewModelFactory { LibraryViewModel(container.library, container.downloader) }
                )
                DownloadsScreen(
                    viewModel = vm,
                    onOpenDetail = { navController.navigate(Routes.detail(it)) },
                    onBrowse = { navController.navigate(Routes.HOME) }
                )
            }

            composable(Routes.CREDITS) {
                val vm: LibraryViewModel = viewModel(
                    factory = HeroViewModelFactory { LibraryViewModel(container.library, container.downloader) }
                )
                CreditsScreen(
                    viewModel = vm,
                    onOpenDetail = { navController.navigate(Routes.detail(it)) }
                )
            }
        }

        if (showBottomBar) {
            HeroBottomBar(
                currentRoute = currentRoute,
                onSelect = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun HeroBottomBar(
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xF20A0A0C))
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { tab ->
                val selected = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelect(tab.route) }
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (selected) Crimson else TextTertiary,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Crimson else TextTertiary
                    )
                }
            }
        }
    }
}
