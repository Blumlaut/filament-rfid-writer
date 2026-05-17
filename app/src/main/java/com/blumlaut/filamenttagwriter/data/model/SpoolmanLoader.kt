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
