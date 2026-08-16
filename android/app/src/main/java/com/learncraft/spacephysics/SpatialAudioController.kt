package com.learncraft.spacephysics

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.math.max
import kotlin.math.min

/**
 * Android-only audio adapter. The shared physics engine emits events; this adapter maps
 * them to pooled sounds and stereo pan without coupling the engine to Android APIs.
 */
class SpatialAudioController(
    context: Context,
    private val maxSimultaneousStreams: Int = 8,
) : AutoCloseable {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(maxSimultaneousStreams)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val appContext = context.applicationContext
    private var collisionSoundId: Int? = null
    private var wellSoundId: Int? = null
    private var volume = 0.07f
    private var collisionGapNanos = 90_000_000L
    private var lastCollisionNanos = 0L

    /** Call after adding collision.wav and gravity_well.wav to res/raw. */
    fun loadSounds(collisionResId: Int, wellResId: Int) {
        collisionSoundId = soundPool.load(appContext, collisionResId, 1)
        wellSoundId = soundPool.load(appContext, wellResId, 1)
    }

    fun setVolume(value: Float) { volume = value.coerceIn(0f, 0.18f) }
    fun setCollisionGapMillis(value: Long) { collisionGapNanos = value.coerceIn(30L, 500L) * 1_000_000L }

    fun consume(events: List<PhysicsEvent>, viewportWidth: Float) {
        val now = System.nanoTime()
        events.forEach { event ->
            when (event) {
                is PhysicsEvent.Collision -> {
                    if (now - lastCollisionNanos < collisionGapNanos) return@forEach
                    lastCollisionNanos = now
                    play(collisionSoundId, event.x, event.impactSpeed, viewportWidth)
                }
                is PhysicsEvent.WellCapture -> play(wellSoundId, event.x, event.intensity, viewportWidth)
            }
        }
    }

    private fun play(soundId: Int?, x: Float, intensity: Float, viewportWidth: Float) {
        val id = soundId ?: return
        val pan = ((x / max(1f, viewportWidth)) * 2f - 1f).coerceIn(-1f, 1f)
        val gain = min(1f, volume * (0.8f + intensity.coerceIn(0f, 3f) * 0.15f))
        val left = gain * if (pan > 0f) 1f - pan else 1f
        val right = gain * if (pan < 0f) 1f + pan else 1f
        soundPool.play(id, left, right, 1, 0, 0.9f + intensity.coerceIn(0f, 2f) * 0.08f)
    }

    override fun close() { soundPool.release() }
}
