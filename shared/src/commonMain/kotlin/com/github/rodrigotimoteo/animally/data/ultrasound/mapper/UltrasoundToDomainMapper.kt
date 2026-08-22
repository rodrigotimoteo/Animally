package com.github.rodrigotimoteo.animally.data.ultrasound.mapper

import com.github.rodrigotimoteo.animally.data.migrations.Ultrasound
import com.github.rodrigotimoteo.animally.domain.ultrasound.model.Ultrasound as DomainUltrasound

/**
 * Converts this persistence [Ultrasound] to a domain [DomainUltrasound].
 *
 * @return mapped [DomainUltrasound]
 */
fun Ultrasound.toDomain(): DomainUltrasound =
    DomainUltrasound(
        id = id,
        patientId = patientId,
        date = date,
        ovaryStatus = ovaryStatus,
        uterineStatus = uterineStatus,
        follicleSizeMm = follicleSizeMm,
        leftOvaryStatus = leftOvaryStatus,
        rightOvaryStatus = rightOvaryStatus,
        leftFollicleSizeMm = leftFollicleSizeMm,
        rightFollicleSizeMm = rightFollicleSizeMm,
        uterineEdema = uterineEdema,
        uterineLiquid = uterineLiquid,
        uterineLiquidDescription = uterineLiquidDescription,
        uterusDescription = uterusDescription,
        findings = findings,
        imageUris = imageUris,
        vetName = vetName,
        notes = notes,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
