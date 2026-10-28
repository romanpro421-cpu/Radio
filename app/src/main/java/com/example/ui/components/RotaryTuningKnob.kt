package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.FmFrequency
import com.example.ui.theme.LocalFmReceiverColors
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RotaryTuningCluster(
    currentFreqTenths: Int,
    isPowerOn: Boolean,
    isPlaying: Boolean,
    knobSize: Dp = 176.dp,
    onStepFrequency: (Int) -> Unit,
    onSeek: (Boolean) -> Unit,
    onTogglePower: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RotaryTuningWheel(
            currentFreqTenths = currentFreqTenths,
            isPowerOn = isPowerOn,
            knobSize = knobSize,
            onStepFrequency = onStepFrequency
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Precision Transport & Step Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onStepFrequency(-1) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("step_down_button")
            ) {
                Text(
                    text = stringResource(R.string.btn_step_down),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            FilledTonalIconButton(
                onClick = { onSeek(false) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("seek_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_seek_prev)
                )
            }

            FilledIconButton(
                onClick = onTogglePower,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .size(62.dp)
                    .testTag("power_play_button")
            ) {
                Icon(
                    imageVector = if (isPowerOn && isPlaying) {
                        Icons.Filled.Pause
                    } else if (isPowerOn) {
                        Icons.Filled.Pause
                    } else {
                        Icons.Filled.PlayArrow
                    },
                    contentDescription = stringResource(R.string.cd_play_pause),
                    modifier = Modifier.size(30.dp)
                )
            }

            FilledTonalIconButton(
                onClick = { onSeek(true) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("seek_next_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.cd_seek_next)
                )
            }

            OutlinedButton(
                onClick = { onStepFrequency(1) },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("step_up_button")
            ) {
                Text(
                    text = stringResource(R.string.btn_step_up),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun RotaryTuningWheel(
    currentFreqTenths: Int,
    isPowerOn: Boolean,
    knobSize: Dp,
    onStepFrequency: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val receiverColors = LocalFmReceiverColors.current
    val haptic = LocalHapticFeedback.current
    val dialDesc = stringResource(R.string.cd_rotary_dial)
    val latestOnStep by rememberUpdatedState(onStepFrequency)

    Box(
        modifier = modifier
            .size(knobSize)
            .semantics { contentDescription = dialDesc }
            .testTag("rotary_tuning_knob")
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    var prevAngle = atan2(down.position.y - cy, down.position.x - cx) * (180f / PI.toFloat())
                    var accumulatedDegrees = 0f
                    val degreesPerStep = 6.5f

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()

                        val curAngle = atan2(change.position.y - cy, change.position.x - cx) * (180f / PI.toFloat())
                        var delta = curAngle - prevAngle
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        prevAngle = curAngle

                        accumulatedDegrees += delta
                        val steps = (accumulatedDegrees / degreesPerStep).toInt()
                        if (steps != 0) {
                            accumulatedDegrees -= steps * degreesPerStep
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            latestOnStep(steps)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)
            val maxR = size.minDimension / 2f

            // Outer precision tick ring
            val tickCount = 60
            val outerTickR = maxR * 0.96f
            val innerTickR = maxR * 0.89f
            val progressFraction = (currentFreqTenths - FmFrequency.MIN_TENTHS).toFloat() /
                (FmFrequency.MAX_TENTHS - FmFrequency.MIN_TENTHS).toFloat()

            for (i in 0 until tickCount) {
                val angleRad = ((i.toFloat() / tickCount) * 360f - 90f) * (PI.toFloat() / 180f)
                val lit = isPowerOn && (i.toFloat() / tickCount) <= progressFraction
                drawLine(
                    color = if (lit) {
                        receiverColors.phosphorActive.copy(alpha = 0.85f)
                    } else {
                        receiverColors.knobHighlight.copy(alpha = 0.45f)
                    },
                    start = Offset(cx + cos(angleRad) * innerTickR, cy + sin(angleRad) * innerTickR),
                    end = Offset(cx + cos(angleRad) * outerTickR, cy + sin(angleRad) * outerTickR),
                    strokeWidth = if (i % 5 == 0) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Outer anodized shadow & bevel
            val bodyRadius = maxR * 0.80f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        receiverColors.knobHighlight,
                        receiverColors.knobOuter,
                        receiverColors.knobInner
                    ),
                    start = Offset(cx - bodyRadius, cy - bodyRadius),
                    end = Offset(cx + bodyRadius, cy + bodyRadius)
                ),
                radius = bodyRadius,
                center = center
            )

            // Rotating knurled notches on rim
            val rotationDeg = (currentFreqTenths - FmFrequency.MIN_TENTHS) * 6f
            val knurlCount = 36
            val knurlOuter = bodyRadius * 0.96f
            val knurlInner = bodyRadius * 0.86f
            for (k in 0 until knurlCount) {
                val deg = rotationDeg + k * (360f / knurlCount)
                val rad = deg * (PI.toFloat() / 180f)
                drawLine(
                    color = receiverColors.knobInner.copy(alpha = 0.75f),
                    start = Offset(cx + cos(rad) * knurlInner, cy + sin(rad) * knurlInner),
                    end = Offset(cx + cos(rad) * knurlOuter, cy + sin(rad) * knurlOuter),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Inner concave brushed aluminum cap
            val capRadius = bodyRadius * 0.78f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        receiverColors.knobOuter,
                        receiverColors.knobInner
                    ),
                    center = Offset(cx - capRadius * 0.2f, cy - capRadius * 0.2f),
                    radius = capRadius * 1.3f
                ),
                radius = capRadius,
                center = center
            )

            drawCircle(
                color = receiverColors.knobHighlight.copy(alpha = 0.5f),
                radius = capRadius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Glowing Amber / Vermilion Position Indicator Notch
            val indicatorDeg = -90f + rotationDeg
            val indicatorRad = indicatorDeg * (PI.toFloat() / 180f)
            val notchInner = capRadius * 0.36f
            val notchOuter = capRadius * 0.84f

            drawLine(
                color = receiverColors.needleColor.copy(alpha = 0.32f),
                start = Offset(cx + cos(indicatorRad) * notchInner, cy + sin(indicatorRad) * notchInner),
                end = Offset(cx + cos(indicatorRad) * notchOuter, cy + sin(indicatorRad) * notchOuter),
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = if (isPowerOn) receiverColors.phosphorActive else receiverColors.needleColor,
                start = Offset(cx + cos(indicatorRad) * notchInner, cy + sin(indicatorRad) * notchInner),
                end = Offset(cx + cos(indicatorRad) * notchOuter, cy + sin(indicatorRad) * notchOuter),
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
