package com.learncraft.spacephysics.benchmark

import com.learncraft.spacephysics.shared.Body
import com.learncraft.spacephysics.shared.PhysicsEngine
import com.learncraft.spacephysics.shared.PhysicsSettings
import kotlin.math.cos
import kotlin.math.sin
import kotlin.system.measureNanoTime

private fun populate(engine: PhysicsEngine, count: Int) {
    repeat(count) { index ->
        val angle = index * 0.6180339f
        val radius = 120f + (index % 40) * 8f
        engine.bodies += Body(index, 640f + cos(angle.toDouble()).toFloat() * radius, 420f + sin(angle.toDouble()).toFloat() * radius, 0f, 0f, 1f, 3f, .82f, 0xFFA78BFA)
    }
}

fun main() {
    val settings = PhysicsSettings()
    println("count,steps,total_ms,avg_step_ms,collision_count")
    listOf(1_000, 2_000, 5_000).forEach { count ->
        val engine = PhysicsEngine()
        populate(engine, count)
        repeat(30) { engine.step(1f / 60f, 1280f, 840f, settings) }
        val steps = 300
        val totalNanos = measureNanoTime { repeat(steps) { engine.step(1f / 60f, 1280f, 840f, settings) } }
        val totalMs = totalNanos / 1_000_000.0
        println("$count,$steps,%.2f,%.4f,%d".format(totalMs, totalMs / steps, engine.collisionCount))
    }
}
