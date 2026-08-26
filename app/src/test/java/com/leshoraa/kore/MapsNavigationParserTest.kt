package com.leshoraa.kore

import com.leshoraa.kore.data.parser.MapsNavigationParser
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MapsNavigationParserTest {

    private lateinit var parser: MapsNavigationParser

    @Before
    fun setUp() {
        parser = MapsNavigationParser()
    }

    @Test
    fun parseIndonesianHeadingStraightWithEtaAndDuration() {
        val event = parser.parseRaw(
            title = "Ke arah utara",
            text = "ke arah Karanggeneng · 2,9 km · 14.49",
            subText = "9 mnt"
        )

        assertTrue(event.isActive)
        assertEquals("straight", event.icon)
        assertEquals("2,9 km", event.distance)
        assertEquals("Ke arah utara", event.instruction)
        assertEquals("14.49", event.eta)
        assertEquals("9 mnt", event.duration)
        assertTrue(event.street.contains("Karanggeneng"))
    }

    @Test
    fun etaDoesNotTriggerArriveFalsePositive() {
        val event = parser.parseRaw(
            title = "Lanjut lurus di Jl. Magelang",
            text = "Waktu tiba 14.49 · 3 km",
            subText = ""
        )

        assertTrue(event.isActive)
        assertNotEquals("arrive", event.icon)
        assertEquals("straight", event.icon)
        assertEquals("3 km", event.distance)
        assertEquals("14.49", event.eta)
    }

    @Test
    fun parseIndonesianTurnRightWithDistanceStreetEtaAndDuration() {
        val event = parser.parseRaw(
            title = "Belok kanan ke Jl. Jend. Sudirman",
            text = "200 m · 14.30",
            subText = "15 mnt"
        )

        assertTrue(event.isActive)
        assertEquals("turn_right", event.icon)
        assertEquals("200 m", event.distance)
        assertEquals("Belok kanan ke Jl. Jend.", event.instruction)
        assertEquals("14.30", event.eta)
        assertEquals("15 mnt", event.duration)
        assertTrue(event.street.contains("Jend. Sudirman"))
    }

    @Test
    fun parseIndonesianTurnLeftWithKilometerDistance() {
        val event = parser.parseRaw(
            title = "Belok kiri di Jl. Gatot Subroto",
            text = "1,5 km · 12 mnt",
            subText = ""
        )

        assertTrue(event.isActive)
        assertEquals("turn_left", event.icon)
        assertEquals("1,5 km", event.distance)
        assertEquals("Belok kiri di Jl. Gatot", event.instruction)
        assertEquals("12 mnt", event.duration)
        assertTrue(event.street.contains("Gatot Subroto"))
    }


    @Test
    fun parseDestinationArrivedEndsNavigation() {
        val event = parser.parseRaw(
            title = "Anda telah tiba di tujuan",
            text = "Rute selesai",
            subText = ""
        )

        assertEquals("arrive", event.icon)
        assertEquals("Anda telah tiba di tujua", event.instruction)
        assertFalse(event.isActive)
    }
}

