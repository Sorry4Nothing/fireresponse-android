package io.github.sorry4nothing.cevios.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRulesTest {
    private val ageGroup = ChatAgeGroup("s1", "Pfadi", "PF")
    private val leader = ChatPerson("l1", "Leitung")
    private val group = Chat("c1", "stufe", ageGroup, null, listOf(leader))
    private val direct = Chat("c2", "direkt", ageGroup, ChatPerson("e1", "Eltern"), listOf(leader))

    private val groupLeader = Profile("l1", roles = listOf(ProfileRole("s1", "stufenleitung")))
    private val parent = Profile(
        "e1",
        ageGroups = listOf(ProfileAgeGroup("s1", "Pfadi"), ProfileAgeGroup("s2", "Wölfe")),
        children = listOf(ProfileChild("k1", "s1")),
    )

    private fun message(
        authorId: String = "e2",
        authorIsLeader: Boolean = false,
        own: Boolean = false,
    ) = ChatMessage(
        id = "m1", text = "t", createdAt = "2027-01-01T10:00:00Z",
        author = ChatPerson(authorId, "Name", groupLeader = authorIsLeader), own = own,
    )

    @Test
    fun groupLeaderOpensDirectChatViaParentMessage() {
        assertTrue(ChatRules.mayOpenDirectChat(group, message(), groupLeader))
        assertFalse("eigene Nachricht", ChatRules.mayOpenDirectChat(group, message(own = true), groupLeader))
        assertFalse("Autor ist selbst Stufenleitung", ChatRules.mayOpenDirectChat(group, message(authorIsLeader = true), groupLeader))
        assertFalse("nicht im Einzelchat", ChatRules.mayOpenDirectChat(direct, message(), groupLeader))
        assertFalse("Eltern nicht", ChatRules.mayOpenDirectChat(group, message(), parent))
    }

    @Test
    fun actionsListOnlyWhatIsAllowed() {
        assertEquals(
            listOf(MessageAction.OPEN_DIRECT_CHAT),
            ChatRules.actions(group, message(), groupLeader),
        )
        assertEquals("Eltern haben keine Aktion", emptyList<MessageAction>(), ChatRules.actions(group, message(), parent))
        assertEquals(emptyList<MessageAction>(), ChatRules.actions(null, message(own = true), null))
    }

    @Test
    fun parentStartsDirectChatOnlyInAgeGroupsWithOwnChildAndNoExistingChat() {
        assertEquals(listOf("s1"), ChatRules.ageGroupsForDirectChat(parent, listOf(group)).map { it.id })
        assertEquals("schon ein Einzelchat", emptyList<String>(), ChatRules.ageGroupsForDirectChat(parent, listOf(group, direct)).map { it.id })
        assertTrue("ohne Profil", ChatRules.ageGroupsForDirectChat(null, listOf(group)).isEmpty())
    }

    @Test
    fun whoLeadsAgeGroupStartsNoDirectChatAsParent() {
        val both = parent.copy(roles = listOf(ProfileRole("s1", "stufenleitung")))
        assertTrue(ChatRules.ageGroupsForDirectChat(both, listOf(group)).isEmpty())
    }
}
