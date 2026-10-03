package com.radityodwiki.maptrack.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraFollowTest {

    @Test
    fun firstFixZoomsIn() {
        assertEquals(CameraAction.ZOOM_TO_FIX, cameraActionFor(following = true, hasCenteredOnce = false))
    }

    @Test
    fun laterFixesFollow() {
        assertEquals(CameraAction.FOLLOW, cameraActionFor(following = true, hasCenteredOnce = true))
    }

    @Test
    fun userGestureStopsFollowing() {
        assertEquals(CameraAction.NONE, cameraActionFor(following = false, hasCenteredOnce = true))
        assertEquals(CameraAction.NONE, cameraActionFor(following = false, hasCenteredOnce = false))
    }
}
