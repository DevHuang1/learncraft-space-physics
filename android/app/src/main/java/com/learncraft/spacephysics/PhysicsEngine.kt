package com.learncraft.spacephysics

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Lightweight deterministic physics core kept independent from Compose rendering. */
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
)

data class GravityWell(val x: Float, val y: Float, val mass: Float = 0.56f, val radius: Float = 220f)

data class PhysicsSettings(
    var selectedMass: Float = 1f,
    var selectedRadius: Float = 3f,
    var selectedElasticity: Float = 0.82f,
)

class PhysicsEngine(private val cellSize: Float = 56f) {
    val bodies = mutableListOf<Body>()
    val wells = mutableListOf<GravityWell>()
    var collisionCount: Long = 0
        private set

    private val grid = HashMap<Long, MutableList<Int>>()

    fun step(dt: Float, width: Float, height: Float, settings: PhysicsSettings, draggingId: Int? = null) {
        val coreX = width / 2f
        val coreY = height / 2f
        bodies.forEach { body ->
            if (body.id == draggingId) return@forEach
            attract(body, coreX, coreY, 1.25f, max(width, height), 0.34f)
            wells.forEach { well -> attract(body, well.x, well.y, well.mass, well.radius, 8f) }
            body.vx = body.vx.coerceIn(-5f, 5f) * 0.93f
            body.vy = body.vy.coerceIn(-5f, 5f) * 0.93f
            body.x += body.vx * dt * 30f
            body.y += body.vy * dt * 30f
            if (body.x !in 0f..width) { body.vx *= -body.elasticity; body.x = body.x.coerceIn(0f, width) }
            if (body.y !in 0f..height) { body.vy *= -body.elasticity; body.y = body.y.coerceIn(0f, height) }
        }
        resolveCollisions()
    }

    fun applySelectedSettings(settings: PhysicsSettings) {
        bodies.firstOrNull { it.id == selectedId }?.apply {
            mass = settings.selectedMass
            radius = settings.selectedRadius
            elasticity = settings.selectedElasticity
        }
    }

    var selectedId: Int? = null

    private fun attract(body: Body, x: Float, y: Float, mass: Float, radius: Float, scale: Float) {
        val dx = x - body.x
        val dy = y - body.y
        val distance = hypot(dx, dy).coerceAtLeast(1f)
        if (distance > radius) return
        val falloff = 1f - distance / radius
        val force = min(10f, mass * falloff * falloff * scale / (distance / 80f + 1f))
        body.vx += dx / distance * force
        body.vy += dy / distance * force
    }

    private fun cellKey(x: Float, y: Float): Long {
        val cx = (x / cellSize).toInt()
        val cy = (y / cellSize).toInt()
        return (cx.toLong() shl 32) xor (cy.toLong() and 0xffffffffL)
    }

    private fun resolveCollisions() {
        grid.clear()
        bodies.forEachIndexed { index, body -> grid.getOrPut(cellKey(body.x, body.y)) { mutableListOf() }.add(index) }
        bodies.forEachIndexed { index, a ->
            val cx = (a.x / cellSize).toInt()
            val cy = (a.y / cellSize).toInt()
            for (ox in -1..1) for (oy in -1..1) {
                val key = ((cx + ox).toLong() shl 32) xor ((cy + oy).toLong() and 0xffffffffL)
                grid[key]?.forEach { otherIndex -> if (otherIndex > index) resolvePair(a, bodies[otherIndex]) }
            }
        }
    }

    private fun resolvePair(a: Body, b: Body) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val distance = hypot(dx, dy).coerceAtLeast(0.001f)
        val minDistance = a.radius + b.radius + 1f
        if (distance >= minDistance) return
        val nx = dx / distance
        val ny = dy / distance
        val overlap = minDistance - distance
        a.x -= nx * overlap * 0.5f
        a.y -= ny * overlap * 0.5f
        b.x += nx * overlap * 0.5f
        b.y += ny * overlap * 0.5f
        val relativeNormalVelocity = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
        if (relativeNormalVelocity > 0f) return
        val restitution = (a.elasticity + b.elasticity) * 0.5f
        val impulse = -(1f + restitution) * relativeNormalVelocity / (1f / a.mass + 1f / b.mass)
        a.vx -= impulse * nx / a.mass
        a.vy -= impulse * ny / a.mass
        b.vx += impulse * nx / b.mass
        b.vy += impulse * ny / b.mass
        collisionCount++
    }
}
