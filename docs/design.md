# Design

Dieses Dokument hält fest, wie die App das Design des Auftraggebers umsetzt und wo sie davon abweicht. Es wächst mit A5 (Design und Barrierefreiheit, #29) und wird mit dem TalkBack-Durchgang abgeschlossen (#35).

## Vorlagen

| Vorlage | Stand |
|---|---|
| Design System «FireResponse»: Farben, Schrift, Abstände, Bausteine | liegt beim Auftraggeber, die Tokens kommen mit #30 |
| Bildschirme «FireResponse Screens»: Nacht und Tag, alle Zustände | liegt beim Auftraggeber, die Umsetzung folgt mit #32 und #33 |

## Platzhalter aus der Übernahme

Der Stand stammt aus einer früheren App. Bis die Vorlagen umgesetzt sind, gelten diese Platzhalter:

- **Logo:** eine neutrale Flamme (`ic_logo`, `ic_splash_logo`, `ic_stage_fallback`, `ic_stat_logo`, `ic_launcher_foreground`). Das echte Logo ersetzt sie.
- **Farben und Schrift:** die Tokens in `ui/theme/` sind noch die der früheren App. Sie werden mit #30 und #31 ersetzt.
- **Begriffe:** Einheit (mit Bezirk), Kommando, Teilnehmende und Termin; in der Oberfläche heisst ein Termin «Übung» und das Kommando «Leitung». Die Namen in der Schnittstelle (`@SerialName`) bleiben, bis der neue Vertrag (E4) kopiert wird.

## Abweichungen von der Vorlage

Wo die Vorlage dem Auftrag oder dem Plan widerspricht, gilt der Auftrag. Das betrifft vier Stellen:

1. **Mehrere Einheiten pro Konto.** Die Vorlage geht von einer Einheit pro Konto aus. Ein Konto kann mehrere haben; die Übungsliste zeigt alle und kennzeichnet die Einheit.
2. **Push.** Die Vorlage zeigt Push als «bald verfügbar». Push ist Teil von V1.
3. **Anwesenheit.** Die Vorlage führt «Anwesenheit» in der Verwaltung. Die App erfasst keine Anwesenheit.
4. **Schreibweise.** Die Vorlage schreibt «Fire-Response». Die App heisst «FireResponse».
