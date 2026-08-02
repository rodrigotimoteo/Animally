package com.github.rodrigotimoteo.animally.data.storage

/**
 * A file selected through the platform file picker, decoupled from the
 * platform representation so view models can be unit-tested without a device.
 *
 * @param name The display name of the picked file.
 * @param readBytes Suspended loader of the file content.
 */
data class PickedFile(
    val name: String,
    val readBytes: suspend () -> ByteArray,
)
