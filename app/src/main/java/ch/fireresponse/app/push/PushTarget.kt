package ch.fireresponse.app.push

/** Wohin ein Push führt: in einen Termin oder in einen Infokanal. */
sealed interface PushTarget {
    val id: String

    data class Appointment(override val id: String) : PushTarget

    /** Der Infokanal einer Einheit; `id` ist die Einheit. */
    data class InfoChannel(override val id: String) : PushTarget

    companion object {
        private val ID = Regex("^[0-9a-fA-F-]{36}$")

        /**
         * Aus den Daten des Pushes (`typ`, `id`). Der Inhalt kommt von aussen: Eine Kennung, die keine ist, führt nirgends
         * hin (sie würde sonst im Pfad einer Anfrage landen).
         */
        fun from(type: String?, id: String?): PushTarget? {
            if (id == null || !ID.matches(id)) return null
            return when (type) {
                "programm" -> Appointment(id)
                "infokanal" -> InfoChannel(id)
                else -> null
            }
        }
    }
}
