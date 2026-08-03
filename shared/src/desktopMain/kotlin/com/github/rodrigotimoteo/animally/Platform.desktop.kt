@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally

class DesktopPlatform : Platform {
    override val name: String = "Desktop ${System.getProperty("os.name")}"
}

actual fun getPlatform(): Platform = DesktopPlatform()
