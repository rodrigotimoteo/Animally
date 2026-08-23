package com.github.rodrigotimoteo.animally.data.owner

import com.github.rodrigotimoteo.animally.data.AnimallyDatabase
import com.github.rodrigotimoteo.animally.di.database.createTestDatabase
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class OwnerRepositoryImplTest {
    private lateinit var database: AnimallyDatabase
    private lateinit var sut: OwnerRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        sut = OwnerRepositoryImpl(database.ownerQueries, database)
    }

    @Test
    fun `when database is empty then returns empty list`() {
        assertEquals(emptyList(), sut.getOwnerList())
    }

    @Test
    fun `when database has owners then returns mapped list ordered by id`() {
        database.ownerQueries.insert(
            name = "Alice",
            email = "alice@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        database.ownerQueries.insert(
            name = "Bob",
            email = null,
            phone = "12345",
            address = "Somewhere",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(1L),
            updatedAt = Instant.fromEpochMilliseconds(1L),
        )

        val result = sut.getOwnerList()

        assertEquals(2, result.size)
        with(result[0]) {
            assertEquals("Alice", name)
            assertEquals("alice@example.com", email)
            assertNull(phone)
            assertNull(address)
        }
        with(result[1]) {
            assertEquals("Bob", name)
            assertNull(email)
            assertEquals("12345", phone)
            assertEquals("Somewhere", address)
        }
    }

    @Test
    fun `when owner exists then returns owner by id`() {
        database.ownerQueries.insert(
            name = "Alice",
            email = "alice@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        val id = sut.getOwnerList().single().id

        val result = sut.getOwnerById(id)

        assertNotNull(result)
        assertEquals("Alice", assertNotNull(result).name)
        assertEquals("alice@example.com", result.email)
    }

    @Test
    fun `when owner does not exist then returns null`() {
        assertNull(sut.getOwnerById(999L))
    }

    @Test
    fun `when owner id is negative then returns null`() {
        assertNull(sut.getOwnerById(-1L))
    }

    @Test
    fun `when owner id is zero then returns null`() {
        assertNull(sut.getOwnerById(0L))
    }

    @Test
    fun `when inserting owner then returns rows affected`() {
        val owner =
            Owner(
                id = 0L,
                name = "Charlie",
                email = "charlie@example.com",
                phone = null,
                address = null,
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(100L),
                updatedAt = Instant.fromEpochMilliseconds(100L),
            )

        val result = sut.insertOwner(owner)

        assertEquals(1L, result)
        assertEquals(1, sut.getOwnerList().size)
    }

    @Test
    fun `when inserting same owner data twice then produces different ids`() {
        val owner =
            Owner(
                id = 0L,
                name = "Test",
                email = null,
                phone = null,
                address = null,
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(0L),
            )
        sut.insertOwner(owner)

        val other =
            owner.copy(
                createdAt = Instant.fromEpochMilliseconds(1L),
                updatedAt = Instant.fromEpochMilliseconds(1L),
            )
        sut.insertOwner(other)

        val result = sut.getOwnerList()
        assertEquals(2, result.size)
        assertNotEquals(result[0].id, result[1].id)
    }

    @Test
    fun `when inserting owner with empty name then stores and retrieves`() {
        database.ownerQueries.insert(
            name = "",
            email = null,
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        val id = sut.getOwnerList().single().id

        assertEquals("", assertNotNull(sut.getOwnerById(id)).name)
    }

    @Test
    fun `when owner has all nullable fields null then inserts and retrieves correctly`() {
        database.ownerQueries.insert(
            name = "NoContact",
            email = null,
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        val id = sut.getOwnerList().single().id

        val result = sut.getOwnerById(id)
        assertNotNull(result)
        assertEquals("NoContact", assertNotNull(result).name)
        assertNull(result.email)
        assertNull(result.phone)
        assertNull(result.address)
    }

    @Test
    fun `when updating owner then modifies existing fields`() {
        database.ownerQueries.insert(
            name = "Alice",
            email = "alice@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        val id = sut.getOwnerList().single().id

        sut.updateOwner(
            Owner(
                id = id,
                name = "Alice Updated",
                email = "alice@new.com",
                phone = "999",
                address = "New Address",
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(200L),
            ),
        )

        with(assertNotNull(sut.getOwnerById(id))) {
            assertEquals("Alice Updated", name)
            assertEquals("alice@new.com", email)
            assertEquals("999", phone)
            assertEquals("New Address", address)
        }
    }

    @Test
    fun `when updating owner with nulls then stores nulls`() {
        database.ownerQueries.insert(
            name = "Alice",
            email = "alice@example.com",
            phone = "123",
            address = "Somewhere",
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        val id = sut.getOwnerList().single().id

        sut.updateOwner(
            Owner(
                id = id,
                name = "Alice",
                email = null,
                phone = null,
                address = null,
                isActive = true,
                createdAt = Instant.fromEpochMilliseconds(0L),
                updatedAt = Instant.fromEpochMilliseconds(100L),
            ),
        )

        with(assertNotNull(sut.getOwnerById(id))) {
            assertNull(email)
            assertNull(phone)
            assertNull(address)
        }
    }

    @Test
    fun `when updating non-existent owner then returns zero rows affected`() {
        val result =
            sut.updateOwner(
                Owner(
                    id = 999L,
                    name = "Ghost",
                    email = null,
                    phone = null,
                    address = null,
                    isActive = true,
                    createdAt = Instant.fromEpochMilliseconds(0L),
                    updatedAt = Instant.fromEpochMilliseconds(0L),
                ),
            )

        assertEquals(0L, result)
    }

    @Test
    fun `when owner is inactive then excludes from queries`() {
        database.ownerQueries.insert(
            name = "Alice",
            email = null,
            phone = null,
            address = null,
            isActive = false,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        val id = 1L

        assertEquals(emptyList(), sut.getOwnerList())
        assertNull(sut.getOwnerById(id))
    }

    @Test
    fun `when setting inactive then marks owner as inactive`() {
        database.ownerQueries.insert(
            name = "Alice",
            email = "alice@example.com",
            phone = null,
            address = null,
            isActive = true,
            createdAt = Instant.fromEpochMilliseconds(0L),
            updatedAt = Instant.fromEpochMilliseconds(0L),
        )
        val id = sut.getOwnerList().single().id

        sut.setInactive(id, Instant.fromEpochMilliseconds(100L))

        assertNull(sut.getOwnerById(id))
        assertEquals(emptyList(), sut.getOwnerList())
    }

    @Test
    fun `when setting inactive on non-existent owner then returns zero rows affected`() {
        val result = sut.setInactive(999L, Instant.fromEpochMilliseconds(0L))

        assertEquals(0L, result)
    }
}
