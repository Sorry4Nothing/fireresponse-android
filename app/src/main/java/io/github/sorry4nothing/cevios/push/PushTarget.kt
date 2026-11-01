package io.github.sorry4nothing.cevios.push

/** Wohin ein Push führt: in einen Chat oder in ein Programm. */
sealed interface PushTarget {
    val id: String

    data class Chat(override val id: String) : PushTarget

    data class Program(override val id: String) : PushTarget

    companion object {
        private val ID = Regex("^[0-9a-fA-F-]{36}$")

        /**
         * Aus den Daten des Pushes (`typ`, `id`). Der Inhalt kommt von aussen: Eine Kennung, die keine ist, führt nirgends
         * hin (sie würde sonst im Pfad einer Anfrage landen).
         */
        fun from(type: String?, id: String?): PushTarget? {
            if (id == null || !ID.matches(id)) return null
            return when (type) {
                "chat" -> Chat(id)
                "programm" -> Program(id)
                else -> null
            }
        }
    }
}
