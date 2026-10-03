package com.radityodwiki.maptrack.ui.map

enum class CameraAction { NONE, ZOOM_TO_FIX, FOLLOW }

/**
 * What the camera does on a new fix: zoom in the first time, then follow the user until they
 * move the map themselves.
 */
fun cameraActionFor(following: Boolean, hasCenteredOnce: Boolean): CameraAction = when {
    !following -> CameraAction.NONE
    !hasCenteredOnce -> CameraAction.ZOOM_TO_FIX
    else -> CameraAction.FOLLOW
}
