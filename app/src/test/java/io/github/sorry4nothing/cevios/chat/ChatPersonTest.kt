package io.github.sorry4nothing.cevios.chat

import io.github.sorry4nothing.cevios.contacts.ContactList
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatPersonTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun theChatShowsTheCeviNameAndWithoutItTheFullName() {
        assertEquals("Fuchs", ChatPerson("1", "Anna Muster", ceviName = "Fuchs").displayName)
        assertEquals("Anna Muster", ChatPerson("1", "Anna Muster").displayName)
        assertEquals("Anna Muster", ChatPerson("1", "Anna Muster", ceviName = "").displayName)
        assertEquals("Anna Muster", ChatPerson("1", "Anna Muster", ceviName = "   ").displayName)
    }

    @Test
    fun theCeviNameIsReadFromTheApi() {
        val person = json.decodeFromString<ChatPerson>("""{"id":"1","name":"Anna Muster","ceviName":"Fuchs"}""")
        assertEquals("Fuchs", person.displayName)
        assertEquals("Anna Muster", json.decodeFromString<ChatPerson>("""{"id":"1","name":"Anna Muster","ceviName":null}""").displayName)
    }

    @Test
    fun theProfileReadsTheCeviName() {
        assertEquals("Fuchs", json.decodeFromString<Profile>("""{"id":"1","name":"Anna Muster","ceviName":"Fuchs"}""").ceviName)
        assertNull(json.decodeFromString<Profile>("""{"id":"1","name":"Anna Muster","ceviName":null}""").ceviName)
    }

    @Test
    fun contactsShowBothNames() {
        val list = json.decodeFromString<ContactList>(
            """{"kontakte":[{"id":"1","name":"Anna Muster","ceviName":"Fuchs"},{"id":"2","name":"Ben Beispiel","ceviName":null}]}""",
        )
        val (withCevi, without) = list.contacts
        assertEquals("Fuchs", withCevi.title)
        assertEquals("Anna Muster", withCevi.fullName)
        assertEquals("Ben Beispiel", without.title)
        assertNull(without.fullName)
    }
}
