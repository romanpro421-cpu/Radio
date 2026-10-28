package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalFmReceiverColors
import com.example.ui.viewmodel.RadioUiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LcdDisplayPanel(
    state: RadioUiState,
    onToggleFavorite: () -> Unit,
    onRetryStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    val receiverColors = LocalFmReceiverColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "lcd_pulse")
    val livePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )

    val stationName = state.directOverrideStationName
        ?: state.tunedStation?.name
        ?: if (state.isSeeking) {
            stringResource(R.string.badge_buffering)
        } else {
            stringResource(R.string.badge_static)
        }

    val rdsLine = when {
        state.isSeeking -> stringResource(R.string.rds_scanning)
        !state.isPowerOn && state.tunedStation != null -> {
            buildString {
                append(state.tunedStation.name)
                if (state.tunedStation.tags.isNotBlank()) {
                    append(" · ")
                    append(state.tunedStation.tags)
                }
                append(" · ")
                append(stringResource(R.string.rds_paused))
            }
        }
        state.playback.hasStreamError -> stringResource(R.string.rds_stream_error)
        state.playback.isBuffering -> stringResource(R.string.rds_buffering_stream)
        !state.playback.liveStreamTitle.isNullOrBlank() -> state.playback.liveStreamTitle
        state.tunedStation != null -> {
            buildString {
                if (state.tunedStation.tags.isNotBlank()) {
                    append(state.tunedStation.tags)
                }
                if (state.tunedStation.state.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(state.tunedStation.state)
                }
                if (state.tunedStation.country.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(state.tunedStation.country)
                }
            }.ifBlank { state.tunedStation.name }
        }
        else -> stringResource(R.string.rds_no_carrier)
    }

    val panelShape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (state.isPowerOn) receiverColors.phosphorGlow else receiverColors.lcdBackground,
                        receiverColors.lcdBackground
                    )
                )
            )
            .border(1.5.dp, receiverColors.lcdBorder, panelShape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("lcd_display_panel")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Status Strip: Status LED + STEREO/RDS Badges + Signal RSSI Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val dotColor = when {
                        !state.isPowerOn -> receiverColors.phosphorDim.copy(alpha = 0.45f)
                        state.playback.isPlaying -> receiverColors.signalActive.copy(alpha = livePulseAlpha)
                        state.playback.isBuffering || state.isSeeking ->
                            receiverColors.phosphorActive.copy(alpha = livePulseAlpha)
                        else -> receiverColors.needleColor
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    val statusLabel = when {
                        !state.isPowerOn -> stringResource(R.string.badge_standby)
                        state.playback.isPlaying -> stringResource(R.string.badge_live)
                        state.playback.isBuffering || state.isSeeking -> stringResource(R.string.badge_buffering)
                        else -> stringResource(R.string.band_label_fm)
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isPowerOn) receiverColors.phosphorActive else receiverColors.phosphorDim
                    )
                    LcdMicroBadge(
                        text = if (state.isStereoLocked) {
                            stringResource(R.string.badge_stereo)
                        } else {
                            stringResource(R.string.badge_mono)
                        },
                        lit = state.isStereoLocked
                    )
                    LcdMicroBadge(
                        text = stringResource(R.string.badge_rds),
                        lit = state.isPowerOn && state.tunedStation != null
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.tunedStation?.let { st ->
                        val techInfo = if (st.bitrate > 0) "${st.codec} ${st.bitrate}k" else st.codec
                        Text(
                            text = techInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = receiverColors.phosphorDim
                        )
                    }
                    SignalStrengthBars(bars = state.signalBars)
                }
            }

            // Main Frequency Readout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.band_label_fm),
                        style = MaterialTheme.typography.titleSmall,
                        color = receiverColors.needleColor,
                        modifier = Modifier.padding(bottom = 10.dp, end = 8.dp)
                    )
                    Text(
                        text = state.formattedFrequency,
                        style = MaterialTheme.typography.displayLarge,
                        color = if (state.isPowerOn) {
                            receiverColors.phosphorActive
                        } else {
                            receiverColors.phosphorActive.copy(alpha = 0.72f)
                        },
                        modifier = Modifier.testTag("frequency_readout_text")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.unit_mhz),
                        style = MaterialTheme.typography.titleSmall,
                        color = receiverColors.phosphorDim,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.playback.isPlaying) {
                        MiniAcousticSpectrum(modifier = Modifier.padding(end = 6.dp))
                    }
                    if (state.tunedStation != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("favorite_current_button")
                        ) {
                            Icon(
                                imageVector = if (state.isCurrentStationFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                                contentDescription = stringResource(R.string.cd_toggle_favorite),
                                tint = if (state.isCurrentStationFavorite) {
                                    receiverColors.needleColor
                                } else {
                                    receiverColors.phosphorDim
                                }
                            )
                        }
                    }
                }
            }

            // Station Title + Scrolling RDS Ticker
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.tunedStation != null || state.directOverrideStationName != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        receiverColors.phosphorDim
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("station_name_text")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = rdsLine,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (state.playback.hasStreamError) {
                            receiverColors.needleColor
                        } else {
                            receiverColors.phosphorActive.copy(alpha = 0.85f)
                        },
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(iterations = Int.MAX_VALUE)
                            .testTag("rds_marquee_text")
                    )

                    if (state.playback.hasStreamError) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = onRetryStream,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("retry_stream_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_retry),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LcdMicroBadge(text: String, lit: Boolean) {
    val receiverColors = LocalFmReceiverColors.current
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (lit) receiverColors.phosphorActive else receiverColors.phosphorDim.copy(alpha = 0.4f),
                shape = shape
            )
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (lit) receiverColors.phosphorActive else receiverColors.phosphorDim.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun SignalStrengthBars(bars: Int) {
    val receiverColors = LocalFmReceiverColors.current
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        for (i in 1..5) {
            val active = i <= bars
            val barColor = when {
                !active -> receiverColors.phosphorDim.copy(alpha = 0.28f)
                bars >= 4 -> receiverColors.signalActive
                else -> receiverColors.phosphorActive
            }
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((4 + i * 2.5f).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun MiniAcousticSpectrum(modifier: Modifier = Modifier) {
    val receiverColors = LocalFmReceiverColors.current
    val transition = rememberInfiniteTransition(label = "spectrum")
    val barDurations = listOf(420, 310, 510, 360, 460)
    Row(
        modifier = modifier.height(22.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        barDurations.forEachIndexed { idx, duration ->
            val frac by transition.animateFloat(
                initialValue = 0.22f,
                targetValue = 0.98f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, delayMillis = idx * 45),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "spec_bar_$idx"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((22f * frac).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(receiverColors.phosphorActive)
            )
        }
    }
}
