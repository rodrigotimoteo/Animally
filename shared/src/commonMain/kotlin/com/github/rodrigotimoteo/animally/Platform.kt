package com.github.rodrigotimoteo.animally

interface Platform {
    val name: String

    /** True on JVM desktop builds; local LLM presets are only offered there. */
    val isDesktop: Boolean get() = false
}

expect fun getPlatform(): Platform
