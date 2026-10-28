package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.LocalFmReceiverColors
import com.example.ui.viewmodel.AppDestination
import com.example.ui.viewmodel.RadioTunerViewModel
import com.example.ui.viewmodel.RadioUiState

private data class NavItemSpec(
    val destination: AppDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun OndaFmApp(viewModel: RadioTunerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.destination != AppDestination.TUNER) {
        viewModel.selectDestination(AppDestination.TUNER)
    }

    val navItems = listOf(
        NavItemSpec(
            destination = AppDestination.TUNER,
            labelRes = R.string.nav_tuner,
            selectedIcon = Icons.Filled.Radio,
            unselectedIcon = Icons.Outlined.Radio,
            testTag = "nav_item_tuner"
        ),
        NavItemSpec(
            destination = AppDestination.STATIONS,
            labelRes = R.string.nav_stations,
            selectedIcon = Icons.Filled.CellTower,
            unselectedIcon = Icons.Outlined.CellTower,
            testTag = "nav_item_stations"
        ),
        NavItemSpec(
            destination = AppDestination.MEMORIES,
            labelRes = R.string.nav_presets,
            selectedIcon = Icons.Filled.Bookmarks,
            unselectedIcon = Icons.Outlined.Bookmarks,
            testTag = "nav_item_memories"
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 720.dp

        if (useNavRail) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = state.destination == item.destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectDestination(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f)) {
                            DestinationContent(state = state, viewModel = viewModel)
                        }
                        if (state.destination != AppDestination.TUNER) {
                            MiniTunerStrip(
                                state = state,
                                onOpenTuner = { viewModel.selectDestination(AppDestination.TUNER) },
                                onTogglePower = viewModel::togglePower
                            )
                        }
                    }
                }
            }
        } else {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    Column {
                        if (state.destination != AppDestination.TUNER) {
                            MiniTunerStrip(
                                state = state,
                                onOpenTuner = { viewModel.selectDestination(AppDestination.TUNER) },
                                onTogglePower = viewModel::togglePower
                            )
                        }
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { item ->
                                val selected = state.destination == item.destination
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectDestination(item.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) },
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                DestinationContent(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun DestinationContent(
    state: RadioUiState,
    viewModel: RadioTunerViewModel,
    modifier: Modifier = Modifier
) {
    when (state.destination) {
        AppDestination.TUNER -> TunerScreen(
            state = state,
            onTuneFrequency = { viewModel.tuneToFrequency(it) },
            onStepFrequency = viewModel::stepFrequency,
            onSeek = viewModel::seekNextStation,
            onTogglePower = viewModel::togglePower,
            onToggleFavorite = viewModel::toggleCurrentStationFavorite,
            onRetryStream = viewModel::retryStream,
            onSelectRegion = viewModel::selectRegion,
            onToggleAnalogStatic = viewModel::toggleAnalogStatic,
            onSetSleepTimer = viewModel::setSleepTimer,
            onPresetTap = viewModel::onHardwarePresetTapped,
            onPresetSave = viewModel::saveCurrentToPreset,
            modifier = modifier
        )

        AppDestination.STATIONS -> StationsDirectoryScreen(
            state = state,
            onSelectRegion = viewModel::selectRegion,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onRefreshBand = viewModel::refreshCurrentBand,
            onStationClick = { station ->
                viewModel.tuneAndPlayStation(station, switchToTuner = false)
            },
            onToggleFavorite = viewModel::toggleFavorite,
            modifier = modifier
        )

        AppDestination.MEMORIES -> MemoriesScreen(
            state = state,
            onPresetTap = viewModel::onHardwarePresetTapped,
            onAssignCurrentToPreset = viewModel::saveCurrentToPreset,
            onClearPreset = viewModel::clearHardwarePreset,
            onPlayFavorite = viewModel::playFavoriteStation,
            onRemoveFavorite = viewModel::removeFavorite,
            modifier = modifier
        )
    }
}

@Composable
private fun MiniTunerStrip(
    state: RadioUiState,
    onOpenTuner: () -> Unit,
    onTogglePower: () -> Unit
) {
    val receiverColors = LocalFmReceiverColors.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTuner)
            .testTag("mini_tuner_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${state.formattedFrequency} MHz",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = receiverColors.phosphorActive
                )
                Text(
                    text = state.directOverrideStationName
                        ?: state.tunedStation?.name
                        ?: stringResource(R.string.badge_static),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onTogglePower,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mini_tuner_play_pause")
            ) {
                Icon(
                    imageVector = if (state.isPowerOn) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
