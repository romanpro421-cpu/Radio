package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FavoriteStationEntity
import com.example.model.FmFrequency
import com.example.ui.theme.LocalFmReceiverColors
import com.example.ui.viewmodel.RadioUiState

@Composable
fun MemoriesScreen(
    state: RadioUiState,
    onPresetTap: (Int) -> Unit,
    onAssignCurrentToPreset: (Int) -> Unit,
    onClearPreset: (Int) -> Unit,
    onPlayFavorite: (FavoriteStationEntity) -> Unit,
    onRemoveFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val receiverColors = LocalFmReceiverColors.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("memories_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "memories_header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.memories_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.memories_hardware_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "presets_section_title") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.memories_hardware_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = receiverColors.phosphorActive
                )
            }

            items(items = (1..6).toList(), key = { "slot_$it" }) { slot ->
                val preset = state.hardwarePresets[slot]
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPresetTap(slot) }
                        .testTag("memory_card_m$slot"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = receiverColors.lcdBackground,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, receiverColors.lcdBorder)
                            ) {
                                Text(
                                    text = "M$slot",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = receiverColors.phosphorActive,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                if (preset != null) {
                                    Text(
                                        text = "${FmFrequency.formatMhz(preset.freqTenths)} MHz · ${preset.regionCode}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (preset.stationName.isNotBlank()) {
                                        Text(
                                            text = preset.stationName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.preset_empty_label),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { onAssignCurrentToPreset(slot) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("assign_preset_button_$slot")
                            ) {
                                Icon(
                                    imageVector = if (preset != null) {
                                        Icons.Filled.BookmarkAdded
                                    } else {
                                        Icons.Outlined.BookmarkBorder
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(
                                        R.string.preset_assign_current,
                                        state.formattedFrequency
                                    ),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            if (preset != null) {
                                IconButton(
                                    onClick = { onClearPreset(slot) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("clear_preset_button_$slot")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "favorites_section_title") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.memories_favorites_section),
                    style = MaterialTheme.typography.titleMedium,
                    color = receiverColors.phosphorActive
                )
            }

            if (state.favorites.isEmpty()) {
                item(key = "favorites_empty") {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = stringResource(R.string.memories_favorites_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.memories_favorites_empty_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = state.favorites,
                    key = { "fav_${it.stationUuid}" }
                ) { fav ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayFavorite(fav) }
                            .testTag("favorite_card_${fav.stationUuid}"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = receiverColors.lcdBackground,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, receiverColors.lcdBorder)
                            ) {
                                Text(
                                    text = "${FmFrequency.formatMhz(fav.freqTenths)} FM",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = receiverColors.phosphorActive,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fav.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append(fav.regionCode)
                                        if (fav.tags.isNotBlank()) {
                                            append(" · ")
                                            append(fav.tags)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { onPlayFavorite(fav) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.station_tune_action),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { onRemoveFavorite(fav.stationUuid) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("remove_fav_${fav.stationUuid}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
