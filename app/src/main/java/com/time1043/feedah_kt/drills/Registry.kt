package com.time1043.feedah_kt.drills

/**
 * The drill registry: the core app's only point of contact with concrete
 * drills. Screens, settings and the session layer read from here — they
 * must never import a drill module directly.
 *
 * Adding a drill: implement [Drill] in a self-contained object in this
 * package (see Drill.kt for the contract), then add it to the manifest
 * below. Nothing else in the core changes.
 */
object DrillRegistry {
    /** The core app's manifest, in chip order — a new drill joins here. */
    private val MANIFEST: List<Drill> = listOf(SwipeDrill, QuizDrill, TypeDrill)

    private val drills: MutableList<Drill> = mutableListOf()

    init {
        for (drill in MANIFEST) registerDrill(drill)
    }

    /** Registers a drill; refuses a second drill under the same id. */
    fun registerDrill(drill: Drill) {
        if (drills.any { known -> known.id == drill.id }) {
            throw IllegalArgumentException("A drill with id \"${drill.id}\" is already registered")
        }
        drills += drill
    }

    /** Every registered drill, in manifest order — the settings chips' order. */
    fun getDrills(): List<Drill> = drills.toList()

    /** Looks a drill up by id; unknown ids (e.g. legacy stored values) fall
     * back to swipe, which records nothing and so is always safe. */
    fun getDrill(id: String): Drill = drills.find { it.id == id } ?: SwipeDrill
}
