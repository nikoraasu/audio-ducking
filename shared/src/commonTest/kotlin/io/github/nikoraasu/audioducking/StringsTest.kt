package io.github.nikoraasu.audioducking

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {
    @Test
    fun numbersUseTheLanguagesDecimalSeparator() {
        assertEquals("0.50", EnglishStrings.decimal(0.5f))
        assertEquals("0,05", PolishStrings.decimal(0.05f))
        assertEquals("1.5 s", EnglishStrings.seconds(1500f))
        assertEquals("2,0 s", GermanStrings.seconds(2000f))
    }

    @Test
    fun explicitChoiceWinsAndUnknownCodesFallBackToEnglish() {
        assertEquals(Language.ES, resolveLanguage("es"))
        assertEquals(Language.EN, resolveLanguage("xx"))
    }

    @Test
    fun recordingErrorsAreWordedPerLanguage() {
        val error = EnrollException(EnrollException.Reason.TOO_LITTLE_SPEECH, heardSeconds = 2)
        assertEquals("Usłyszano tylko 2 s mowy – mów przez całe nagranie.", PolishStrings.enrollError(error))
        assertEquals(EnglishStrings.recordingFailed, EnglishStrings.enrollError(RuntimeException()))
    }

    @Test
    fun setupGuideHasTheSameStepsInEveryLanguage() {
        for (language in Language.entries) {
            val strings = stringsFor(language)
            assertEquals(6, strings.setupSteps(phone = true).size, language.name)
            assertEquals(5, strings.setupSteps(phone = false).size, language.name) // no car on desktop
        }
    }
}
