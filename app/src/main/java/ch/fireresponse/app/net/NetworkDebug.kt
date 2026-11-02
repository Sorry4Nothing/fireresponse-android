package ch.fireresponse.app.net

import okhttp3.Interceptor

/**
 * Haken für den Debug-Build: Ein hier gesetzter [Interceptor] beantwortet Anfragen selbst, ohne Netz (Fake-Server im Quellordner
 * `debug`). Im Release-Build setzt ihn niemand, er bleibt `null` und ist ohne Wirkung.
 */
object NetworkDebug {
    @Volatile
    var interceptor: Interceptor? = null
}
