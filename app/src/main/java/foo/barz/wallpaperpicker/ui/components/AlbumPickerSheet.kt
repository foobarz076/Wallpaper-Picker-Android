package foo.barz.wallpaperpicker.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import foo.barz.wallpaperpicker.R

/**
 * Universal album item model for the refactored album picker.
 */
data class PickerAlbumItem(
    val id: String?,
    val name: String,
    val count: Int,
    val coverUri: Uri? = null,
    val coverUrl: String? = null,
    val apiKey: String? = null
)

/**
 * Refactored album picker bottom sheet featuring instant search filtering,
 * vertical scrolling, thumbnail previews, and multi-album combination selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPickerSheet(
    title: String,
    albums: List<PickerAlbumItem>,
    selectedIds: Set<String>,
    onSelectionConfirmed: (Set<String>) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var tempSelectedIds by remember(selectedIds) { mutableStateOf(selectedIds) }
    val context = LocalContext.current

    val allPhotosItem = albums.firstOrNull { it.id == null }
    val specificAlbums = albums.filter { it.id != null }

    val filteredAlbums = remember(specificAlbums, searchQuery) {
        if (searchQuery.isBlank()) {
            specificAlbums
        } else {
            specificAlbums.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(R.string.album_picker_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_close))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search filter field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.album_picker_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.album_picker_clear_search))
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick action chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tempSelectedIds.isEmpty(),
                    onClick = { tempSelectedIds = emptySet() },
                    label = { Text(stringResource(R.string.album_picker_chip_all_random)) }
                )
                FilterChip(
                    selected = tempSelectedIds.isNotEmpty() && tempSelectedIds.size == specificAlbums.size,
                    onClick = {
                        tempSelectedIds = specificAlbums.mapNotNull { it.id }.toSet()
                    },
                    label = { Text(stringResource(R.string.album_picker_chip_select_all_format, specificAlbums.size)) }
                )
                if (tempSelectedIds.isNotEmpty()) {
                    FilterChip(
                        selected = false,
                        onClick = { tempSelectedIds = emptySet() },
                        label = { Text(stringResource(R.string.album_picker_chip_clear_selection)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vertical scrolling albums list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // "All Photos" item when search query is empty
                if (searchQuery.isBlank() && allPhotosItem != null) {
                    item(key = "all_photos_root") {
                        val isAllSelected = tempSelectedIds.isEmpty()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { tempSelectedIds = emptySet() },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAllSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (allPhotosItem.coverUri != null || allPhotosItem.coverUrl != null) {
                                        val model = if (allPhotosItem.coverUrl != null && allPhotosItem.apiKey != null) {
                                            ImageRequest.Builder(context)
                                                .data(allPhotosItem.coverUrl)
                                                .addHeader("x-api-key", allPhotosItem.apiKey)
                                                .build()
                                        } else {
                                            allPhotosItem.coverUri ?: allPhotosItem.coverUrl
                                        }
                                        AsyncImage(
                                            model = model,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = allPhotosItem.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.album_picker_all_photos_desc, allPhotosItem.count),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Checkbox(
                                    checked = isAllSelected,
                                    onCheckedChange = { if (it) tempSelectedIds = emptySet() }
                                 )
                            }
                        }
                    }
                }

                items(
                    items = filteredAlbums,
                    key = { it.id ?: it.name }
                ) { album ->
                    val isSelected = tempSelectedIds.contains(album.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val current = tempSelectedIds.toMutableSet()
                                val id = album.id ?: return@clickable
                                if (current.contains(id)) {
                                    current.remove(id)
                                } else {
                                    current.add(id)
                                }
                                tempSelectedIds = current
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                if (album.coverUri != null || album.coverUrl != null) {
                                    val model = if (album.coverUrl != null && album.apiKey != null) {
                                        ImageRequest.Builder(context)
                                            .data(album.coverUrl)
                                            .addHeader("x-api-key", album.apiKey)
                                            .build()
                                    } else {
                                        album.coverUri ?: album.coverUrl
                                    }
                                    AsyncImage(
                                        model = model,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Collections,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = album.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.album_picker_photos_count_format, album.count),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val current = tempSelectedIds.toMutableSet()
                                    val id = album.id ?: return@Checkbox
                                    if (checked) current.add(id) else current.remove(id)
                                    tempSelectedIds = current
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom confirmation action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (tempSelectedIds.isEmpty()) {
                        stringResource(R.string.album_picker_mode_all)
                    } else {
                        stringResource(R.string.album_picker_mode_selected_format, tempSelectedIds.size)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = {
                        onSelectionConfirmed(tempSelectedIds)
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        }
    }
}
