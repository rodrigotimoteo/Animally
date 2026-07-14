@file:Suppress("ktlint:standard:filename")

package com.github.rodrigotimoteo.animally

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
