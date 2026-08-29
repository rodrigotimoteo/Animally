package com.github.rodrigotimoteo.animally.llm

import com.github.rodrigotimoteo.animally.domain.owner.IOwnerRepository
import com.github.rodrigotimoteo.animally.domain.owner.model.Owner
import com.github.rodrigotimoteo.animally.domain.search.ISearchRepository
import com.github.rodrigotimoteo.animally.domain.search.model.SearchResult
import kotlinx.coroutines.flow.FlowCollector

/** Exact owner fields that should never be rewritten by a language model. */
internal object OwnerIdentityAnswer {
    private val addressRegex =
        Regex(
            "\\b(address|addresses|morada|moradas|endereço|endereços|localização|localizacao)\\b|" +
                "\\b(where\\s+(?:does|is).{0,40}\\b(?:live|based|located))\\b",
            RegexOption.IGNORE_CASE,
        )
    private val emailRegex =
        Regex(
            "\\b(e-?mail|email|correio\\s+eletrónico|correio\\s+eletronico)\\b",
            RegexOption.IGNORE_CASE,
        )
    private val phoneRegex =
        Regex(
            "\\b(phone|telephone|mobile|contact\\s+number|telefone|telemóvel|telemovel|" +
                "número\\s+de\\s+telefone|numero\\s+de\\s+telefone)\\b",
            RegexOption.IGNORE_CASE,
        )
    private val contactDetailsRegex =
        Regex(
            "\\b(contact|contacts|contact\\s+details|contacto|contactos|detalhes\\s+de\\s+contacto)\\b",
            RegexOption.IGNORE_CASE,
        )

    internal enum class Field {
        ADDRESS,
        EMAIL,
        PHONE,
    }

    fun requestedFields(query: String): Set<Field> {
        val fields =
            buildSet {
                if (addressRegex.containsMatchIn(query)) add(Field.ADDRESS)
                if (emailRegex.containsMatchIn(query)) add(Field.EMAIL)
                if (phoneRegex.containsMatchIn(query)) add(Field.PHONE)
            }
        return if (fields.isEmpty() && contactDetailsRegex.containsMatchIn(query)) {
            Field.entries.toSet()
        } else {
            fields
        }
    }
}

/** Emits exact contact data for an explicitly resolved active owner. */
internal suspend fun FlowCollector<RagStreamEvent>.emitOwnerContactAnswer(
    query: String,
    scopedOwner: String?,
    ownerRepository: IOwnerRepository?,
): Boolean {
    val fields = OwnerIdentityAnswer.requestedFields(query)
    if (fields.isEmpty() || scopedOwner == null || ownerRepository == null) return false
    val owner =
        ownerRepository
            .getOwnerList()
            .singleOrNull { it.isActive && it.name.equals(scopedOwner, ignoreCase = true) }
            ?: return false
    val portuguese = AssistantPrompts.isPortugueseQuery(query)
    val answer =
        fields
            .sortedBy(OwnerIdentityAnswer.Field::ordinal)
            .joinToString("\n") { field ->
                val value = field.value(owner)
                if (portuguese) {
                    "${field.labelPortuguese} de ${owner.name}: ${value ?: "não está registado"}."
                } else {
                    "${owner.name}'s ${field.label}: ${value ?: "not recorded"}."
                }
            }
    emit(RagStreamEvent.Chunk(answer))
    emit(RagStreamEvent.Sources(listOf(ownerSource(owner))))
    return true
}

private fun OwnerIdentityAnswer.Field.value(owner: Owner): String? =
    when (this) {
        OwnerIdentityAnswer.Field.ADDRESS -> owner.address
        OwnerIdentityAnswer.Field.EMAIL -> owner.email
        OwnerIdentityAnswer.Field.PHONE -> owner.phone
    }?.takeIf(String::isNotBlank)

private val OwnerIdentityAnswer.Field.label: String
    get() =
        when (this) {
            OwnerIdentityAnswer.Field.ADDRESS -> "address"
            OwnerIdentityAnswer.Field.EMAIL -> "email"
            OwnerIdentityAnswer.Field.PHONE -> "phone number"
        }

private val OwnerIdentityAnswer.Field.labelPortuguese: String
    get() =
        when (this) {
            OwnerIdentityAnswer.Field.ADDRESS -> "Morada"
            OwnerIdentityAnswer.Field.EMAIL -> "Email"
            OwnerIdentityAnswer.Field.PHONE -> "Telefone"
        }

private fun ownerSource(owner: Owner): SearchResult =
    SearchResult(
        patientId = owner.id,
        patientName = owner.name,
        breed = null,
        microchipId = null,
        recordType = ISearchRepository.TYPE_OWNER,
        recordId = owner.id,
        date = null,
        snippet =
            listOf(
                "Name: ${owner.name}",
                "Address: ${owner.address ?: "not recorded"}",
                "Email: ${owner.email ?: "not recorded"}",
                "Phone: ${owner.phone ?: "not recorded"}",
            ).joinToString(" "),
    )
