package com.github.rodrigotimoteo.animally.presentation.theme

/**
 * User-selectable brand accents.
 *
 * IDs are persisted rather than enum ordinals so adding or reordering accents
 * cannot silently change an existing user's choice.
 */
enum class AccentColor(
    val id: String,
    val label: String,
) {
    FOREST("forest", "Forest"),
    OCEAN("ocean", "Ocean"),
    PLUM("plum", "Plum"),
    TERRACOTTA("terracotta", "Terracotta"),
    SLATE("slate", "Slate"),
    ;

    companion object {
        /** Resolves an unknown or missing persisted value to the default accent. */
        fun fromId(id: String?): AccentColor = entries.firstOrNull { it.id == id } ?: FOREST
    }
}
