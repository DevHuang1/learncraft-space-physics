package com.learncraft.spacephysics.shared

import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.math.hypot

/** Browser adapter: commonMain owns simulation; this class owns Canvas and pointer APIs. */
class CanvasPhysicsAdapter(private val canvas: HTMLCanvasElement) {
    private val context = canvas.getContext("2d") as org.w3c.dom.CanvasRenderingContext2D
    private val engine = PhysicsEngine()
    private val settings = PhysicsSettings()
    private val runner = FixedStepRunner()
    private var draggingId: Int? = null
    private var lastFrame = 0.0

    fun start() {
        canvas.addEventListener("pointerdown", ::onPointerDown)
        canvas.addEventListener("pointermove", ::onPointerMove)
        canvas.addEventListener("pointerup", ::onPointerUp)
        window.requestAnimationFrame(::frame)
    }

    fun stop() {
        canvas.removeEventListener("pointerdown", ::onPointerDown)
        canvas.removeEventListener("pointermove", ::onPointerMove)
        canvas.removeEventListener("pointerup", ::onPointerUp)
    }

    fun seed(count: Int, width: Float, height: Float) {
        repeat(count) { i ->
            val angle = i * .6180339f
            val radius = 90f + (i % 20) * 20f
            engine.bodies += Body(i, width / 2f + kotlin.math.cos(angle.toDouble()).toFloat() * radius, height / 2f + kotlin.math.sin(angle.toDouble()).toFloat() * radius, 0f, 0f, 1f, 3f, .82f, 0xFFA78BFA)
        }
    }

    private fun frame(now: Double) {
        if (lastFrame == 0.0) lastFrame = now
        val frameSeconds = ((now - lastFrame) / 1000.0).toFloat().coerceIn(0f, .05f)
        lastFrame = now
        runner.advance(frameSeconds) { engine.step(it, canvas.width.toFloat(), canvas.height.toFloat(), settings, draggingId) }
        render()
        window.requestAnimationFrame(::frame)
    }

    private fun render() {
        val w = canvas.width.toDouble(); val h = canvas.height.toDouble()
        context.fillStyle = "#070B16"; context.fillRect(0.0, 0.0, w, h)
        context.fillStyle = "#8B5CF6"; context.beginPath(); context.arc(w / 2, h / 2, 65.0, 0.0, 6.283); context.fill()
        engine.wells.forEach { well -> context.strokeStyle = "#FBBF24"; context.beginPath(); context.arc(well.x.toDouble(), well.y.toDouble(), well.radius.toDouble(), 0.0, 6.283); context.stroke() }
        engine.bodies.forEach { body -> context.fillStyle = "#A78BFA"; context.beginPath(); context.arc(body.x.toDouble(), body.y.toDouble(), body.radius.toDouble(), 0.0, 6.283); context.fill() }
    }

    private fun point(event: MouseEvent): Pair<Float, Float> {
        val rect = canvas.getBoundingClientRect()
        return ((event.clientX - rect.left).toFloat()) to ((event.clientY - rect.top).toFloat())
    }

    private fun onPointerDown(event: Event) {
        val mouse = event as MouseEvent; val (x, y) = point(mouse)
        val nearest = engine.bodies.minByOrNull { hypot(it.x - x, it.y - y) }
        if (nearest != null && hypot(nearest.x - x, nearest.y - y) < 40f) { draggingId = nearest.id; engine.selectedId = nearest.id } else engine.wells += GravityWell(x, y)
    }

    private fun onPointerMove(event: Event) {
        val id = draggingId ?: return; val mouse = event as MouseEvent; val (x, y) = point(mouse)
        engine.bodies.firstOrNull { it.id == id }?.apply { this.x = x; this.y = y; vx = 0f; vy = 0f }
    }

    private fun onPointerUp(@Suppress("UNUSED_PARAMETER") event: Event) { draggingId = null }
}
