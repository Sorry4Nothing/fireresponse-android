package ch.fireresponse.app.infochannel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.fireresponse.app.net.ApiError
import ch.fireresponse.app.net.ApiResult
import ch.fireresponse.app.net.Background
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Der offene Verlauf einer Einheit. `messages` ist neueste zuerst. `error` gilt für das erste Laden (dann ist `messages` leer);
 * `loading` heisst: Die erste Seite kommt noch.
 */
data class History(
    val unitId: String,
    val canWrite: Boolean = false,
    val messages: List<InfoMessage> = emptyList(),
    val next: String? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val error: ApiError? = null,
)

/** Schreiben, Bearbeiten und Löschen: läuft gerade etwas, und was ging schief. */
data class ChannelAction(val running: Boolean = false, val error: ApiError? = null)

/**
 * Die Infokanäle der Person und der gerade offene Verlauf. Die Liste der Kanäle liefert den Zähler am Reiter und lädt im Takt still
 * neu ([refreshChannels]); ein Fehler lässt den alten Stand stehen. Nichts davon liegt auf der Platte.
 */
class InfoChannelViewModel(private val api: InfoChannelApi) : ViewModel() {
    private val _channels = MutableStateFlow<ApiResult<List<InfoChannel>>?>(null)
    val channels: StateFlow<ApiResult<List<InfoChannel>>?> = _channels.asStateFlow()

    private val _history = MutableStateFlow<History?>(null)
    val history: StateFlow<History?> = _history.asStateFlow()

    private val _action = MutableStateFlow(ChannelAction())
    val action: StateFlow<ChannelAction> = _action.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        _channels.value = null
        viewModelScope.launch { _channels.value = fetchChannels() }
    }

    /** Im Takt, ohne Ladezustand: Ein Fehler lässt die Liste stehen. */
    fun refreshChannels() {
        viewModelScope.launch(Background()) { (fetchChannels() as? ApiResult.Ok)?.let { _channels.value = it } }
    }

    private suspend fun fetchChannels(): ApiResult<List<InfoChannel>> = when (val result = api.list()) {
        is ApiResult.Ok -> ApiResult.Ok(result.value.channels)
        is ApiResult.Failure -> result
    }

    /** Öffnet den Verlauf einer Einheit: erste Seite laden, danach als gelesen melden. */
    fun open(unitId: String) {
        if (_history.value?.unitId == unitId) return
        _history.value = History(unitId)
        _action.value = ChannelAction()
        viewModelScope.launch {
            when (val result = api.messages(unitId)) {
                is ApiResult.Failure -> if (_history.value?.unitId == unitId) _history.value = History(unitId, loading = false, error = result.error)
                is ApiResult.Ok -> if (_history.value?.unitId == unitId) {
                    _history.value = History(unitId, result.value.canWrite, result.value.messages, result.value.next, loading = false)
                    markRead(unitId)
                }
            }
        }
    }

    /** Schliesst den Verlauf (zurück zur Liste oder in einen anderen Reiter). */
    fun close() {
        _history.value = null
        _action.value = ChannelAction()
    }

    /** Wiederholt das erste Laden nach einem Fehler. */
    fun retry() {
        val unitId = _history.value?.unitId ?: return
        _history.value = null
        open(unitId)
    }

    /** Liest die erste Seite still neu, solange der Verlauf offen ist (neue Mitteilungen, Bearbeitetes, Gelöschtes). */
    fun refreshHistory() {
        val current = _history.value ?: return
        if (current.loading || _action.value.running) return
        viewModelScope.launch(Background()) {
            val result = api.messages(current.unitId) as? ApiResult.Ok ?: return@launch
            val now = _history.value ?: return@launch
            if (now.unitId != current.unitId || _action.value.running) return@launch
            val hadNew = result.value.messages.any { fresh -> now.messages.none { it.id == fresh.id } }
            _history.value = now.copy(
                canWrite = result.value.canWrite,
                messages = mergeFirstPage(result.value.messages, now.messages),
                next = if (now.messages.size > result.value.messages.size) now.next else result.value.next,
            )
            if (hadNew) markRead(current.unitId)
        }
    }

    /** Ältere Mitteilungen: die nächste Seite, falls es eine gibt. */
    fun loadOlder() {
        val current = _history.value ?: return
        val before = current.next ?: return
        if (current.loading || current.loadingMore) return
        _history.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            val result = api.messages(current.unitId, before)
            val now = _history.value ?: return@launch
            if (now.unitId != current.unitId) return@launch
            _history.value = when (result) {
                is ApiResult.Ok -> now.copy(
                    messages = now.messages + result.value.messages.filter { page -> now.messages.none { it.id == page.id } },
                    next = result.value.next,
                    loadingMore = false,
                )
                is ApiResult.Failure -> now.copy(loadingMore = false)
            }
        }
    }

    /** Schreibt eine Mitteilung an die ganze Einheit; `onDone` läuft nach Erfolg (die Eingabe leert sich erst dann). */
    fun send(text: String, onDone: () -> Unit) = run(onDone) { unitId ->
        when (val result = api.send(unitId, text)) {
            is ApiResult.Ok -> { updateMessages { listOf(result.value) + it }; null }
            is ApiResult.Failure -> result.error
        }
    }

    fun edit(id: String, text: String, onDone: () -> Unit) = run(onDone) { unitId ->
        when (val result = api.edit(unitId, id, text)) {
            is ApiResult.Ok -> { updateMessages { list -> list.map { if (it.id == id) result.value else it } }; null }
            is ApiResult.Failure -> result.error
        }
    }

    /** Löscht eine Mitteilung; sie bleibt als «Nachricht gelöscht» im Verlauf (Text und Autor weg). */
    fun delete(id: String) = run({}) { unitId ->
        when (val result = api.delete(unitId, id)) {
            is ApiResult.Ok -> {
                updateMessages { list ->
                    list.map { if (it.id == id) it.copy(text = null, author = null, deleted = true, canEdit = false, canDelete = false) else it }
                }
                null
            }
            is ApiResult.Failure -> result.error
        }
    }

    fun clearActionError() {
        if (!_action.value.running) _action.value = ChannelAction()
    }

    private fun run(onDone: () -> Unit, block: suspend (unitId: String) -> ApiError?) {
        val unitId = _history.value?.unitId ?: return
        if (_action.value.running) return
        _action.value = ChannelAction(running = true)
        viewModelScope.launch {
            val error = block(unitId)
            _action.value = ChannelAction(error = error)
            if (error == null) onDone()
        }
    }

    private fun updateMessages(change: (List<InfoMessage>) -> List<InfoMessage>) {
        _history.value = _history.value?.let { it.copy(messages = change(it.messages)) }
    }

    /** Meldet den Verlauf als gelesen und holt den Zähler neu; ein Fehler ist egal (beim nächsten Öffnen klappt es wieder). */
    private fun markRead(unitId: String) {
        viewModelScope.launch(Background()) {
            api.markRead(unitId)
            (fetchChannels() as? ApiResult.Ok)?.let { _channels.value = it }
        }
    }
}
