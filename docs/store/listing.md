# Play-Store-Eintrag (Entwurf)

Entwurf für #39. Die Texte stammen aus dem, was die App tatsächlich tut (Stand: Version 0.9.0). Was offen ist und von der verantwortlichen Person kommen muss, steht unten unter «Offene Punkte». Die Data-Safety-Angaben bitte vor dem Eintragen gegen die Datenschutzerklärung des Servers (`GET /datenschutz`) prüfen: Beide müssen übereinstimmen.

## Texte (Deutsch, Schweiz)

**App-Name** (höchstens 30 Zeichen): `FireResponse`

**Kurzbeschreibung** (höchstens 80 Zeichen, 76 gezählt):

> Übungen der Jugend- und Kinderfeuerwehr ansehen und mit einem Tipp zusagen.

**Beschreibung** (höchstens 4000 Zeichen):

> FireResponse ist die App der Jugend- und Kinderfeuerwehren im Kanton Zürich. Eltern, Teilnehmende und Leitende sehen alle Übungen ihrer Einheit auf einen Blick und sagen mit einem Tipp Ja oder Nein.
>
> Das kann die App:
>
> • Alle Übungen deiner Einheit (oder mehrerer Einheiten) in einer Liste, mit Thema, Aufgebot und Info
> • Zusagen und Absagen für jedes Kind oder für dich selbst, sobald die Anmeldung offen ist
> • Die Kontaktperson der Einheit mit Telefonnummer: ein Tipp zum Anrufen
> • Für Kommando und Leitende: wer sich angemeldet hat, mit Zahlen und Listen
> • Mitteilungen bei neuen, abgesagten oder geänderten Übungen und Erinnerungen, ohne Namen und ohne den Inhalt einer Nachricht
> • Entsperren mit Fingerabdruck oder Gesicht, wenn du das möchtest
> • Hell und dunkel, die Schrift folgt der Einstellung deines Geräts
>
> Das Konto bekommst du von der Leitung deiner Einheit (Benutzername und Startpasswort). Ohne Konto lässt sich die App nicht nutzen.
>
> Keine Werbung, keine Tracker.

## Data Safety (Angaben zur Datensicherheit)

Grundlage: Vertrag der API (`api-vertrag/openapi.yaml`) und der Code der App. Die App hat keine Analyse-, Absturzbericht- oder Werbe-Bibliothek; die einzige Firebase-Komponente ist Cloud Messaging für Mitteilungen.

| Datenart (Play) | Erhoben | Zweck | Wer sieht es | Hinweis |
| --- | --- | --- | --- | --- |
| Name | Ja | App-Funktionen, Kontoverwaltung | Leitung und Kommando der Einheit | Vom Konto, nicht von der App erfasst |
| Benutzername / Nutzer-ID | Ja | Kontoverwaltung, Anmeldung | Leitung und Kommando | |
| Telefonnummer | Ja, nur Kommando und Stellvertretung, freiwillig | App-Funktionen (Kontakt) | Eltern und Teilnehmende der Einheit | Wird nur für die Kontaktperson gezeigt |
| Fotos | Ja, freiwillig (Profilbild) | App-Funktionen | Wer die Person mit Namen sieht | Server verkleinert und speichert nur das Ergebnis |
| Aktivität in der App (Zu- und Absagen) | Ja | App-Funktionen | Leitung und Kommando | |
| Geräte- oder andere IDs (Push-Token) | Ja | Mitteilungen | Nur der Server | Token von Firebase Cloud Messaging (Google) |
| Standort, Kontakte, Nachrichten, Dateien, Gesundheit, Zahlungen, Browserverlauf, Audio | Nein | | | |

- **Daten werden verschlüsselt übertragen:** Ja. Die App spricht nur HTTPS (`cleartextTrafficPermitted="false"`).
- **Nutzer können die Löschung verlangen:** Ja, auf Anfrage an das Kommando oder an die Kontaktadresse des Servers. Die Person löscht ihr Konto nicht selbst, und die App bietet keine Löschung an; die Verwaltung löscht das Konto. Die öffentliche Seite `/konto-loeschen` des Servers beschreibt den Weg und sagt, was gelöscht wird und was bleibt (sie ist für die App-Stores gedacht). Ob Play für diesen Fall eine Löschung in der App verlangt, ist unter «Offene Punkte» aufgeführt.
- **Weitergabe an Dritte:** Das Push-Token geht an Google (Firebase Cloud Messaging), damit Mitteilungen ankommen. Play wertet das als Verarbeitung im Auftrag, nicht als Weitergabe, solange Google die Daten nur dafür nutzt; bitte im Formular so beantworten und mit der Datenschutzerklärung abgleichen.
- **Pflicht zur Angabe:** Konto, Name und Zu-/Absagen sind für die Funktion nötig; Profilbild und Telefonnummer sind freiwillig.

## Berechtigungen (so erklärt der Eintrag sie)

| Berechtigung | Wofür |
| --- | --- |
| Internet (`INTERNET`, `ACCESS_NETWORK_STATE`) | Übungen und Zusagen laden und speichern |
| Mitteilungen (`POST_NOTIFICATIONS`) | Hinweise auf neue, abgesagte oder geänderte Übungen; die App fragt einmal, sonst im Profil |
| Biometrie (`USE_BIOMETRIC`, `USE_FINGERPRINT`) | Freiwilliges Entsperren der App mit Fingerabdruck oder Gesicht |
| Push-Empfang (`RECEIVE`, `WAKE_LOCK`) | Von Firebase Cloud Messaging vorgegeben |

Die App fragt nie nach Standort, Kontakten, Kamera oder Speicher. Ein Profilbild wählt die Person über die Foto-Auswahl des Systems (`PickVisualMedia`); die App bekommt nur das gewählte Bild.

## Technische Angaben

- Paket: `ch.fireresponse.app`, Version 0.9.0 (`versionCode` 900), `minSdk` 26, `targetSdk` 37
- Sprache: nur Deutsch
- Sicherung: `allowBackup="false"`, nichts geht in die Cloud-Sicherung oder auf ein neues Gerät
- App-Symbol (512 × 512): `docs/store/app-icon-512.png`

## Offene Punkte (brauchen Entscheide oder Material von aussen)

1. **Zielgruppe und Altersangabe.** Die Teilnehmenden sind Kinder; Eltern oder die Kinder selbst haben ein Konto (Konto-Wechsel Eltern → Kind). Gibt der Eintrag Kinder als Zielgruppe an, gelten die Play-Richtlinien für Familien und Kinder mit strengeren Regeln für Bibliotheken und Daten; was das für Firebase Cloud Messaging heisst, ist vor dem Eintragen in der aktuellen Richtlinie nachzulesen (hier nicht geprüft). Zu klären ist, ob die Zielgruppe Erwachsene sind (Eltern und Leitende) und wie die Konten der Kinder dazu passen. Das entscheidet die verantwortliche Person, nicht dieser Entwurf.
2. **Löschung des Kontos.** Die Person kann ihr Konto in der App nicht löschen (Konten legt die Verwaltung an). Play verlangt eine Löschung in der App für Apps, in denen sich Personen selbst ein Konto anlegen; ob das hier gilt, ist in der aktuellen Richtlinie nachzulesen (hier nicht geprüft).
3. **Adresse der Datenschutzerklärung** (öffentlich, `https://<Server>/datenschutz`) und der Seite zum Löschen (`https://<Server>/konto-loeschen`): hängen an der endgültigen Server-Adresse.
4. **Entwickler- und Kontaktangaben** (Name der Organisation, E-Mail, Adresse): von der verantwortlichen Stelle.
5. **Kategorie und Inhaltseinstufung** (Fragebogen): vorschlagen lässt sich «Produktivität» oder «Soziales»; beides ist eine Wahl der Organisation.
6. **Grafiken.** Fünf Telefon-Screenshots liegen als Entwurf in `docs/store/screenshots/` (1080 × 2160, Seitenverhältnis 1:2, Beispieldaten aus der Galerie, Statusleiste 09:41 ohne Mitteilungen): Übungen, Übung, Anmeldungen, Profil, Übung im Nachtmodus. Sie müssen neu aufgenommen werden, sobald das Original-Rangabzeichen und das offizielle Wappen eingebaut sind (heute Platzhalter) und der Entscheid zum Kürzel am Rang steht. Aufnahme: `adb shell wm size 1080x2160`, `GalleryActivity` mit `--es screen list|detail|registrations|profile`, Demo-Modus der Statusleiste. Die Feature-Grafik (1024 × 500) braucht ein Design.
7. **Play-Konto:** Die Identitätsprüfung läuft noch (A6, #36); bis dahin gilt `docs/release.md` (APK über GitHub).
