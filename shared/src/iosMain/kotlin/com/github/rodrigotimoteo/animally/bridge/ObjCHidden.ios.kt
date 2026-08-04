package com.github.rodrigotimoteo.animally.bridge

/**
 * iOS actual: the real Kotlin/Native annotation. Carrying it makes the ObjC
 * exporter skip the annotated class (and the Koin-generated `module()`
 * extensions whose receiver it is).
 */
actual typealias ObjCHidden = kotlin.native.HiddenFromObjC
