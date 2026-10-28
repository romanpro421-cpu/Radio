package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.HardwarePresetEntity
import com.example.model.FmFrequency
import com.example.ui.theme.LocalFmReceiverColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HardwarePresetsRow(
    presets: Map<Int, HardwarePresetEntity>,
    currentFreqTenths: Int,
    currentRegionCode: String,
    onPresetTap: (Int) -> Unit,
    onPresetLongPress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val receiverColors = LocalFmReceiverColors.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.presets_strip_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 1..3) {
                        val slot = row * 3 + col
                        val preset = presets[slot]
                        val isActive = preset != null &&
                            preset.freqTenths == currentFreqTenths &&
                            preset.regionCode == currentRegionCode

                        val buttonShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .clip(buttonShape)
                                .background(
                                    if (isActive) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .border(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) {
                                        receiverColors.phosphorActive
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    shape = buttonShape
                                )
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onPresetTap(slot)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onPresetLongPress(slot)
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("preset_slot_$slot")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "M$slot",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) {
                                            receiverColors.phosphorActive
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isActive -> receiverColors.phosphorActive
                                                    preset != null -> receiverColors.signalActive.copy(alpha = 0.65f)
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            )
                                    )
                                }
                                Text(
                                    text = if (preset != null) {
                                        FmFrequency.formatMhz(preset.freqTenths)
                                    } else {
                                        stringResource(R.string.preset_empty_label)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (preset != null) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
