# FireResponse Android

Native Android-App (Kotlin, Jetpack Compose) für FireResponse, die App für Jugend- und Kinderfeuerwehren im Kanton Zürich: Übungen aus Lodur, Ja/Nein-Anmeldung, Kontakt und Push.

Das Backend liegt in [Sorry4Nothing/fireresponse-api](https://github.com/Sorry4Nothing/fireresponse-api). Die API-Beschreibung steht dort unter `docs/api/`; `api-vertrag/` hier ist die Kopie des Vertrags (E4, Stand 2. November 2026); die App wird gegen sie geprüft (`ApiContractTest`).

## Bauen und prüfen

Voraussetzung: JDK 21 und das Android SDK (`local.properties` mit `sdk.dir`, nicht im Repo).

```
./gradlew assembleDebug lintDebug testDebugUnitTest
```

Das ist auch der Check `build` der CI. Die Adresse des Servers kommt aus `fireresponseApiBaseUrl` in `~/.gradle/gradle.properties`; die Release-Signierung steht in [docs/release.md](docs/release.md).

## Ohne Backend ausprobieren

Ein Debug-Build ohne `fireresponseApiBaseUrl` läuft gegen einen Fake-Server (`app/src/debug/…/FakeBackend.kt`): Er hält seinen Stand im Arbeitsspeicher und kennt zwei erfundene Konten mit Startpasswort `startpasswort123`: `demo` (Elternteil von Lena und Noah, muss das Passwort ändern) und `kommando` (Kommandantin der Einheit). Damit lässt sich der ganze Ablauf bis zur Zusage durchspielen. Die Galerie zeigt einzelne Bildschirme: `adb shell am start -S -n ch.fireresponse.app/.GalleryActivity --es screen list` (Namen in `GalleryActivity.kt`). Beides gibt es nur im Debug-Build.

## Arbeitsweise

- Ein Branch und ein Pull Request pro Issue, abgezweigt von `develop`: `feature/<nr>-<kurzname>`, für Bugs `fix/<nr>-<kurzname>`.
- `main` und `develop` nimmt nur Pull Requests an, mit grünem Check `build`.
- Gradle prüft die Abhängigkeiten gegen `gradle/verification-metadata.xml`. Ein Update (auch von Dependabot) bringt die neuen Prüfsummen mit.
- Das Design und seine Abweichungen stehen in [docs/design.md](docs/design.md).
