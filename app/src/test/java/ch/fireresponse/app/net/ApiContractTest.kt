package ch.fireresponse.app.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Jeder Aufruf der App steht im Vertrag des Backends (`api-vertrag/openapi.yaml`, siehe dort die README). Geprüft werden
 * Methode und Pfad; die Felder der Antworten prüfen die Tests der Schnittstellen mit ihrem Beispiel-JSON.
 */
class ApiContractTest {
    private data class Call(val method: String, val path: String, val source: String)

    private val projectRoot: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "api-vertrag/openapi.yaml").isFile }

    /** `{id}`, `{kindId}` und `$id` zählen gleich: Es kommt auf den Pfad an, nicht auf den Namen des Platzhalters. */
    private fun normalize(path: String) = path.substringBefore('?')
        .replace(Regex("\\{[^}]*}"), "{}")
        .replace(Regex("\\$\\{[^}]*}|\\$[A-Za-z_][A-Za-z0-9_]*"), "{}")

    private fun contract(): Set<Pair<String, String>> {
        val operations = mutableSetOf<Pair<String, String>>()
        var path: String? = null
        var inPaths = false
        File(projectRoot, "api-vertrag/openapi.yaml").forEachLine { line ->
            when {
                line == "paths:" -> inPaths = true
                inPaths && line == "components:" -> inPaths = false
                !inPaths -> Unit
                else -> {
                    Regex("^  (/\\S*):\\s*$").matchEntire(line)?.let { path = it.groupValues[1] }
                    Regex("^    (get|post|put|patch|delete):\\s*$").matchEntire(line)?.let { match ->
                        path?.let { operations += match.groupValues[1].uppercase() to normalize(it) }
                    }
                }
            }
        }
        return operations
    }

    /** Die Aufrufe der Klassen `*Api` in `app/src/main`, mit Methode aus dem Namen der Funktion von `ApiClient`. */
    private fun calls(): List<Call> {
        val methods = mapOf(
            "get" to "GET", "post" to "POST", "postAction" to "POST", "postWithoutBody" to "POST",
            "put" to "PUT", "putWithoutBody" to "PUT", "putBytes" to "PUT", "patch" to "PATCH", "delete" to "DELETE",
        )
        val call = Regex("api\\.(get|post|put|patch|postAction|postWithoutBody|putWithoutBody|putBytes|delete)(?:<[^>]*>)?\\(\\s*\"([^\"]+)\"")
        val page = Regex("pageUrl\\(\\s*\"([^\"]+)\"")
        return File(projectRoot, "app/src/main").walkTopDown()
            .filter { it.isFile && it.name.endsWith("Api.kt") }
            .flatMap { file ->
                val text = file.readText()
                call.findAll(text).map { Call(methods.getValue(it.groupValues[1]), normalize(it.groupValues[2]), file.name) } +
                    page.findAll(text).map { Call("GET", normalize(it.groupValues[1]), file.name) }
            }
            .toList()
    }

    /**
     * Aufrufe, die der Vertrag der App (`api-vertrag/`) noch nicht enthält, weil das Backend die Route noch nicht gebaut hat; sie stehen
     * in `Claude/FireResponse/API/FR API Vertrag Apps Nr 27-34`. Mit der neuen Vertragskopie hier streichen.
     */
    // shortcut: Liste von Hand, bis der Vertrag der Nr. 27 kopiert ist.
    private val pendingInBackend = setOf(
        "GET" to "/api/v1/wartung",
        "GET" to "/api/v1/infokanaele",
        "GET" to "/api/v1/infokanaele/{}/mitteilungen",
        "POST" to "/api/v1/infokanaele/{}/mitteilungen",
        "PUT" to "/api/v1/infokanaele/{}/mitteilungen/{}",
        "DELETE" to "/api/v1/infokanaele/{}/mitteilungen/{}",
        "PUT" to "/api/v1/infokanaele/{}/gelesen",
    )

    @Test
    fun aPendingCallIsRemovedOnceTheContractHasIt() {
        assertEquals(emptySet<Pair<String, String>>(), pendingInBackend.intersect(contract()))
    }

    @Test
    fun theTestFindsWhatItChecks() {
        // Ein Test, der nichts findet, besteht immer: Darum wird zuerst geprüft, dass er etwas findet.
        assertTrue("Vertrag nicht gelesen", contract().size > 40)
        assertTrue("Keine Aufrufe der App gefunden", calls().size > 10)
    }

    @Test
    fun everyCallOfTheAppIsInTheContract() {
        val contract = contract()
        val missing = calls().filter { (it.method to it.path) !in contract && (it.method to it.path) !in pendingInBackend }
        assertEquals(
            "Nicht im Vertrag: " + missing.joinToString { "${it.method} ${it.path} (${it.source})" },
            emptyList<Call>(),
            missing,
        )
    }

    @Test
    fun theKnownRoutesAreReadCorrectly() {
        val contract = contract()
        assertTrue(("GET" to "/api/v1/programme") in contract)
        assertTrue(("PUT" to "/api/v1/programme/{}/teilnahmen/{}") in contract)
        assertTrue(("POST" to "/api/auth/sign-in/username") in contract)
        assertTrue(("GET" to "/api/v1/einwilligung") in contract)
        assertEquals("/api/v1/programme/{}/teilnahmen/{}", normalize("/api/v1/programme/\$id/teilnahmen/\$participantId?x=\$y"))
    }
}
