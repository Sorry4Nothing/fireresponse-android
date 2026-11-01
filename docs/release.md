# Release als APK über GitHub

Solange die Play Console die Identität noch prüft (Epic #7), bekommt der Pilot die App als signierte APK über ein GitHub-Release. Dieser Weg ersetzt den Store nicht: `assetlinks.json`, AAB und Play-Upload bleiben bei #7.

## Voraussetzungen (nur lokal, nie im Repo)

In `~/.gradle/gradle.properties`:

| Schlüssel | Inhalt |
| --- | --- |
| `ceviApiBaseUrl` | Adresse des Servers (`https://…`), wird in die APK eingebrannt |
| `ceviSigningStoreFile` | Pfad zur Schlüsseldatei (`.jks`, liegt ausserhalb des Repos) |
| `ceviSigningStorePassword`, `ceviSigningKeyAlias`, `ceviSigningKeyPassword` | Zugang zum Schlüssel |

Ohne die Signierung-Angaben baut `assembleRelease` eine **unsignierte** APK (`app-release-unsigned.apk`): Sie lässt sich nicht installieren.

**Den Schlüssel sichern.** Wer ihn verliert, kann eine verteilte App nicht mehr aktualisieren: Alle müssen sie deinstallieren (und verlieren die lokalen Daten). Die Signatur der Pilot-APK ist ausserdem nicht die der späteren Play-Version; wer von der APK in den Store wechselt, installiert neu.

## Version anheben

`app/build.gradle.kts`: `versionName` im Format `x.y.z` (das Backend antwortet sonst mit 426) und `versionCode = Major·10000 + Minor·100 + Patch` (0.9.0 → 900, 1.0.0 → 10000).

## Bauen und prüfen

```
./gradlew testDebugUnitTest lintDebug assembleRelease
```

Die Datei liegt unter `app/build/outputs/apk/release/app-release.apk`. Prüfen:

- Signatur: `apksigner verify --print-certs app-release.apk` (der Fingerabdruck muss der des Schlüssels sein, nicht der Debug-Schlüssel).
- Paket: `aapt2 dump badging app-release.apk` zeigt `versionName` und **keine** Zeile `application-debuggable`.
- Auf dem Emulator installieren und starten: Der Login erscheint. Die Release-Variante ist mit R8 minimiert; Fehler zeigen sich oft erst nach dem Anmelden, deshalb einmal anmelden und die Bildschirme öffnen.
- Prüfsumme: `sha256sum app-release.apk`.

`app/build/outputs/mapping/release/mapping.txt` aufbewahren (nicht ins Release hängen): Ohne sie sind Absturzberichte der Release-Variante nicht lesbar.

## Veröffentlichen

1. PR `develop` nach `main`, den die Betreiberin merged. `main` verlangt den Check `build`.
2. Auf dem Stand von `main` die APK bauen (siehe oben), nicht auf einem lokalen Zwischenstand.
3. Tag und Release in einem Schritt, als Vorabversion:

```
gh release create v0.9.0 app-release.apk --target main --prerelease --title "Pilot 0.9.0" --notes-file notes.md
```

Die Notizen nennen Version, was neu ist, die Prüfsumme und den Hinweis zur Installation («Aus unbekannten Quellen installieren»).

## Was nie ins Release gehört

- Die Server-Adresse in den Notizen oder in Anhängen. **Die APK selbst enthält sie** (`BuildConfig.API_BASE_URL`), ebenso die Firebase-Konfiguration. In einem öffentlichen Repo ist die Adresse damit für jeden lesbar, der die APK lädt. Wer das nicht will, veröffentlicht das Release zunächst als Entwurf (`--draft`) und gibt die APK anders weiter.
- Der Schlüssel, seine Passwörter, `google-services.json`, `mapping.txt`.
