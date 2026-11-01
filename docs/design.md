# Design

Dieses Dokument hält fest, wie die App das Design des Auftraggebers umsetzt und wo sie davon abweicht.

## Vorlagen

| Vorlage | Stand |
|---|---|
| Design System «Fire-Response Android»: Farben, Schrift, Abstände, Bausteine (`tokens.json`, Komponenten) | umgesetzt in `ui/theme/` und `ui/` |
| Bildschirme «Fire-Response Android Screens»: Nacht und Tag, alle Zustände | teilweise umgesetzt, siehe unten |

Stil «Feuerwache»: ruhiges, warmes Graphit als Grund. Akzent in der Nacht Warngelb, am Tag Signalrot; Signalrot als Fläche nur für die eine Hauptaktion pro Screen. Die App folgt der Systemeinstellung (Nacht oder Tag), es gibt keinen Umschalter und kein Dynamic Color.

## Zuordnung Baustein → Code

| Baustein | Code |
|---|---|
| Farb-Tokens (Nacht, Tag) | `ui/theme/Colors.kt`: `ColorScheme` für Material, zusätzliche Rollen in `FireResponseColors` (`MaterialTheme.fire.accent`, `accentText`, `accentFill`, `primaryText`, `switchOn`, `tabSelected`, `focus`, Erfolg und Warnung) |
| Schrift | `ui/theme/Typography.kt`: Barlow Condensed (Titel) und Inter (Text) aus `res/font`, alle Grössen in sp; `Eyebrow` für Überzeilen |
| Abstände, Grössen, Ecken | `ui/theme/Dimensions.kt`: `Spacing`, `Sizes` (Touch-Ziel 48 dp, Button 56 dp, Feld 52 dp, Textbreite 640 dp), `FireResponseShapes`, `Pill` |
| Kopf | `ui/Header.kt`: runde Buttons, Überzeile über dem Titel, Titel als `heading()`, Fokus auf den Titel nach der Navigation |
| Dock | `ui/Dock.kt`: schwebend unten, ab 600 dp senkrecht links; aktiv mit `tab-selected` und `accent` |
| Buttons | `ui/Buttons.kt`: `PrimaryButton`, `OutlinedPillButton`, `TextActionButton`, `DangerButton` (nur im Bestätigungsdialog) |
| Karten, Zeilen | Karten mit `surfaceContainerLow` und feinem Rand statt Schatten; Einstellungszeilen in `auth/ProfileScreen.kt` |
| StatusChip, Zusage | `ui/Chips.kt` (immer Icon und Wort), Zusage in `appointments/RegistrationSection.kt` |
| Dialog | `ui/Dialog.kt`; «Abmelden?» mit Fokus auf «Abbrechen» |
| Hinweis | `ui/Notice.kt` |
| Textfeld | `ui/TextField.kt`: Label immer sichtbar, Rand `outline`, Fokus 2 dp `focus` |
| Zustände | `ui/States.kt`: Laden (Skelett), Leer, Offline, Fehler, Nicht gefunden, Kein Zugriff, Sitzung abgelaufen |
| Querformat | `MainActivity.kt`: ab 600 dp Liste und Detail nebeneinander; `ui/ContentWidth.kt` begrenzt den Text auf 640 dp |
| Profil | `auth/ProfileScreen.kt`: Profilbild, Benachrichtigungen, Datenschutz, Biometrie, Teilnehmende, Einheiten als Info-Zeile, Abmelden |

## Regeln

- **Kein Zustand nur über Farbe.** Immer Icon und Wort (StatusChip, Zusage, Hinweise).
- **Kontrast:** Textpaare mindestens 4.5:1, `outline`, `focus` und `switch-on` mindestens 3:1 auf allen Flächen. `ContrastTest` prüft das für beide Themen.
- **Touch-Ziele:** jedes bedienbare Element mindestens 48 dp.
- **Schrift:** Grössen in sp, keine festen Höhen für Text; Karten wachsen mit.
- **Fokusring:** 3 dp in `focus`, 2 dp Abstand (`ui/Focus.kt`).
- **TalkBack:** Karten sind ein Element mit vollständigem Satz, Titel sind `heading()`, Langes Drücken hat immer eine Alternative. Nach der Navigation geht der Fokus auf den Titel, bei Dialogen auf deren Titel oder auf «Abbrechen».
- **Fehlt eine Aktion in der API-Antwort, fehlt der Button;** er wird nicht ausgegraut.

## Stand der Prüfung

Geprüft: Build, Lint und Unit-Tests (inklusive Kontrast); Start, Offline-Zustand und Nachtmodus auf dem Emulator (API 37, Hochformat).

**Nicht geprüft:** TalkBack, Switch Access und Voice Access auf einem Gerät; Querformat und Zwei-Spalten-Ansicht (der Emulator ging nicht ins Querformat, und die Ansicht liegt hinter dem Login); grosse Schrift (200 %); der Tag-Modus ausserhalb des Startbildschirms. Der Durchgang mit TalkBack ist für #35 noch zu machen, sobald ein Server zum Anmelden bereitsteht.

## Platzhalter aus der Übernahme

- **Logo:** eine neutrale Flamme (`ic_logo`, `ic_splash_logo`, `ic_stage_fallback`, `ic_stat_logo`, `ic_launcher_foreground`). Das echte Zeichen und das Wappen des Kantons Zürich ersetzen sie; das Design liefert beide als Platzhalter.
- **Begriffe:** Einheit (mit Bezirk), Kommando, Teilnehmende und Termin; in der Oberfläche heisst ein Termin «Übung» und das Kommando «Leitung». Die Namen in der Schnittstelle (`@SerialName`) bleiben, bis der neue Vertrag (E4) kopiert wird.

## Noch nicht umgesetzt

Diese Teile hängen am neuen API-Vertrag (E4) oder an Design, das noch folgt:

- Zeitleiste der Übungsliste (Datum links an einer Linie), Treffpunkt- und Schluss-Kacheln im Detail, Kontaktperson pro Termin mit Rangabzeichen (A2, A3)
- Rangabzeichen (`RangAbzeichen`) bei den Teilnehmenden; der Rang kommt als Bild von der API
- Zustand «Keine Einheit», Willkommen mit Einführung und Einwilligung nach neuer Vorlage, Anmeldung mit Wappen (A1)
- Kennzeichnung der Einheit in der Liste, Push-Einstellungen im Profil, Info-Seite «Konto aufs Kind übertragen» (Design folgt)

## Abweichungen von der Vorlage

Wo die Vorlage dem Auftrag oder dem Plan widerspricht, gilt der Auftrag:

1. **Mehrere Einheiten pro Konto.** Die Vorlage geht von genau einer Einheit pro Konto aus. Ein Konto kann mehrere haben; die Übungsliste zeigt alle und kennzeichnet die Einheit, das Profil nennt sie in einer Info-Zeile.
2. **Push.** Die Vorlage zeigt Benachrichtigungen als «Kommt bald». Push ist Teil von V1; das Profil führt zu den Schaltern.
3. **Anwesenheit.** Die Vorlage führt «Anwesenheit» in der Verwaltung. Die App erfasst keine Anwesenheit.
4. **Schreibweise.** Die Vorlage schreibt «Fire-Response». Die App heisst «FireResponse».
