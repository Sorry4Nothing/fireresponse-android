# FireResponse Android

Native Android-App (Kotlin, Jetpack Compose) für FireResponse, die App für Jugend- und Kinderfeuerwehren im Kanton Zürich: Übungen aus Lodur, Ja/Nein-Anmeldung, Kontakt und Push.

Das Backend liegt in [Sorry4Nothing/fireresponse-api](https://github.com/Sorry4Nothing/fireresponse-api). Die API-Beschreibung steht dort unter `docs/api/`; `api-vertrag/` hier ist bis zum neuen Vertrag (E4) noch der alte Stand.

## Bauen und prüfen

Voraussetzung: JDK 21 und das Android SDK (`local.properties` mit `sdk.dir`, nicht im Repo).

```
./gradlew assembleDebug lintDebug testDebugUnitTest
```

Das ist auch der Check `build` der CI. Die Adresse des Servers kommt aus `fireresponseApiBaseUrl` in `~/.gradle/gradle.properties`; die Release-Signierung steht in [docs/release.md](docs/release.md).

## Arbeitsweise

- Ein Branch und ein Pull Request pro Issue, abgezweigt von `develop`: `feature/<nr>-<kurzname>`, für Bugs `fix/<nr>-<kurzname>`.
- `main` und `develop` nimmt nur Pull Requests an, mit grünem Check `build`.
- Gradle prüft die Abhängigkeiten gegen `gradle/verification-metadata.xml`. Ein Update (auch von Dependabot) bringt die neuen Prüfsummen mit.
- Das Design und seine Abweichungen stehen in [docs/design.md](docs/design.md).
