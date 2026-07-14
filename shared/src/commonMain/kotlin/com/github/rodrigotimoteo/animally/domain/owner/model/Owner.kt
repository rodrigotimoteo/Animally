package com.github.rodrigotimoteo.animally.domain.owner.model

import com.github.rodrigotimoteo.animally.domain.common.Identifiable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Owner(
    @SerialName("id")
    override val id: Long,
    @SerialName("name")
    val name: String,
    @SerialName("phone")
    val phone: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("address")
    val address: String?,
    @SerialName("notes")
    val notes: String?,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
) : Identifiable
