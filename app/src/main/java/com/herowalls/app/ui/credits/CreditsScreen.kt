package com.herowalls.app.ui.credits

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Public
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.herowalls.app.data.model.Wallpaper
import com.herowalls.app.data.remote.LicensePolicy
import com.herowalls.app.ui.components.GlassSurface
import com.herowalls.app.ui.components.LicenseChip
import com.herowalls.app.ui.favorites.LibraryViewModel
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.SurfaceMid
import com.herowalls.app.ui.theme.TextPrimary
import com.herowalls.app.ui.theme.TextSecondary
import com.herowalls.app.ui.theme.TextTertiary

/**
 * Complete credits: the licence policy in plain language plus a per-image
 * attribution ledger for every image the user has seen or saved.
 */
@Composable
fun CreditsScreen(viewModel: LibraryViewModel, onOpenDetail: (Long) -> Unit) {
    val credited by viewModel.creditedImages.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Credits & licensing", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "How HeroWalls sources and attributes artwork",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        item {
            GlassSurface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Public, null, tint = Crimson, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Image source",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "All wallpapers are fetched live from Wikimedia Commons through the official MediaWiki API " +
                            "(commons.wikimedia.org/w/api.php). Nothing is bundled with the app and no image is generated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        LicensePolicy.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { uriHandler.openUri("https://commons.wikimedia.org/wiki/Commons:Licensing") }
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.OpenInNew, null, tint = Crimson, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Commons licensing rules",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    "Image credits",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (credited.isEmpty()) "Images you open or save will be listed here with full attribution."
                    else "${credited.size} image${if (credited.size == 1) "" else "s"} credited on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        items(credited, key = { it.pageId }) { wallpaper ->
            CreditRow(wallpaper = wallpaper, onOpen = { onOpenDetail(wallpaper.pageId) })
        }

        item {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Trademark notice",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Marvel, DC, Spider-Man, Batman and all related characters, logos and indicia are trademarks of " +
                        "Marvel Characters Inc., DC Comics / Warner Bros. Discovery or their respective owners. " +
                        "HeroWalls is an unofficial, non-commercial artwork browser and is not affiliated with, " +
                        "endorsed by, or sponsored by any comic publisher or film studio. Only freely licensed " +
                        "photographs and artwork are shown, with attribution preserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "HeroWalls 1.0.0 · built with Jetpack Compose",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun CreditRow(wallpaper: Wallpaper, onOpen: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(wallpaper.thumbUrl).crossfade(true).build(),
            contentDescription = wallpaper.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceMid)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                wallpaper.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                wallpaper.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.clickable { uriHandler.openUri(wallpaper.filePageUrl) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                LicenseChip(wallpaper.licenseShort.ifBlank { "n/a" })
                Spacer(Modifier.width(8.dp))
                Text(
                    "source",
                    style = MaterialTheme.typography.bodySmall,
                    color = Crimson
                )
            }
        }
    }
}
