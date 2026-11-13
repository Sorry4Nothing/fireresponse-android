package ch.fireresponse.app.infochannel

import ch.fireresponse.app.appointments.SWISS_ZONE
import ch.fireresponse.app.net.ApiClient
import ch.fireresponse.app.net.ApiResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Die Einheit eines Infokanals; `type` ist «jf» oder «kf». */
@Serializable
data class ChannelUnit(val id: String, val name: String, @SerialName("typ") val type: String = "jf")

/** Ein Infokanal: einer je Einheit der Person (Rolle oder Teilnehmende). Der Name ist «Infokanal» plus Einheitsname. */
@Serializable
data class InfoChannel(
    @SerialName("einheit") val unit: ChannelUnit,
    val name: String,
    @SerialName("darfSchreiben") val canWrite: Boolean = false,
    @SerialName("ungelesen") val unread: Int = 0,
    @SerialName("letzteMitteilungAm") val lastAt: String? = null,
)

@Serializable
data class InfoChannels(@SerialName("infokanaele") val channels: List<InfoChannel> = emptyList())

/** Wer schrieb; `role` ist «kommando» oder «leitung», `null` nach einem Rollenwechsel. */
@Serializable
data class MessageAuthor(
    val name: String,
    @SerialName("rolle") val role: String? = null,
    @SerialName("bildUrl") val imageUrl: String? = null,
)

/**
 * Eine Mitteilung. Eine gelöschte hat `deleted`, keinen Text und keinen Autor und bleibt als Platzhalter im Verlauf. Was die Person
 * damit tun darf, sagt der Server in `canEdit` und `canDelete`; die App zeigt nur das an.
 */
@Serializable
data class InfoMessage(
    val id: String,
    val text: String? = null,
    @SerialName("geloescht") val deleted: Boolean = false,
    @SerialName("bearbeitet") val edited: Boolean = false,
    @SerialName("erstelltAm") val createdAt: String,
    @SerialName("autor") val author: MessageAuthor? = null,
    @SerialName("eigene") val own: Boolean = false,
    @SerialName("darfBearbeiten") val canEdit: Boolean = false,
    @SerialName("darfLoeschen") val canDelete: Boolean = false,
)

/** Eine Seite des Verlaufs, neueste zuerst; `next` ist das `vor` der nächsten Seite, `null` am Ende. */
@Serializable
data class MessagePage(
    @SerialName("darfSchreiben") val canWrite: Boolean = false,
    @SerialName("mitteilungen") val messages: List<InfoMessage> = emptyList(),
    @SerialName("naechste") val next: String? = null,
)

@Serializable
private data class TextRequest(val text: String)

/** Höchstlänge einer Mitteilung (Vertrag: 1 bis 2000 Zeichen). */
const val MAX_MESSAGE_CHARS = 2000

private const val PAGE_SIZE = 50

class InfoChannelApi(private val api: ApiClient) {
    suspend fun list(): ApiResult<InfoChannels> = api.get("/api/v1/infokanaele")

    /** Eine Seite des Verlaufs, neueste zuerst; `before` ist die Kennung der ältesten schon geladenen Mitteilung. */
    suspend fun messages(unitId: String, before: String? = null): ApiResult<MessagePage> =
        api.get("/api/v1/infokanaele/$unitId/mitteilungen?limit=$PAGE_SIZE" + (before?.let { "&vor=${URLEncoder.encode(it, "UTF-8")}" } ?: ""))

    suspend fun send(unitId: String, text: String): ApiResult<InfoMessage> =
        api.post("/api/v1/infokanaele/$unitId/mitteilungen", TextRequest(text))

    suspend fun edit(unitId: String, id: String, text: String): ApiResult<InfoMessage> {
        val request = api.request("/api/v1/infokanaele/$unitId/mitteilungen/$id").put(api.jsonBody(api.json.encodeToString(TextRequest(text)))).build()
        return api.send(request) { api.json.decodeFromString<InfoMessage>(it) }
    }

    suspend fun delete(unitId: String, id: String): ApiResult<Unit> = api.delete("/api/v1/infokanaele/$unitId/mitteilungen/$id")

    /** Setzt den Lesestand auf jetzt. */
    suspend fun markRead(unitId: String): ApiResult<Unit> = api.putWithoutBody("/api/v1/infokanaele/$unitId/gelesen")
}

/** Alles Ungelesene aller Kanäle: die Zahl am Reiter. */
fun unreadTotal(channels: List<InfoChannel>): Int = channels.sumOf { it.unread }

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

/** Die Zeit einer Mitteilung in Schweizer Zeit; `null`, wenn der Server keine lesbare schickt. */
fun messageTime(iso: String): ZonedDateTime? =
    runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(SWISS_ZONE) }.getOrNull()

/** «14:30». */
fun clockText(iso: String): String? = messageTime(iso)?.let { CLOCK.format(it) }

/** Der Tag einer Mitteilung in Schweizer Zeit; `null`, wenn die Zeit nicht lesbar ist. */
fun messageDay(iso: String): LocalDate? = messageTime(iso)?.toLocalDate()

/** Der Zeitpunkt einer Mitteilung; `null`, wenn er nicht lesbar ist. */
private fun instantOf(iso: String): Instant? = runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()

/**
 * Führt die frisch gelesene erste Seite (`fresh`, neueste zuerst) mit dem, was schon da ist (`old`), zusammen: Alles Frische bleibt,
 * von den alten bleiben nur die, die älter als die älteste frische sind (so überleben nachgeladene Seiten, und Gelöschtes verschwindet nicht).
 */
fun mergeFirstPage(fresh: List<InfoMessage>, old: List<InfoMessage>): List<InfoMessage> {
    val oldest = fresh.lastOrNull()?.let { instantOf(it.createdAt) } ?: return old
    val freshIds = fresh.mapTo(HashSet()) { it.id }
    val tail = old.filter { message ->
        message.id !in freshIds && (instantOf(message.createdAt)?.isBefore(oldest) ?: false)
    }
    return fresh + tail
}

/** Eine Zeile des Verlaufs: die Mitteilung oder der Tagestrenner über der ältesten Mitteilung eines Tages. */
sealed interface HistoryRow {
    data class Message(val message: InfoMessage) : HistoryRow

    data class Day(val date: LocalDate) : HistoryRow
}

/**
 * Die Zeilen für einen Verlauf, der von unten nach oben wächst (neueste Mitteilung unten): `messages` ist neueste zuerst, und der
 * Tagestrenner steht nach der ältesten Mitteilung des Tages, also darüber.
 */
fun historyRows(messages: List<InfoMessage>): List<HistoryRow> = buildList {
    messages.forEachIndexed { index, message ->
        add(HistoryRow.Message(message))
        val day = messageDay(message.createdAt)
        val olderDay = messages.getOrNull(index + 1)?.let { messageDay(it.createdAt) }
        if (day != null && day != olderDay) add(HistoryRow.Day(day))
    }
}
