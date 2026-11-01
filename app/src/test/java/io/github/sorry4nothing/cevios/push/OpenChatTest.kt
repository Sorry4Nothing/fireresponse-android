package io.github.sorry4nothing.cevios.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenChatTest {
    private val chat = "0190a1b2-3333-7e5f-8a9b-0c1d2e3f4a5b"

    @Test
    fun onlyAPushToTheOpenChatShowsNothing() {
        assertTrue(OpenChat.hides("chat", chat, open = chat))
        assertFalse(OpenChat.hides("chat", chat, open = "andere"))
        assertFalse(OpenChat.hides("chat", chat, open = null))
        assertFalse(OpenChat.hides("programm", chat, open = chat))
        assertFalse(OpenChat.hides("chat", null, open = chat))
    }

    @Test
    fun unknownChannelsFallBackToMessages() {
        assertEquals(Notifications.CHAT_CHANNEL, Notifications.channelOf("chat"))
        assertEquals(Notifications.PROGRAMS_CHANNEL, Notifications.channelOf("programme"))
        assertEquals(Notifications.MESSAGES_CHANNEL, Notifications.channelOf("fremd"))
        assertEquals(Notifications.MESSAGES_CHANNEL, Notifications.channelOf(null))
    }
}
