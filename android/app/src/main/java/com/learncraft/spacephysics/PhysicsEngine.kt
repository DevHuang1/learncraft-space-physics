package com.learncraft.spacephysics

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

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
)

data class GravityWell(val x: Float, val y: Float, val mass: Float = 0.56f, val radius: Float = 220f)
data class PhysicsSettings(var selectedMass: Float = 1f, var selectedRadius: Float = 3f, var selectedElasticity: Float = 0.82f)

sealed interface PhysicsEvent {
    data class Collision(val x: Float, val y: Float, val impactSpeed: Float) : PhysicsEvent
    data class WellCapture(val x: Float, val y: Float, val intensity: Float) : PhysicsEvent
}

class PhysicsEngine(private val cellSize: Float = 56f) {
    val bodies = mutableListOf<Body>()
    val wells = mutableListOf<GravityWell>()
    var collisionCount: Long = 0
        private set
    var selectedId: Int? = null
    private val grid = HashMap<Long, MutableList<Int>>()

    fun step(dt: Float, width: Float, height: Float, settings: PhysicsSettings, draggingId: Int? = null): List<PhysicsEvent> {
        val events = ArrayList<PhysicsEvent>()
        val now = System.nanoTime()
        val coreX = width / 2f
        val coreY = height / 2f
        bodies.forEach { body ->
            if (body.id == draggingId) return@forEach
            attract(body, coreX, coreY, 1.25f, max(width, height), 0.34f)
            wells.forEach { well ->
                val result = attract(body, well.x, well.y, well.mass, well.radius, 8f)
                if (result != null && result.distance < well.radius * 0.28f && now >= body.wellCueAtNanos) {
                    events += PhysicsEvent.WellCapture(body.x, body.y, result.falloff)
                    body.wellCueAtNanos = now + 700_000_000L
                }
            }
            body.vx = (body.vx * 0.93f).coerceIn(-5f, 5f)
            body.vy = (body.vy * 0.93f).coerceIn(-5f, 5f)
            body.x += body.vx * dt * 30f
            body.y += body.vy * dt * 30f
            if (body.x !in 0f..width) { body.vx *= -body.elasticity; body.x = body.x.coerceIn(0f, width) }
            if (body.y !in 0f..height) { body.vy *= -body.elasticity; body.y = body.y.coerceIn(0f, height) }
        }
        resolveCollisions(events)
        return events
    }

    fun applySelectedSettings(settings: PhysicsSettings) {
        bodies.firstOrNull { it.id == selectedId }?.apply {
            mass = settings.selectedMass
            radius = settings.selectedRadius
            elasticity = settings.selectedElasticity
        }
    }

    private data class Attraction(val distance: Float, val falloff: Float)

    private fun attract(body: Body, x: Float, y: Float, mass: Float, radius: Float, scale: Float): Attraction? {
        val dx = x - body.x
        val dy = y - body.y
        val distance = hypot(dx, dy).coerceAtLeast(1f)
        if (distance > radius) return null
        val falloff = 1f - distance / radius
        val force = min(10f, mass * falloff * falloff * scale / (distance / 80f + 1f))
        body.vx += dx / distance * force
        body.vy += dy / distance * force
        return Attraction(distance, falloff)
    }

    private fun key(cellX: Int, cellY: Int): Long = (cellX.toLong() shl 32) xor (cellY.toLong() and 0xffffffffL)
    private fun keyFor(x: Float, y: Float): Long = key((x / cellSize).toInt(), (y / cellSize).toInt())

    private fun resolveCollisions(events: MutableList<PhysicsEvent>) {
        grid.clear()
        bodies.forEachIndexed { index, body -> grid.getOrPut(keyFor(body.x, body.y)) { mutableListOf() }.add(index) }
        bodies.forEachIndexed { index, a ->
            val cellX = (a.x / cellSize).toInt()
            val cellY = (a.y / cellSize).toInt()
            for (offsetX in -1..1) for (offsetY in -1..1) {
                grid[key(cellX + offsetX, cellY + offsetY)]?.forEach { otherIndex ->
                    if (otherIndex > index) resolvePair(a, bodies[otherIndex], events)
                }
            }
        }
    }

    private fun resolvePair(a: Body, b: Body, events: MutableList<PhysicsEvent>) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val distance = hypot(dx, dy).coerceAtLeast(0.001f)
        val minimumDistance = a.radius + b.radius + 1f
        if (distance >= minimumDistance) return
        val nx = dx / distance
        val ny = dy / distance
        val overlap = minimumDistance - distance
        a.x -= nx * overlap * 0.5f
        a.y -= ny * overlap * 0.5f
        b.x += nx * overlap * 0.5f
        b.y += ny * overlap * 0.5f
        val relativeNormalVelocity = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
        if (relativeNormalVelocity > 0f) return
        val restitution = ((a.elasticity + b.elasticity) * 0.5f).coerceIn(0f, 1f)
        val impulse = -(1f + restitution) * relativeNormalVelocity / (1f / a.mass + 1f / b.mass)
        a.vx -= impulse * nx / a.mass
        a.vy -= impulse * ny / a.mass
        b.vx += impulse * nx / b.mass
        b.vy += impulse * ny / b.mass
        collisionCount++
        events += PhysicsEvent.Collision((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, -relativeNormalVelocity)
    }
}

class FixedStepRunner(private val stepSeconds: Float = 1f / 60f, private val maxSubSteps: Int = 4) {
    private var accumulator = 0f

    fun advance(frameSeconds: Float, update: (fixedSeconds: Float) -> Unit) {
        accumulator = min(accumulator + frameSeconds.coerceIn(0f, 0.25f), stepSeconds * maxSubSteps)
        var subSteps = 0
        while (accumulator >= stepSeconds && subSteps < maxSubSteps) {
            update(stepSeconds)
            accumulator -= stepSeconds
            subSteps++
        }
    }
}
