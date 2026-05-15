package com.blumlaut.filamenttagwriter.data.model

/**
 * Parses a filament name string to auto-detect material, subtype, and color.
 *
 * Examples:
 *   "TPU Black"          → material=TPU, color=Black
 *   "ELEGOO PLA-CF Red"  → material=PLA, subtype=PLA-CF, color=Red
 *   "PETG Silk Blue"     → material=PETG, color=Blue
 */
object NameParser {

    private val KNOWN_MATERIALS = Materials.getAllMaterials()
        .map { it.uppercase() }
        .sortedByDescending { it.length } // longest first to match "PETG" before "PC"

    private val KNOWN_SUBTYPES = Materials.SUBTYPE_CODES.values
        .map { it.uppercase() }
        .sortedByDescending { it.length }

    private val KNOWN_COLORS = listOf(
        "White", "Black", "Red", "Green", "Blue", "Orange", "Purple",
        "Yellow", "Cyan", "Pink", "Brown", "Gray", "Grey",
        "Silver", "Gold", "Copper",
        "Transparent", "Clear", "Translucent",
        "Marble", "Wood", "Galaxy", "Sparkle", "Matte", "Silk",
        "Fluo", "Neon",
        "Ivory", "Cream", "Beige",
        "Turquoise", "Magenta", "Violet", "Indigo",
        "Navy", "Teal", "Olive", "Lime", "Coral", "Salmon",
        "Maroon", "Crimson", "Scarlet",
        "Azure", "Cobalt", "Sapphire", "Emerald", "Jade",
        "Amber", "Bronze", "Copper",
        "Charcoal", "Slate", "Steel",
    ).map { it.lowercase() }

    /**
     * Try to parse material, subtype, and color from a name string.
     * Returns null for any field that couldn't be detected.
     */
    fun parse(name: String): ParsedName {
        val upper = name.uppercase()
        val words = name.split(Regex("[\\s,|/-]+"))

        // Detect material
        val materialMatch = KNOWN_MATERIALS.find { upper.contains(it) }
        val material = materialMatch
            ?.let { Materials.MATERIAL_CODES_REVERSE[it]?.let { code -> Materials.resolveMaterial(code) } }
            ?: materialMatch

        // Detect subtype — try exact subtype names first (e.g. "PLA-CF", "PLA Silk"), then material default
        val subtypeMatch = KNOWN_SUBTYPES.find { upper.contains(it) }
        val subtype = subtypeMatch
            ?.let { Materials.SUBTYPE_CODES_REVERSE[it]?.let { code -> Materials.resolveSubtype(code) } }
            ?: material // fallback: subtype = material (e.g. "PLA")

        // Detect color — look for color words that are NOT part of the detected material/subtype.
        // Only exclude words that are substrings of the ACTUALLY detected material/subtype,
        // not any material/subtype in the known lists. (Fix: "Red" alone should not be blocked
        // by the "PLA Red Copper" subtype if that subtype wasn't detected.)
        val color = words.find { word ->
            val lower = word.lowercase()
            val wUpper = word.uppercase()
            KNOWN_COLORS.contains(lower) &&
                !(materialMatch != null && materialMatch.contains(wUpper)) &&
                !(subtypeMatch != null && subtypeMatch.contains(wUpper))
        }?.let { normalizeColor(it) }

        return ParsedName(material, subtype, color)
    }

    private fun normalizeColor(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower == "grey" -> "Gray"
            lower == "translucent" -> "Transparent"
            lower == "clear" -> "Transparent"
            else -> raw.replaceFirstChar { it.uppercaseChar() }
        }
    }

    /**
     * Resolve a color name to RGB. Returns null if not a known color.
     */
    fun resolveColor(name: String): Int? {
        val lower = name.lowercase()
        return ColorNames.find { it.first.lowercase() == lower }?.second
    }

    data class ParsedName(
        val material: String?,
        val subtype: String?,
        val color: String?,
    )
}

/** Common color names with RGB values for autofill. */
private val ColorNames = listOf(
    "White" to 0xFFFFFF,
    "Black" to 0x000000,
    "Red" to 0xFF3700,
    "Green" to 0x33D700,
    "Blue" to 0x0080FF,
    "Orange" to 0xFF8C00,
    "Purple" to 0x735DF9,
    "Yellow" to 0xFFC800,
    "Cyan" to 0x44F1FF,
    "Pink" to 0xFF69B4,
    "Brown" to 0x964B00,
    "Gray" to 0x808080,
    "Silver" to 0xC0C0C0,
    "Gold" to 0xFFD700,
    "Copper" to 0xB87333,
    "Transparent" to 0xFFFFFF,
    "Maroon" to 0x800000,
    "Navy" to 0x000080,
    "Teal" to 0x008080,
    "Olive" to 0x808000,
    "Lime" to 0x00FF00,
    "Coral" to 0xFF7F50,
    "Salmon" to 0xFA8072,
    "Crimson" to 0xDC143C,
    "Scarlet" to 0xFF2400,
    "Azure" to 0x007FFF,
    "Cobalt" to 0x0047AB,
    "Sapphire" to 0x0F52BA,
    "Emerald" to 0x50C878,
    "Jade" to 0x00A86B,
    "Amber" to 0xFFBF00,
    "Bronze" to 0xCD7F32,
    "Charcoal" to 0x36454F,
    "Slate" to 0x708090,
    "Steel" to 0x71797E,
    "Ivory" to 0xFFFFF0,
    "Cream" to 0xFFFDD0,
    "Beige" to 0xF5F5DC,
    "Turquoise" to 0x40E0D0,
    "Magenta" to 0xFF00FF,
    "Violet" to 0x9400D3,
    "Indigo" to 0x4B0082,
)
