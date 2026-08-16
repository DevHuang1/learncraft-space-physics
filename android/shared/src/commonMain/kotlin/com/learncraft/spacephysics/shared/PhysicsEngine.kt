package com.learncraft.spacephysics.shared

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class Body(val id: Int, var x: Float, var y: Float, var vx: Float, var vy: Float, var mass: Float, var radius: Float, var elasticity: Float, val color: Long, var wellCueAtNanos: Long = 0L)
data class GravityWell(val x: Float, val y: Float, val mass: Float = .56f, val radius: Float = 220f)
data class PhysicsSettings(var selectedMass: Float = 1f, var selectedRadius: Float = 3f, var selectedElasticity: Float = .82f)
sealed interface PhysicsEvent { data class Collision(val x: Float, val y: Float, val impactSpeed: Float) : PhysicsEvent; data class WellCapture(val x: Float, val y: Float, val intensity: Float) : PhysicsEvent }

class PhysicsEngine(private val cellSize: Float = 56f) {
    val bodies = mutableListOf<Body>(); val wells = mutableListOf<GravityWell>(); var selectedId: Int? = null; var collisionCount: Long = 0; private set
    private val grid = HashMap<Long, MutableList<Int>>()
    private fun key(x: Int, y: Int) = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)
    private fun keyFor(x: Float, y: Float) = key((x / cellSize).toInt(), (y / cellSize).toInt())
    private data class Pull(val distance: Float, val falloff: Float)
    fun applySelectedSettings(settings: PhysicsSettings) { bodies.firstOrNull { it.id == selectedId }?.apply { mass = settings.selectedMass; radius = settings.selectedRadius; elasticity = settings.selectedElasticity } }
    fun step(dt: Float, width: Float, height: Float, settings: PhysicsSettings, draggingId: Int? = null): List<PhysicsEvent> {
        val events = ArrayList<PhysicsEvent>(); val now = 0L
        bodies.forEach { b -> if (b.id == draggingId) return@forEach; pull(b, width / 2f, height / 2f, 1.25f, max(width, height), .34f); wells.forEach { w -> pull(b, w.x, w.y, w.mass, w.radius, 8f)?.let { p -> if (p.distance < w.radius * .28f && now >= b.wellCueAtNanos) { events += PhysicsEvent.WellCapture(b.x, b.y, p.falloff); b.wellCueAtNanos = now + 700 } } }; b.vx = (b.vx * .93f).coerceIn(-5f, 5f); b.vy = (b.vy * .93f).coerceIn(-5f, 5f); b.x += b.vx * dt * 30f; b.y += b.vy * dt * 30f; if (b.x !in 0f..width) { b.vx *= b.elasticity; b.x = b.x.coerceIn(0f, width) }; if (b.y !in 0f..height) { b.vy *= b.elasticity; b.y = b.y.coerceIn(0f, height) } }
        grid.clear(); bodies.forEachIndexed { i, b -> grid.getOrPut(keyFor(b.x, b.y)) { mutableListOf() }.add(i) }
        bodies.forEachIndexed { i, a -> val cx = (a.x / cellSize).toInt(); val cy = (a.y / cellSize).toInt(); for (ox in -1..1) for (oy in -1..1) grid[key(cx + ox, cy + oy)]?.forEach { j -> if (j > i) collide(a, bodies[j], events) } }
        return events
    }
    private fun pull(b: Body, x: Float, y: Float, mass: Float, radius: Float, scale: Float): Pull? { val dx = x - b.x; val dy = y - b.y; val d = hypot(dx, dy).coerceAtLeast(1f); if (d > radius) return null; val f = 1f - d / radius; val force = min(10f, mass * f * f * scale / (d / 80f + 1f)); b.vx += dx / d * force; b.vy += dy / d * force; return Pull(d, f) }
    private fun collide(a: Body, b: Body, events: MutableList<PhysicsEvent>) { val dx = b.x - a.x; val dy = b.y - a.y; val d = hypot(dx, dy).coerceAtLeast(.001f); val minD = a.radius + b.radius + 1f; if (d >= minD) return; val nx = dx / d; val ny = dy / d; val overlap = minD - d; a.x -= nx * overlap * .5f; a.y -= ny * overlap * .5f; b.x += nx * overlap * .5f; b.y += ny * overlap * .5f; val rel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny; if (rel > 0f) return; val e = ((a.elasticity + b.elasticity) * .5f).coerceIn(0f, 1f); val impulse = -(1f + e) * rel / (1f / a.mass + 1f / b.mass); a.vx -= impulse * nx / a.mass; a.vy -= impulse * ny / a.mass; b.vx += impulse * nx / b.mass; b.vy += impulse * ny / b.mass; collisionCount++; events += PhysicsEvent.Collision((a.x + b.x) * .5f, (a.y + b.y) * .5f, -rel) }
}

class FixedStepRunner(private val stepSeconds: Float = 1f / 60f, private val maxSubSteps: Int = 4) { private var accumulator = 0f; fun advance(frameSeconds: Float, update: (Float) -> Unit) { accumulator = min(accumulator + frameSeconds.coerceIn(0f, .25f), stepSeconds * maxSubSteps); var n = 0; while (accumulator >= stepSeconds && n++ < maxSubSteps) { update(stepSeconds); accumulator -= stepSeconds } } }
