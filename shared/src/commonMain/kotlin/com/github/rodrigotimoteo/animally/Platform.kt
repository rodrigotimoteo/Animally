package com.github.rodrigotimoteo.animally

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
