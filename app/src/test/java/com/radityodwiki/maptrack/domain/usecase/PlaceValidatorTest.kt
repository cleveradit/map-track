package com.radityodwiki.maptrack.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceValidatorTest {

    @Test
    fun blankName_isEmpty() {
        assertEquals(PlaceNameError.EMPTY, PlaceValidator.nameError(""))
        assertEquals(PlaceNameError.EMPTY, PlaceValidator.nameError("   "))
    }

    @Test
    fun nameLengthBoundary() {
        assertNull(PlaceValidator.nameError("R"))
        assertNull(PlaceValidator.nameError("a".repeat(50)))
        assertNull(PlaceValidator.nameError("  " + "a".repeat(50) + "  "))
        assertEquals(PlaceNameError.TOO_LONG, PlaceValidator.nameError("a".repeat(51)))
    }

    @Test
    fun emojiCountsAsOneCharacter() {
        assertNull(PlaceValidator.nameError("🏠".repeat(50)))
        assertEquals(PlaceNameError.TOO_LONG, PlaceValidator.nameError("🏠".repeat(51)))
    }

    @Test
    fun radiusBoundary() {
        assertFalse(PlaceValidator.isValidRadius(49.9))
        assertTrue(PlaceValidator.isValidRadius(50.0))
        assertTrue(PlaceValidator.isValidRadius(1_000.0))
        assertFalse(PlaceValidator.isValidRadius(1_000.1))
    }

    @Test
    fun coordinateRange() {
        assertTrue(PlaceValidator.isValidCoordinate(-90.0, 180.0))
        assertFalse(PlaceValidator.isValidCoordinate(90.1, 0.0))
        assertFalse(PlaceValidator.isValidCoordinate(0.0, -180.1))
    }
}
