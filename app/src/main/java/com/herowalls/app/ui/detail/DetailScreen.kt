package com.herowalls.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.herowalls.app.data.model.Wallpaper
import com.herowalls.app.ui.components.EmptyState
import com.herowalls.app.ui.components.GlassSurface
import com.herowalls.app.ui.components.LicenseChip
import com.herowalls.app.ui.theme.AmoledBlack
import com.herowalls.app.ui.theme.Crimson
import com.herowalls.app.ui.theme.SurfaceMid
import com.herowalls.app.ui.theme.TextPrimary
import com.herowalls.app.ui.theme.TextSecondary
import com.herowalls.app.ui.theme.TextTertiary
import com.herowalls.app.util.ShareHelper
import com.herowalls.app.util.Text
import com.herowalls.app.wallpaper.WallpaperTarget

@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    var showTargetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            val wallpaper = state.wallpaper
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Crimson, strokeWidth = 2.dp)
                }

                wallpaper == null -> EmptyState(
                    title = "Wallpaper unavailable",
                    message = state.loadError ?: "This file is no longer on Wikimedia Commons.",
                    actionLabel = "Go back",
                    onAction = onBack,
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
                        item { PreviewHeader(wallpaper) }

                        item {
                            ActionBar(
                                isFavorite = state.isFavorite,
                                isDownloaded = state.downloadedPath != null,
                                downloading = state.downloading,
                                progress = state.progress,
                                onDownload = { viewModel.download() },
                                onSet = { showTargetDialog = true },
                                onFavorite = { viewModel.toggleFavorite() },
                                onShare = {
                                    ShareHelper.share(context, wallpaper, viewModel.localFile())
                                }
                            )
                        }

                        if (state.downloading) {
                            item {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    color = Crimson,
                                    trackColor = SurfaceMid,
                                    modifier = Modifier
                                        .padding(horizontal = 20.dp, vertical = 4.dp)
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }

                        item { CreditsCard(wallpaper) }

                        item {
                            Spacer(Modifier.height(14.dp))
                            LegalNote(
                                onOpenLicense = {
                                    if (wallpaper.licenseUrl.isNotBlank()) uriHandler.openUri(wallpaper.licenseUrl)
                                },
                                onOpenSource = { uriHandler.openUri(wallpaper.filePageUrl) }
                            )
                        }

                        if (state.downloadedPath != null) {
                            item {
                                Spacer(Modifier.height(14.dp))
                                TextButton(
                                    onClick = { viewModel.removeDownload() },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text("Delete offline copy", color = TextTertiary)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
                        Spacer(Modifier.weight(1f))
                        RoundIconButton(Icons.Rounded.Share, "Share") {
                            ShareHelper.share(context, wallpaper, viewModel.localFile())
                        }
                    }
                }
            }
        }
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = SurfaceMid,
            title = {
                Text("Set wallpaper", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column {
                    Text(
                        "HeroWalls applies the full-resolution image through Android's WallpaperManager.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    WallpaperTarget.entries.forEach { target ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showTargetDialog = false
                                    viewModel.setWallpaper(target)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.PhoneAndroid, null,
                                tint = Crimson, modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(target.label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PreviewHeader(wallpaper: Wallpaper) {
    val context = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(440.dp)
            .background(SurfaceMid)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(wallpaper.previewUrl)
                .crossfade(true)
                .build(),
            contentDescription = wallpaper.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.45f to Color.Transparent,
                        1f to AmoledBlack
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = wallpaper.displayName,
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "by ${wallpaper.artist}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(8.dp))
                LicenseChip(wallpaper.licenseShort.ifBlank { "licence n/a" })
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${wallpaper.resolutionLabel} · ${Text.humanBytes(wallpaper.sizeBytes)} · ${wallpaper.characterName}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun ActionBar(
    isFavorite: Boolean,
    isDownloaded: Boolean,
    downloading: Boolean,
    progress: Float,
    onDownload: () -> Unit,
    onSet: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionTile(
            icon = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
            label = when {
                downloading -> "${(progress * 100).toInt()}%"
                isDownloaded -> "Saved"
                else -> "Download"
            },
            highlighted = isDownloaded,
            modifier = Modifier.weight(1f),
            onClick = onDownload
        )
        ActionTile(
            icon = Icons.Rounded.Wallpaper,
            label = "Set",
            modifier = Modifier.weight(1f),
            onClick = onSet
        )
        ActionTile(
            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = if (isFavorite) "Saved" else "Favourite",
            highlighted = isFavorite,
            modifier = Modifier.weight(1f),
            onClick = onFavorite
        )
        ActionTile(
            icon = Icons.Rounded.Share,
            label = "Share",
            modifier = Modifier.weight(1f),
            onClick = onShare
        )
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    GlassSurface(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) Crimson else TextPrimary,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (highlighted) Crimson else TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CreditsCard(wallpaper: Wallpaper) {
    val uriHandler = LocalUriHandler.current
    GlassSurface(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Image credits",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(12.dp))
            MetaRow("Character / collection", wallpaper.characterName)
            MetaRow("Creator", wallpaper.artist)
            MetaRow("Licence", wallpaper.licenseShort.ifBlank { Wallpaper.UNKNOWN })
            if (wallpaper.licenseCode.isNotBlank()) MetaRow("Licence code", wallpaper.licenseCode)
            MetaRow("Resolution", wallpaper.resolutionLabel)
            MetaRow("File size", Text.humanBytes(wallpaper.sizeBytes))
            MetaRow("File type", wallpaper.mime.ifBlank { Wallpaper.UNKNOWN })
            MetaRow("Attribution required", if (wallpaper.attributionRequired) "Yes" else "No")
            if (wallpaper.dateTime.isNotBlank()) MetaRow("Published", Text.prettyDate(wallpaper.dateTime))
            if (wallpaper.uploader.isNotBlank()) MetaRow("Uploaded by", wallpaper.uploader)

            if (wallpaper.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text("Description", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Spacer(Modifier.height(4.dp))
                Text(
                    wallpaper.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(Modifier.height(10.dp))
            Text("Modifications", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Spacer(Modifier.height(4.dp))
            Text(
                wallpaper.modifications,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (wallpaper.restrictions.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    wallpaper.restrictions,
                    style = MaterialTheme.typography.bodySmall,
                    color = Crimson.copy(alpha = 0.9f)
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { uriHandler.openUri(wallpaper.filePageUrl) }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.OpenInNew, null, tint = Crimson, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Original source page",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text(
                        wallpaper.filePageUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.width(132.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LegalNote(onOpenLicense: () -> Unit, onOpenSource: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            "HeroWalls does not own this artwork. The character and any associated trademarks belong to their respective owners. " +
                "The image is redistributed under the licence shown above, with attribution preserved.",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary
        )
        Spacer(Modifier.height(8.dp))
        Row {
            TextButton(onClick = onOpenLicense, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("Licence deed", color = Crimson, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onOpenSource, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("Commons page", color = Crimson, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = Color.White, modifier = Modifier.size(19.dp))
    }
}
