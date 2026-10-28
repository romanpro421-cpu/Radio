package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FmFrequency
import com.example.model.FmRegion
import com.example.ui.components.FmScaleRuler
import com.example.ui.components.HardwarePresetsRow
import com.example.ui.components.LcdDisplayPanel
import com.example.ui.components.RotaryTuningCluster
import com.example.ui.theme.LocalFmReceiverColors
import com.example.ui.viewmodel.BandSyncStatus
import com.example.ui.viewmodel.RadioUiState
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TunerScreen(
    state: RadioUiState,
    onTuneFrequency: (Int) -> Unit,
    onStepFrequency: (Int) -> Unit,
    onSeek: (Boolean) -> Unit,
    onTogglePower: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRetryStream: () -> Unit,
    onSelectRegion: (FmRegion) -> Unit,
    onToggleAnalogStatic: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onPresetTap: (Int) -> Unit,
    onPresetSave: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val receiverColors = LocalFmReceiverColors.current
    var showRegionMenu by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var savedSlotToast by remember { mutableStateOf<Pair<Int, String>?>(null) }

    LaunchedEffect(savedSlotToast) {
        if (savedSlotToast != null) {
            delay(2000)
            savedSlotToast = null
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            currentMinutes = state.sleepTimerMinutesLeft,
            onSelectMinutes = { mins ->
                onSetSleepTimer(mins)
                showSleepDialog = false
            },
            onDismiss = { showSleepDialog = false }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("tuner_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWideLayout = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = if (isWideLayout) 1040.dp else 580.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Hardware Faceplate Header + Region/Squelch/Sleep Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Radio,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.header_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { showRegionMenu = true },
                            label = {
                                Text(
                                    text = "${state.selectedRegion.badge} · ${stringResource(state.selectedRegion.labelRes)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("region_selector_button")
                        )

                        DropdownMenu(
                            expanded = showRegionMenu,
                            onDismissRequest = { showRegionMenu = false }
                        ) {
                            FmRegion.entries.forEach { region ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${region.badge} — ${stringResource(region.labelRes)}")
                                    },
                                    onClick = {
                                        showRegionMenu = false
                                        onSelectRegion(region)
                                    },
                                    modifier = Modifier.testTag("region_menu_${region.code}")
                                )
                            }
                        }
                    }
                }

                // Auxiliary Receiver Switches (Analog Static / Squelch + Sleep Timer)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = state.analogStaticEnabled,
                        onClick = onToggleAnalogStatic,
                        label = {
                            Text(
                                text = if (state.analogStaticEnabled) {
                                    stringResource(R.string.label_analog_static)
                                } else {
                                    stringResource(R.string.label_squelch_active)
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (state.analogStaticEnabled) {
                                    Icons.Outlined.GraphicEq
                                } else {
                                    Icons.Outlined.VolumeOff
                                },
                                contentDescription = stringResource(R.string.cd_squelch_toggle),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("squelch_toggle_chip")
                    )

                    FilterChip(
                        selected = state.sleepTimerMinutesLeft != null,
                        onClick = { showSleepDialog = true },
                        label = {
                            val label = if (state.sleepTimerMinutesLeft != null) {
                                stringResource(R.string.label_sleep_active, state.sleepTimerMinutesLeft)
                            } else {
                                stringResource(R.string.label_sleep_off)
                            }
                            Text(text = label, style = MaterialTheme.typography.labelMedium)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Nightlight,
                                contentDescription = stringResource(R.string.cd_sleep_timer),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sleep_timer_chip")
                    )
                }

                if (state.bandSyncStatus is BandSyncStatus.Loading && state.bandStations.isEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = receiverColors.phosphorActive
                    )
                }

                AnimatedVisibility(
                    visible = savedSlotToast != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    savedSlotToast?.let { (slot, mhz) ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.preset_saved_toast, mhz, "M$slot"),
                                style = MaterialTheme.typography.labelMedium,
                                color = receiverColors.phosphorActive,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (isWideLayout) {
                    // Canonical Two-Pane Receiver Faceplate for Tablets / Foldables / Landscape
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1.15f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LcdDisplayPanel(
                                state = state,
                                onToggleFavorite = onToggleFavorite,
                                onRetryStream = onRetryStream
                            )
                            FmScaleRuler(
                                currentFreqTenths = state.currentFreqTenths,
                                bandStations = state.bandStations,
                                onFrequencySelected = onTuneFrequency
                            )
                            HardwarePresetsRow(
                                presets = state.hardwarePresets,
                                currentFreqTenths = state.currentFreqTenths,
                                currentRegionCode = state.selectedRegion.code,
                                onPresetTap = { slot ->
                                    if (state.hardwarePresets[slot] == null) {
                                        savedSlotToast = slot to FmFrequency.formatMhz(state.currentFreqTenths)
                                    }
                                    onPresetTap(slot)
                                },
                                onPresetLongPress = { slot ->
                                    savedSlotToast = slot to FmFrequency.formatMhz(state.currentFreqTenths)
                                    onPresetSave(slot)
                                }
                            )
                        }

                        OutlinedCard(
                            modifier = Modifier.weight(0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                RotaryTuningCluster(
                                    currentFreqTenths = state.currentFreqTenths,
                                    isPowerOn = state.isPowerOn,
                                    isPlaying = state.playback.isPlaying,
                                    knobSize = 196.dp,
                                    onStepFrequency = onStepFrequency,
                                    onSeek = onSeek,
                                    onTogglePower = onTogglePower
                                )
                            }
                        }
                    }
                } else {
                    // Compact Vertical Handheld Receiver Layout
                    LcdDisplayPanel(
                        state = state,
                        onToggleFavorite = onToggleFavorite,
                        onRetryStream = onRetryStream
                    )

                    FmScaleRuler(
                        currentFreqTenths = state.currentFreqTenths,
                        bandStations = state.bandStations,
                        onFrequencySelected = onTuneFrequency
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    RotaryTuningCluster(
                        currentFreqTenths = state.currentFreqTenths,
                        isPowerOn = state.isPowerOn,
                        isPlaying = state.playback.isPlaying,
                        knobSize = 172.dp,
                        onStepFrequency = onStepFrequency,
                        onSeek = onSeek,
                        onTogglePower = onTogglePower
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    HardwarePresetsRow(
                        presets = state.hardwarePresets,
                        currentFreqTenths = state.currentFreqTenths,
                        currentRegionCode = state.selectedRegion.code,
                        onPresetTap = { slot ->
                            if (state.hardwarePresets[slot] == null) {
                                savedSlotToast = slot to FmFrequency.formatMhz(state.currentFreqTenths)
                            }
                            onPresetTap(slot)
                        },
                        onPresetLongPress = { slot ->
                            savedSlotToast = slot to FmFrequency.formatMhz(state.currentFreqTenths)
                            onPresetSave(slot)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    currentMinutes: Int?,
    onSelectMinutes: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf<Pair<Int?, Int>>(
        null to R.string.sleep_option_off,
        15 to R.string.sleep_option_15,
        30 to R.string.sleep_option_30,
        60 to R.string.sleep_option_60
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.sleep_dialog_title)) },
        text = {
            Column {
                options.forEach { (mins, labelRes) ->
                    Surface(
                        onClick = { onSelectMinutes(mins) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentMinutes == mins,
                                onClick = { onSelectMinutes(mins) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close))
            }
        }
    )
}
