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
        engine.bodies += Body(
            id = index,
            x = 640f + cos(angle.toDouble()).toFloat() * radius,
            y = 420f + sin(angle.toDouble()).toFloat() * radius,
            vx = -sin(angle.toDouble()).toFloat() * .3f,
            vy = cos(angle.toDouble()).toFloat() * .3f,
            mass = 1f,
            radius = 3f,
            elasticity = .82f,
            color = 0xFFA78BFA,
        )
    }
}

private fun percentile(values: List<Double>, percentile: Double): Double {
    val sorted = values.sorted()
    val index = ((sorted.lastIndex) * percentile).toInt().coerceIn(0, sorted.lastIndex)
    return sorted[index]
}

fun main() {
    val settings = PhysicsSettings()
    println("count,steps,median_step_ms,p95_step_ms,candidate_pairs,collisions")
    listOf(100, 250, 500, 1_000, 2_000, 5_000).forEach { count ->
        val engine = PhysicsEngine()
        populate(engine, count)
        repeat(120) { engine.step(1f / 60f, 1280f, 840f, settings) }

        val timings = ArrayList<Double>(600)
        val steps = 600
        repeat(steps) {
            val elapsed = measureNanoTime {
                engine.step(1f / 60f, 1280f, 840f, settings)
            }
            timings += elapsed / 1_000_000.0
        }
        println(
            "$count,$steps,%.4f,%.4f,%d,%d".format(
                percentile(timings, .50),
                percentile(timings, .95),
                engine.broadPhaseCandidateCount,
                engine.collisionCount,
            ),
        )
    }
}
