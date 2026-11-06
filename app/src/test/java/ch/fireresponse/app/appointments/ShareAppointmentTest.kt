package ch.fireresponse.app.appointments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ShareAppointmentTest {
    @Test
    fun theTextHasTitleTimeSquadAndCallUp() {
        val text = appointmentShareText("Schläuche kuppeln", "Samstag, 14. November, 14:00–17:00", "JF Beispielstadt", "Treffpunkt 13:45 Uhr", "Aufgebot")
        assertEquals("Schläuche kuppeln\nSamstag, 14. November, 14:00–17:00\nJF Beispielstadt\nAufgebot: Treffpunkt 13:45 Uhr", text)
    }

    @Test
    fun missingOrBlankPartsAreLeftOut() {
        assertEquals("Lager\nJF Beispielstadt", appointmentShareText("Lager", null, "JF Beispielstadt", "  ", "Aufgebot"))
        assertFalse(appointmentShareText("Lager", null, "JF Beispielstadt", null, "Aufgebot").contains("Aufgebot"))
    }
}
