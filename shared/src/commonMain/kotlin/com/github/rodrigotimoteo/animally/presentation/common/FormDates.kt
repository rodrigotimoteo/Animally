package com.github.rodrigotimoteo.animally.presentation.common

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Returns today's date as an ISO `yyyy-MM-dd` string for form defaults, so new
 * records are valid without requiring the user to touch the date picker.
 */
fun todayIso(): String =
    Clock.System
        .now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
