package com.github.rodrigotimoteo.animally.domain.export

/**
 * Pure CSV formatting utilities used by [CsvExporter].
 *
 * Follows RFC 4180: fields containing a comma, a double quote, or a line
 * break are wrapped in double quotes, with embedded quotes doubled.
 * Lines end with CRLF.
 */
object CsvFormatter {
    private const val COMMA = ','
    private const val QUOTE = '"'
    private const val CARRIAGE_RETURN = '\r'
    private const val LINE_FEED = '\n'
    private const val LINE_ENDING = "\r\n"

    /** UTF-8 byte-order mark; prepended so Excel detects the encoding. */
    const val UTF8_BOM = "\uFEFF"

    /** First column of every section, making mixed rows self-describing. */
    const val RECORD_TYPE_HEADER = "Record Type"

    /** Splits PascalCase names at lowercase→uppercase boundaries. */
    private val CAMEL_BOUNDARY = Regex("(?<=[a-z])(?=[A-Z])")

    /** Human-readable overrides for internal field names. */
    private val DISPLAY_OVERRIDES =
        mapOf(
            "Id" to "ID",
            "OwnerId" to "Owner",
            "PatientId" to "Patient",
            "VetName" to "Veterinarian",
            "MicrochipId" to "Microchip",
            "DateOfBirth" to "Date of Birth",
            "WeightKg" to "Weight (kg)",
            "FollicleSizeMm" to "Follicle Size (mm)",
            "GradeAAEP" to "AAEP Grade",
            "TrimOrShoe" to "Trim/Shoe",
            "ImageUris" to "Image URIs",
        )

    /**
     * Escapes a single field for CSV output. `null` becomes the empty string.
     */
    fun escape(field: String?): String {
        if (field == null) return ""
        val requiresQuoting = field.any { it == COMMA || it == QUOTE || it == CARRIAGE_RETURN || it == LINE_FEED }
        return if (requiresQuoting) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    /**
     * Renders one CSV line from [fields], applying [escape] per field and
     * appending the CRLF line ending.
     */
    fun line(fields: List<Any?>): String = fields.joinToString(separator = ",") { escape(it?.toString()) } + LINE_ENDING

    /**
     * Maps internal field names to human-readable display headers, e.g.
     * `VetName` → `Veterinarian`, `NextDueDate` → `Next Due Date`.
     * Acronyms (`UELN`) pass through unchanged.
     */
    fun displayHeaders(fields: List<String>): List<String> = fields.map(::displayHeader)

    /** Maps one internal field name to its display form. */
    fun displayHeader(field: String): String = DISPLAY_OVERRIDES[field] ?: field.replace(CAMEL_BOUNDARY, " ")
}
