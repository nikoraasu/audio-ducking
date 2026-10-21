package io.github.nikoraasu.audioducking

object GermanStrings : Strings {
    override val decimalSeparator = ','

    override val notListening = "Hört nicht zu"
    override val notListeningDetail = "Starten, um die Lautstärke zu senken oder zu erhöhen, wenn jemand spricht."
    override val listening = "Hört zu"
    override val waitingForSpeech = "Wartet, bis jemand spricht…"
    override val ducking = "Leiser gestellt"
    override val boosting = "Lauter gestellt"
    override val paused = "Pausiert"
    override fun playbackPaused(who: String) = "$who, Wiedergabe pausiert."
    override val stopped = "Gestoppt"
    override fun isTalking(name: String?) = if (name != null) "$name spricht" else "Jemand spricht"
    override fun volumeLowered(who: String) = "$who, Lautstärke gesenkt."
    override fun volumeRaised(who: String) = "$who, Lautstärke erhöht."
    override fun carBoost(percent: Int) = "Automodus: +$percent % Lautstärke"
    override fun ignoringVoice(score: String) = "Unbekannte Stimme wird ignoriert (Übereinstimmung $score)."
    override fun withMatch(who: String, score: String) = "$who (Übereinstimmung $score)"
    override val startListening = "Zuhören starten"
    override val stopListening = "Zuhören beenden"
    override fun error(error: MonitorError) = when (error) {
        MonitorError.NO_PERMISSION -> "Die Mikrofonberechtigung fehlt."
        MonitorError.MIC_UNAVAILABLE -> "Das Mikrofon konnte nicht geöffnet werden."
        MonitorError.STOPPED_UNEXPECTEDLY -> "Das Zuhören wurde unerwartet beendet."
    }

    override val mode = "Modus"
    override val duck = "Leiser"
    override val noise = "Lauter"
    override val duckDescription = "Leiser, während jemand spricht, damit du ihn hörst."
    override val noiseDescription = "Lauter, während jemand spricht, damit du weiter zuhören kannst."
    override val podcast = "Podcast"
    override val podcastDescription = "Pausiert, während jemand spricht, und spielt danach ein paar Sekunden früher weiter, damit du nichts verpasst."
    override val rewind = "Danach zurückspringen"
    override val rewindInfo =
        "Wie weit die Wiedergabe nach dem Gespräch zurückspringt. Über Lautsprecher kann die App den Podcast selbst hören und dafür pausieren; " +
            "Kopfhörer oder „Nur registrierte Stimmen“ verhindern das."
    override val mediaAccessNeeded =
        "Zum Zurückspringen braucht die App Zugriff auf die Medienwiedergabe (Benachrichtigungszugriff). Ohne ihn pausiert und spielt sie nur."
    override val allowMediaAccess = "Erlauben"
    override val volumeWhileTalking = "Lautstärke beim Sprechen"
    override val duckLevelInfo =
        "Wie weit die Medienlautstärke sinkt, während jemand spricht. Sie wird dadurch nie erhöht."
    override val boostLevelInfo =
        "Wie weit die Medienlautstärke steigt, während jemand spricht. Sie wird dadurch nie gesenkt " +
            "und kehrt danach zum vorherigen Wert zurück."

    override val advanced = "Erweitert"
    override val expand = "Ausklappen"
    override val collapse = "Einklappen"
    override val speechHold = "Reaktionszeit"
    override val speechHoldInfo =
        "Wie lange jemand sprechen muss, bevor sich die Lautstärke ändert. Länger ignoriert kurze Wörter und Geräusche."
    override val allowedPause = "Erlaubte Pause"
    override val allowedPauseInfo =
        "Wie lange Stille dauern darf, bevor das Gespräch als beendet gilt und die Lautstärke zurückkehrt."
    override val speechThreshold = "Sprachschwelle"
    override val speechThresholdInfo =
        "Wie sicher der Detektor sein muss, dass ein Geräusch Sprache ist. Niedriger erkennt leise oder " +
            "entfernte Stimmen, reagiert aber eventuell auf andere Geräusche."

    override val voices = "Stimmen"
    override val voicesInfo =
        "Nimm die Personen auf, auf die die App reagieren soll. Mit „Nur registrierte Stimmen“ werden alle anderen, " +
            "Radio und Podcasts ignoriert. Das Erkennen des Sprechers braucht etwa 1,5 s Sprache."
    override val onlyRegisteredVoices = "Nur registrierte Stimmen"
    override val extraSamples = "Zusätzliche Aufnahmen mit Musik und Hall"
    override val extraSamplesInfo =
        "Beim Registrieren einer Stimme erstellt die App auch Kopien der Aufnahme mit Musik und mit Raumhall. Ist das an, werden Stimmen auch mit diesen Kopien verglichen: Bei lauter Musik oder aus größerer Entfernung wirst du öfter erkannt, Fremde kommen aber etwas öfter durch. Gilt für Stimmen, die ab jetzt registriert werden oder eine neue Aufnahme bekommen.\n\nMusik: „Hackbeat“ und „Volatile Reaction“ von Kevin MacLeod (incompetech.com), lizenziert unter Creative Commons: Namensnennung 4.0 (creativecommons.org/licenses/by/4.0)."
    override val matchStrictness = "Erkennungsstrenge"
    override val matchStrictnessInfo =
        "Wie ähnlich eine Stimme einer registrierten klingen muss. Höher lässt weniger Fremde durch, " +
            "kann aber eine registrierte Person im lauten Auto verpassen."
    override val noVoicesYet = "Noch keine Stimmen registriert, daher reagiert die App auf alle."
    override val outdatedVoices = "Mit dem vorherigen Erkennungsmodell aufgenommene Stimmen müssen neu registriert werden."
    override fun remove(name: String) = "$name entfernen"
    override val registerVoice = "Stimme registrieren"
    override val name = "Name"
    override fun recording(secondsLeft: Int) = "Aufnahme läuft, weitersprechen… $secondsLeft s"
    override val processing = "Aufnahme wird geprüft…"
    override fun enrollInstructions(seconds: Int) =
        "Tippe auf Aufnehmen und sprich während der ganzen Aufnahme in deinem normalen Tempo, wie im Gespräch mit jemandem. Nur diese Person sollte sprechen. Nimm dort auf, wo du die App normalerweise benutzt."
    override fun sampleCount(count: Int) = if (count == 1) "1 Aufnahme" else "$count Aufnahmen"
    override val addSample = "Aufnahme hinzufügen"
    override fun addSampleTitle(name: String) = "Weitere Aufnahme für $name"
    override val addSampleHint =
        "Nimm in einer anderen Situation als zuvor auf, z. B. im Auto von deinem Platz aus, mit Musik wie üblich. " +
            "Die App vergleicht eine Stimme mit der am besten passenden Aufnahme."
    override val enrollPassageNote =
        "Der Text unten ist nur eine Hilfe, damit du weißt, was du sagen kannst. Du musst ihn nicht Wort für Wort vorlesen, und wenn du früher fertig bist, sprich einfach weiter."
    override val enrollPassage =
        "Hallo, das ist meine Stimme. Am Samstagmorgen fuhren wir auf der ruhigen Küstenstraße nach " +
            "Norden, vorbei an Geschäften, Feldern und verschlafenen Dörfern. Im Radio lief Jazz, also " +
            "drehten wir die Musik leiser. Wie weit ist es noch bis zum See? Ungefähr sechzig Kilometer, " +
            "vielleicht weniger, wenn der Verkehr so ruhig bleibt. Lass uns zuerst einen Kaffee trinken und " +
            "frisches Brot holen, dann sehen wir weiter." +
            "\n\n" +
            "Am Nachmittag frischte der Wind auf, und vom Meer zogen dunkle Wolken heran. Wir packten die " +
            "Decken, die Thermoskanne und die alte Gitarre ein und liefen kurz vor dem Regen zurück zum " +
            "Auto. Auf dem Heimweg sprachen wir über alles, was wir gesehen hatten, lachten über den " +
            "verbrannten Toast vom Frühstück und versprachen uns, nächsten Sommer eine ganze Woche zu " +
            "bleiben."
    override val tooQuiet =
        "Die Aufnahme war zu leise. Halte das Gerät näher und sprich in normaler Lautstärke."
    override val inconsistentRecording =
        "Die Aufnahme klang nicht durchgehend nach einer Person, vielleicht hat jemand anderes oder das " +
            "Radio mitgesprochen. Versuche es an einem ruhigeren Ort noch einmal, während nur diese Person " +
            "spricht."
    override val record = "Aufnehmen"
    override val cancel = "Abbrechen"
    override val recordingFailed = "Die Aufnahme ist fehlgeschlagen."
    override fun tooLittleSpeech(seconds: Int) = "Nur $seconds s Sprache erkannt; sprich während der gesamten Aufnahme."
    override val micStopped = "Das Mikrofon wurde unterbrochen."
    override val micPermissionNeeded =
        "Mikrofonberechtigung nötig: Tippe einmal auf „Zuhören starten“, um sie zu erteilen."

    override val car = "Auto"
    override val carInfo =
        "Erhöht die Medienlautstärke, je schneller du fährst und je lauter es im Auto wird. " +
            "Das Zuhören muss trotzdem zuerst gestartet werden."
    override val off = "Aus"
    override val on = "An"
    override val carDescription = "Lauter, je schneller du fährst, wenn du beschleunigst und je lauter die Umgebung wird."
    override val speedSteps = "Geschwindigkeitsstufen"
    override val speedStepsInfo =
        "Zusätzliche Lautstärke ab der jeweiligen Geschwindigkeit. Du kannst beliebig viele Stufen hinzufügen. Benötigt die Standortberechtigung."
    override fun speedStepTitle(number: Int) = "Stufe $number"
    override val fromSpeed = "Ab"
    override val extraVolume = "Zusätzliche Lautstärke"
    override val addSpeedStep = "Stufe hinzufügen"
    override fun removeSpeedStep(number: Int) = "Stufe $number entfernen"
    override val smoothSteps = "Fließend zwischen Stufen"
    override val smoothStepsInfo =
        "Blendet die Lautstärke zwischen den Stufen über, sodass sie beim Schnellerwerden nach und nach steigt statt an jeder Stufe zu springen. Aus: Die Lautstärke ändert sich an jeder Stufe, und eine Stufe fällt erst 5 km/h darunter wieder weg."
    override val transitionTime = "Übergangszeit"
    override val transitionTimeInfo =
        "Wie lange der Automodus braucht, um die Lautstärke auf ein neues Niveau zu bringen, damit Änderungen sanft wirken. Das Leiserstellen bei Sprache bleibt sofort."
    override val accelerationBoost = "Beim Beschleunigen, bis"
    override val accelerationBoostInfo =
        "Zusätzliche Lautstärke beim Beschleunigen, umso mehr, je stärker du beschleunigst (der Motor wird lauter), und wieder weniger, sobald du gleichmäßig fährst. 0 % schaltet es aus. Benötigt die Standortberechtigung."
    override val noiseBoostUpTo = "Lärmausgleich, bis zu"
    override val noiseBoostInfo =
        "Zusätzliche Lautstärke, wenn es im Auto lauter wird. Die Grenze verhindert auch, dass die App die Lautsprecher des Autos hört und immer weiter aufdreht. Die Benachrichtigung zeigt, wie laut es ist."
    override val carModeAppliesNextStart = "Das Ein- oder Ausschalten des Automodus gilt ab dem nächsten Start des Zuhörens."

    override val setupGuide = "Einrichtungshilfe"
    override val setupGuideIntro =
        "Die Einrichtung dauert nur ein paar Minuten. Die Werte unten passen für die meisten; feinjustieren kannst du später."
    override val useRecommendedValues = "Empfohlene Werte verwenden"
    override val close = "Schließen"
    override fun setupSteps(phone: Boolean) = listOfNotNull(
        "Modus wählen" to "„Leiser“ macht die Musik leiser, während jemand spricht (35 % funktioniert gut). „Lauter“ macht sie lauter, damit du weiterhören kannst (80 %). „Podcast“ pausiert und spielt danach 5 s früher weiter.",
        "Zuhören starten" to "Tippe auf „Zuhören starten“ und erlaube das Mikrofon. Sag ein paar Sätze und prüfe, ob sich die Lautstärke ändert." + (if (phone) " Mit der Kachel in den Schnelleinstellungen startest du das Zuhören, ohne die App zu öffnen." else ""),
        "Stimmen registrieren (optional)" to "Wenn Radio, Podcasts oder andere Leute es auslösen, registriere die Personen, die zählen sollen, und schalte „Nur registrierte Stimmen“ ein. Nimm mit dem Handy dort auf, wo es normalerweise liegt. Füge dann eine zweite Aufnahme im Auto mit laufender Musik hinzu, damit du auch dort erkannt wirst.",
        "Übereinstimmung prüfen" to "Während eine registrierte Person spricht, zeigt der Status, wie gut die Stimme passt, z. B. 0,59. Lass die „Erkennungsstrenge“ wie sie ist. Wirst du nicht erkannt, stell sie etwas unter die niedrigste Zahl, die du siehst; kommen andere durch, erhöhe sie etwas.",
        ("Auto" to "Der Automodus dreht lauter, je schneller du fährst (ab 50, 70 und 90 km/h) und je lauter es im Auto wird (bis +20 %). Lass das Mikrofon auf „Assistent“, außer die App hört dich schlecht.").takeIf { phone },
        "Feinabstimmung" to "Unter „Erweitert“: Reaktionszeit 1,5 s, Erlaubte Pause 1,2 s, Sprachschwelle 0,25. Reagiert es auf kurze Geräusche? Reaktionszeit erhöhen. Kommt die Lautstärke zwischen Sätzen zurück? Erlaubte Pause erhöhen. Überhört es leise Stimmen? Sprachschwelle etwas senken."
    )

    override val ok = "OK"
    override fun about(topic: String) = "Info: $topic"
    override val language = "Sprache"
    override val microphone = "Mikrofon"
    override val microphoneInfo =
        "Mit welcher Mikrofoneinstellung des Handys zugehört wird. „Assistent“ nimmt Stimmen aus der Entfernung und aus jeder Richtung auf, wie ein Sprachassistent, und passt für die meisten. Probiere die anderen, wenn die App dich schlecht hört. Gilt ab dem nächsten Start des Zuhörens. Nach einer Änderung die Stimmen neu registrieren."
    override val micAssistant = "Assistent"
    override val micStandard = "Standard"
    override val micVideo = "Video"
    override val micRaw = "Roh"
    override val systemLanguage = "Systemsprache"
    override val addTile = "Kachel zu den Schnelleinstellungen hinzufügen"

    override val display = "Anzeige"
    override val floatingStatus = "Schwebender Status"
    override val floatingStatusInfo =
        "Während die App zuhört und nicht geöffnet ist, zeigt eine kleine Karte über anderen Apps (z. B. Navigation), was sie tut. " +
            "Zum Verschieben ziehen, zum Öffnen der App antippen."
    override val notificationListening = "Wartet auf Sprache"
    override fun notificationVoiceHeard(who: String) = "Stimme erkannt$who"
    override fun notificationLowered(who: String) = "Sprache erkannt$who: Lautstärke gesenkt"
    override fun notificationRaised(who: String) = "Sprache erkannt$who: Lautstärke erhöht"
    override fun notificationPaused(who: String) = "Sprache erkannt$who: pausiert"
    override fun notificationCar(kmh: Int, noiseDb: Int, boostPercent: Int) =
        "Auto: $kmh km/h, Lärm $noiseDb dB, +$boostPercent %"
    override val notificationChannelDescription = "Mikrofonüberwachung im Hintergrund"
    override val unknownVoice = "unbekannt"
}
