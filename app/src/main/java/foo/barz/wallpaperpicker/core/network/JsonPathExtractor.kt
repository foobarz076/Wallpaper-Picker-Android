package foo.barz.wallpaperpicker.core.network

import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight JSON parser supporting dot-notation and array indexing for extracting URLs.
 *
 * Supported formats:
 * - "images[0].url"
 * - "data[*].path" (picks a random element from the JSON array)
 * - "photo.urls.regular"
 * - "[0].url"
 */
object JsonPathExtractor {

    fun extractString(jsonStr: String, path: String): String? {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) return null

        val root: Any = runCatching {
            val content = jsonStr.trimStart()
            if (content.startsWith("[")) JSONArray(content) else JSONObject(content)
        }.getOrNull() ?: return null

        val segments = parsePathSegments(trimmed)
        var current: Any? = root

        for (segment in segments) {
            if (current == null) return null
            current = when (segment) {
                is Segment.Property -> {
                    if (current is JSONObject) current.opt(segment.name) else null
                }
                is Segment.ArrayIndex -> {
                    if (current is JSONArray) {
                        if (segment.index in 0 until current.length()) {
                            current.opt(segment.index)
                        } else null
                    } else null
                }
                is Segment.ArrayRandom -> {
                    if (current is JSONArray && current.length() > 0) {
                        val randomIndex = (0 until current.length()).random()
                        current.opt(randomIndex)
                    } else null
                }
            }
        }

        return current?.toString()?.takeIf { it.isNotEmpty() && it != "null" }
    }

    private sealed interface Segment {
        data class Property(val name: String) : Segment
        data class ArrayIndex(val index: Int) : Segment
        data object ArrayRandom : Segment
    }

    private fun parsePathSegments(path: String): List<Segment> {
        val result = mutableListOf<Segment>()
        val normalized = path.replace("[", ".[").replace("..", ".")
        val tokens = normalized.split(".").filter { it.isNotEmpty() }

        for (token in tokens) {
            if (token.startsWith("[") && token.endsWith("]")) {
                val inner = token.substring(1, token.length - 1).trim()
                if (inner == "*") {
                    result.add(Segment.ArrayRandom)
                } else {
                    inner.toIntOrNull()?.let { result.add(Segment.ArrayIndex(it)) }
                }
            } else {
                result.add(Segment.Property(token))
            }
        }
        return result
    }
}
