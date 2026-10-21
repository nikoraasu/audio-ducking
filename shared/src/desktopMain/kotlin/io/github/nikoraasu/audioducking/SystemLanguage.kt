package io.github.nikoraasu.audioducking

import java.util.Locale

actual fun systemLanguageCode(): String = Locale.getDefault().language
