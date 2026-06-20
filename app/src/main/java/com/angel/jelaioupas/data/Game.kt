package com.angel.jelaioupas.data

data class Game(
    val title: String,
    val ean: String,      // normalisé sur 13 chiffres
    val owned: Boolean,
    val console: String = ""
)

object EanUtils {
    /** Normalise un code-barres : trim, retire espaces/tirets, pad à 13 chiffres. */
    fun normalize(raw: String): String =
        raw.trim().replace(" ", "").replace("-", "").padStart(13, '0')
}

/**
 * Parser CSV minimaliste mais correct (guillemets, virgules dans les champs,
 * guillemets échappés "" et retours à la ligne dans les champs).
 */
object Csv {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ }
                        else inQuotes = false
                    } else field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { row.add(field.toString()); field.clear() }
                c == '\r' -> { /* ignore */ }
                c == '\n' -> {
                    row.add(field.toString()); field.clear()
                    rows.add(row.toList()); row.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row.toList())
        }
        return rows
    }
}
