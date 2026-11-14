package com.time1043.feedah_kt.drills

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

//
// The registry is the core's only contact with concrete drills: manifest
// order, id lookup with the swipe fallback, and duplicate refusal.
//
class RegistryTest {
    @Test
    fun exposesTheManifestOrder() {
        assertEquals(listOf("swipe", "quiz", "type"), DrillRegistry.getDrills().map { it.id })
    }

    @Test
    fun looksDrillsUpByIdAndFallsBackToSwipe() {
        assertEquals("quiz", DrillRegistry.getDrill("quiz").id)
        // An unknown id (a legacy stored value) is always safe: swipe
        // records nothing.
        assertEquals("swipe", DrillRegistry.getDrill("nope").id)
    }

    @Test
    fun refusesASecondDrillUnderTheSameId() {
        val impostor = object : Drill by DrillRegistry.getDrill("swipe") {
            override val id: DrillId = "quiz"
            override val label: String = "Impostor"
        }
        try {
            DrillRegistry.registerDrill(impostor)
            fail("a second drill under the same id must be refused")
        } catch (expected: IllegalArgumentException) {
            assertEquals(
                "A drill with id \"quiz\" is already registered",
                expected.message,
            )
        }
    }
}
