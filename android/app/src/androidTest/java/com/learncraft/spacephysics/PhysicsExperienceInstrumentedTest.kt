package com.learncraft.spacephysics

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.learncraft.spacephysics.shared.Body
import com.learncraft.spacephysics.shared.GravityWell
import com.learncraft.spacephysics.shared.PhysicsEngine
import com.learncraft.spacephysics.shared.PhysicsEvent
import com.learncraft.spacephysics.shared.PhysicsSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PhysicsExperienceInstrumentedTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun launchStaticExperience() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SIMULATION_ENABLED, false)
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun closeExperience() {
        scenario.close()
    }

    @Test
    fun controlPanelShowsInstalledBuildAndObjectPhysicsControls() {
        composeRule.onNodeWithText(
            "BUILD ${BuildConfig.VERSION_NAME}  ·  CODE ${BuildConfig.VERSION_CODE}",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("MASS", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("SIZE", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("ELASTICITY", substring = true).assertIsDisplayed()
    }

    @Test
    fun tappingViewportCreatesAGravityWell() {
        composeRule.onNodeWithContentDescription("Physics viewport")
            .performTouchInput { click(Offset(180f, 220f)) }

        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching {
                composeRule.onNodeWithText("1 WELLS", substring = true).assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun engineProducesWellPullAndElasticCollisionEvents() {
        val settings = PhysicsSettings(pairwiseAttraction = 0f, maxVelocity = 18f)

        val wellEngine = PhysicsEngine()
        val pulled = Body(1, 150f, 150f, 0f, 0f, 1f, 4f, .9f, 0xFFA78BFA)
        wellEngine.bodies += pulled
        wellEngine.wells += GravityWell(180f, 150f, mass = 1f, radius = 160f)
        val wellEvents = wellEngine.step(1f / 60f, 400f, 400f, settings)
        assertTrue("Well should pull body rightward", pulled.vx > 0f)
        assertTrue("Well should emit a capture event near its center", wellEvents.any { it is PhysicsEvent.WellCapture })

        val collisionEngine = PhysicsEngine()
        collisionEngine.bodies += Body(1, 180f, 200f, 2f, 0f, 1f, 8f, 1f, 0xFFA78BFA)
        collisionEngine.bodies += Body(2, 192f, 200f, -2f, 0f, 1f, 8f, 1f, 0xFF34D399)
        val collisionEvents = collisionEngine.step(1f / 60f, 400f, 400f, settings)
        assertTrue("Overlapping approaching bodies should collide", collisionEngine.collisionCount > 0)
        assertTrue("Audible collision events should be emitted", collisionEvents.any { it is PhysicsEvent.Collision })
    }

    @Test
    fun orbitalHomeOpensTheNativeExperimentAndSettingsPages() {
        scenario.close()
        scenario = ActivityScenario.launch(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java))

        composeRule.onNodeWithContentDescription("Open Experiments").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching {
                composeRule.onNodeWithText("EXPERIMENT LAB").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("BINARY STAR").assertIsDisplayed()
        composeRule.onNodeWithText("THREE-BODY").assertIsDisplayed()
        composeRule.onNodeWithText("ORBIT").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching {
                composeRule.onNodeWithContentDescription("Open Settings").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithContentDescription("Open Settings").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            runCatching {
                composeRule.onNodeWithText("COMMAND SETTINGS").assertIsDisplayed()
            }.isSuccess
        }
    }

    @Test
    fun versionedShareCodeRoundTripsSimulationState() {
        val store = NativeSimulationStore(ApplicationProvider.getApplicationContext())
        val original = SavedSimulation(
            title = "Twin-star test",
            experimentLabel = "binary-star",
            selectedMass = 1.2f,
            selectedRadius = 4f,
            selectedElasticity = .9f,
            pairwiseAttraction = .06f,
            maxVelocity = 7f,
            bodies = listOf(SimulationBodySnapshot(7, 20f, 30f, 1f, -1f, 1.2f, 4f, .9f, 0xFFFBBF24)),
            wells = listOf(SimulationWellSnapshot(90f, 120f, 1.1f, 220f)),
        )

        val code = store.exportShareCode(original)
        val restored = store.importShareCode(code)

        assertTrue("Share code should carry the versioned prefix", code.startsWith("LCSP1:"))
        assertNotNull("Valid codes should import", restored)
        assertEquals("Imported snapshot should preserve body count", 1, restored!!.bodies.size)
        assertEquals("Imported snapshot should preserve the experiment", "binary-star", restored.experimentLabel)
        assertTrue("Imported snapshot receives a new local identifier", restored.id != original.id)
    }
}
