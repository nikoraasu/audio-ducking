package io.github.nikoraasu.audioducking

object PolishStrings : Strings {
    override val decimalSeparator = ','

    override val notListening = "Nie nasłuchuje"
    override val notListeningDetail = "Włącz, aby ściszać lub podgłaśniać dźwięk, gdy ktoś mówi."
    override val listening = "Nasłuchiwanie"
    override val waitingForSpeech = "Czekam, aż ktoś się odezwie…"
    override val ducking = "Ściszanie"
    override val boosting = "Podgłaśnianie"
    override val paused = "Wstrzymano"
    override fun playbackPaused(who: String) = "$who, odtwarzanie wstrzymane."
    override val stopped = "Zatrzymano"
    override fun isTalking(name: String?) = if (name != null) "$name mówi" else "Ktoś mówi"
    override fun volumeLowered(who: String) = "$who, głośność zmniejszona."
    override fun volumeRaised(who: String) = "$who, głośność zwiększona."
    override fun carBoost(percent: Int) = "Tryb samochodowy: +$percent% głośności"
    override fun ignoringVoice(score: String) = "Ignoruję niezarejestrowany głos (dopasowanie $score)."
    override fun withMatch(who: String, score: String) = "$who (dopasowanie $score)"
    override val startListening = "Rozpocznij nasłuchiwanie"
    override val stopListening = "Zatrzymaj nasłuchiwanie"
    override fun error(error: MonitorError) = when (error) {
        MonitorError.NO_PERMISSION -> "Brak uprawnienia do mikrofonu."
        MonitorError.MIC_UNAVAILABLE -> "Nie udało się otworzyć mikrofonu."
        MonitorError.STOPPED_UNEXPECTEDLY -> "Nasłuchiwanie nieoczekiwanie się zatrzymało."
    }

    override val mode = "Tryb"
    override val duck = "Ciszej"
    override val noise = "Głośniej"
    override val duckDescription = "Ciszej, gdy ktoś mówi, żeby było go słychać."
    override val noiseDescription = "Głośniej, gdy ktoś mówi, żeby nic Ci nie umknęło."
    override val podcast = "Podcast"
    override val podcastDescription = "Wstrzymuje, gdy ktoś mówi, a potem wznawia kilka sekund wcześniej, żeby nic Ci nie umknęło."
    override val rewind = "Cofnij po przerwie"
    override val rewindInfo =
        "O ile cofnąć odtwarzanie po zakończeniu rozmowy. Przy głośnikach aplikacja może usłyszeć sam podcast i go wstrzymać; " +
            "pomagają słuchawki lub „Tylko zarejestrowane głosy”."
    override val mediaAccessNeeded =
        "Do cofania aplikacja potrzebuje dostępu do odtwarzania (dostęp do powiadomień). Bez niego tylko wstrzymuje i wznawia."
    override val allowMediaAccess = "Zezwól"
    override val volumeWhileTalking = "Głośność podczas mowy"
    override val duckLevelInfo =
        "Do jakiego poziomu spada głośność multimediów, gdy ktoś mówi. To ustawienie nigdy jej nie podnosi."
    override val boostLevelInfo =
        "Do jakiego poziomu rośnie głośność multimediów, gdy ktoś mówi. To ustawienie nigdy jej nie obniża, " +
            "a gdy rozmowa się skończy, głośność wraca do poprzedniej."

    override val advanced = "Zaawansowane"
    override val expand = "Rozwiń"
    override val collapse = "Zwiń"
    override val speechHold = "Czas do reakcji"
    override val speechHoldInfo =
        "Jak długo ktoś musi mówić, zanim zmieni się głośność. Dłuższy czas pomija krótkie słowa i odgłosy."
    override val allowedPause = "Dozwolona przerwa"
    override val allowedPauseInfo =
        "Jak długo może trwać cisza, zanim rozmowa zostanie uznana za zakończoną i głośność wróci."
    override val speechThreshold = "Próg mowy"
    override val speechThresholdInfo =
        "Jak pewny musi być detektor, że dźwięk to mowa. Niższa wartość wyłapuje ciche lub odległe głosy, " +
            "ale może reagować na inne odgłosy."

    override val voices = "Głosy"
    override val voicesInfo =
        "Nagraj osoby, na które aplikacja ma reagować. Gdy włączone jest „Tylko zarejestrowane głosy”, " +
            "wszyscy inni, radio i podcasty są ignorowani. Rozpoznanie mówiącego zajmuje około 1,5 s mowy."
    override val onlyRegisteredVoices = "Tylko zarejestrowane głosy"
    override val extraSamples = "Dodatkowe próbki z muzyką i echem"
    override val extraSamplesInfo =
        "Przy rejestracji głosu aplikacja tworzy też kopie nagrania z dodaną muzyką i z echem pomieszczenia. Gdy to jest włączone, głosy są porównywane także z tymi kopiami: przy głośnej muzyce albo z większej odległości aplikacja częściej cię rozpozna, ale obce osoby będą przechodzić trochę częściej. Dotyczy głosów zarejestrowanych lub uzupełnionych o próbkę od teraz.\n\nMuzyka: „Hackbeat” i „Volatile Reaction”, Kevin MacLeod (incompetech.com), licencja Creative Commons: Uznanie autorstwa 4.0 (creativecommons.org/licenses/by/4.0)."
    override val matchStrictness = "Dokładność dopasowania"
    override val matchStrictnessInfo =
        "Jak bardzo głos musi przypominać zarejestrowany. Wyższa wartość przepuszcza mniej obcych osób, " +
            "ale w głośnym samochodzie może nie rozpoznać zarejestrowanej osoby."
    override val noVoicesYet = "Nie zarejestrowano jeszcze żadnego głosu, więc aplikacja reaguje na wszystkich."
    override val outdatedVoices = "Głosy nagrane poprzednim modelem rozpoznawania trzeba zarejestrować ponownie."
    override fun remove(name: String) = "Usuń: $name"
    override val registerVoice = "Zarejestruj głos"
    override val name = "Imię"
    override fun recording(secondsLeft: Int) = "Nagrywanie – mów dalej… $secondsLeft s"
    override val processing = "Sprawdzanie nagrania…"
    override fun enrollInstructions(seconds: Int) =
        "Naciśnij Nagraj i mów przez całe nagranie w swoim zwykłym tempie, jak w rozmowie z kimś. Powinna mówić tylko ta osoba. Nagrywaj z miejsca, z którego zwykle mówisz do urządzenia."
    override fun sampleCount(count: Int) = if (count == 1) "1 próbka" else "$count próbki"
    override val addSample = "Dodaj próbkę"
    override fun addSampleTitle(name: String) = "Nowa próbka: $name"
    override val addSampleHint =
        "Nagraj w innej sytuacji niż poprzednio, np. w samochodzie ze swojego miejsca, z muzyką grającą jak zwykle. " +
            "Aplikacja porównuje głos z najlepiej pasującą próbką."
    override val enrollPassageNote =
        "Poniższy tekst to tylko podpowiedź, co można powiedzieć. Nie trzeba czytać go słowo w słowo, a jeśli skończysz wcześniej, po prostu mów dalej."
    override val enrollPassage =
        "Cześć, to jest mój głos. W sobotę rano jechaliśmy na północ spokojną drogą wzdłuż wybrzeża, " +
            "mijając sklepy, pola i senne wioski. W radiu leciał jazz, więc ściszyliśmy muzykę. Jak daleko " +
            "jeszcze do jeziora? Około sześćdziesięciu kilometrów, może mniej, jeśli ruch dalej będzie taki " +
            "mały. Zatrzymajmy się najpierw na kawę i świeże pieczywo, a potem zdecydujemy." +
            "\n\n" +
            "Później po południu zerwał się wiatr, a od morza nadciągnęły ciemne chmury. Spakowaliśmy koce, " +
            "termos i starą gitarę, i pobiegliśmy do samochodu tuż przed deszczem. W drodze do domu " +
            "rozmawialiśmy o wszystkim, co widzieliśmy, śmialiśmy się z przypalonych tostów na śniadanie i " +
            "obiecaliśmy sobie, że następnego lata zostaniemy tam na cały tydzień."
    override val tooQuiet =
        "Nagranie jest za ciche. Trzymaj urządzenie bliżej i mów normalną głośnością."
    override val inconsistentRecording =
        "Nagranie nie brzmiało przez cały czas jak jedna osoba – może wtrącił się ktoś inny albo radio. " +
            "Spróbuj ponownie w cichszym miejscu, gdy mówi tylko ta osoba."
    override val record = "Nagraj"
    override val cancel = "Anuluj"
    override val recordingFailed = "Nagrywanie nie powiodło się."
    override fun tooLittleSpeech(seconds: Int) = "Usłyszano tylko $seconds s mowy – mów przez całe nagranie."
    override val micStopped = "Mikrofon przestał działać."
    override val micPermissionNeeded =
        "Potrzebne uprawnienie do mikrofonu – naciśnij raz „Rozpocznij nasłuchiwanie”, aby je przyznać."

    override val car = "Samochód"
    override val carInfo =
        "Zwiększa głośność multimediów, gdy jedziesz szybciej i gdy w samochodzie robi się głośniej. " +
            "Nasłuchiwanie trzeba najpierw włączyć."
    override val off = "Wył."
    override val on = "Wł."
    override val carDescription = "Głośniej, gdy jedziesz szybciej, gdy przyspieszasz i gdy rośnie hałas w tle."
    override val speedSteps = "Progi prędkości"
    override val speedStepsInfo =
        "Dodatkowa głośność od danej prędkości. Możesz dodać dowolnie wiele progów. Wymaga dostępu do lokalizacji."
    override fun speedStepTitle(number: Int) = "Próg $number"
    override val fromSpeed = "Od"
    override val extraVolume = "Dodatkowa głośność"
    override val addSpeedStep = "Dodaj próg"
    override fun removeSpeedStep(number: Int) = "Usuń próg $number"
    override val smoothSteps = "Płynnie między progami"
    override val smoothStepsInfo =
        "Łączy głośność między progami, więc rośnie stopniowo, gdy przyspieszasz, zamiast skakać na każdym progu. Wyłączone: głośność zmienia się na każdym progu, a próg znika dopiero 5 km/h poniżej niego."
    override val transitionTime = "Czas przejścia"
    override val transitionTimeInfo =
        "Ile czasu tryb samochodowy potrzebuje, żeby zmienić głośność na nowy poziom, żeby zmiany były płynne. Ściszanie przy mowie nadal działa od razu."
    override val accelerationBoost = "Przy przyspieszaniu, do"
    override val accelerationBoostInfo =
        "Dodatkowa głośność podczas przyspieszania – tym większa, im mocniej przyspieszasz (silnik robi się głośniejszy). Wraca, gdy jedziesz równo. 0% wyłącza. Wymaga dostępu do lokalizacji."
    override val noiseBoostUpTo = "Wzmocnienie od hałasu, maks."
    override val noiseBoostInfo =
        "Dodatkowa głośność, gdy w samochodzie robi się głośniej. Limit sprawia też, że aplikacja nie reaguje na głośniki samochodu i nie podkręca głośności coraz bardziej. Powiadomienie pokazuje, jak jest głośno."
    override val carModeAppliesNextStart =
        "Włączenie lub wyłączenie trybu samochodowego zadziała przy następnym uruchomieniu nasłuchiwania."

    override val setupGuide = "Przewodnik konfiguracji"
    override val setupGuideIntro =
        "Konfiguracja zajmie kilka minut. Poniższe wartości sprawdzają się u większości osób; później możesz je dopracować."
    override val useRecommendedValues = "Użyj zalecanych wartości"
    override val close = "Zamknij"
    override fun setupSteps(phone: Boolean) = listOfNotNull(
        "Wybierz tryb" to "„Ciszej” ścisza muzykę, gdy ktoś mówi (dobrze działa 35%). „Głośniej” ją pogłaśnia, żeby dało się dalej słuchać (80%). „Podcast” wstrzymuje odtwarzanie, a potem wznawia je 5 s wcześniej.",
        "Rozpocznij nasłuchiwanie" to "Naciśnij „Rozpocznij nasłuchiwanie” i zezwól na mikrofon. Powiedz kilka zdań i sprawdź, czy głośność się zmienia." + (if (phone) " Kafelek w Szybkich ustawieniach uruchamia nasłuchiwanie bez otwierania aplikacji." else ""),
        "Zarejestruj głosy (opcjonalnie)" to "Jeśli aplikacja reaguje na radio, podcasty albo inne osoby, zarejestruj osoby, które mają się liczyć, i włącz „Tylko zarejestrowane głosy”. Nagrywaj z telefonem tam, gdzie zwykle leży. Potem dodaj drugą próbkę w samochodzie przy włączonej muzyce, żeby rozpoznawała cię także tam.",
        "Sprawdź dopasowanie" to "Gdy mówi zarejestrowana osoba, status pokazuje, jak bardzo głos pasuje, np. 0,59. Zostaw „Dokładność dopasowania” bez zmian. Jeśli cię nie rozpoznaje, ustaw ją trochę poniżej najniższej widzianej liczby; jeśli przechodzą inne osoby, trochę ją podnieś.",
        ("Samochód" to "Tryb samochodowy pogłaśnia wraz z prędkością (od 50, 70 i 90 km/h) i gdy w aucie robi się głośniej (do +20%). Zostaw mikrofon na „Asystent”, chyba że aplikacja słabo cię słyszy.").takeIf { phone },
        "Dostrajanie" to "W „Zaawansowane”: Czas do reakcji 1,5 s, Dozwolona przerwa 1,2 s, Próg mowy 0,25. Reaguje na krótkie dźwięki? Wydłuż czas do reakcji. Głośność wraca między zdaniami? Wydłuż dozwoloną przerwę. Nie słyszy cichych głosów? Trochę obniż próg mowy."
    )

    override val ok = "OK"
    override fun about(topic: String) = "Informacje: $topic"
    override val language = "Język"
    override val microphone = "Mikrofon"
    override val microphoneInfo =
        "Których ustawień mikrofonu telefonu używać. „Asystent” wyłapuje głosy z odległości i z każdego kierunku, jak asystent głosowy, i pasuje większości osób. Jeśli aplikacja słabo cię słyszy, wypróbuj inne. Zmiana działa od następnego uruchomienia nasłuchiwania. Po zmianie zarejestruj głosy ponownie."
    override val micAssistant = "Asystent"
    override val micStandard = "Standardowy"
    override val micVideo = "Wideo"
    override val micRaw = "Surowy"
    override val systemLanguage = "Język systemu"
    override val addTile = "Dodaj kafelek do Szybkich ustawień"

    override val display = "Wyświetlanie"
    override val floatingStatus = "Pływający status"
    override val floatingStatusInfo =
        "Gdy aplikacja słucha i nie jest otwarta, mała karta nad innymi aplikacjami (np. nawigacją) pokazuje, co się dzieje. " +
            "Przeciągnij, aby ją przesunąć, dotknij, aby otworzyć aplikację."
    override val notificationListening = "Nasłuchiwanie mowy"
    override fun notificationVoiceHeard(who: String) = "Usłyszano głos$who"
    override fun notificationLowered(who: String) = "Wykryto mowę$who: głośność zmniejszona"
    override fun notificationRaised(who: String) = "Wykryto mowę$who: głośność zwiększona"
    override fun notificationPaused(who: String) = "Wykryto mowę$who: wstrzymano"
    override fun notificationCar(kmh: Int, noiseDb: Int, boostPercent: Int) =
        "Samochód: $kmh km/h, hałas $noiseDb dB, +$boostPercent%"
    override val notificationChannelDescription = "Nasłuchiwanie mikrofonu w tle"
    override val unknownVoice = "nieznany"
}
