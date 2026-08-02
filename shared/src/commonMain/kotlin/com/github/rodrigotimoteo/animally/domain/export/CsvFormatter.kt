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
}
