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
| Hochformat | `AndroidManifest.xml`: `screenOrientation="portrait"`, die App dreht sich nicht. Ab Android 16 ignoriert das System die Sperre auf Bildschirmen ab 600 dp (Tablets, aufgeklappte Foldables); dort gelten `ui/TwoPane.kt` (Liste und Detail nebeneinander, Dock links) und `ui/ContentWidth.kt` (Text höchstens 640 dp) |
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

Geprüft:

- Build, Lint und Unit-Tests (inklusive Kontrast), strikte Abhängigkeitsprüfung für Debug und Release.
- Auf dem Emulator (API 37) mit der Galerie: Login, Passwort, Willkommen, Liste, Detail, Anmeldungen, Profil und «Keine Einheit» in Tag, dazu Willkommen und Liste in Nacht, die Liste im breiten Format (Dock links).
- Grosse Schrift (200 %): Das Layout bleibt lesbar; die dekorative Flamme entfällt ab 130 %, weil Text auf Rot kaum lesbar wäre.
- Touch-Ziele im Detail: alle 8 klickbaren Elemente sind mindestens 48 dp gross (UI-Dump).
- Release-Build mit R8, lokal mit dem Debug-Schlüssel signiert: Er startet ohne Absturz und zeigt den Offline-Zustand.

**Nicht geprüft:** TalkBack, Switch Access und Voice Access auf einem Gerät; die Zwei-Spalten-Ansicht mit offenem Detail; Benachrichtigungen, Sperre und Lade-Zustand sind nur im Code durchgesehen. Der Durchgang mit TalkBack ist für #35 noch zu machen, sobald ein Server zum Anmelden bereitsteht.

## Platzhalter aus der Übernahme

- **Logo:** eine neutrale Flamme (`ic_logo`, `ic_splash_logo`, `ic_stage_fallback`, `ic_stat_logo`, `ic_launcher_foreground`). Das echte Zeichen und das Wappen des Kantons Zürich ersetzen sie; das Design liefert beide als Platzhalter.
- **Begriffe:** Einheit (mit Bezirk), Kommando, Teilnehmende und Termin; in der Oberfläche heisst ein Termin «Übung» und das Kommando «Leitung». Die Namen in der Schnittstelle (`@SerialName`) sind die des Vertrags (`api-vertrag/openapi.yaml`): Einheit = `einheit`, Teilnehmende = `teilnehmende`, Termin = `programm`.

## Noch nicht umgesetzt

Oberfläche und Komponenten sind nach der Vorlage gebaut. Der Vertrag (E4) ist übernommen: Profil, Termine, Anmeldungen, Kontaktperson, Abzeichen (Sterne), Info und Aufgebot, «Anmeldung ab» und die Einwilligung mit Text vom Server sind gebaut. Offen ist, was ein Design oder eine Datei von aussen braucht:

- **Abzeichen als Bild:** Der Vertrag liefert ein Rang-Abzeichen entweder als Sterne (0 bis 5) oder als Bild (`bildUrl`, PNG oder SVG). `RankBadge` zeigt Sterne, aber nur 0 bis 2 (die Platzhalter-Bilder reichen nicht weiter); die Standard-Ränge der Einheiten gehen bis 5, Ränge 3 bis 5 zeigt die App darum nicht; Bild-Abzeichen zeigt die App noch nicht, weil sie SVG nicht ohne Zusatzbibliothek lesen kann und ein Original-Abzeichen fehlt.
- **Einheitslogo:** Der Vertrag liefert es jetzt (`logoUrl` in `/ich`, SVG oder PNG). Der Entwurf kennt kein Logo je Einheit, und SVG liest die App nicht ohne Zusatzbibliothek: Es bleibt aus, bis das Design es vorsieht.
- **Datenschutz** als Screen mit Abschnittskarten: Bis dahin öffnet die App die Seite des Servers im Browser; die Einwilligung selbst steht im Willkommen.
- **Kennzeichnung der Einheit in der Liste** und Push-Einstellungen im Profil (Design folgt), Info-Seite «Konto aufs Kind übertragen» (Design folgt).
- **Wappen und Rangabzeichen** sind Platzhalter nach dem Entwurf; das offizielle Wappen mit Nutzungsrecht und das Original der Jugendfeuerwehr als Vektor ersetzen sie.

## Abweichungen von der Vorlage

Wo die Vorlage dem Auftrag oder dem Plan widerspricht, gilt der Auftrag:

1. **Mehrere Einheiten pro Konto.** Die Vorlage geht von genau einer Einheit pro Konto aus. Ein Konto kann mehrere haben; die Übungsliste zeigt alle und kennzeichnet die Einheit, das Profil nennt sie in einer Info-Zeile.
2. **Push.** Die Vorlage zeigt Benachrichtigungen als «Kommt bald». Push ist Teil von V1; das Profil führt zu den Schaltern.
3. **Anwesenheit.** Die Vorlage führt «Anwesenheit» in der Verwaltung. Die App erfasst keine Anwesenheit.
4. **Schreibweise.** Die Vorlage schreibt «Fire-Response». Die App heisst «FireResponse».

## Weitere Entscheide

- **Passwort wiederholen:** Die Vorlage hat zwei Felder (neu und wiederholen) mit der Regel «Stimmt überein»; die App folgt ihr. «Passwort anzeigen» bleibt daneben.
- **Bildschirme nach den Entwürfen:** Login, Passwort, Willkommen, Liste, Detail, Profil und «Keine Einheit» sind gegen die Entwurfsdateien (`L1` bis `L13`, `S4`, `S9`) abgeglichen: echte Flamme und Zürcher Wappen, Titel 34/36 und Begrüssung 40/44, Inter 400 bis 800, Kontaktkarte mit Anrufen-Knopf, Zusage mit Initialen, Kalender-Knopf im Detail. Die Galerie `GalleryActivity` (nur Debug-Build, Beispieldaten) zeigt sie ohne Server: `adb shell am start -S -n ch.fireresponse.app/.GalleryActivity --es screen list`.
- **Kein Logo je Einheit:** Die Vorlage kennt es nicht mehr. Die Einheit steht als Text; Logos, ihr Laden und der Fallback entfallen.
- **Keine Einheit:** Ein Konto ohne Rolle, Einheit und Teilnehmende zeigt den Zustand «Keine Einheit» mit dem Weg zurück zum Login.
- **Kein Ort, keine Zuordnung:** Der Vertrag kennt keinen Treffpunkt, keinen Schlussort und kein «Mitnehmen» mehr, dafür Thema, Info und Aufgebot (Text, wie der Server ihn liefert). Termine ordnet die App keiner Einheit zu: Das geschieht im Admin-Bereich, die Seite «Einheiten und Sichtbarkeit» entfällt.
- **Termine ohne Anmeldung:** Gibt die Einheit die Art eines Termins (Sitzung, Übung …) nicht frei, ist `anmeldbar` falsch. Die App zeigt ihn dann nur mit Zeit und Titel: ohne Antworten, ohne «Anmeldung ab <Datum>» und ohne «Anmeldungen ansehen». Die Art selbst zeigt sie nicht.
