package com.github.rodrigotimoteo.animally.domain.owner

import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import kotlin.time.Instant

/**
 * Repository contract for reading and mutating persisted `Owner` records.
 */
interface IOwnerRepository {
    /**
     * Returns all persisted owners currently available to the application.
     *
     * @return the list of all persisted owners.
     */
    fun getOwnerList(): List<Owner>

    /**
     * Returns the owner with the given [id], or `null` when no record exists.
     *
     * @param id the owner identifier to look up.
     * @return the matching owner, or `null` if none exists.
     */
    fun getOwnerById(id: Long): Owner?

    /**
     * Inserts [owner] into persistence and returns the generated identifier.
     *
     * @param owner the owner to persist.
     * @return tables updated row count.
     */
    fun insertOwner(owner: Owner): Long

    /**
     * Updates the persisted data for [owner].
     *
     * @param owner the owner containing the updated data.
     * @return tables updated row count.
     */
    fun updateOwner(owner: Owner): Long

    /**
     * Marks the owner identified by [id] as inactive without removing it.
     *
     * @param id the identifier of the owner to deactivate.
     * @param updatedAt the current date and time.
     * @return tables updated row count.
     */
    fun setInactive(
        id: Long,
        updatedAt: Instant,
    ): Long
}
