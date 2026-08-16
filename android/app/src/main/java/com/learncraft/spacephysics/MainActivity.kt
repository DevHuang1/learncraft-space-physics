package com.learncraft.spacephysics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.learncraft.spacephysics.shared.Body
import com.learncraft.spacephysics.shared.FixedStepRunner
import com.learncraft.spacephysics.shared.GravityWell
import com.learncraft.spacephysics.shared.PhysicsEngine
import com.learncraft.spacephysics.shared.PhysicsSettings
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpacePhysicsApp() }
    }
}

@Composable
private fun SpacePhysicsApp() {
    val context = LocalContext.current
    val engine = remember { PhysicsEngine() }
    val audio = remember { SpatialAudioController(context) }
    val settings = remember { PhysicsSettings() }
    var selected by remember { mutableStateOf<Int?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var frameTick by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(true) }
    val colors = remember { listOf(0xFFA78BFA, 0xFF34D399, 0xFF60A5FA, 0xFFFBBF24, 0xFFF472B6) }

    DisposableEffect(audio) { onDispose { audio.close() } }

    LaunchedEffect(Unit) {
        audio.loadSounds(R.raw.collision, R.raw.gravity_well)
        repeat(220) { i ->
            val angle = (i * 0.618f) % 6.28f
            val radius = 90f + (i % 18) * 20f
            engine.bodies += Body(
                id = i,
                x = 640f + cos(angle) * radius,
                y = 420f + sin(angle) * radius,
                vx = -sin(angle) * .35f,
                vy = cos(angle) * .35f,
                mass = 0.7f + (i % 5) * .15f,
                radius = 2f + (i % 4),
                elasticity = .82f,
                color = colors[i % colors.size],
            )
        }
        val runner = FixedStepRunner()
        var previous = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (previous == 0L) previous = now
                val dt = ((now - previous) / 1_000_000_000f).coerceIn(0f, .05f)
                previous = now
                runner.advance(dt) { fixedSeconds ->
                    val width = viewport.width.toFloat().coerceAtLeast(1f)
                    val height = viewport.height.toFloat().coerceAtLeast(1f)
                    audio.consume(engine.step(fixedSeconds, width, height, settings, draggingId), width)
                }
                // Engine state is mutable by design; this tick explicitly invalidates Compose Canvas.
                frameTick++
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize().background(Color(0xFF050814))) {
            Box(modifier = Modifier.fillMaxSize()) {
                SpaceViewport(
                    engine = engine,
                    frameTick = frameTick,
                    modifier = Modifier.fillMaxSize(),
                    onSize = { viewport = it },
                    onSelect = { id ->
                        selected = id
                        engine.selectedId = id
                        engine.applySelectedSettings(settings)
                    },
                    onDragStart = { id -> draggingId = id },
                    onDrag = { x, y ->
                        engine.bodies.firstOrNull { it.id == draggingId }?.apply {
                            this.x = x
                            this.y = y
                            vx = 0f
                            vy = 0f
                        }
                    },
                    onDragEnd = { draggingId = null },
                )
                Column(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = "${engine.bodies.size} ELEMENTS  ·  ${engine.collisionCount} BOUNCES",
                            color = Color(0xFFA6AEC8),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    if (showSettings) {
                        PhysicsPanel(settings, selected) { engine.applySelectedSettings(settings) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceViewport(
    engine: PhysicsEngine,
    frameTick: Int,
    modifier: Modifier,
    onSize: (IntSize) -> Unit,
    onSelect: (Int) -> Unit,
    onDragStart: (Int) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    Canvas(
        modifier = modifier
            .background(Color(0xFF050814))
            .onSizeChanged(onSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val nearest = engine.bodies.minByOrNull { hypot(it.x - offset.x, it.y - offset.y) }
                        if (nearest != null && hypot(nearest.x - offset.x, nearest.y - offset.y) < 44f) {
                            onSelect(nearest.id)
                            onDragStart(nearest.id)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDrag(change.position.x, change.position.y)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            }
            .pointerInput("gravity-wells") {
                detectTapGestures(onTap = { offset ->
                    engine.wells += GravityWell(offset.x, offset.y, expiresAtNanos = Long.MAX_VALUE)
                })
            },
    ) {
        drawSpaceBackdrop(frameTick)

        val center = Offset(size.width / 2f, size.height / 2f)
        val orbitScale = min(size.width, size.height)
        listOf(.18f, .31f, .44f).forEach { factor ->
            drawCircle(
                color = Color(0x224D5C91),
                radius = orbitScale * factor,
                center = center,
                style = Stroke(width = 1f),
            )
        }

        engine.wells.forEach { well ->
            val wellCenter = Offset(well.x, well.y)
            drawCircle(Color(0x22FBBF24), radius = well.radius, center = wellCenter, style = Stroke(1f))
            drawCircle(Color(0x99FBBF24), radius = 11f, center = wellCenter)
            drawCircle(Color(0xFFFFE7A3), radius = 4f, center = wellCenter)
        }

        drawCircle(Color(0x332A1A65), radius = 106f, center = center)
        drawCircle(Color(0x558B5CF6), radius = 72f, center = center, style = Stroke(width = 2f))
        drawCircle(Color(0xFF8B5CF6), radius = 56f, center = center)
        drawCircle(Color(0xFFB9A4FF), radius = 13f, center = center)

        val maxVisible = when {
            size.minDimension < 600f -> 150
            engine.bodies.size > 500 -> 360
            else -> engine.bodies.size
        }
        engine.bodies.take(maxVisible).forEach { body ->
            val point = Offset(body.x, body.y)
            val selected = body.id == engine.selectedId
            if (selected) {
                drawCircle(Color(0x559FE7FF), radius = body.radius + 12f, center = point, style = Stroke(2f))
                drawCircle(Color(0x229FE7FF), radius = body.radius + 20f, center = point)
            }
            drawCircle(Color(body.color), radius = if (selected) body.radius + 1.5f else body.radius, center = point)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpaceBackdrop(frameTick: Int) {
    drawRect(Color(0xFF050814))
    drawCircle(Color(0xFF101D48), radius = size.minDimension * .62f, center = center)
    repeat(110) { index ->
        val x = ((index * 97 + 31) % 1000) / 1000f * size.width
        val y = ((index * 173 + 47) % 1000) / 1000f * size.height
        val pulse = .18f + (((frameTick / 8 + index * 13) % 100) / 100f) * .35f
        drawCircle(Color.White.copy(alpha = pulse), radius = if (index % 11 == 0) 1.7f else .9f, center = Offset(x, y))
    }
}

@Composable
private fun PhysicsPanel(settings: PhysicsSettings, selected: Int?, onChange: () -> Unit) {
    Column(
        modifier = Modifier.width(300.dp).padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (selected == null) "TOUCH AN ORBITING ELEMENT" else "OBJECT #${selected + 1}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
        )
        NativeSlider("MASS", settings.selectedMass, .3f..2.5f) { settings.selectedMass = it; onChange() }
        NativeSlider("SIZE", settings.selectedRadius, 1.5f..9f) { settings.selectedRadius = it; onChange() }
        NativeSlider("ELASTICITY", settings.selectedElasticity, .1f..1f) { settings.selectedElasticity = it; onChange() }
    }
}

@Composable
private fun NativeSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label  ${"%.2f".format(value)}", color = Color(0xFFA6AEC8), style = MaterialTheme.typography.labelSmall)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
