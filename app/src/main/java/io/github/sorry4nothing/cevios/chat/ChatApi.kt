package io.github.sorry4nothing.cevios.chat

import io.github.sorry4nothing.cevios.contacts.PhoneRequest
import io.github.sorry4nothing.cevios.net.ApiClient
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.net.replacingError

class ChatApi(private val api: ApiClient) {
    suspend fun chats(): ApiResult<ChatList> = api.get("/api/v1/chats")

    /** Eine Seite zu 50 Nachrichten, neueste zuerst. `before` ist der Cursor `next` der vorherigen Seite. */
    suspend fun messages(chatId: String, before: String? = null): ApiResult<MessagePage> =
        api.get("/api/v1/chats/$chatId/nachrichten" + if (before == null) "" else "?vor=$before")

/** Höchstens 20 Nachrichten pro Minute: Bei 429 sagt der Text das, nicht das allgemeine «zu viele Anfragen». */
    suspend fun send(chatId: String, text: String): ApiResult<MessageIdResponse> =
        api.post<SendMessageRequest, MessageIdResponse>("/api/v1/chats/$chatId/nachrichten", SendMessageRequest(text))
            .replacingError(ApiError.TOO_MANY_REQUESTS, ApiError.MESSAGE_TOO_FAST)

    /** Legt den Einzelchat an oder liefert den bestehenden (idempotent). Nur Stufenleitungen nennen `parentUserId`. */
    suspend fun directChat(ageGroupId: String, parentUserId: String? = null): ApiResult<ChatIdResponse> =
        api.post<DirectChatRequest, ChatIdResponse>("/api/v1/chats/direkt", DirectChatRequest(ageGroupId, parentUserId))
            .replacingError(ApiError.NOT_FOUND, ApiError.DIRECT_CHAT_UNAVAILABLE)


    /** Setzt den Gelesen-Stand auf die Serverzeit; danach zählt `ungelesen` nur noch spätere Nachrichten. */
    suspend fun markRead(chatId: String): ApiResult<Unit> = api.putWithoutBody("/api/v1/chats/$chatId/gelesen")

    /** Alle Stufenleitungen der Stufe (Kontaktpersonen im Gruppenchat). */

    /** Setzt die eigene Telefonnummer (nur Stufenleitungen); ein leerer Text entfernt sie. */
    suspend fun setPhone(phone: String): ApiResult<Unit> = api.patch("/api/v1/ich", PhoneRequest(phone))

    suspend fun profile(): ApiResult<Profile> = api.get("/api/v1/ich")
}
