package com.github.rodrigotimoteo.animally.bridge

/**
 * KMP-safe alias for [kotlin.native.HiddenFromObjC].
 *
 * `kotlin.native.HiddenFromObjC` is an `@OptionalExpectation` annotation with
 * native-only actuals — the symbol does not exist on JVM/Android/desktop
 * targets, so it cannot be referenced from commonMain directly. This wrapper
 * keeps Koin `@Module` classes out of the exported ObjC header (the Kotlin/Native
 * exporter asserts "Shouldn't be exposed" on the KSP-generated public
 * `module()` extensions otherwise) while compiling on every target.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class ObjCHidden()
