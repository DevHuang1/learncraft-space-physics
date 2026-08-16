package com.learncraft.spacephysics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.hypot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpacePhysicsApp() }
    }
}

@androidx.compose.runtime.Composable
private fun SpacePhysicsApp() {
    val engine = remember { PhysicsEngine() }
    val settings = remember { PhysicsSettings() }
    var selected by remember { mutableStateOf<Int?>(null) }
    var viewport by remember { mutableStateOf(Offset.Zero) }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var showSettings by remember { mutableStateOf(true) }
    val colors = listOf(0xFFA78BFA, 0xFF34D399, 0xFF60A5FA, 0xFFFBBF24, 0xFFF472B6)

    LaunchedEffect(Unit) {
        repeat(220) { i ->
            val angle = (i * 0.618f) % 6.28f
            val radius = 90f + (i % 18) * 20f
            engine.bodies += Body(i, 640f + kotlin.math.cos(angle.toDouble()).toFloat() * radius, 420f + kotlin.math.sin(angle.toDouble()).toFloat() * radius, 0f, 0f, 0.7f + (i % 5) * .15f, 2f + (i % 4), .82f, colors[i % colors.size])
        }
        var previous = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (previous == 0L) previous = now
                val dt = ((now - previous) / 1_000_000_000f).coerceIn(0f, .05f)
                previous = now
                engine.step(dt, viewport.x.coerceAtLeast(1f), viewport.y.coerceAtLeast(1f), settings, draggingId)
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize().background(Color(0xFF070B16))) {
            Column(modifier = Modifier.fillMaxSize()) {
                SpaceViewport(
                    engine = engine,
                    modifier = Modifier.weight(1f),
                    onSize = { viewport = it },
                    onSelect = { id -> selected = id; engine.selectedId = id; engine.applySelectedSettings(settings) },
                    onDragStart = { id -> draggingId = id },
                    onDrag = { x, y -> engine.bodies.firstOrNull { it.id == draggingId }?.apply { this.x = x; this.y = y; vx = 0f; vy = 0f } },
                    onDragEnd = { draggingId = null },
                )
                if (showSettings) PhysicsPanel(settings, selected) { engine.applySelectedSettings(settings) }
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(8.dp)) {
                    Text("${engine.bodies.size} elements · ${engine.collisionCount} bounces", color = Color(0xFFA6AEC8))
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SpaceViewport(engine: PhysicsEngine, modifier: Modifier, onSize: (Offset) -> Unit, onSelect: (Int) -> Unit, onDragStart: (Int) -> Unit, onDrag: (Float, Float) -> Unit, onDragEnd: () -> Unit) {
    Canvas(modifier = modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset ->
                val nearest = engine.bodies.minByOrNull { hypot(it.x - offset.x, it.y - offset.y) }
                if (nearest != null && hypot(nearest.x - offset.x, nearest.y - offset.y) < 40f) { onSelect(nearest.id); onDragStart(nearest.id) }
            },
            onDrag = { change, _ -> change.consume(); onDrag(change.position.x, change.position.y) },
            onDragEnd = onDragEnd,
            onDragCancel = onDragEnd,
        )
    }) {
        onSize(Offset(size.width, size.height))
        drawRect(Color(0xFF070B16))
        drawCircle(Color(0xFF172456), radius = size.minDimension * .42f, center = center)
        drawCircle(Color(0xFF8B5CF6), radius = 65f, center = center)
        drawCircle(Color(0x228B5CF6), radius = 94f, center = center)
        engine.bodies.forEach { body -> drawCircle(Color(body.color), body.radius, Offset(body.x, body.y), alpha = if (body.id == engine.selectedId) 1f else .72f) }
    }
}

@androidx.compose.runtime.Composable
private fun PhysicsPanel(settings: PhysicsSettings, selected: Int?, onChange: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp).width(280.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (selected == null) "Select an orbiting element" else "Object #${selected + 1}", color = Color.White)
        NativeSlider("Mass", settings.selectedMass, .3f..2.5f) { settings.selectedMass = it; onChange() }
        NativeSlider("Size", settings.selectedRadius, 1.5f..9f) { settings.selectedRadius = it; onChange() }
        NativeSlider("Elasticity", settings.selectedElasticity, .1f..1f) { settings.selectedElasticity = it; onChange() }
    }
}

@androidx.compose.runtime.Composable
private fun NativeSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column { Text("$label  ${"%.2f".format(value)}", color = Color(0xFFA6AEC8)); Slider(value = value, onValueChange = onValueChange, valueRange = range) }
}
