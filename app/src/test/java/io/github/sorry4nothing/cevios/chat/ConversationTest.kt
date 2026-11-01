package io.github.sorry4nothing.cevios.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTest {
    private fun n(id: String) =
        ChatMessage(id = id, text = "t", createdAt = "2027-01-01T10:00:00Z", author = ChatPerson("a", "A"))

    private fun page(vararg ids: String, next: String? = null) = MessagePage(ids.map(::n), next)

    private fun ids(v: Conversation) = v.messages.map { it.id }

    @Test
    fun firstPageIsTaken() {
        val v = Conversation().withNewestPage(page("0003", "0002", next = "0002"))
        assertEquals(listOf("0003", "0002"), ids(v))
        assertEquals("0002", v.next)
    }

    @Test
    fun newMessagesAddOnTopAndOlderStay() {
        val existing = Conversation(listOf("0005", "0004", "0003", "0002", "0001").map(::n), next = "0001")
        val v = existing.withNewestPage(page("0007", "0006", "0005", "0004", next = "0004"))
        assertEquals(listOf("0007", "0006", "0005", "0004", "0003", "0002", "0001"), ids(v))
        assertEquals("Cursor der älteren Seiten bleibt", "0001", v.next)
    }

    @Test
    fun touchingAtEdgeCountsAsOverlap() {
        val existing = Conversation(listOf("0003", "0002").map(::n), next = null)
        val v = existing.withNewestPage(page("0004", "0003", next = "0003"))
        assertEquals(listOf("0004", "0003", "0002"), ids(v))
    }

    @Test
    fun tooManyNewMessagesLeaveGapAndNewPageWins() {
        val existing = Conversation(listOf("0002", "0001").map(::n), next = null)
        val v = existing.withNewestPage(page("0099", "0098", "0097", next = "0097"))
        assertEquals(listOf("0099", "0098", "0097"), ids(v))
        assertEquals("0097", v.next)
    }

    @Test
    fun messageMissingFromTheNewPageDisappears() {
        val existing = Conversation(listOf("0005", "0004", "0003").map(::n), next = null)
        val v = existing.withNewestPage(page("0005", "0003"))
        assertEquals(listOf("0005", "0003"), ids(v))
    }

    @Test
    fun emptyPageGivesEmptyConversation() {
        val v = Conversation(listOf("0001").map(::n)).withNewestPage(page())
        assertEquals(Conversation(), v)
    }

    @Test
    fun olderPageIsAppendedWithoutDuplicates() {
        val existing = Conversation(listOf("0005", "0004").map(::n), next = "0004")
        val v = existing.withOlder(page("0004", "0003", "0002", next = null))
        assertEquals(listOf("0005", "0004", "0003", "0002"), ids(v))
        assertEquals(null, v.next)
    }

    @Test
    fun onlyMessagesOfOthersNewerThanKnownAreAnnounced() {
        val own = n("0006").copy(own = true)
        val conversation = Conversation(listOf(own, n("0004"), n("0003"), n("0002")))
        assertEquals(2, conversation.newFromOthers("0002"))
        assertEquals(0, conversation.newFromOthers("0004"))
        assertEquals("beim ersten Laden gibt es nichts anzusagen", 0, conversation.newFromOthers(null))
    }
}
