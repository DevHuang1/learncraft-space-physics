package com.learncraft.spacephysics.shared

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Mutable simulation state shared by Android, JVM benchmark, and browser adapters. */
data class Body(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var mass: Float,
    var radius: Float,
    var elasticity: Float,
    val color: Long,
    var wellCueAtNanos: Long = 0L,
    var collisionCueAtNanos: Long = 0L,
)

data class GravityWell(
    val x: Float,
    val y: Float,
    val mass: Float = .56f,
    val radius: Float = 220f,
    val expiresAtNanos: Long = Long.MAX_VALUE,
)

data class PhysicsSettings(
    var selectedMass: Float = 1f,
    var selectedRadius: Float = 3f,
    var selectedElasticity: Float = .82f,
    var pairwiseAttraction: Float = .08f,
    var maxVelocity: Float = 5f,
    var soundCooldownMillis: Long = 90L,
)

sealed interface PhysicsEvent {
    data class Collision(val x: Float, val y: Float, val impactSpeed: Float) : PhysicsEvent
    data class WellCapture(val x: Float, val y: Float, val intensity: Float) : PhysicsEvent
}

/**
 * Fixed-step, spatial-hash physics engine.
 *
 * The broad phase is rebuilt once for local attraction and once after integration for
 * collision response. Pairwise attraction is restricted to nearby cells, so the normal
 * update remains O(n) for a sparse field rather than O(n²).
 */
class PhysicsEngine(
    private val cellSize: Float = 96f,
    private val maxCollisionEventsPerStep: Int = 64,
) {
    val bodies = mutableListOf<Body>()
    val wells = mutableListOf<GravityWell>()
    var selectedId: Int? = null
    var collisionCount: Long = 0
        private set
    var broadPhaseCandidateCount: Long = 0
        private set

    private val grid = HashMap<Long, MutableList<Int>>()
    private var simulationNanos = 0L

    private fun key(x: Int, y: Int): Long = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)

    private fun cellCoordinate(value: Float): Int = kotlin.math.floor(value / cellSize).toInt()

    private fun keyFor(x: Float, y: Float): Long = key(cellCoordinate(x), cellCoordinate(y))

    private data class Pull(val distance: Float, val falloff: Float)

    fun reset() {
        bodies.clear()
        wells.clear()
        selectedId = null
        collisionCount = 0L
        broadPhaseCandidateCount = 0L
        simulationNanos = 0L
        grid.clear()
    }

    fun applySelectedSettings(settings: PhysicsSettings) {
        bodies.firstOrNull { it.id == selectedId }?.apply {
            mass = settings.selectedMass.coerceAtLeast(.05f)
            radius = settings.selectedRadius.coerceIn(.75f, 32f)
            elasticity = settings.selectedElasticity.coerceIn(0f, 1f)
        }
    }

    fun step(
        dt: Float,
        width: Float,
        height: Float,
        settings: PhysicsSettings,
        draggingId: Int? = null,
    ): List<PhysicsEvent> {
        val safeDt = dt.coerceIn(1f / 240f, 1f / 20f)
        simulationNanos += (safeDt * 1_000_000_000f).toLong()
        wells.removeAll { it.expiresAtNanos <= simulationNanos }

        val events = ArrayList<PhysicsEvent>(min(maxCollisionEventsPerStep, bodies.size))
        buildSpatialHash()

        bodies.forEach { body ->
            if (body.id == draggingId) return@forEach

            pull(body, width / 2f, height / 2f, 1.25f, max(width, height), .34f)
            wells.forEach { well ->
                pull(body, well.x, well.y, well.mass, well.radius, 8f)?.let { influence ->
                    if (
                        influence.distance < well.radius * .28f &&
                        simulationNanos >= body.wellCueAtNanos
                    ) {
                        events += PhysicsEvent.WellCapture(body.x, body.y, influence.falloff)
                        body.wellCueAtNanos = simulationNanos + 700_000_000L
                    }
                }
            }

            applyNearbyAttraction(body, settings.pairwiseAttraction)
            val damping = (1f - 3.2f * safeDt).coerceIn(.82f, .99f)
            val velocityLimit = settings.maxVelocity.coerceIn(1f, 18f)
            body.vx = (body.vx * damping).coerceIn(-velocityLimit, velocityLimit)
            body.vy = (body.vy * damping).coerceIn(-velocityLimit, velocityLimit)
            body.x += body.vx * safeDt * 30f
            body.y += body.vy * safeDt * 30f
            bounceFromBounds(body, width, height)
        }

        buildSpatialHash()
        bodies.forEachIndexed { index, body ->
            val cx = cellCoordinate(body.x)
            val cy = cellCoordinate(body.y)
            for (offsetX in -1..1) {
                for (offsetY in -1..1) {
                    grid[key(cx + offsetX, cy + offsetY)]?.forEach { otherIndex ->
                        if (otherIndex > index) {
                            broadPhaseCandidateCount++
                            collide(body, bodies[otherIndex], events)
                        }
                    }
                }
            }
        }
        return events
    }

    private fun buildSpatialHash() {
        grid.clear()
        bodies.forEachIndexed { index, body ->
            grid.getOrPut(keyFor(body.x, body.y)) { ArrayList(4) }.add(index)
        }
    }

    private fun applyNearbyAttraction(body: Body, factor: Float) {
        if (factor <= 0f) return
        val cx = cellCoordinate(body.x)
        val cy = cellCoordinate(body.y)
        for (offsetX in -1..1) {
            for (offsetY in -1..1) {
                grid[key(cx + offsetX, cy + offsetY)]?.forEach { index ->
                    val other = bodies[index]
                    if (other.id == body.id) return@forEach
                    val dx = other.x - body.x
                    val dy = other.y - body.y
                    val distanceSq = dx * dx + dy * dy + 36f * 36f
                    val distance = hypot(dx, dy).coerceAtLeast(1f)
                    val force = min(1.4f, factor * body.mass * other.mass / distanceSq * 900f)
                    body.vx += dx / distance * force
                    body.vy += dy / distance * force
                }
            }
        }
    }

    private fun pull(
        body: Body,
        x: Float,
        y: Float,
        mass: Float,
        radius: Float,
        scale: Float,
    ): Pull? {
        val dx = x - body.x
        val dy = y - body.y
        val distance = hypot(dx, dy).coerceAtLeast(1f)
        if (distance > radius) return null
        val falloff = 1f - distance / radius
        val force = min(10f, mass * falloff * falloff * scale / (distance / 80f + 1f))
        body.vx += dx / distance * force
        body.vy += dy / distance * force
        return Pull(distance, falloff)
    }

    private fun bounceFromBounds(body: Body, width: Float, height: Float) {
        if (body.x < body.radius || body.x > width - body.radius) {
            body.vx *= -body.elasticity.coerceIn(0f, 1f)
            body.x = body.x.coerceIn(body.radius, max(body.radius, width - body.radius))
        }
        if (body.y < body.radius || body.y > height - body.radius) {
            body.vy *= -body.elasticity.coerceIn(0f, 1f)
            body.y = body.y.coerceIn(body.radius, max(body.radius, height - body.radius))
        }
    }

    private fun collide(a: Body, b: Body, events: MutableList<PhysicsEvent>) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val distance = hypot(dx, dy)
        val safeDistance = distance.coerceAtLeast(.001f)
        val minDistance = a.radius + b.radius + 1f
        if (distance >= minDistance) return

        val nx = dx / safeDistance
        val ny = dy / safeDistance
        val overlap = minDistance - safeDistance
        val inverseMassA = 1f / a.mass.coerceAtLeast(.05f)
        val inverseMassB = 1f / b.mass.coerceAtLeast(.05f)
        val inverseMassTotal = inverseMassA + inverseMassB
        a.x -= nx * overlap * inverseMassA / inverseMassTotal
        a.y -= ny * overlap * inverseMassA / inverseMassTotal
        b.x += nx * overlap * inverseMassB / inverseMassTotal
        b.y += ny * overlap * inverseMassB / inverseMassTotal

        val relativeVelocity = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
        if (relativeVelocity >= 0f) return

        val elasticity = ((a.elasticity + b.elasticity) * .5f).coerceIn(0f, 1f)
        val impulse = -(1f + elasticity) * relativeVelocity / inverseMassTotal
        a.vx -= impulse * nx * inverseMassA
        a.vy -= impulse * ny * inverseMassA
        b.vx += impulse * nx * inverseMassB
        b.vy += impulse * ny * inverseMassB
        collisionCount++

        if (
            events.size < maxCollisionEventsPerStep &&
            -relativeVelocity > .08f &&
            simulationNanos >= a.collisionCueAtNanos &&
            simulationNanos >= b.collisionCueAtNanos
        ) {
            events += PhysicsEvent.Collision(
                (a.x + b.x) * .5f,
                (a.y + b.y) * .5f,
                -relativeVelocity,
            )
            val nextCue = simulationNanos + 90_000_000L
            a.collisionCueAtNanos = nextCue
            b.collisionCueAtNanos = nextCue
        }
    }
}

/** Accumulates render-frame time and caps catch-up work to avoid a spiral of death. */
class FixedStepRunner(
    private val stepSeconds: Float = 1f / 60f,
    private val maxSubSteps: Int = 4,
) {
    private var accumulator = 0f

    fun reset() {
        accumulator = 0f
    }

    fun advance(frameSeconds: Float, update: (Float) -> Unit) {
        accumulator = min(
            accumulator + frameSeconds.coerceIn(0f, .25f),
            stepSeconds * maxSubSteps,
        )
        var steps = 0
        while (accumulator >= stepSeconds && steps < maxSubSteps) {
            update(stepSeconds)
            accumulator -= stepSeconds
            steps++
        }
    }
}
