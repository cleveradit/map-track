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

    /**
     * Ambient tile cache size (PRD §38 Fase 4, v2.4). Tiles once shown stay available offline;
     * there are no region downloads because OpenFreeMap's terms forbid automated bulk fetching.
     */
    const val MAP_CACHE_MAX_BYTES = 200L * 1024 * 1024
}
