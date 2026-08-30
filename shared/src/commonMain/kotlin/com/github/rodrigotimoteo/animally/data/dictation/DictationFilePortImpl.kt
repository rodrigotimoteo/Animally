package com.github.rodrigotimoteo.animally.data.dictation

import com.github.rodrigotimoteo.animally.data.storage.FileStorage
import com.github.rodrigotimoteo.animally.domain.dictation.DictationFilePort
import org.koin.core.annotation.Single

/**
 * Data-side adapter for [DictationFilePort] that delegates to the
 * platform-specific [FileStorage] expect/actual.
 */
@Single(binds = [DictationFilePort::class])
class DictationFilePortImpl : DictationFilePort {
    override fun delete(path: String): Boolean = FileStorage.delete(path)
}
