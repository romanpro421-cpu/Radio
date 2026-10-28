package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FmStationEntity
import com.example.model.FmFrequency
import com.example.ui.theme.LocalFmReceiverColors
import kotlin.math.roundToInt

@Composable
fun FmScaleRuler(
    currentFreqTenths: Int,
    bandStations: List<FmStationEntity>,
    onFrequencySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val receiverColors = LocalFmReceiverColors.current
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = receiverColors.phosphorDim
    )
    val scaleContentDesc = stringResource(R.string.cd_frequency_ruler)

    val currentOnSelect by rememberUpdatedState(onFrequencySelected)
    val currentTenthsState by rememberUpdatedState(currentFreqTenths)

    var widthPx by remember { mutableFloatStateOf(1f) }
    var lastHapticTenths by remember { mutableIntStateOf(currentFreqTenths) }

    val stationSet = remember(bandStations) {
        bandStations.mapTo(HashSet()) { it.freqTenths }
    }

    val shape = RoundedCornerShape(16.dp)
    val totalSteps = (FmFrequency.MAX_TENTHS - FmFrequency.MIN_TENTHS).toFloat()

    fun xToTenths(x: Float, horizontalInset: Float): Int {
        val usable = (widthPx - horizontalInset * 2f).coerceAtLeast(1f)
        val fraction = ((x - horizontalInset) / usable).coerceIn(0f, 1f)
        return FmFrequency.clamp(
            FmFrequency.MIN_TENTHS + (fraction * totalSteps).roundToInt()
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(shape)
            .background(receiverColors.lcdBackground)
            .border(1.dp, receiverColors.lcdBorder, shape)
            .semantics { contentDescription = scaleContentDesc }
            .testTag("fm_scale_ruler")
            .onSizeChanged { size ->
                widthPx = size.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) {
                val inset = 20.dp.toPx()
                detectTapGestures { offset ->
                    val target = xToTenths(offset.x, inset)
                    if (target != currentTenthsState) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastHapticTenths = target
                        currentOnSelect(target)
                    }
                }
            }
            .pointerInput(Unit) {
                val inset = 20.dp.toPx()
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val target = xToTenths(change.position.x, inset)
                    if (target != lastHapticTenths) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        lastHapticTenths = target
                        currentOnSelect(target)
                    }
                }
            }
            .padding(vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 20.dp.toPx()
            val usableWidth = (size.width - inset * 2f).coerceAtLeast(1f)
            val centerY = size.height * 0.56f

            // Subtle illuminated scale guide rail
            drawLine(
                color = receiverColors.phosphorDim.copy(alpha = 0.25f),
                start = Offset(inset, centerY),
                end = Offset(size.width - inset, centerY),
                strokeWidth = 1.dp.toPx()
            )

            // Draw FM ticks from 87.5 to 108.0 MHz
            for (tenths in FmFrequency.MIN_TENTHS..FmFrequency.MAX_TENTHS) {
                val fraction = (tenths - FmFrequency.MIN_TENTHS) / totalSteps
                val x = inset + fraction * usableWidth
                val isMajor2Mhz = tenths % 20 == 0
                val isWholeMhz = tenths % 10 == 0
                val isHalfMhz = tenths % 5 == 0

                if (isWholeMhz || isHalfMhz || tenths % 2 == 0) {
                    val tickHalfHeight = when {
                        isMajor2Mhz -> 13.dp.toPx()
                        isWholeMhz -> 9.dp.toPx()
                        isHalfMhz -> 6.dp.toPx()
                        else -> 3.dp.toPx()
                    }
                    val tickColor = when {
                        isMajor2Mhz -> receiverColors.phosphorActive.copy(alpha = 0.85f)
                        isWholeMhz -> receiverColors.phosphorDim.copy(alpha = 0.75f)
                        else -> receiverColors.phosphorDim.copy(alpha = 0.38f)
                    }
                    drawLine(
                        color = tickColor,
                        start = Offset(x, centerY - tickHalfHeight),
                        end = Offset(x, centerY + tickHalfHeight * 0.65f),
                        strokeWidth = if (isWholeMhz) 1.5.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Draw numeric label at 88, 92, 96, 100, 104, 108 MHz
                if (tenths % 40 == 0 || tenths == 880 || tenths == 1080) {
                    if (tenths in listOf(880, 920, 960, 1000, 1040, 1080)) {
                        val label = (tenths / 10).toString()
                        val measured = textMeasurer.measure(label, style = labelStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x = (x - measured.size.width / 2f).coerceIn(2f, size.width - measured.size.width - 2f),
                                y = 2.dp.toPx()
                            )
                        )
                    }
                }

                // Draw glowing amber dot for active broadcast stations on the band
                if (stationSet.contains(tenths)) {
                    drawCircle(
                        color = receiverColors.phosphorActive,
                        radius = 2.4.dp.toPx(),
                        center = Offset(x, size.height - 6.dp.toPx())
                    )
                }
            }

            // Draw Vermilion Tuning Needle + Soft Halo
            val needleFraction =
                ((currentFreqTenths - FmFrequency.MIN_TENTHS) / totalSteps).coerceIn(0f, 1f)
            val needleX = inset + needleFraction * usableWidth

            drawRoundRect(
                color = receiverColors.needleColor.copy(alpha = 0.24f),
                topLeft = Offset(needleX - 6.dp.toPx(), 4.dp.toPx()),
                size = Size(12.dp.toPx(), size.height - 8.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawRoundRect(
                color = receiverColors.needleColor,
                topLeft = Offset(needleX - 1.5.dp.toPx(), 2.dp.toPx()),
                size = Size(3.dp.toPx(), size.height - 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
    }
}
