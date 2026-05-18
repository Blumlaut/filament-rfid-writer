package com.blumlaut.filamenttagwriter.data.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "SpoolmanLoader"
private const val ASSET_PATH = "spoolman/filaments.json"

/**
 * Loads and searches the SpoolmanDB filament database bundled as an app asset.
 *
 * The JSON is downloaded at build time by the `downloadSpoolmanDB` Gradle task.
 * If the asset is missing (e.g. no network during build), search returns empty results.
 */
object SpoolmanLoader {

    private var filaments: List<SpoolmanFilament>? = null
    private var loadError: String? = null

    /**
     * Load the SpoolmanDB from assets. Called once on first use.
     */
    suspend fun load(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        if (filaments != null) {
            return@withContext Result.success(Unit)
        }

        try {
            val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            val list = mutableListOf<SpoolmanFilament>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(parseFilament(obj))
            }

            filaments = list
            Log.d(TAG, "Loaded ${list.size} filaments from SpoolmanDB")
            Result.success(Unit)

        } catch (e: Exception) {
            loadError = e.message ?: e.toString()
            Log.w(TAG, "Failed to load SpoolmanDB: $loadError", e)
            Result.failure(e)
        }
    }

    /**
     * Search filaments by query string. Matches against manufacturer, name, and material.
     * Returns at most [limit] results, sorted by relevance (exact prefix matches first).
     *
     * Results are deduplicated by (manufacturer, name, material) so that the same filament
     * listed at different spool weights (e.g. 1 kg vs 2 kg) appears only once.
     */
    fun search(query: String, limit: Int = 20): List<SpoolmanFilament> {
        val data = filaments ?: return emptyList()
        if (query.isBlank()) return emptyList()

        val lower = query.lowercase()
        val words = lower.split(Regex("\\s+"))

        return data
            .map { filament ->
                val searchable = "${filament.manufacturer} ${filament.name} ${filament.material}".lowercase()
                val score = computeRelevance(searchable, words)
                filament to score
            }
            .filter { it.second > 0 }
            // Sort by relevance, then prefer entries closest to 1 kg (industry standard)
            .sortedWith(compareByDescending<Pair<SpoolmanFilament, Int>> { it.second }
                .thenBy { kotlin.math.abs(it.first.weight - 1000f) }
            )
            .distinctBy { it.first.let { f -> f.manufacturer to f.name to f.material } }
            .take(limit)
            .map { it.first }
    }

    /**
     * Get all unique manufacturers.
     */
    fun getManufacturers(): List<String> {
        return filaments
            ?.mapNotNull { it.manufacturer }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
    }

    /**
     * Get all unique materials.
     */
    fun getMaterials(): List<String> {
        return filaments
            ?.mapNotNull { it.material }
            ?.distinct()
            ?.sorted()
            ?: emptyList()
    }

    /**
     * Total number of filaments loaded.
     */
    fun count(): Int = filaments?.size ?: 0

    /**
     * Try to match a scanned filament (from tag data) against the SpoolmanDB catalog.
     *
     * Matching strategy:
     * 1. Map ELEGOO material/subtype → set of SpoolmanDB material names
     * 2. Filter by material
     * 3. Filter by color with tolerance (RGB Euclidean distance ≤ threshold)
     * 4. Sort by color proximity
     *
     * Returns a sealed result:
     * - ExactMatch: single best match (color within tight threshold)
     * - MultipleMatches: several candidates (user should pick)
     * - NoMatch: nothing close enough
     */
    sealed interface SpoolmanMatchResult {
        object NoMatch : SpoolmanMatchResult
        data class ExactMatch(val filament: SpoolmanFilament) : SpoolmanMatchResult
        data class MultipleMatches(val candidates: List<SpoolmanFilament>) : SpoolmanMatchResult
    }

    /**
     * RGB Euclidean distance between two colors. 0 = identical, ~441 = max (complementary).
     */
    private fun rgbDistance(r1: Int, g1: Int, b1: Int, r2: Int, g2: Int, b2: Int): Int {
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        return dr * dr + dg * dg + db * db
    }

    fun matchByTagData(
        elegooMaterial: String,
        elegooSubtype: String,
        colorRgb: Int,
        tolerance: Int = 2500, // ~50 per channel squared sum
    ): SpoolmanMatchResult {
        val data = filaments ?: return SpoolmanMatchResult.NoMatch

        // Map ELEGOO material/subtype → SpoolmanDB material names
        val spoolmanMaterials = SpoolmanMaterialMapper.mapToSpoolman(elegooMaterial, elegooSubtype)
        if (spoolmanMaterials.isEmpty()) {
            return SpoolmanMatchResult.NoMatch
        }

        val cr = (colorRgb shr 16) and 0xFF
        val cg = (colorRgb shr 8) and 0xFF
        val cb = colorRgb and 0xFF

        // Filter by material + color proximity
        val candidates = data
            .filter { spoolmanMaterials.contains(it.material) }
            .filterNotNull()
            .mapNotNull { entry ->
                entry.colorHex?.toIntOrNull(16)?.let { hex ->
                    val er = (hex shr 16) and 0xFF
                    val eg = (hex shr 8) and 0xFF
                    val eb = hex and 0xFF
                    val dist = rgbDistance(cr, cg, cb, er, eg, eb)
                    entry to dist
                }
            }
            .filter { it.second <= tolerance }
            .sortedBy { it.second }

        val matched = candidates.map { it.first }

        return when {
            matched.size == 1 -> SpoolmanMatchResult.ExactMatch(matched[0])
            matched.isNotEmpty() -> SpoolmanMatchResult.MultipleMatches(matched.take(10))
            else -> SpoolmanMatchResult.NoMatch
        }
    }

    /**
     * Whether the database loaded successfully.
     */
    fun isLoaded(): Boolean = filaments != null

    /**
     * Error message if loading failed, null otherwise.
     */
    fun getError(): String? = loadError

    /**
     * Compute a relevance score for the search query against the searchable text.
     * Higher score = more relevant.
     */
    private fun computeRelevance(text: String, words: List<String>): Int {
        if (words.isEmpty()) return 0

        // All words must match (AND logic)
        val allMatch = words.all { text.contains(it) }
        if (!allMatch) return 0

        // Bonus for prefix matches
        var score = words.size // base: number of matching words
        for (word in words) {
            if (text.startsWith(word)) score += 10
            // Bonus for exact word boundary match
            if (text.split(Regex("\\s+")).any { it == word }) score += 5
        }

        // Bonus for shorter matches (more specific)
        score += (50 - text.length).coerceAtLeast(0)

        return score
    }

    private fun parseTempRange(obj: JSONObject, key: String): Pair<Int, Int>? {
        return if (obj.has(key) && !obj.isNull(key)) {
            val arr = obj.getJSONArray(key)
            if (arr.length() == 2) arr.getInt(0) to arr.getInt(1) else null
        } else null
    }

    private fun parseFilament(obj: JSONObject): SpoolmanFilament {
        return SpoolmanFilament(
            id = obj.optString("id", ""),
            manufacturer = obj.optString("manufacturer", ""),
            name = obj.optString("name", ""),
            material = obj.optString("material", ""),
            density = obj.optDouble("density", 0.0).toFloat(),
            weight = obj.optDouble("weight", 0.0).toFloat(),
            spoolWeight = if (obj.has("spool_weight") && !obj.isNull("spool_weight")) obj.optInt("spool_weight") else null,
            spoolType = if (obj.has("spool_type") && !obj.isNull("spool_type")) obj.optString("spool_type") else null,
            diameter = obj.optDouble("diameter", 0.0).toFloat(),
            colorHex = if (obj.has("color_hex") && !obj.isNull("color_hex")) obj.optString("color_hex") else null,
            extruderTemp = if (obj.has("extruder_temp") && !obj.isNull("extruder_temp")) obj.optInt("extruder_temp") else null,
            extruderTempRange = parseTempRange(obj, "extruder_temp_range"),
            bedTemp = if (obj.has("bed_temp") && !obj.isNull("bed_temp")) obj.optInt("bed_temp") else null,
            bedTempRange = parseTempRange(obj, "bed_temp_range"),
            finish = if (obj.has("finish") && !obj.isNull("finish")) obj.optString("finish") else null,
            pattern = if (obj.has("pattern") && !obj.isNull("pattern")) obj.optString("pattern") else null,
            translucent = obj.optBoolean("translucent", false),
            glow = obj.optBoolean("glow", false),
        )
    }
}
