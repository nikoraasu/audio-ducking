package io.github.nikoraasu.audioducking

object SpanishStrings : Strings {
    override val decimalSeparator = ','

    override val notListening = "Sin escuchar"
    override val notListeningDetail = "Actívalo para bajar o subir el volumen cuando alguien hable."
    override val listening = "Escuchando"
    override val waitingForSpeech = "Esperando a que alguien hable…"
    override val ducking = "Volumen bajado"
    override val boosting = "Volumen subido"
    override val paused = "En pausa"
    override fun playbackPaused(who: String) = "$who, reproducción en pausa."
    override val stopped = "Detenido"
    override fun isTalking(name: String?) = if (name != null) "$name está hablando" else "Alguien está hablando"
    override fun volumeLowered(who: String) = "$who, volumen bajado."
    override fun volumeRaised(who: String) = "$who, volumen subido."
    override fun carBoost(percent: Int) = "Modo coche: +$percent % de volumen"
    override fun ignoringVoice(score: String) = "Ignorando una voz no registrada (coincidencia $score)."
    override fun withMatch(who: String, score: String) = "$who (coincidencia $score)"
    override val startListening = "Empezar a escuchar"
    override val stopListening = "Dejar de escuchar"
    override fun error(error: MonitorError) = when (error) {
        MonitorError.NO_PERMISSION -> "Falta el permiso del micrófono."
        MonitorError.MIC_UNAVAILABLE -> "No se pudo abrir el micrófono."
        MonitorError.STOPPED_UNEXPECTEDLY -> "La escucha se detuvo inesperadamente."
    }

    override val mode = "Modo"
    override val duck = "Bajar"
    override val noise = "Subir"
    override val duckDescription = "Más bajo mientras alguien habla, para que puedas oírle."
    override val noiseDescription = "Más alto mientras alguien habla, para que sigas escuchando."
    override val podcast = "Pódcast"
    override val podcastDescription = "Pausa mientras alguien habla y luego sigue unos segundos antes, para que no te pierdas nada."
    override val rewind = "Retroceder tras la pausa"
    override val rewindInfo =
        "Cuánto retrocede la reproducción al terminar la conversación. Con altavoces, la app puede oír el propio pódcast y pausarlo; " +
            "los auriculares o «Solo voces registradas» lo evitan."
    override val mediaAccessNeeded =
        "Para retroceder, la app necesita acceso a la reproducción (acceso a notificaciones). Sin él solo pausa y reanuda."
    override val allowMediaAccess = "Permitir"
    override val volumeWhileTalking = "Volumen al hablar"
    override val duckLevelInfo =
        "Hasta dónde baja el volumen multimedia mientras alguien habla. Este ajuste nunca lo sube."
    override val boostLevelInfo =
        "Hasta dónde sube el volumen multimedia mientras alguien habla. Este ajuste nunca lo baja, " +
            "y el volumen vuelve a su nivel cuando dejan de hablar."

    override val advanced = "Avanzado"
    override val expand = "Desplegar"
    override val collapse = "Contraer"
    override val speechHold = "Tiempo de reacción"
    override val speechHoldInfo =
        "Cuánto tiempo debe hablar alguien antes de que cambie el volumen. Un valor mayor ignora palabras sueltas y ruidos breves."
    override val allowedPause = "Pausa permitida"
    override val allowedPauseInfo =
        "Cuánto puede durar un silencio antes de dar la conversación por terminada y restaurar el volumen."
    override val speechThreshold = "Umbral de voz"
    override val speechThresholdInfo =
        "Qué seguridad necesita el detector para considerar un sonido como voz. Un valor menor capta voces bajas " +
            "o lejanas, pero puede reaccionar a otros ruidos."

    override val voices = "Voces"
    override val voicesInfo =
        "Graba a las personas a las que debe reaccionar la app. Con «Solo voces registradas» activado, se ignora " +
            "a los demás, la radio y los pódcasts. Reconocer quién habla requiere unos 1,5 s de voz."
    override val onlyRegisteredVoices = "Solo voces registradas"
    override val extraSamples = "Muestras extra con música y eco"
    override val extraSamplesInfo =
        "Al registrar una voz, la app también crea copias de la grabación con música y con eco de sala. Si está activado, las voces se comparan también con esas copias: con música alta o desde más lejos te reconoce más a menudo, pero las personas desconocidas se cuelan un poco más. Se aplica a las voces que se registren o reciban una muestra nueva a partir de ahora.\n\nMúsica: «Hackbeat» y «Volatile Reaction» de Kevin MacLeod (incompetech.com), con licencia Creative Commons: Reconocimiento 4.0 (creativecommons.org/licenses/by/4.0)."
    override val matchStrictness = "Exigencia de coincidencia"
    override val matchStrictnessInfo =
        "Cuánto debe parecerse una voz a una registrada. Un valor mayor deja pasar a menos desconocidos, " +
            "pero puede no reconocer a una persona registrada en un coche ruidoso."
    override val noVoicesYet = "Aún no hay voces registradas, así que reacciona a todo el mundo."
    override val outdatedVoices = "Las voces grabadas con el modelo de reconocimiento anterior deben registrarse de nuevo."
    override fun remove(name: String) = "Eliminar a $name"
    override val registerVoice = "Registrar una voz"
    override val name = "Nombre"
    override fun recording(secondsLeft: Int) = "Grabando, sigue hablando… $secondsLeft s"
    override val processing = "Comprobando la grabación…"
    override fun enrollInstructions(seconds: Int) =
        "Pulsa Grabar y sigue hablando durante toda la grabación a tu ritmo normal, como si hablaras con alguien. Solo debe hablar esta persona. Graba desde donde sueles estar al usar la app."
    override fun sampleCount(count: Int) = if (count == 1) "1 muestra" else "$count muestras"
    override val addSample = "Añadir una muestra"
    override fun addSampleTitle(name: String) = "Nueva muestra de $name"
    override val addSampleHint =
        "Graba en una situación distinta a la anterior, por ejemplo en el coche desde tu asiento, con música como " +
            "de costumbre. La app compara cada voz con la muestra que mejor encaja."
    override val enrollPassageNote =
        "El texto de abajo es solo una ayuda para saber qué decir. No hace falta leerlo palabra por palabra y, si terminas antes, sigue hablando."
    override val enrollPassage =
        "Hola, esta es mi voz. El sábado por la mañana fuimos hacia el norte por la tranquila carretera " +
            "de la costa, pasando tiendas, campos y pueblos dormidos. En la radio sonaba jazz, así que " +
            "bajamos la música. ¿Cuánto falta para llegar al lago? Unos sesenta kilómetros, quizá menos si " +
            "el tráfico sigue así de ligero. Paremos primero a tomar un café y comprar pan recién hecho, y " +
            "luego decidimos." +
            "\n\n" +
            "Por la tarde se levantó el viento y llegaron nubes oscuras desde el mar. Recogimos las mantas, " +
            "el termo y la vieja guitarra, y corrimos al coche justo antes de que empezara a llover. De " +
            "camino a casa hablamos de todo lo que habíamos visto, nos reímos de las tostadas quemadas del " +
            "desayuno y nos prometimos que el próximo verano nos quedaríamos una semana entera."
    override val tooQuiet =
        "La grabación ha sonado demasiado baja. Acerca el dispositivo y habla a tu volumen normal."
    override val inconsistentRecording =
        "La grabación no sonó todo el rato como una sola persona; quizá se coló otra voz o la radio. " +
            "Inténtalo de nuevo en un sitio más tranquilo, con solo esta persona hablando."
    override val record = "Grabar"
    override val cancel = "Cancelar"
    override val recordingFailed = "La grabación ha fallado."
    override fun tooLittleSpeech(seconds: Int) =
        "Solo se oyeron $seconds s de voz; sigue hablando durante toda la grabación."
    override val micStopped = "El micrófono se ha detenido."
    override val micPermissionNeeded =
        "Se necesita permiso del micrófono: pulsa «Empezar a escuchar» una vez para concederlo."

    override val car = "Coche"
    override val carInfo =
        "Sube el volumen multimedia a medida que conduces más rápido y aumenta el ruido en el coche. " +
            "Aun así, primero hay que empezar a escuchar."
    override val off = "No"
    override val on = "Sí"
    override val carDescription = "Más alto cuanto más rápido conduces, al acelerar y cuanto más ruido de fondo hay."
    override val speedSteps = "Tramos de velocidad"
    override val speedStepsInfo =
        "Volumen extra a partir de cada velocidad. Puedes añadir tantos tramos como quieras. Requiere permiso de ubicación."
    override fun speedStepTitle(number: Int) = "Tramo $number"
    override val fromSpeed = "Desde"
    override val extraVolume = "Volumen extra"
    override val addSpeedStep = "Añadir tramo"
    override fun removeSpeedStep(number: Int) = "Quitar tramo $number"
    override val smoothSteps = "Suave entre tramos"
    override val smoothStepsInfo =
        "Mezcla el volumen entre tramos, así sube poco a poco al acelerar en vez de saltar en cada tramo. Desactivado: el volumen cambia en cada tramo, y un tramo solo se retira cuando vas 5 km/h por debajo."
    override val transitionTime = "Tiempo de transición"
    override val transitionTimeInfo =
        "Cuánto tarda el modo coche en llevar el volumen a un nuevo nivel, para que los cambios sean suaves. Bajar el volumen al hablar sigue siendo inmediato."
    override val accelerationBoost = "Al acelerar, hasta"
    override val accelerationBoostInfo =
        "Volumen extra mientras aceleras, más cuanto más fuerte aceleres (el motor suena más), y vuelve al ir a velocidad constante. 0 % lo desactiva. Requiere permiso de ubicación."
    override val noiseBoostUpTo = "Refuerzo por ruido, hasta"
    override val noiseBoostInfo =
        "Volumen extra cuando hay más ruido en el coche. El límite también evita que la app oiga los altavoces del coche y suba el volumen cada vez más. La notificación muestra cuánto ruido hay."
    override val carModeAppliesNextStart =
        "Activar o desactivar el modo coche se aplica la próxima vez que empiece a escuchar."

    override val setupGuide = "Guía de configuración"
    override val setupGuideIntro =
        "Se configura en pocos minutos. Los valores de abajo funcionan para la mayoría; luego puedes ajustarlos."
    override val useRecommendedValues = "Usar valores recomendados"
    override val close = "Cerrar"
    override fun setupSteps(phone: Boolean) = listOfNotNull(
        "Elige un modo" to "«Bajar» baja la música mientras alguien habla (35 % funciona bien). «Subir» la sube para que puedas seguir escuchando (80 %). «Pódcast» pausa y luego sigue desde 5 s antes.",
        "Empieza a escuchar" to "Pulsa «Empezar a escuchar» y permite el micrófono. Di unas frases y comprueba que el volumen cambia." + (if (phone) " Con el botón de Ajustes rápidos empieza a escuchar sin abrir la app." else ""),
        "Registra voces (opcional)" to "Si la radio, los pódcasts u otras personas lo activan, registra a las personas que deben contar y activa «Solo voces registradas». Graba con el teléfono donde suele estar. Después añade una segunda muestra en el coche con música, para que también te reconozca allí.",
        "Comprueba la coincidencia" to "Mientras habla una persona registrada, el estado muestra cuánto se parece la voz, por ejemplo 0,59. Deja la «Exigencia de coincidencia» como está. Si no te reconoce, ponla un poco por debajo del número más bajo que veas; si se cuelan otras personas, súbela un poco.",
        ("Coche" to "El modo coche sube el volumen cuanto más rápido vas (desde 50, 70 y 90 km/h) y cuanto más ruido hay (hasta +20 %). Deja el micrófono en «Asistente», salvo que la app no te oiga bien.").takeIf { phone },
        "Ajuste fino" to "En «Avanzado»: Tiempo de reacción 1,5 s, Pausa permitida 1,2 s, Umbral de voz 0,25. ¿Reacciona a ruidos cortos? Sube el tiempo de reacción. ¿El volumen vuelve entre frases? Sube la pausa permitida. ¿No oye voces bajas? Baja un poco el umbral de voz."
    )

    override val ok = "Aceptar"
    override fun about(topic: String) = "Información: $topic"
    override val language = "Idioma"
    override val microphone = "Micrófono"
    override val microphoneInfo =
        "Qué ajuste de micrófono del teléfono se usa para escuchar. «Asistente» capta voces a distancia y desde cualquier dirección, como un asistente de voz, y va bien a la mayoría. Prueba los otros si la app no te oye bien. Se aplica la próxima vez que empiece a escuchar. Después de cambiarlo, registra las voces de nuevo."
    override val micAssistant = "Asistente"
    override val micStandard = "Estándar"
    override val micVideo = "Vídeo"
    override val micRaw = "Sin procesar"
    override val systemLanguage = "Idioma del sistema"
    override val addTile = "Añadir a Ajustes rápidos"

    override val display = "Pantalla"
    override val floatingStatus = "Estado flotante"
    override val floatingStatusInfo =
        "Mientras la app escucha y no está abierta, una pequeña tarjeta sobre otras apps (como la navegación) muestra qué hace. " +
            "Arrástrala para moverla y tócala para abrir la app."
    override val notificationListening = "Escuchando voces"
    override fun notificationVoiceHeard(who: String) = "Voz detectada$who"
    override fun notificationLowered(who: String) = "Voz detectada$who: volumen bajado"
    override fun notificationRaised(who: String) = "Voz detectada$who: volumen subido"
    override fun notificationPaused(who: String) = "Voz detectada$who: en pausa"
    override fun notificationCar(kmh: Int, noiseDb: Int, boostPercent: Int) =
        "Coche: $kmh km/h, ruido $noiseDb dB, +$boostPercent %"
    override val notificationChannelDescription = "Escucha del micrófono en segundo plano"
    override val unknownVoice = "desconocida"
}
