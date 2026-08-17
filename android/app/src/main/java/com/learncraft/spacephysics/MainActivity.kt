package com.learncraft.spacephysics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val SpaceNavy = Color(0xFF050814)
private val OrbitViolet = Color(0xFF8B5CF6)
private val OrbitMint = Color(0xFF34D399)
private val OrbitBlue = Color(0xFF60A5FA)
private val OrbitGold = Color(0xFFFBBF24)
private val MutedSpaceText = Color(0xFFA6AEC8)

private enum class NativePage { HOME, EXPERIMENTS, SAVED, SETTINGS, SIMULATOR }

private data class ExperimentSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val bodyCount: Int,
    val initialWells: Int,
    val pairwiseAttraction: Float,
    val tint: Color,
)

private data class SimulationLaunch(
    val experiment: ExperimentSpec,
    val snapshot: SavedSimulation? = null,
)

private val experimentCatalog = listOf(
    ExperimentSpec("free-orbit", "FREE ORBIT", "220 bodies · balanced field", 220, 0, .08f, OrbitViolet),
    ExperimentSpec("collision-lab", "COLLISION LAB", "120 bodies · elastic focus", 120, 0, .02f, OrbitMint),
    ExperimentSpec("gravity-garden", "GRAVITY GARDEN", "320 bodies · seeded wells", 320, 3, .10f, OrbitGold),
    ExperimentSpec("dense-field", "DENSE FIELD", "500 bodies · adaptive detail", 500, 1, .05f, OrbitBlue),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val simulationEnabled = intent?.getBooleanExtra(EXTRA_SIMULATION_ENABLED, true) ?: true
        setContent { SpacePhysicsApp(simulationEnabled) }
    }

    companion object {
        const val EXTRA_SIMULATION_ENABLED = "com.learncraft.spacephysics.SIMULATION_ENABLED"
    }
}

@Composable
private fun SpacePhysicsApp(simulationEnabled: Boolean) {
    val context = LocalContext.current
    val store = remember { NativeSimulationStore(context) }
    var preferences by remember { mutableStateOf(store.loadSettings()) }
    var savedSimulations by remember { mutableStateOf(store.loadSimulations()) }
    var page by remember { mutableStateOf(if (simulationEnabled) NativePage.HOME else NativePage.SIMULATOR) }
    var launch by remember {
        mutableStateOf(SimulationLaunch(experimentCatalog.first()))
    }

    fun updatePreferences(next: NativeAppSettings) {
        preferences = next
        store.saveSettings(next)
    }

    fun startExperiment(experiment: ExperimentSpec) {
        launch = SimulationLaunch(experiment)
        page = NativePage.SIMULATOR
    }

    fun startSavedSimulation(snapshot: SavedSimulation) {
        val fallback = experimentCatalog.firstOrNull { it.id == snapshot.experimentLabel } ?: experimentCatalog.first()
        launch = SimulationLaunch(fallback, snapshot)
        page = NativePage.SIMULATOR
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = SpaceNavy) {
            Crossfade(targetState = page, label = "native-orbital-page") { destination ->
                when (destination) {
                    NativePage.HOME -> OrbitalHomePage(
                        onNavigate = { target -> page = target },
                        onStart = { startExperiment(experimentCatalog.first()) },
                        savedCount = savedSimulations.size,
                    )
                    NativePage.EXPERIMENTS -> ExperimentsPage(
                        onBack = { page = NativePage.HOME },
                        onStart = ::startExperiment,
                    )
                    NativePage.SAVED -> SavedSimulationsPage(
                        snapshots = savedSimulations,
                        onBack = { page = NativePage.HOME },
                        onLoad = ::startSavedSimulation,
                        onDelete = { id ->
                            store.deleteSimulation(id)
                            savedSimulations = store.loadSimulations()
                        },
                    )
                    NativePage.SETTINGS -> SettingsPage(
                        settings = preferences,
                        onBack = { page = NativePage.HOME },
                        onChange = ::updatePreferences,
                    )
                    NativePage.SIMULATOR -> key(launch.snapshot?.id ?: launch.experiment.id) {
                        SimulationPage(
                            launch = launch,
                            preferences = preferences,
                            simulationEnabled = simulationEnabled,
                            onBack = { page = NativePage.HOME },
                            onSave = { snapshot ->
                                store.saveSimulation(snapshot)
                                savedSimulations = store.loadSimulations()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbitalHomePage(
    onNavigate: (NativePage) -> Unit,
    onStart: () -> Unit,
    savedCount: Int,
) {
    Column(modifier = Modifier.fillMaxSize().background(SpaceNavy).padding(20.dp)) {
        Text("LEARNCRAFT SPACE PHYSICS", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("ORBITAL MODE  /  FREE ROAM", color = MutedSpaceText, style = MaterialTheme.typography.labelSmall)
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            OrbitalBackdrop(Modifier.fillMaxSize())
            OrbitalNode(
                label = "EXPERIMENTS",
                caption = "Physics labs",
                tint = OrbitMint,
                modifier = Modifier.align(Alignment.TopEnd).offset((-10).dp, 64.dp),
                onClick = { onNavigate(NativePage.EXPERIMENTS) },
            )
            OrbitalNode(
                label = "SAVED",
                caption = "$savedCount local states",
                tint = OrbitBlue,
                modifier = Modifier.align(Alignment.BottomEnd).offset((-10).dp, (-78).dp),
                onClick = { onNavigate(NativePage.SAVED) },
            )
            OrbitalNode(
                label = "SETTINGS",
                caption = "Sound & motion",
                tint = OrbitGold,
                modifier = Modifier.align(Alignment.BottomStart).offset(10.dp, (-78).dp),
                onClick = { onNavigate(NativePage.SETTINGS) },
            )
            OrbitalNode(
                label = "HOME",
                caption = "Mission control",
                tint = OrbitViolet,
                modifier = Modifier.align(Alignment.TopStart).offset(10.dp, 64.dp),
                onClick = { },
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(150.dp).semantics { contentDescription = "Open Physics Core" }.clickable(onClick = onStart),
                    shape = CircleShape,
                    color = OrbitViolet.copy(alpha = .84f),
                    shadowElevation = 12.dp,
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PHYSICS", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("CORE", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("TAP TO LAUNCH", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Tap an orbiting world to navigate", color = MutedSpaceText, style = MaterialTheme.typography.labelSmall)
            }
        }
        Text("SAVED LOCALLY  ·  HIGH-DENSITY KMP PHYSICS", color = MutedSpaceText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun OrbitalNode(label: String, caption: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.width(112.dp).semantics { contentDescription = "Open ${label.lowercase().replaceFirstChar { it.uppercase() }}" }.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(modifier = Modifier.size(72.dp).border(1.dp, tint.copy(alpha = .8f), CircleShape), shape = CircleShape, color = tint.copy(alpha = .17f)) {
            Box(contentAlignment = Alignment.Center) {
                Text(label.take(2), color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(caption, color = MutedSpaceText, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun OrbitalBackdrop(modifier: Modifier) {
    Canvas(modifier = modifier) {
        drawRect(SpaceNavy)
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color(0xFF101D48), radius = size.minDimension * .58f, center = center)
        listOf(.23f, .37f, .49f).forEach { ratio ->
            drawCircle(Color(0x334D5C91), radius = size.minDimension * ratio, center = center, style = Stroke(1.4f))
        }
        repeat(84) { index ->
            val x = ((index * 97 + 31) % 1000) / 1000f * size.width
            val y = ((index * 173 + 47) % 1000) / 1000f * size.height
            drawCircle(Color.White.copy(alpha = if (index % 9 == 0) .6f else .28f), radius = if (index % 9 == 0) 1.6f else .8f, center = Offset(x, y))
        }
    }
}

@Composable
private fun ExperimentsPage(onBack: () -> Unit, onStart: (ExperimentSpec) -> Unit) {
    NativePageScaffold(title = "EXPERIMENT LAB", subtitle = "Choose a world to launch", onBack = onBack) {
        experimentCatalog.forEach { experiment ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics { contentDescription = "Launch ${experiment.title}" }.clickable { onStart(experiment) },
                shape = RoundedCornerShape(24.dp),
                color = experiment.tint.copy(alpha = .13f),
                border = androidx.compose.foundation.BorderStroke(1.dp, experiment.tint.copy(alpha = .7f)),
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(52.dp), shape = CircleShape, color = experiment.tint.copy(alpha = .26f)) {
                        Box(contentAlignment = Alignment.Center) { Text(experiment.bodyCount.toString(), color = experiment.tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(experiment.title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(experiment.subtitle, color = MutedSpaceText, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("LAUNCH", color = experiment.tint, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SavedSimulationsPage(
    snapshots: List<SavedSimulation>,
    onBack: () -> Unit,
    onLoad: (SavedSimulation) -> Unit,
    onDelete: (String) -> Unit,
) {
    NativePageScaffold(title = "SAVED SIGNALS", subtitle = "Local simulation snapshots", onBack = onBack) {
        if (snapshots.isEmpty()) {
            Surface(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), shape = RoundedCornerShape(24.dp), color = OrbitBlue.copy(alpha = .12f)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NO SAVED SIMULATIONS", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Launch an experiment, tune the field, then use SAVE in the simulator.", color = MutedSpaceText, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            snapshots.forEach { snapshot ->
                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp), shape = RoundedCornerShape(20.dp), color = OrbitBlue.copy(alpha = .10f), border = androidx.compose.foundation.BorderStroke(1.dp, OrbitBlue.copy(alpha = .46f))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).semantics { contentDescription = "Restore ${snapshot.title}" }.clickable { onLoad(snapshot) }) {
                            Text(snapshot.title, color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${snapshot.bodies.size} elements · ${snapshot.wells.size} wells · ${snapshot.experimentLabel}", color = MutedSpaceText, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { onDelete(snapshot.id) }) { Text("DELETE", color = Color(0xFFFCA5A5)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPage(settings: NativeAppSettings, onBack: () -> Unit, onChange: (NativeAppSettings) -> Unit) {
    NativePageScaffold(title = "COMMAND SETTINGS", subtitle = "Saved locally on this device", onBack = onBack) {
        SettingsToggle("SPATIAL AUDIO", "Collision and gravity-well feedback", settings.soundEnabled) { onChange(settings.copy(soundEnabled = it)) }
        NativeSlider("AUDIO VOLUME", settings.soundVolume, 0f..0.18f) { onChange(settings.copy(soundVolume = it)) }
        SettingsToggle("REDUCED MOTION", "Freeze the physics field while retaining touch controls", settings.reducedMotion) { onChange(settings.copy(reducedMotion = it)) }
        NativeSlider("NEARBY ATTRACTION", settings.defaultPairwiseAttraction, 0f..0.16f) { onChange(settings.copy(defaultPairwiseAttraction = it)) }
        NativeSlider("VELOCITY LIMIT", settings.defaultMaxVelocity, 2f..12f) { onChange(settings.copy(defaultMaxVelocity = it)) }
        Spacer(Modifier.height(18.dp))
        Text("Settings apply to future launches and are stored only on this Android device.", color = MutedSpaceText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsToggle(title: String, caption: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(caption, color = MutedSpaceText, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NativePageScaffold(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(SpaceNavy).padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("ORBIT", color = OrbitViolet) }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MutedSpaceText, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) { content() }
    }
}

@Composable
private fun SimulationPage(
    launch: SimulationLaunch,
    preferences: NativeAppSettings,
    simulationEnabled: Boolean,
    onBack: () -> Unit,
    onSave: (SavedSimulation) -> Unit,
) {
    val context = LocalContext.current
    val engine = remember { PhysicsEngine() }
    val audio = remember { SpatialAudioController(context) }
    val settings = remember { PhysicsSettings() }
    var selected by remember { mutableStateOf<Int?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var draggingId by remember { mutableStateOf<Int?>(null) }
    var frameTick by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(true) }

    DisposableEffect(audio) { onDispose { audio.close() } }

    LaunchedEffect(launch) {
        engine.reset()
        if (launch.snapshot == null) {
            seedBodies(engine, launch.experiment.bodyCount)
            repeat(launch.experiment.initialWells) { index ->
                engine.wells += GravityWell(240f + index * 190f, 240f + (index % 2) * 220f, mass = .78f, radius = 190f)
            }
            settings.pairwiseAttraction = launch.experiment.pairwiseAttraction
        } else {
            launch.snapshot.bodies.forEach { body ->
                engine.bodies += Body(body.id, body.x, body.y, body.vx, body.vy, body.mass, body.radius, body.elasticity, body.color)
            }
            launch.snapshot.wells.forEach { well ->
                engine.wells += GravityWell(well.x, well.y, well.mass, well.radius)
            }
            settings.selectedMass = launch.snapshot.selectedMass
            settings.selectedRadius = launch.snapshot.selectedRadius
            settings.selectedElasticity = launch.snapshot.selectedElasticity
            settings.pairwiseAttraction = launch.snapshot.pairwiseAttraction
            settings.maxVelocity = launch.snapshot.maxVelocity
        }
        audio.loadSounds(R.raw.collision, R.raw.gravity_well)
    }

    LaunchedEffect(preferences) {
        audio.setEnabled(preferences.soundEnabled)
        audio.setVolume(preferences.soundVolume)
        if (launch.snapshot == null) {
            settings.pairwiseAttraction = preferences.defaultPairwiseAttraction.coerceAtLeast(launch.experiment.pairwiseAttraction)
            settings.maxVelocity = preferences.defaultMaxVelocity
        }
    }

    LaunchedEffect(simulationEnabled, preferences.reducedMotion) {
        if (!simulationEnabled || preferences.reducedMotion) return@LaunchedEffect
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
                frameTick++
            }
        }
    }

    fun saveCurrentSimulation() {
        val snapshot = SavedSimulation(
            title = "${launch.experiment.title.lowercase().replaceFirstChar { it.uppercase() }} orbit",
            experimentLabel = launch.experiment.id,
            selectedMass = settings.selectedMass,
            selectedRadius = settings.selectedRadius,
            selectedElasticity = settings.selectedElasticity,
            pairwiseAttraction = settings.pairwiseAttraction,
            maxVelocity = settings.maxVelocity,
            bodies = engine.bodies.map { body -> SimulationBodySnapshot(body.id, body.x, body.y, body.vx, body.vy, body.mass, body.radius, body.elasticity, body.color) },
            wells = engine.wells.map { well -> SimulationWellSnapshot(well.x, well.y, well.mass, well.radius) },
        )
        onSave(snapshot)
    }

    Box(modifier = Modifier.fillMaxSize().background(SpaceNavy)) {
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
            onGravityWellAdded = { frameTick++ },
        )
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("ORBIT", color = Color.White) }
            Text(launch.snapshot?.title ?: launch.experiment.title, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            TextButton(onClick = ::saveCurrentSimulation) { Text("SAVE", color = OrbitMint) }
        }
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${engine.bodies.size} ELEMENTS  ·  ${engine.collisionCount} BOUNCES  ·  ${engine.wells.size} WELLS",
                color = MutedSpaceText,
                style = MaterialTheme.typography.labelSmall,
            )
            if (showSettings) PhysicsPanel(settings, selected) { engine.applySelectedSettings(settings) }
        }
    }
}

private fun seedBodies(engine: PhysicsEngine, count: Int) {
    val colors = listOf(0xFFA78BFA, 0xFF34D399, 0xFF60A5FA, 0xFFFBBF24, 0xFFF472B6)
    repeat(count) { index ->
        val angle = (index * 0.618f) % 6.28f
        val radius = 90f + (index % 18) * 20f
        engine.bodies += Body(
            id = index,
            x = 640f + cos(angle) * radius,
            y = 420f + sin(angle) * radius,
            vx = -sin(angle) * .35f,
            vy = cos(angle) * .35f,
            mass = 0.7f + (index % 5) * .15f,
            radius = 2f + (index % 4),
            elasticity = .82f,
            color = colors[index % colors.size],
        )
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
    onGravityWellAdded: () -> Unit,
) {
    Canvas(
        modifier = modifier
            .background(SpaceNavy)
            .onSizeChanged(onSize)
            .semantics { contentDescription = "Physics viewport" }
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
                    onGravityWellAdded()
                })
            },
    ) {
        drawSpaceBackdrop(frameTick)
        val center = Offset(size.width / 2f, size.height / 2f)
        val orbitScale = min(size.width, size.height)
        listOf(.18f, .31f, .44f).forEach { factor ->
            drawCircle(Color(0x224D5C91), radius = orbitScale * factor, center = center, style = Stroke(width = 1f))
        }
        engine.wells.forEach { well ->
            val wellCenter = Offset(well.x, well.y)
            drawCircle(Color(0x22FBBF24), radius = well.radius, center = wellCenter, style = Stroke(1f))
            drawCircle(Color(0x99FBBF24), radius = 11f, center = wellCenter)
            drawCircle(Color(0xFFFFE7A3), radius = 4f, center = wellCenter)
        }
        drawCircle(Color(0x332A1A65), radius = 106f, center = center)
        drawCircle(Color(0x558B5CF6), radius = 72f, center = center, style = Stroke(width = 2f))
        drawCircle(OrbitViolet, radius = 56f, center = center)
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
    drawRect(SpaceNavy)
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
    Column(modifier = Modifier.width(300.dp).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BUILD ${BuildConfig.VERSION_NAME}  ·  CODE ${BuildConfig.VERSION_CODE}", color = Color(0xFFB9A4FF), style = MaterialTheme.typography.labelSmall)
        Text(if (selected == null) "TOUCH AN ORBITING ELEMENT" else "OBJECT #${selected + 1}", color = Color.White, style = MaterialTheme.typography.labelMedium)
        NativeSlider("MASS", settings.selectedMass, .3f..2.5f) { settings.selectedMass = it; onChange() }
        NativeSlider("SIZE", settings.selectedRadius, 1.5f..9f) { settings.selectedRadius = it; onChange() }
        NativeSlider("ELASTICITY", settings.selectedElasticity, .1f..1f) { settings.selectedElasticity = it; onChange() }
    }
}

@Composable
private fun NativeSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label  ${"%.2f".format(value)}", color = MutedSpaceText, style = MaterialTheme.typography.labelSmall)
        Slider(value = value, onValueChange = onValueChange, valueRange = range)
    }
}
