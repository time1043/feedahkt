package com.time1043.feedah_kt.db

import kotlinx.serialization.json.Json

/** The db layer's shared serializer. Seed.kt keeps its own instance with
 * `ignoreUnknownKeys` — the bundled files omit optional columns. */
internal val json = Json
