@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.github.rodrigotimoteo.animally.domain.backup

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json

/**
 * Serializes and deserializes [BackupPayload] to and from pretty-printed JSON.
 */
object BackupSerializer {
    private val json: Json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    /**
     * Encodes [payload] to a pretty-printed JSON document.
     */
    fun encode(payload: BackupPayload): String = json.encodeToString(BackupPayload.serializer(), payload)

    /**
     * Decodes [content], migrates the supported v1 shape, and rejects payloads
     * whose [BackupPayload.schemaVersion] is newer than this reader.
     */
    fun decode(content: String): BackupPayload {
        require(content.length <= MAX_BACKUP_INPUT_CHARS) {
            "Backup input exceeds the maximum size accepted for restore"
        }
        require(content.encodeToByteArray().size <= MAX_BACKUP_BYTES) {
            "Backup exceeds the maximum size of $MAX_BACKUP_BYTES bytes"
        }
        val payload = json.decodeFromString(QuotaBackupPayloadDeserializer, content)
        return when (payload.schemaVersion) {
            MIN_SUPPORTED_BACKUP_SCHEMA_VERSION -> payload.copy(schemaVersion = BACKUP_SCHEMA_VERSION)
            BACKUP_SCHEMA_VERSION -> payload
            else ->
                error(
                    "Unsupported backup schema version ${payload.schemaVersion}, " +
                        "supported $MIN_SUPPORTED_BACKUP_SCHEMA_VERSION..$BACKUP_SCHEMA_VERSION",
                )
        }
    }
}

private object QuotaBackupPayloadDeserializer : DeserializationStrategy<BackupPayload> {
    private val delegate = BackupPayload.serializer()

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): BackupPayload = delegate.deserialize(QuotaDecoder(decoder, DecodeLimits()))
}

private class QuotaDecoder(
    private val delegate: Decoder,
    private val limits: DecodeLimits,
    private val collectionName: String? = null,
) : Decoder by delegate {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        QuotaCompositeDecoder(delegate.beginStructure(descriptor), limits, collectionName)

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = QuotaDecoder(delegate.decodeInline(descriptor), limits, collectionName)
}

private class QuotaCompositeDecoder(
    private val delegate: CompositeDecoder,
    private val limits: DecodeLimits,
    private val collectionName: String?,
) : CompositeDecoder by delegate {
    private var collectionSizeWasChecked = false

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        val size = delegate.decodeCollectionSize(descriptor)
        if (descriptor.kind == StructureKind.LIST && size >= 0) {
            limits.record(size, collectionName)
            collectionSizeWasChecked = true
        }
        return size
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val index = delegate.decodeElementIndex(descriptor)
        if (descriptor.kind == StructureKind.LIST && !collectionSizeWasChecked && index != CompositeDecoder.DECODE_DONE) {
            limits.record(1, collectionName)
        }
        return index
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T =
        delegate.decodeSerializableElement(
            descriptor,
            index,
            QuotaDeserializationStrategy(deserializer, limits, descriptor.getElementName(index)),
            previousValue,
        )

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
    ): T? =
        delegate.decodeNullableSerializableElement(
            descriptor,
            index,
            QuotaDeserializationStrategy(deserializer, limits, descriptor.getElementName(index)),
            previousValue,
        )
}

private class QuotaDeserializationStrategy<T>(
    private val delegate: DeserializationStrategy<T>,
    private val limits: DecodeLimits,
    private val collectionName: String,
) : DeserializationStrategy<T> {
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): T = delegate.deserialize(QuotaDecoder(decoder, limits, collectionName))
}

private class DecodeLimits {
    private var totalCollectionItems = 0

    fun record(
        count: Int,
        collectionName: String?,
    ) {
        require(count <= MAX_BACKUP_COLLECTION_ITEMS) {
            "Backup collection ${collectionName ?: "unknown"} exceeds the maximum of " +
                "$MAX_BACKUP_COLLECTION_ITEMS items"
        }
        require(count <= MAX_BACKUP_TOTAL_COLLECTION_ITEMS - totalCollectionItems) {
            "Backup exceeds the maximum of $MAX_BACKUP_TOTAL_COLLECTION_ITEMS collection items"
        }
        totalCollectionItems += count
    }
}
