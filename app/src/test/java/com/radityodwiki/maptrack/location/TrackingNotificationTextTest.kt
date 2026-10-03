package com.radityodwiki.maptrack.location

import com.radityodwiki.maptrack.domain.model.GpsFix
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingNotificationTextTest {

    private val minutes18 = 18 * 60_000L

    @Test
    fun freshFixShowsSpeedAndDuration() {
        val fix = GpsFix(0.0, 0.0, 5f, 12.5f, null, null, time = minutes18 - 1_000)
        assertEquals("Kecepatan: 45 km/h · Durasi: 18 menit", trackingNotificationText(fix, 0, minutes18, true))
    }

    @Test
    fun staleOrMissingFixShowsWaiting() {
        val stale = GpsFix(0.0, 0.0, 5f, 12.5f, null, null, time = minutes18 - 16_000)
        assertEquals("Menunggu sinyal GPS… · Durasi: 18 menit", trackingNotificationText(stale, 0, minutes18, true))
        assertEquals("Menunggu sinyal GPS… · Durasi: 18 menit", trackingNotificationText(null, 0, minutes18, true))
    }

    @Test
    fun locationDisabledWins() {
        assertEquals("Location service tidak aktif · Durasi: 18 menit", trackingNotificationText(null, 0, minutes18, false))
    }
}
