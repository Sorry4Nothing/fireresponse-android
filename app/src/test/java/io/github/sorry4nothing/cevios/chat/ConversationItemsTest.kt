package io.github.sorry4nothing.cevios.chat

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ConversationItemsTest {
    private fun message(id: String, createdAt: String) =
        ChatMessage(id = id, text = "t", createdAt = createdAt, author = ChatPerson("a", "A"))

    private fun describe(items: List<ConversationItem>) = items.map {
        when (it) {
            is ConversationItem.Message -> it.message.id
            is ConversationItem.DayDivider -> "day " + it.date
        }
    }

    @Test
    fun dividerSitsAfterOldestMessageOfEachDayNewestFirst() {
        val messages = listOf(
            message("4", "2027-01-03T12:00:00Z"),
            message("3", "2027-01-03T09:00:00Z"),
            message("2", "2027-01-02T12:00:00Z"),
            message("1", "2027-01-01T12:00:00Z"),
        )
        assertEquals(
            listOf("4", "3", "day 2027-01-03", "2", "day 2027-01-02", "1", "day 2027-01-01"),
            describe(withDayDividers(messages)),
        )
    }

    @Test
    fun dayChangesAtMidnightSwissTimeNotUtc() {
        // 23:30 UTC ist in Zürich schon 00:30 am nächsten Tag (Winterzeit).
        val messages = listOf(message("2", "2027-01-01T23:30:00Z"), message("1", "2027-01-01T22:00:00Z"))
        assertEquals(listOf("2", "day 2027-01-02", "1", "day 2027-01-01"), describe(withDayDividers(messages)))
    }

    @Test
    fun noMessagesGiveNoItems() {
        assertEquals(emptyList<ConversationItem>(), withDayDividers(emptyList()))
    }

    @Test
    fun unreadableTimeGetsNoDivider() {
        assertEquals(listOf("1"), describe(withDayDividers(listOf(message("1", "kein Datum")))))
    }

    @Test
    fun timeIsSwissTimeAcrossDaylightSaving() {
        assertEquals("11:00", message("1", "2027-01-01T10:00:00Z").timeText())
        assertEquals("12:00", message("1", "2027-07-01T10:00:00Z").timeText())
        assertEquals("", message("1", "kein Datum").timeText())
    }

    @Test
    fun labelIsTodayYesterdayOrDate() {
        val today = LocalDate.of(2027, 3, 1)
        assertEquals(DayLabel.Today, dayLabel(today, today))
        assertEquals(DayLabel.Yesterday, dayLabel(LocalDate.of(2027, 2, 28), today))
        assertEquals(DayLabel.On(LocalDate.of(2027, 2, 27)), dayLabel(LocalDate.of(2027, 2, 27), today))
    }

    @Test
    fun initialsUseFirstAndLastWord() {
        assertEquals("SL", initials("Sarah Läuchli"))
        assertEquals("AM", initials("Anna Maria Meier"))
        assertEquals("T", initials("  tom "))
        assertEquals("", initials("   "))
    }
}
