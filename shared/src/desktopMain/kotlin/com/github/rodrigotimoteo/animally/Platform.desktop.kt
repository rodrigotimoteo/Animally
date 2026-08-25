@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally

class DesktopPlatform : Platform {
    override val name: String = "Desktop ${System.getProperty("os.name")}"
    override val isDesktop: Boolean = true
}

actual fun getPlatform(): Platform = DesktopPlatform()
