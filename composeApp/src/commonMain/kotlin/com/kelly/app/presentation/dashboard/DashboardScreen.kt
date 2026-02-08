package com.kelly.app.presentation.dashboard

import androidx.compose.animation.core.EaseOutCirc
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelly.app.domain.model.BmsData
import com.kelly.app.domain.model.MonitorData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// -- VESC Tool authentic color palette ----------------------------------------
// Extracted from Utility.getAppHexColor() in VESC Tool dark mode

private val DarkBackground = Color(0xFF202020)    // VESC "darkBackground"
private val LightestBg = Color(0xFF505050)        // VESC "lightestBackground"
private val LightText = Color(0xFFD5D5D5)         // VESC "lightText"
private val DisabledText = Color(0xFF808080)       // VESC "disabledText"
private val Tertiary1 = Color(0xFFd32f2f)         // VESC red
private val Tertiary2 = Color(0xFFf9a825)         // VESC amber/yellow
private val Tertiary3 = Color(0xFF00bcd4)         // VESC cyan
private val VescOrange = Color(0xFFff6f00)        // VESC orange
private val VescGreen = Color(0xFF4CAF50)         // VESC green
private val RedDanger = Color(0xFFd32f2f)
private val GreenGood = Color(0xFF4CAF50)
private val CardBackground = Color(0xFF2A2A2A)

// =============================================================================
// Dashboard Screen
// =============================================================================

@Composable
fun DashboardScreen(component: DashboardComponent) {
    val monitorData by component.monitorData.collectAsState()
    val bmsData by component.bmsData.collectAsState()
    val settings by component.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        ErrorStatusBar(monitorData)

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isWide = maxWidth > maxHeight * 1.1f
            val density = LocalDensity.current
            val maxW = with(density) { maxWidth.toPx() }
            val maxH = with(density) { maxHeight.toPx() }

            val gaugeSizePx = if (isWide) {
                min(maxH / 1.35f, maxW / 3.1f)
            } else {
                min(maxW / 1.8f, maxH / 2.7f)
            }
            val gaugeSize = with(density) { gaugeSizePx.toDp() }
            val gaugeSize2 = gaugeSize * 0.55f

            if (isWide) {
                WideLayout(monitorData, bmsData, settings, gaugeSize, gaugeSize2)
            } else {
                NarrowLayout(monitorData, bmsData, settings, gaugeSize, gaugeSize2)
            }
        }

        BottomInfoPanel(monitorData)
    }
}

// =============================================================================
// Wide layout (landscape)
// =============================================================================

@Composable
private fun WideLayout(
    monitorData: MonitorData,
    bmsData: BmsData,
    settings: DashboardSettings,
    gaugeSize: Dp,
    gaugeSize2: Dp
) {
    val values = monitorData.values

    Row(modifier = Modifier.fillMaxSize()) {
        // Left column — two clusters
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GaugeCluster(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                gaugeSize = gaugeSize2,
                leftGauge = { mod ->
                    VescGauge(
                        modifier = mod, value = values["Phase Current"].toSafeFloat(),
                        minValue = 0f, maxValue = settings.maxCurrentA.toFloat(),
                        minAngle = -210f, maxAngle = 15f,
                        labelStep = niceStepAndMax(settings.maxCurrentA.toFloat()).first,
                        typeText = "CURRENT", unitText = "A", nibColor = Tertiary1
                    )
                },
                rightGauge = { mod ->
                    VescGauge(
                        modifier = mod, value = values["TPS Pedel"].toSafeFloat(),
                        minValue = 0f, maxValue = 255f,
                        minAngle = 210f, maxAngle = -15f, labelStep = 50f,
                        typeText = "TPS", unitText = "%", nibColor = Tertiary3
                    )
                },
                centerGauge = if (bmsData.isConnected) { { mod ->
                    VescGauge(
                        modifier = mod, value = bmsData.power,
                        minValue = 0f, maxValue = 5000f,
                        minAngle = -225f, maxAngle = 45f, labelStep = 1000f,
                        typeText = "POWER", unitText = "W", nibColor = Tertiary3
                    )
                } } else null
            )

            GaugeCluster(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                gaugeSize = gaugeSize2,
                leftGauge = { mod ->
                    val t = values["Controller Temp"].toSafeFloat()
                    VescGauge(
                        modifier = mod, value = t,
                        minValue = 0f, maxValue = 150f,
                        minAngle = -195f, maxAngle = 30f, labelStep = 30f,
                        typeText = "TEMP\nCTRL", unitText = "\u00B0C", nibColor = tempColor(t)
                    )
                },
                rightGauge = { mod ->
                    val t = values["Motor Temp"].toSafeFloat()
                    VescGauge(
                        modifier = mod, value = t,
                        minValue = 0f, maxValue = 150f,
                        minAngle = 195f, maxAngle = -30f, labelStep = 30f,
                        typeText = "TEMP\nMOTOR", unitText = "\u00B0C", nibColor = tempColor(t)
                    )
                },
                centerGauge = if (bmsData.isConnected) { { mod ->
                    VescGauge(
                        modifier = mod, value = bmsData.soc,
                        minValue = 0f, maxValue = 100f,
                        minAngle = -225f, maxAngle = 45f, labelStep = 20f,
                        typeText = "SOC", unitText = "%", nibColor = VescGreen
                    )
                } } else null,
                invertVertical = true
            )
        }

        // Right column — Speed + nested Battery
        SpeedCluster(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            values = values, bmsData = bmsData, settings = settings,
            gaugeSize = gaugeSize, gaugeSize2 = gaugeSize2
        )
    }
}

// =============================================================================
// Narrow layout (portrait)
// =============================================================================

@Composable
private fun NarrowLayout(
    monitorData: MonitorData,
    bmsData: BmsData,
    settings: DashboardSettings,
    gaugeSize: Dp,
    gaugeSize2: Dp
) {
    val values = monitorData.values

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        GaugeCluster(
            modifier = Modifier.weight(0.32f).fillMaxWidth(),
            gaugeSize = gaugeSize2,
            leftGauge = { mod ->
                VescGauge(
                    modifier = mod, value = values["Phase Current"].toSafeFloat(),
                    minValue = 0f, maxValue = settings.maxCurrentA.toFloat(),
                    minAngle = -210f, maxAngle = 15f,
                    labelStep = niceStepAndMax(settings.maxCurrentA.toFloat()).first,
                    typeText = "CURRENT", unitText = "A", nibColor = Tertiary1
                )
            },
            rightGauge = { mod ->
                VescGauge(
                    modifier = mod, value = values["TPS Pedel"].toSafeFloat(),
                    minValue = 0f, maxValue = 255f,
                    minAngle = 210f, maxAngle = -15f, labelStep = 50f,
                    typeText = "TPS", unitText = "%", nibColor = Tertiary3
                )
            },
            centerGauge = if (bmsData.isConnected) { { mod ->
                VescGauge(
                    modifier = mod, value = bmsData.power,
                    minValue = 0f, maxValue = 5000f,
                    minAngle = -225f, maxAngle = 45f, labelStep = 1000f,
                    typeText = "POWER", unitText = "W", nibColor = Tertiary3
                )
            } } else null
        )

        SpeedCluster(
            modifier = Modifier.weight(0.38f).fillMaxWidth(),
            values = values, bmsData = bmsData, settings = settings,
            gaugeSize = gaugeSize, gaugeSize2 = gaugeSize2
        )

        GaugeCluster(
            modifier = Modifier.weight(0.30f).fillMaxWidth(),
            gaugeSize = gaugeSize2,
            leftGauge = { mod ->
                val t = values["Controller Temp"].toSafeFloat()
                VescGauge(
                    modifier = mod, value = t,
                    minValue = 0f, maxValue = 150f,
                    minAngle = -195f, maxAngle = 30f, labelStep = 30f,
                    typeText = "TEMP\nCTRL", unitText = "\u00B0C", nibColor = tempColor(t)
                )
            },
            rightGauge = { mod ->
                val t = values["Motor Temp"].toSafeFloat()
                VescGauge(
                    modifier = mod, value = t,
                    minValue = 0f, maxValue = 150f,
                    minAngle = 195f, maxAngle = -30f, labelStep = 30f,
                    typeText = "TEMP\nMOTOR", unitText = "\u00B0C", nibColor = tempColor(t)
                )
            },
            centerGauge = if (bmsData.isConnected) { { mod ->
                VescGauge(
                    modifier = mod, value = bmsData.soc,
                    minValue = 0f, maxValue = 100f,
                    minAngle = -225f, maxAngle = 45f, labelStep = 20f,
                    typeText = "SOC", unitText = "%", nibColor = VescGreen
                )
            } } else null,
            invertVertical = true
        )
    }
}

// =============================================================================
// Speed cluster — Speed gauge + nested Battery gauge
// =============================================================================

@Composable
private fun SpeedCluster(
    modifier: Modifier,
    values: Map<String, String>,
    bmsData: BmsData,
    settings: DashboardSettings,
    gaugeSize: Dp,
    gaugeSize2: Dp
) {
    val motorRpm = values["Motor Speed"].toSafeFloat()
    val displaySpeed = convertSpeed(motorRpm, settings)
    val maxRpm = 15000f

    // Use clean values for RPM; cap at user-configured maxSpeedDisplay for km/h/mph
    val maxVal: Float
    val labelStep: Float
    if (settings.speedUnit == SpeedUnit.RPM) {
        maxVal = maxRpm
        labelStep = 3000f
    } else {
        val cap = settings.maxSpeedDisplay.toFloat().coerceAtLeast(10f)
        val (s, m) = niceStepAndMax(cap)
        labelStep = s; maxVal = m
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        VescGauge(
            modifier = Modifier.size(gaugeSize),
            value = displaySpeed,
            minValue = 0f, maxValue = maxVal,
            minAngle = -225f, maxAngle = 45f,
            labelStep = labelStep,
            typeText = "SPEED", unitText = settings.speedUnit.suffix,
            nibColor = VescGreen
        )
        // Battery gauge nested to the right of speed
        Box(
            modifier = Modifier
                .size(gaugeSize2)
                .offset(x = gaugeSize * 0.22f + gaugeSize2 * 0.35f)
        ) {
            val bVolt = if (bmsData.isConnected) bmsData.voltage else values["B+ Volt"].toSafeFloat()
            VescGauge(
                modifier = Modifier.fillMaxSize(),
                value = bVolt,
                minValue = 0f, maxValue = 100f,
                minAngle = -225f, maxAngle = 45f, labelStep = 20f,
                typeText = "B+", unitText = "V",
                nibColor = batteryColor(bVolt), precision = 1
            )
        }
    }
}

// =============================================================================
// GaugeCluster — 3 overlapping gauges
// =============================================================================

@Composable
private fun GaugeCluster(
    modifier: Modifier,
    gaugeSize: Dp,
    leftGauge: @Composable (Modifier) -> Unit,
    rightGauge: @Composable (Modifier) -> Unit,
    centerGauge: (@Composable (Modifier) -> Unit)? = null,
    invertVertical: Boolean = false
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val hOffset = if (centerGauge != null) gaugeSize * 0.675f else gaugeSize * 0.5f
        val vOffset = gaugeSize * 0.1f
        val sideV = if (invertVertical) -vOffset else vOffset

        leftGauge(Modifier.size(gaugeSize).offset(x = -hOffset, y = sideV))
        rightGauge(Modifier.size(gaugeSize).offset(x = hOffset, y = sideV))
        centerGauge?.invoke(Modifier.size(gaugeSize * 1.05f).offset(y = -sideV))
    }
}

// =============================================================================
// VescGauge — VESC Tool CustomGauge port
// =============================================================================

@Composable
private fun VescGauge(
    modifier: Modifier,
    value: Float,
    minValue: Float,
    maxValue: Float,
    minAngle: Float = -140f,
    maxAngle: Float = 140f,
    labelStep: Float = 10f,
    typeText: String,
    unitText: String,
    nibColor: Color,
    precision: Int = 0,
    centerTextVisible: Boolean = true,
    tickmarkScale: Float = 1f,
    tickmarkSuffix: String = ""
) {
    val textMeasurer = rememberTextMeasurer()
    val clampedValue = value.coerceIn(minValue, maxValue)
    val animatedValue by animateFloatAsState(
        targetValue = clampedValue,
        animationSpec = tween(durationMillis = 100, easing = EaseOutCirc)
    )
    val isInverted = if (maxAngle > minAngle) 1 else -1
    val traceColor = nibColor.lighter(0.5f)

    Canvas(modifier = modifier) {
        val dim = min(size.width, size.height)
        if (dim <= 0f) return@Canvas
        val outerRadius = dim / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val angleRange = maxAngle - minAngle
        // outerRadius is in px; sp is multiplied by density again when measured.
        // Divide by density so text stays proportional to gauge size on any screen.
        val pxToSp = 1f / density

        fun valueToAngle(v: Float): Float {
            val n = ((v - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
            return angleRange * n + minAngle
        }

        fun isCovered(tv: Float): Boolean {
            return if (animatedValue >= 0f) tv in 0f..animatedValue
            else tv in animatedValue..0f
        }

        // -- 1. Dark circular background (same as app bg for seamless overlap) --
        drawCircle(color = DarkBackground, radius = outerRadius, center = Offset(cx, cy))

        // -- 2. Gradient bezels (VESC metallic 3D effect) --
        val bw = outerRadius * 0.035f
        val bezelGrad1 = Brush.linearGradient(
            colors = listOf(LightestBg, DarkBackground, LightestBg),
            start = Offset(cx + outerRadius, cy - outerRadius),
            end = Offset(cx - outerRadius, cy + outerRadius)
        )
        drawCircle(
            brush = bezelGrad1,
            radius = outerRadius - bw / 2f,
            center = Offset(cx, cy),
            style = Stroke(width = bw)
        )
        val bezelGrad2 = Brush.linearGradient(
            colors = listOf(DarkBackground, LightestBg, DarkBackground),
            start = Offset(cx + outerRadius, cy - outerRadius),
            end = Offset(cx - outerRadius, cy + outerRadius)
        )
        drawCircle(
            brush = bezelGrad2,
            radius = outerRadius - bw * 3f / 2f + 1f,
            center = Offset(cx, cy),
            style = Stroke(width = bw)
        )

        // -- 3. Tick marks --
        val tickmarkCount = if (labelStep > 0f)
            floor((maxValue - minValue) / labelStep + 1f).toInt().coerceAtMost(100)
        else 0
        val minorCount = 4
        val tickInset = outerRadius * 0.07f

        if (tickmarkCount > 1) {
            val secSize = angleRange / (tickmarkCount - 1)
            val secVal = (maxValue - minValue) / (tickmarkCount - 1)
            val minSecSize = secSize / (minorCount + 1)
            val minSecVal = secVal / (minorCount + 1)

            // Major ticks
            for (i in 0 until tickmarkCount) {
                val angle = i * secSize + minAngle
                val tv = i * secVal + minValue
                val col = if (isCovered(tv)) LightText else DisabledText
                val ar = (angle - 90f) * PI.toFloat() / 180f
                val r1 = outerRadius - tickInset - outerRadius * 0.1f
                val r2 = outerRadius - tickInset
                drawLine(col, Offset(cx + r1 * cos(ar), cy + r1 * sin(ar)),
                    Offset(cx + r2 * cos(ar), cy + r2 * sin(ar)),
                    strokeWidth = outerRadius * 0.02f, cap = StrokeCap.Butt)
            }

            // Minor ticks
            val totalMinor = minorCount * (tickmarkCount - 1)
            for (i in 0 until totalMinor) {
                val mj = i / minorCount
                val angle = mj * secSize + minAngle + (i % minorCount + 1) * minSecSize
                val tv = mj * secVal + minValue + (i % minorCount + 1) * minSecVal
                val col = if (isCovered(tv)) LightText else DisabledText
                val ar = (angle - 90f) * PI.toFloat() / 180f
                val r1 = outerRadius - tickInset - outerRadius * 0.07f
                val r2 = outerRadius - tickInset
                drawLine(col, Offset(cx + r1 * cos(ar), cy + r1 * sin(ar)),
                    Offset(cx + r2 * cos(ar), cy + r2 * sin(ar)),
                    strokeWidth = outerRadius * 0.015f, cap = StrokeCap.Butt)
            }
        }

        // -- 4. Tick labels --
        if (tickmarkCount > 1 && centerTextVisible) {
            val labelInset = outerRadius * 0.34f
            val labelR = outerRadius - labelInset
            // Smaller font for large numbers (e.g. RPM 15000) to prevent overflow
            val maxDigits = maxValue.roundToInt().toString().length
            val labelScale = if (maxDigits >= 5) 0.085f else if (maxDigits >= 4) 0.10f else 0.12f
            val labelFs = (outerRadius * labelScale * pxToSp).sp
            val secSize = angleRange / (tickmarkCount - 1)
            val secVal = (maxValue - minValue) / (tickmarkCount - 1)

            for (i in 0 until tickmarkCount) {
                val angle = i * secSize + minAngle
                val tv = i * secVal + minValue
                val col = if (isCovered(tv)) LightText else DisabledText
                val ar = (angle - 90f) * PI.toFloat() / 180f
                val txt = (tv * tickmarkScale).roundToInt().toString() + tickmarkSuffix
                val style = TextStyle(color = col, fontSize = labelFs, textAlign = TextAlign.Center)
                val layout = textMeasurer.measure(txt, style)
                drawText(layout, topLeft = Offset(
                    cx + labelR * cos(ar) - layout.size.width / 2f,
                    cy + labelR * sin(ar) - layout.size.height / 2f
                ))
            }
        }

        // -- 5. Glowing arc from 0 to value --
        val zeroAngle = valueToAngle(0f)
        val curAngle = valueToAngle(animatedValue)
        val arcStart: Float
        val arcSweep: Float
        if (animatedValue * isInverted >= 0) {
            arcStart = zeroAngle - 90f
            arcSweep = curAngle - zeroAngle
        } else {
            arcStart = curAngle - 90f
            arcSweep = zeroAngle - curAngle
        }
        val arcInset = outerRadius * 0.12f
        val arcR = outerRadius - arcInset
        val arcTL = Offset(cx - arcR, cy - arcR)
        val arcSz = Size(arcR * 2f, arcR * 2f)

        if (abs(arcSweep) > 0.1f) {
            // Wide translucent glow
            drawArc(
                color = traceColor.copy(alpha = 0.2f),
                startAngle = arcStart, sweepAngle = arcSweep,
                useCenter = false, topLeft = arcTL, size = arcSz,
                style = Stroke(width = outerRadius * 0.18f, cap = StrokeCap.Butt)
            )
            // Bright core
            drawArc(
                color = traceColor.copy(alpha = 0.7f),
                startAngle = arcStart, sweepAngle = arcSweep,
                useCenter = false, topLeft = arcTL, size = arcSz,
                style = Stroke(width = outerRadius * 0.04f, cap = StrokeCap.Butt)
            )
        }

        // -- 6. Glass highlight effect --
        val glassCenter = Offset(cx - outerRadius * 0.22f, cy - outerRadius * 0.32f)
        val glassLayers = 5
        for (layer in 0 until glassLayers) {
            val t = layer.toFloat() / glassLayers
            val layerR = outerRadius * (0.88f + t * 0.06f)
            val layerTL = Offset(cx - layerR, cy - layerR)
            val layerSz = Size(layerR * 2f, layerR * 2f)
            val baseAlpha = 0.14f * (1f - t * 0.7f)
            val strokeW = outerRadius * (0.15f + t * 0.12f)
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = baseAlpha),
                        Color.White.copy(alpha = baseAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = glassCenter,
                    radius = outerRadius * 0.85f
                ),
                startAngle = 195f, sweepAngle = 150f,
                useCenter = false, topLeft = layerTL, size = layerSz,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }

        // -- 7. Triangle needle --
        val needleAngle = valueToAngle(animatedValue)
        val nh = outerRadius * 0.22f
        val nw = outerRadius * 0.12f

        rotate(degrees = needleAngle, pivot = Offset(cx, cy)) {
            val tipY = cy - outerRadius + outerRadius * 0.05f
            val baseY = tipY + nh

            // Right half
            val pathR = Path().apply {
                moveTo(cx, tipY)
                lineTo(cx + nw / 2f, tipY + nh * 0.015f)
                quadraticTo(cx + nw * 0.35f, tipY + nh / 4f, cx + nw * 0.1f, baseY)
                quadraticTo(cx + nw * 0.1f, baseY + nh * 0.08f, cx, baseY + nh * 0.08f)
                close()
            }
            drawPath(pathR, brush = Brush.verticalGradient(
                colors = listOf(Color.White, nibColor),
                startY = tipY, endY = baseY
            ))

            // Left half (slightly darker for 3D)
            val pathL = Path().apply {
                moveTo(cx, tipY)
                lineTo(cx - nw / 2f, tipY + nh * 0.015f)
                quadraticTo(cx - nw * 0.35f, tipY + nh / 4f, cx - nw * 0.1f, baseY)
                quadraticTo(cx - nw * 0.1f, baseY + nh * 0.08f, cx, baseY + nh * 0.08f)
                close()
            }
            drawPath(pathL, brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.8f), nibColor.darker(0.3f)),
                startY = tipY, endY = baseY
            ))
        }

        // -- 7. Center text --
        if (centerTextVisible) {
            val typeLines = typeText.split("\n")
            val typFs = (outerRadius * 0.12f * pxToSp).sp
            val valFs = (outerRadius * 0.3f * pxToSp).sp
            val unitFs = (outerRadius * 0.12f * pxToSp).sp

            val valueStr = if (precision == 0) animatedValue.roundToInt().toString()
            else {
                val ip = animatedValue.toLong()
                val dp = ((abs(animatedValue) - abs(ip.toFloat())) * 10f).roundToInt()
                "$ip.$dp"
            }

            val valStyle = TextStyle(color = LightText, fontSize = valFs,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            val valLay = textMeasurer.measure(valueStr, valStyle)
            drawText(valLay, topLeft = Offset(
                cx - valLay.size.width / 2f, cy - valLay.size.height / 2f))

            // Type text above
            val typStyle = TextStyle(color = LightText, fontSize = typFs,
                textAlign = TextAlign.Center)
            var ty = cy - valLay.size.height / 2f
            for (line in typeLines.reversed()) {
                val ll = textMeasurer.measure(line, typStyle)
                ty -= ll.size.height
                drawText(ll, topLeft = Offset(cx - ll.size.width / 2f, ty))
            }

            // Unit text below
            val unitStyle = TextStyle(color = LightText, fontSize = unitFs,
                textAlign = TextAlign.Center)
            val unitLay = textMeasurer.measure(unitText, unitStyle)
            drawText(unitLay, topLeft = Offset(
                cx - unitLay.size.width / 2f, cy + valLay.size.height / 2f))
        }
    }
}

// =============================================================================
// Bottom info panel
// =============================================================================

@Composable
private fun BottomInfoPanel(monitorData: MonitorData) {
    val values = monitorData.values
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        color = DarkBackground, shape = RoundedCornerShape(6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, LightestBg, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasErrors = monitorData.errorMessages.isNotEmpty()
                PanelItem("ERROR",
                    if (hasErrors) monitorData.errorMessages.first() else "NONE",
                    if (hasErrors) RedDanger else GreenGood)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("HALL", style = MaterialTheme.typography.labelSmall,
                        color = DisabledText, fontSize = 8.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        HallDot("A", values["Hall A"] ?: "-")
                        HallDot("B", values["Hall B"] ?: "-")
                        HallDot("C", values["Hall C"] ?: "-")
                    }
                }

                val dir = values["Actual Dir"] ?: "-"
                val fwd = dir == "1" || dir.equals("Forward", ignoreCase = true)
                PanelItem("DIR", if (fwd) "FWD" else "REV",
                    if (fwd) GreenGood else VescOrange)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SWITCHES", style = MaterialTheme.typography.labelSmall,
                        color = DisabledText, fontSize = 8.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        CompactSwitch("BRK", values["Brake Switch"] ?: "-")
                        CompactSwitch("FOOT", values["Foot Switch"] ?: "-")
                        CompactSwitch("FWD", values["Forward Switch"] ?: "-")
                        CompactSwitch("LOW", values["Low Speed"] ?: "-")
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = DisabledText, fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.labelMedium,
            color = valueColor, fontWeight = FontWeight.Bold)
    }
}

// =============================================================================
// Error status bar
// =============================================================================

@Composable
private fun ErrorStatusBar(monitorData: MonitorData) {
    if (monitorData.communicationError != null) {
        Surface(Modifier.fillMaxWidth(), color = Tertiary2.copy(alpha = 0.85f)) {
            Text(monitorData.communicationError,
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = DarkBackground, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold)
        }
    }
    if (monitorData.errorMessages.isNotEmpty()) {
        Surface(Modifier.fillMaxWidth(), color = RedDanger.copy(alpha = 0.85f)) {
            Text("ERROR: ${monitorData.errorMessages.joinToString(" | ")}",
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color.White, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold)
        }
    } else if (monitorData.isActive && monitorData.communicationError == null) {
        Surface(Modifier.fillMaxWidth(), color = GreenGood.copy(alpha = 0.15f)) {
            Text("NO FAULTS", Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = GreenGood, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold)
        }
    }
}

// =============================================================================
// Small reusable composables
// =============================================================================

@Composable
private fun HallDot(label: String, value: String) {
    val active = value == "1" || value.equals("ON", ignoreCase = true)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(10.dp)) {
            drawCircle(if (active) Tertiary3 else LightestBg, size.minDimension / 2f)
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (active) Tertiary3 else DisabledText, fontSize = 7.sp)
    }
}

@Composable
private fun CompactSwitch(label: String, value: String) {
    val active = value == "1" || value.equals("ON", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = if (active) GreenGood.copy(alpha = 0.2f) else LightestBg.copy(alpha = 0.3f)
    ) {
        Text(label, Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            color = if (active) GreenGood else DisabledText,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

// =============================================================================
// Speed conversion
// =============================================================================

private fun convertSpeed(motorRpm: Float, s: DashboardSettings): Float {
    return when (s.speedUnit) {
        SpeedUnit.RPM -> motorRpm
        SpeedUnit.KMH -> {
            val wheelCircM = PI.toFloat() * s.wheelDiameterMm / 1000f
            motorRpm / s.gearRatio * wheelCircM * 60f / 1000f
        }
        SpeedUnit.MPH -> {
            val wheelCircM = PI.toFloat() * s.wheelDiameterMm / 1000f
            motorRpm / s.gearRatio * wheelCircM * 60f / 1000f * 0.621371f
        }
    }
}

private fun niceStepAndMax(rawMax: Float): Pair<Float, Float> {
    if (rawMax <= 0f) return 10f to 100f
    val steps = floatArrayOf(1f, 2f, 5f, 10f, 20f, 25f, 50f, 100f, 200f, 500f,
        1000f, 2000f, 3000f, 5000f, 10000f)
    for (step in steps) {
        val max = ceil(rawMax / step) * step
        val ticks = (max / step).toInt()
        if (ticks in 4..10) return step to max
    }
    val step = ceil(rawMax / 5f)
    return step to step * 5f
}

// =============================================================================
// Utility
// =============================================================================

private fun String?.toSafeFloat(): Float = this?.toFloatOrNull() ?: 0f

private fun tempColor(temp: Float): Color = when {
    temp > 120f -> Tertiary1
    temp > 80f -> VescOrange
    else -> Tertiary2
}

private fun batteryColor(voltage: Float): Color = when {
    voltage > 58f -> Tertiary1
    voltage < 36f -> Tertiary1
    else -> VescGreen
}

/** Lighten a color by blending toward white */
private fun Color.lighter(amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * a,
        green = green + (1f - green) * a,
        blue = blue + (1f - blue) * a,
        alpha = alpha
    )
}

/** Darken a color by blending toward black */
private fun Color.darker(amount: Float): Color {
    val a = (1f - amount.coerceIn(0f, 1f))
    return Color(red = red * a, green = green * a, blue = blue * a, alpha = alpha)
}
