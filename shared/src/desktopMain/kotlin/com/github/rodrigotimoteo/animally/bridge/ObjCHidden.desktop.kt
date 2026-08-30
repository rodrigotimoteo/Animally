package com.github.rodrigotimoteo.animally.bridge

/**
 * Non-native actual: no-op. ObjC export does not exist on these targets.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
actual annotation class ObjCHidden
