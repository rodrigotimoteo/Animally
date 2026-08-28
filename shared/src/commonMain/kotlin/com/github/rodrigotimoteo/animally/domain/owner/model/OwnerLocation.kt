package com.github.rodrigotimoteo.animally.domain.owner.model

private const val MIN_LATITUDE = -90.0
private const val MAX_LATITUDE = 90.0
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0

/**
 * A validated geographic location associated with an owner.
 *
 * Coordinates are deliberately represented as a small shared value object so
 * every platform persists and consumes the same valid range.
 */
data class OwnerLocation(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in MIN_LATITUDE..MAX_LATITUDE) { "Latitude must be between -90 and 90 degrees" }
        require(longitude in MIN_LONGITUDE..MAX_LONGITUDE) { "Longitude must be between -180 and 180 degrees" }
    }

    companion object {
        /**
         * Builds a location from nullable persistence or bridge values.
         *
         * An incomplete or invalid pair is treated as no location so corrupt
         * legacy/imported data cannot reach map presentation code.
         */
        fun fromNullable(
            latitude: Double?,
            longitude: Double?,
        ): OwnerLocation? {
            if (latitude == null || longitude == null) return null
            return runCatching { OwnerLocation(latitude, longitude) }.getOrNull()
        }
    }
}
