package ch.fireresponse.app.images

import ch.fireresponse.app.appointments.SquadList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SquadLogosTest {
    @Test
    fun onlySquadsWithALogoGetAPath() {
        val json = """
            {"stufen":[
              {"id":"a","name":"Stufe A","kuerzel":"A","logoUrl":"/api/v1/stufen/a/logo?v=1"},
              {"id":"b","name":"Stufe B","kuerzel":"B","logoUrl":null},
              {"id":"c","name":"Stufe C","kuerzel":"C"}
            ]}
        """.trimIndent()
        val list = Json { ignoreUnknownKeys = true }.decodeFromString<SquadList>(json)
        assertEquals(mapOf("a" to "/api/v1/stufen/a/logo?v=1"), squadLogoPaths(list))
    }
}
