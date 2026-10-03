package com.radityodwiki.maptrack.ui.map

/** Map display settings (PRD §22). Tiles are display-only; GPS stays the coordinate source. */
object MapConfig {
    /** OpenFreeMap: no account or API key; attribution comes from the tile source. */
    const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

    /** Camera before the first fix: Indonesia. */
    const val DEFAULT_LATITUDE = -2.5
    const val DEFAULT_LONGITUDE = 118.0
    const val DEFAULT_ZOOM = 3.5

    /** Street-level zoom when centering on the user. */
    const val FOLLOW_ZOOM = 16.0
}
