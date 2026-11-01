package io.github.sorry4nothing.cevios.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sorry4nothing.cevios.R
import io.github.sorry4nothing.cevios.net.ApiError
import io.github.sorry4nothing.cevios.net.ApiResult
import io.github.sorry4nothing.cevios.net.Background
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

/** Zustand beim Senden einer Nachricht. */
data class SendState(val running: Boolean = false, val error: ApiError? = null)

/** Zustand beim Speichern der eigenen Telefonnummer. */
data class PhoneState(val running: Boolean = false, val error: ApiError? = null)

/** Zustand beim Öffnen eines Einzelchats. */
data class ActionState(val running: Boolean = false, val error: ApiError? = null)

/** `null` heisst: wird geladen. Nachrichten und Namen bleiben nur im Arbeitsspeicher. */
class ChatViewModel(private val api: ChatApi) : ViewModel() {
    private val _list = MutableStateFlow<ApiResult<ChatList>?>(null)
    val list: StateFlow<ApiResult<ChatList>?> = _list.asStateFlow()

    private val _conversation = MutableStateFlow<ApiResult<Conversation>?>(null)
    val conversation: StateFlow<ApiResult<Conversation>?> = _conversation.asStateFlow()

    private val _loadingOlder = MutableStateFlow(false)
    val loadingOlder: StateFlow<Boolean> = _loadingOlder.asStateFlow()

    private val _send = MutableStateFlow(SendState())
    val send: StateFlow<SendState> = _send.asStateFlow()

    private val _action = MutableStateFlow(ActionState())
    val action: StateFlow<ActionState> = _action.asStateFlow()

    private val _profile = MutableStateFlow<ApiResult<Profile>?>(null)
    val profile: StateFlow<ApiResult<Profile>?> = _profile.asStateFlow()


    private val _phone = MutableStateFlow(PhoneState())
    val phone: StateFlow<PhoneState> = _phone.asStateFlow()

    private var openedId: String? = null
    private var conversationJob: Job? = null

    init {
        loadList()
        loadProfile()
    }

    /** Das Profil entscheidet, welche Aktionen die App anbietet (Stufenleitung, Kinder). */
    fun loadProfile() {
        viewModelScope.launch { _profile.value = api.profile() }
    }

    /** Die Stufenleitungen der Stufe für den Dialog «Kontakt» im Gruppenchat. */
    fun resetPhone() {
        _phone.value = PhoneState()
    }

    /** Speichert die eigene Nummer (leer entfernt sie), liest danach das Profil neu und meldet den Erfolg. */
    fun savePhone(phone: String, onSaved: () -> Unit) {
        if (_phone.value.running) return
        _phone.value = PhoneState(running = true)
        viewModelScope.launch {
            when (val result = api.setPhone(phone.trim())) {
                is ApiResult.Failure -> _phone.value = PhoneState(error = result.error)
                is ApiResult.Ok -> {
                    _phone.value = PhoneState()
                    _profile.value = api.profile()
                    onSaved()
                }
            }
        }
    }

    /** Mit `silent` bleibt die alte Liste sichtbar, bis die neue da ist. */
    fun loadList(silent: Boolean = false) {
        if (!silent) _list.value = null
        viewModelScope.launch(if (silent) Background() else EmptyCoroutineContext) { _list.value = api.chats() }
    }

    fun openChat(id: String) {
        conversationJob?.cancel()
        openedId = id
        _conversation.value = null
        _loadingOlder.value = false
        _send.value = SendState()
        _action.value = ActionState()
        conversationJob = viewModelScope.launch {
            val result = api.messages(id)
            _conversation.value = result.mapConversation(Conversation())
            if (result is ApiResult.Ok) markRead(id)
        }
    }

    fun closeChat() {
        conversationJob?.cancel()
        openedId = null
        _action.value = ActionState()
    }

    /** Holt die neueste Seite und führt sie mit dem Geladenen zusammen (von Hand oder im Takt). */
    fun refresh() {
        val id = openedId ?: return
        viewModelScope.launch(Background()) {
            val result = api.messages(id)
            if (openedId != id) return@launch
            // Ein Fehler im Takt ersetzt einen guten Verlauf nicht; nur ein fehlender Verlauf zeigt den Fehler.
            val existing = (_conversation.value as? ApiResult.Ok)?.value
            val merged = result.mapConversation(existing ?: Conversation())
            if (existing == null || result is ApiResult.Ok) _conversation.value = merged
            // Was im offenen Chat neu eintrifft, ist sofort gesehen: Der Zähler in der Liste soll es nicht mitzählen.
            val fresh = (merged as? ApiResult.Ok)?.value
            if (existing != null && result is ApiResult.Ok && fresh != null && fresh.newFromOthers(existing.messages.firstOrNull()?.id) > 0) {
                markRead(id)
            }
        }
    }

    /** Meldet dem Server, bis wohin gelesen wurde. Ein Fehler stört das Lesen nicht, der Zähler stimmt dann etwas später. */
    private fun markRead(chatId: String) {
        viewModelScope.launch(Background()) { api.markRead(chatId) }
    }

    fun loadOlder() {
        val id = openedId ?: return
        val existing = (_conversation.value as? ApiResult.Ok)?.value ?: return
        val cursor = existing.next ?: return
        if (_loadingOlder.value) return
        _loadingOlder.value = true
        viewModelScope.launch(Background()) {
            val result = api.messages(id, before = cursor)
            if (openedId == id) {
                if (result is ApiResult.Ok) _conversation.value = ApiResult.Ok(existing.withOlder(result.value))
                _loadingOlder.value = false
            }
        }
    }

    /** Sendet eine Nachricht; `onSent` läuft nur bei Erfolg (der Entwurf bleibt sonst stehen). */
    fun send(text: String, onSent: () -> Unit) {
        val id = openedId ?: return
        if (_send.value.running || text.isBlank()) return
        _send.value = SendState(running = true)
        viewModelScope.launch {
            when (val result = api.send(id, text.trim())) {
                is ApiResult.Failure -> _send.value = SendState(error = result.error)
                is ApiResult.Ok -> {
                    _send.value = SendState()
                    onSent()
                    refresh()
                }
            }
        }
    }

    /**
     * Öffnet den Einzelchat der Stufe (für Stufenleitungen mit dem Elternteil) und lädt die Chatliste neu,
     * damit der Titel stimmt. `onChat` bekommt die ID des Chats.
     */
    fun openDirectChat(ageGroupId: String, parentUserId: String?, onChat: (String) -> Unit) {
        if (_action.value.running) return
        _action.value = ActionState(running = true)
        viewModelScope.launch {
            when (val result = api.directChat(ageGroupId, parentUserId)) {
                is ApiResult.Failure -> _action.value = ActionState(error = result.error)
                is ApiResult.Ok -> {
                    _action.value = ActionState()
                    _list.value = api.chats()
                    onChat(result.value.id)
                }
            }
        }
    }

    private fun ApiResult<MessagePage>.mapConversation(existing: Conversation): ApiResult<Conversation> = when (this) {
        is ApiResult.Ok -> ApiResult.Ok(existing.withNewestPage(value))
        is ApiResult.Failure -> this
    }
}
