package com.learncraft.spacephysics

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SimulationBodySnapshot(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val mass: Float,
    val radius: Float,
    val elasticity: Float,
    val color: Long,
)

data class SimulationWellSnapshot(
    val x: Float,
    val y: Float,
    val mass: Float,
    val radius: Float,
)

data class SavedSimulation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val experimentLabel: String,
    val savedAtMillis: Long = System.currentTimeMillis(),
    val selectedMass: Float,
    val selectedRadius: Float,
    val selectedElasticity: Float,
    val pairwiseAttraction: Float,
    val maxVelocity: Float,
    val bodies: List<SimulationBodySnapshot>,
    val wells: List<SimulationWellSnapshot>,
)

data class NativeAppSettings(
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.07f,
    val reducedMotion: Boolean = false,
    val defaultPairwiseAttraction: Float = 0.08f,
    val defaultMaxVelocity: Float = 5f,
)

/** Local-only persistence for user-created simulation snapshots and display/audio preferences. */
class NativeSimulationStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): NativeAppSettings = NativeAppSettings(
        soundEnabled = preferences.getBoolean(KEY_SOUND_ENABLED, true),
        soundVolume = preferences.getFloat(KEY_SOUND_VOLUME, 0.07f),
        reducedMotion = preferences.getBoolean(KEY_REDUCED_MOTION, false),
        defaultPairwiseAttraction = preferences.getFloat(KEY_PAIRWISE, 0.08f),
        defaultMaxVelocity = preferences.getFloat(KEY_MAX_VELOCITY, 5f),
    )

    fun saveSettings(settings: NativeAppSettings) {
        preferences.edit()
            .putBoolean(KEY_SOUND_ENABLED, settings.soundEnabled)
            .putFloat(KEY_SOUND_VOLUME, settings.soundVolume)
            .putBoolean(KEY_REDUCED_MOTION, settings.reducedMotion)
            .putFloat(KEY_PAIRWISE, settings.defaultPairwiseAttraction)
            .putFloat(KEY_MAX_VELOCITY, settings.defaultMaxVelocity)
            .apply()
    }

    fun loadSimulations(): List<SavedSimulation> = runCatching {
        val array = JSONArray(preferences.getString(KEY_SIMULATIONS, "[]"))
        buildList {
            for (index in 0 until array.length()) add(decodeSimulation(array.getJSONObject(index)))
        }.sortedByDescending { it.savedAtMillis }
    }.getOrDefault(emptyList())

    fun saveSimulation(snapshot: SavedSimulation) {
        val all = (listOf(snapshot) + loadSimulations().filter { it.id != snapshot.id }).take(MAX_SAVED_SIMULATIONS)
        val array = JSONArray()
        all.forEach { array.put(encodeSimulation(it)) }
        preferences.edit().putString(KEY_SIMULATIONS, array.toString()).apply()
    }

    fun deleteSimulation(id: String) {
        val array = JSONArray()
        loadSimulations().filterNot { it.id == id }.forEach { array.put(encodeSimulation(it)) }
        preferences.edit().putString(KEY_SIMULATIONS, array.toString()).apply()
    }

    private fun encodeSimulation(value: SavedSimulation): JSONObject = JSONObject().apply {
        put("id", value.id)
        put("title", value.title)
        put("experimentLabel", value.experimentLabel)
        put("savedAtMillis", value.savedAtMillis)
        put("selectedMass", value.selectedMass.toDouble())
        put("selectedRadius", value.selectedRadius.toDouble())
        put("selectedElasticity", value.selectedElasticity.toDouble())
        put("pairwiseAttraction", value.pairwiseAttraction.toDouble())
        put("maxVelocity", value.maxVelocity.toDouble())
        put("bodies", JSONArray().apply {
            value.bodies.forEach { body ->
                put(JSONObject().apply {
                    put("id", body.id)
                    put("x", body.x.toDouble())
                    put("y", body.y.toDouble())
                    put("vx", body.vx.toDouble())
                    put("vy", body.vy.toDouble())
                    put("mass", body.mass.toDouble())
                    put("radius", body.radius.toDouble())
                    put("elasticity", body.elasticity.toDouble())
                    put("color", body.color)
                })
            }
        })
        put("wells", JSONArray().apply {
            value.wells.forEach { well ->
                put(JSONObject().apply {
                    put("x", well.x.toDouble())
                    put("y", well.y.toDouble())
                    put("mass", well.mass.toDouble())
                    put("radius", well.radius.toDouble())
                })
            }
        })
    }

    private fun decodeSimulation(root: JSONObject): SavedSimulation {
        fun bodyAt(index: Int): SimulationBodySnapshot {
            val body = root.getJSONArray("bodies").getJSONObject(index)
            return SimulationBodySnapshot(
                id = body.getInt("id"),
                x = body.getDouble("x").toFloat(),
                y = body.getDouble("y").toFloat(),
                vx = body.getDouble("vx").toFloat(),
                vy = body.getDouble("vy").toFloat(),
                mass = body.getDouble("mass").toFloat(),
                radius = body.getDouble("radius").toFloat(),
                elasticity = body.getDouble("elasticity").toFloat(),
                color = body.getLong("color"),
            )
        }
        fun wellAt(index: Int): SimulationWellSnapshot {
            val well = root.getJSONArray("wells").getJSONObject(index)
            return SimulationWellSnapshot(
                x = well.getDouble("x").toFloat(),
                y = well.getDouble("y").toFloat(),
                mass = well.getDouble("mass").toFloat(),
                radius = well.getDouble("radius").toFloat(),
            )
        }

        val bodies = root.getJSONArray("bodies")
        val wells = root.getJSONArray("wells")
        return SavedSimulation(
            id = root.getString("id"),
            title = root.getString("title"),
            experimentLabel = root.getString("experimentLabel"),
            savedAtMillis = root.getLong("savedAtMillis"),
            selectedMass = root.getDouble("selectedMass").toFloat(),
            selectedRadius = root.getDouble("selectedRadius").toFloat(),
            selectedElasticity = root.getDouble("selectedElasticity").toFloat(),
            pairwiseAttraction = root.getDouble("pairwiseAttraction").toFloat(),
            maxVelocity = root.getDouble("maxVelocity").toFloat(),
            bodies = List(bodies.length(), ::bodyAt),
            wells = List(wells.length(), ::wellAt),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "learncraft_space_physics"
        const val KEY_SIMULATIONS = "saved_simulations_v1"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        const val KEY_SOUND_VOLUME = "sound_volume"
        const val KEY_REDUCED_MOTION = "reduced_motion"
        const val KEY_PAIRWISE = "default_pairwise"
        const val KEY_MAX_VELOCITY = "default_max_velocity"
        const val MAX_SAVED_SIMULATIONS = 12
    }
}
