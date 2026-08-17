package com.learncraft.spacephysics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.learncraft.spacephysics.shared.Body
import com.learncraft.spacephysics.shared.GravityWell
import com.learncraft.spacephysics.shared.PhysicsEngine
import com.learncraft.spacephysics.shared.PhysicsEvent
import com.learncraft.spacephysics.shared.PhysicsSettings
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PhysicsExperienceInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

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
                composeRule.onNodeWithText("1 WELLS", substring = true).assertExists()
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
}
