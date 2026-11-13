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
| Zustände | `ui/States.kt`: Laden (Skelett), Leer, Offline, Fehler, Nicht gefunden, Kein Zugriff, Sitzung abgelaufen. Das Skelett pulsiert ruhig (Deckkraft 1 auf 0.45, 900 ms, `Modifier.pulse()` in `ui/Loading.kt`); das Design legt nur die Fläche fest, die Bewegung ist eine Ergänzung. Sind die Animationen im System aus (auch durch den Energiesparmodus), steht alles still, auch der Ladekreis (Checkliste 5.9) |
| Widget «Nächste Übungen» (4 × 2 Felder) | `widget/AppointmentWidget.kt` (Provider, Zeilen, Chips, Logos, Tippziel), `widget/WidgetModel.kt` (welche Übungen, Personen mit Stand), `res/layout/widget_appointments.xml`, `widget_chip_name.xml`, `widget_chip_dot.xml`, `res/xml/appointment_widget_info.xml`, Farben `widget_*`. **Vorlage**: «Android-Widget 2×4» (Tag, 2 und 3 Kinder; #92); die Nacht ist aus den Tokens des Design Systems abgeleitet, nicht gezeichnet. Kopfzeile mit Flamme, je Übung Titel, Einheit, «MI 18.11. · 19:00», «x von y» und je Person ein Chip mit Namen (bei einer Person bis 14, bei zwei Personen zusammen bis 10 Buchstaben) oder ein Kreis mit Initiale und Zeichen für den Stand (sonst, also auch ab drei Personen; höchstens fünf; die eigene Anmeldung als «Du»). **Das Logo der Einheit ersetzt das Kürzel JF/KF**, sobald es geladen ist (Prozess-Speicher, 3 Sekunden Zeit, 404 wird gemerkt); sonst steht das Kürzel da. **Zeigt die Vornamen der eigenen Kinder auf dem Startbildschirm**, wie die Vorlage es zeigt (Datenschutz im PR benannt). Vor dem Anmeldefenster steht «ab 5.12.» statt der Chips. Nur Ansehen; ein Tipp öffnet die App bei der Übung auf demselben Weg wie ein Push (`typ`, `id`, `PushTarget`). Aktualisiert alle 30 Minuten (Systemvorgabe), beim Start der App (im Energiesparmodus sperrt das System dem Widget im Hintergrund das Netz; dann steht «Energiesparmodus: Tippe hier …»), wenn die App ihre Liste lädt und beim Abmelden. Ohne neue Abhängigkeit (`RemoteViews`), keine Daten auf der Platte. Debug-Vorschau: `GalleryActivity --es screen widget`, `widget1`, `widget3`, `widgetlogo` |
| Hochformat | `AndroidManifest.xml`: `screenOrientation="portrait"`, die App dreht sich nicht. Ab Android 16 ignoriert das System die Sperre auf Bildschirmen ab 600 dp (Tablets, aufgeklappte Foldables); dort gelten `ui/TwoPane.kt` (Liste und Detail nebeneinander, Dock links) und `ui/ContentWidth.kt` (Text höchstens 640 dp) |
| Profil | `auth/ProfileScreen.kt`: Profilbild, Benachrichtigungen, Datenschutz, Biometrie, Teilnehmende, Einheiten (je Einheit das Logo, sonst «JF» oder «KF», und der Name; Auftrag des Auftraggebers #104), Abmelden |
| Profilbild zuschneiden | `auth/CropImageScreen.kt` (Bildschirm), `images/ProfileImageEncoder.kt` (`Crop`, `squareCrop`, `preview`). **Ohne Vorlage im Design**: Wunsch des Auftraggebers (#98). Nach der Auswahl im Systemdialog steht das Foto in einem Quadrat mit Kreis als Hilfslinie (das Profilbild ist rund); verschieben mit einem Finger, zoomen mit dem Regler «Zoom» (1- bis 4-fach; die Zwei-Finger-Geste geht auch). Für den Screenreader hat das Foto die Aktionen «Ausschnitt nach links/rechts/oben/unten», der Regler ist einstellbar. Der Bildschirm hält die Ränder des Systems ein (`safeDrawingPadding`) und scrollt bei grosser Schrift. «Übernehmen» lädt den Ausschnitt hoch (JPEG, 512 Pixel, nach EXIF gedreht, ohne Metadaten), der Zurück-Pfeil bricht ab. Ohne neue Abhängigkeit. Debug-Vorschau: `GalleryActivity --es screen crop` |

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

- **Logo:** eine neutrale Flamme (`ic_logo`, `ic_splash_logo`, `ic_stage_fallback`, `ic_stat_logo`). Das App-Symbol (`mipmap-*/ic_launcher*`, Adaptive Icon mit Themed Icon) ist das vom Design gelieferte; Splash und Mitteilungs-Symbol folgen. Das echte Zeichen und das Wappen des Kantons Zürich ersetzen sie; das Design liefert beide als Platzhalter.
- **Begriffe:** Einheit (mit Bezirk), Kommando, Teilnehmende und Termin; in der Oberfläche heisst ein Termin «Übung» und das Kommando «Leitung». Die Namen in der Schnittstelle (`@SerialName`) sind die des Vertrags (`api-vertrag/openapi.yaml`): Einheit = `einheit`, Teilnehmende = `teilnehmende`, Termin = `programm`.

## Noch nicht umgesetzt

Oberfläche und Komponenten sind nach der Vorlage gebaut. Der Vertrag (E4) ist übernommen: Profil, Termine, Anmeldungen, Kontaktperson, Abzeichen (Sterne), Info und Aufgebot, «Anmeldung ab» und die Einwilligung mit Text vom Server sind gebaut. Offen ist, was ein Design oder eine Datei von aussen braucht:

- **Abzeichen als Bild:** Der Vertrag liefert ein Rang-Abzeichen mit Kürzel (`kuerzel`, immer da) und entweder Sternen (0 bis 2) oder einem Bild (`bildUrl`, PNG oder SVG), oder mit beidem leer. `RankMark` zeigt immer ein Bild: bei 0 bis 2 Sternen das mitgelieferte (`RankBadge`), sonst das Bild vom Server. Es kommt über `GET /api/v1/raenge/{id}/abzeichen` mit der Sitzung (`images/RankImages.kt`: nur diese Adresse, höchstens 512 KB, nur im Arbeitsspeicher der Sitzung, ein PNG wird beim Lesen verkleinert). Ein SVG zeichnet AndroidSVG 1.4 einmal in ein Bitmap von 156 Pixeln Höhe, ohne Entitäten; der Server hat es schon bereinigt. Das Bild ist 100:130 hoch, hat keinen Rand und trägt «Rang <Kürzel>» für den Screenreader. Während des Ladens bleibt der Platz frei. Nur wenn es kein Bild gibt oder es nicht lädt, steht das Kürzel als kleiner Text da. Das gilt bei Kontaktperson, Zusagen, Profil, Willkommen und Anmeldungen; nicht in der Übungsliste. Das Design System sagt «nur ein Bild, kein Rangtext»; der Text bleibt nur als Ersatz.
- **Logo der Einheit auf der Übungskarte:** Entscheid des Auftraggebers (#16): Oben rechts auf jeder Karte der Liste steht das Logo der Einheit, ohne Logo «JF» oder «KF» im Kreis. Es kommt über `GET /api/v1/einheiten/{id}/logo` (SVG oder PNG, `images/RankImages.kt`, die Adresse baut die App aus der Kennung der Einheit des Termins, auch für Eltern; 404 heisst: ohne Logo). `SquadMark` in `ui/Brand.kt`; der Name der Einheit steht im Satz, den TalkBack für die Karte liest. Das Design System kennt kein Logo je Einheit (README: «Ein Logo je Einheit gibt es nicht mehr»); der Entscheid geht vor, die Grösse (40 dp) und der Platzhalter sind eine Annahme, bis das Design sie festlegt. Das Logo steht nur in der Liste, nicht im Detail.
- **Push-Einstellungen im Profil** sind abgenommen (#28). Die Info-Seite «Konto aufs Kind übertragen» gibt es in der App nicht: Das Übertragen geschieht nur im Admin-Panel (Entscheid des Auftraggebers, #24).
- **Wappen und Rangabzeichen** sind Platzhalter nach dem Entwurf; das offizielle Wappen mit Nutzungsrecht und das Original der Jugendfeuerwehr als Vektor ersetzen sie.

## Abweichungen von der Vorlage

Wo die Vorlage dem Auftrag oder dem Plan widerspricht, gilt der Auftrag:

1. **Mehrere Einheiten pro Konto.** Die Vorlage geht von genau einer Einheit pro Konto aus. Ein Konto kann mehrere haben; die Übungsliste zeigt alle und kennzeichnet die Einheit, das Profil nennt sie in einer Info-Zeile.
2. **Push.** Die Vorlage zeigt Benachrichtigungen als «Kommt bald». Push ist Teil von V1; das Profil führt zu den Schaltern.
3. **Anwesenheit.** Die Vorlage führt «Anwesenheit» in der Verwaltung. Die App erfasst keine Anwesenheit.
4. **Schreibweise.** Die Vorlage schreibt «Fire-Response». Die App heisst «FireResponse».
5. **Aktualisieren der Übungsliste.** Die Vorlage hat einen Aktualisieren-Knopf im Kopf der Liste. Auftrag des Auftraggebers (#102): Die Liste lädt durch Ziehen nach unten neu, der Knopf entfällt. Der Screenreader bekommt die Aktion «Aktualisieren» am Feld der Liste; ein Fehler beim Neuladen steht als Hinweis über der Liste, die alten Übungen bleiben. Leere Liste und Fehlerseite behalten «Nochmals versuchen».
6. **Kopf ohne Knöpfe.** Die Vorlage reserviert über dem Titel immer eine Zeile für Zurück und Aktion. Auftrag des Auftraggebers (#106): Ohne Knopf entfällt die Zeile, der Titel von Übungen und Profil steht direkt unter der Statusleiste. Bildschirme mit Zurück oder Aktion behalten sie.

## Weitere Entscheide

- **Passwort wiederholen:** Die Vorlage hat zwei Felder (neu und wiederholen) mit der Regel «Stimmt überein»; die App folgt ihr. «Passwort anzeigen» bleibt daneben.
- **Bildschirme nach den Entwürfen:** Login, Passwort, Willkommen, Liste, Detail, Profil und «Keine Einheit» sind gegen die Entwurfsdateien (`L1` bis `L13`, `S4`, `S9`) abgeglichen: echte Flamme und Zürcher Wappen, Titel 34/36 und Begrüssung 40/44, Inter 400 bis 800, Kontaktkarte mit Anrufen-Knopf, Zusage mit Initialen, Kalender-Knopf im Detail. Die Galerie `GalleryActivity` (nur Debug-Build, Beispieldaten) zeigt sie ohne Server: `adb shell am start -S -n ch.fireresponse.app/.GalleryActivity --es screen list`.
- **Logo der Einheit:** Die Vorlage kannte keins; auf der Übungskarte steht es seit #16 oben rechts (siehe «Noch nicht umgesetzt», Abschnitt davor). Sonst steht die Einheit als Text, im Profil als Info-Zeile.
- **Keine Einheit:** Ein Konto ohne Rolle, Einheit und Teilnehmende zeigt den Zustand «Keine Einheit» mit dem Weg zurück zum Login.
- **Kein Ort, keine Zuordnung:** Der Vertrag kennt keinen Treffpunkt, keinen Schlussort und kein «Mitnehmen» mehr, dafür Thema, Info und Aufgebot (Text, wie der Server ihn liefert). Termine ordnet die App keiner Einheit zu: Das geschieht im Admin-Bereich, die Seite «Einheiten und Sichtbarkeit» entfällt.
- **Jeder Termin hat eine Anmeldung:** Seit API-PR #194 ist `anmeldbar` immer `true` und `KEINE_ANMELDUNG` entfällt; die App liest das Feld nicht mehr. Welche Termine Eltern und Teilnehmende sehen, entscheidet der Server (Termine «nur Leitende» fehlen dort in Liste und Detail). Die Art des Termins zeigt die App nicht.
- **Leiter-Plätze mit Warteliste (B16–B33/L16–L33):** Nur Kommando und Leitung bekommen `leitungPlaetze` (Höchstzahl, belegt, frei, voll, wartende, eigene Position); ohne den Schlüssel bleibt alles wie vorher. Karte: Etikett «Noch 2 Plätze frei» / «Voll» **unter** dem Stand (der Entwurf setzt es rechts oben in die Zeitzeile; dort sitzt bei uns das Logo der Einheit), bei eigener Warteliste «Warteliste, Platz 2». Detail: Karte `PlacesCard` («2 von 4 belegt», belegte Plätze mit Profilbild und «Lea M.» aus `GET …/teilnahmen`, freie gestrichelt); bei «Voll» statt der Pillen der Block `WaitlistBlock` mit «Auf die Warteliste setzen», wer wartet «Du stehst auf der Warteliste» und «Von der Warteliste nehmen»; Absage mit Platz nur nach Bestätigung (Dialog). `PUT …/leitung` antwortet 200 mit `{status, position}` (204 älterer Server gilt als gewählter Stand); kommt «warteliste» trotz freiem Platz, steht «Der letzte Platz ist soeben vergeben worden …». Nachgerückt (beim Neulesen von «warteliste» auf «ja»): schliessbarer Hinweis. Anmeldungen: Wartende mit «Warteliste, Platz n» und Zähler. Widget: «Voll» bzw. «Warteliste» statt «x von y». Debug-Vorschau: `GalleryActivity --es screen placesfree|placesfull|placeswait`.
- **Wartung (S10/T10):** Antwortet der Server mit 503 `WARTUNG` (auch bei der Anmeldung), liegt der Bildschirm «Wartungsarbeiten» über der ganzen App (`maintenance/MaintenanceScreen.kt`, Zustand in `ApiClient.maintenance`). Text und Ende kommen von `GET /api/v1/wartung`, ohne Text steht der Standardtext, dazu «Deine Zusagen bleiben gespeichert.». «Erneut prüfen» fragt `GET /api/v1/wartung`; bei `aktiv: false` startet die App neu (`AuthViewModel.start`), sonst steht «Noch in Wartung.»; keine Dauerabfrage. Ein 503 ohne den Code bleibt ein gewöhnlicher Fehler. Das Widget zeigt den letzten Stand. Debug-Vorschau: `GalleryActivity --es screen maintenance`.
- **Infokanal (ohne Entwurf):** Zu Nr. 30 gibt es im Artifact «Screens Android» noch keine Boards; die Oberfläche folgt den schriftlichen Anforderungen mit den vorhandenen Bausteinen und ist in der Optik vorläufig (nach dem Board anpassen). Reiter «Infokanal» im Dock (Reihenfolge Übungen, Infokanal, Profil), nur mit mindestens einem Kanal; Zähler am Reiter ist die Summe des Ungelesenen (liest im Takt von 30 Sekunden still neu). Eine Einheit: der Reiter öffnet den Verlauf direkt; mehrere: Liste mit Logo, Zähler und Zeit der letzten Mitteilung. Verlauf: neueste unten, Tagestrenner («Heute», «Gestern», Datum), ältere Seiten laden beim Hochscrollen; Eingabe nur mit `darfSchreiben` (sonst «Nur Kommando und Leitung schreiben hier.»); Bearbeiten und Löschen nur, wo `darfBearbeiten`/`darfLoeschen` es sagen (Löschen nach Bestätigung, gelöscht = «Nachricht gelöscht»); Links (nur http/https) antippbar; die Eingabe leert sich erst nach Erfolg; «gelesen» geht an den Server, sobald der Verlauf offen ist. Push: Kanal `infokanal` (neu), `typ: infokanal` öffnet den Verlauf der Einheit; Schalter «Infokanal» in den Benachrichtigungen (nur mit Kanal). TalkBack: «Infokanal …, 3 ungelesen», je Mitteilung ein Satz. Debug-Vorschau: `GalleryActivity --es screen infolist|info|inforeader|infoempty`.
- **Infokanal, Rückmeldung des Auftraggebers (13.11.2026):** Liste, Verlauf und leerer Verlauf laden durch Ziehen neu (wie die Übungsliste), es gibt keinen Aktualisieren-Knopf und keinen Knopf «Ältere Mitteilungen laden»; der Screenreader hat die Aktion «Aktualisieren» am Feld. Die Eingabe ist eine Zeile wie im Chat: abgerundetes Feld (wächst bis fünf Zeilen) mit «Mitteilung schreiben», rechts ein runder Senden-Knopf; beim Bearbeiten eine schmale Zeile darüber mit Abbrechen; Zähler erst ab 1800 Zeichen. **Das Dock hat in jeder Ansicht dieselben Reiter** (Übungen, Infokanal mit Zähler, Profil): Detail, Anmeldungen und Datenschutz bekamen zuerst das alte Dock mit zwei Reitern.
- **Infokanal: Logo der Einheit im Hintergrund (Wunsch des Auftraggebers, 13.11.2026):** Im Verlauf und im leeren Verlauf steht das Logo gross (280 dp), mittig, weichgezeichnet (18 dp, ab Android 12) und blass (12 % Deckkraft, im Dunkelmodus 25 %, weil das Logo meist dunkel ist) hinter den Mitteilungen (`SquadWatermark`); ohne Logo steht nichts da, kein JF/KF-Ersatz. Dekoration, der Screenreader überspringt es.
- **Lade-Skelette des Infokanals:** Der Verlauf lädt mit einem Chat-Skelett (`ChatSkeleton`: Tagestrenner, Reihen aus rundem Bild, Name und Textzeilen, unten ausgerichtet wie die Mitteilungen), die Liste der Infokanäle mit `CardListSkeleton` (Karten mit rundem Logo). Beide teilen mit `LoadingSkeleton` den Rahmen `SkeletonFrame` (Pulsieren, Ansage erst nach einer Sekunde). Debug-Vorschau: `GalleryActivity --es screen infoload|infolistload`.
