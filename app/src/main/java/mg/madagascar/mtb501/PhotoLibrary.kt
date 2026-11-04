package mg.madagascar.mtb501

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only catalogue hosted by the official 501MTB website.
 * Image binaries stay on GitHub Pages; Firestore will store only selected image IDs/URLs.
 */
data class PhotoLibraryImage(
    val id: String,
    val category: String,
    val fileName: String,
    val ratio: String,
    val width: Int,
    val height: Int,
    val alt: String,
    val tags: List<String>,
    val active: Boolean,
    val url: String
)

data class PhotoLibrarySlot(
    val id: String,
    val category: String,
    val requiredRatio: String,
    val selectionMode: String
)

data class PhotoLibrary(
    val schemaVersion: Int,
    val baseUrl: String,
    val supportedRatios: List<String>,
    val images: List<PhotoLibraryImage>,
    val slots: Map<String, PhotoLibrarySlot>
) {
    fun imagesForSlot(slotId: String): List<PhotoLibraryImage> {
        val slot = slots[slotId] ?: return emptyList()
        return images.filter { image ->
            image.active &&
                image.category == slot.category &&
                image.ratio == slot.requiredRatio &&
                hasValidDimensions(image)
        }
    }

    fun imagesForRatio(requiredRatio: String): List<PhotoLibraryImage> =
        images.filter { image ->
            image.active && image.ratio == requiredRatio && hasValidDimensions(image)
        }

    private fun hasValidDimensions(image: PhotoLibraryImage): Boolean {
        if (image.width <= 0 || image.height <= 0) return false
        val expected = when (image.ratio) {
            "16:9" -> 16.0 / 9.0
            "3:4" -> 3.0 / 4.0
            "9:16" -> 9.0 / 16.0
            "1:1" -> 1.0
            else -> return false
        }
        val actual = image.width.toDouble() / image.height.toDouble()
        return kotlin.math.abs(actual - expected) <= 0.01
    }
}

class PhotoLibraryRepository(
    private val catalogueUrl: String =
        "https://501mtb.github.io/assets/app/photos/photo-library.json"
) {
    suspend fun load(): PhotoLibrary = withContext(Dispatchers.IO) {
        val connection = (URL(catalogueUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            if (connection.responseCode !in 200..299) {
                error("Photo library is unavailable (HTTP ${connection.responseCode}).")
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            parseCatalogue(JSONObject(json))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCatalogue(root: JSONObject): PhotoLibrary {
        val baseUrl = root.getString("baseUrl").trimEnd('/') + "/"
        val categoryJson = root.getJSONObject("categories")
        val categories = categoryJson.keys().asSequence().associateWith { key ->
            categoryJson.getJSONObject(key).getString("requiredRatio")
        }

        val slotJson = root.optJSONObject("slots") ?: JSONObject()
        val slots = slotJson.keys().asSequence().associateWith { key ->
            val item = slotJson.getJSONObject(key)
            PhotoLibrarySlot(
                id = key,
                category = item.getString("category"),
                requiredRatio = item.getString("requiredRatio"),
                selectionMode = item.optString("selectionMode", "single")
            )
        }

        val imageArray = root.optJSONArray("images") ?: JSONArray()
        val images = buildList {
            for (index in 0 until imageArray.length()) {
                val item = imageArray.optJSONObject(index) ?: continue
                val category = item.optString("category")
                val fileName = item.optString("fileName", item.optString("filename"))
                if (category !in categories || fileName.isBlank()) continue

                val width = item.optInt("width", 0)
                val height = item.optInt("height", 0)
                val declaredRatio = item.optString("ratio", "")
                val derivedRatio = deriveRatio(width, height)
                if (derivedRatio == null || declaredRatio != derivedRatio) continue

                val tagsJson = item.optJSONArray("tags") ?: JSONArray()
                val tags = (0 until tagsJson.length()).mapNotNull { tagsJson.optString(it).takeIf(String::isNotBlank) }

                add(
                    PhotoLibraryImage(
                        id = item.optString("id", fileName),
                        category = category,
                        fileName = fileName,
                        ratio = derivedRatio,
                        width = width,
                        height = height,
                        alt = item.optString("alt", fileName),
                        tags = tags,
                        active = item.optBoolean("active", true),
                        url = baseUrl + category + "/" + fileName
                    )
                )
            }
        }

        return PhotoLibrary(
            schemaVersion = root.optInt("schemaVersion", 1),
            baseUrl = baseUrl,
            supportedRatios = root.optJSONArray("supportedRatios").toStringList(),
            images = images,
            slots = slots
        )
    }

    private fun deriveRatio(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        val ratio = width.toDouble() / height.toDouble()
        return when {
            kotlin.math.abs(ratio - 16.0 / 9.0) <= 0.01 -> "16:9"
            kotlin.math.abs(ratio - 3.0 / 4.0) <= 0.01 -> "3:4"
            kotlin.math.abs(ratio - 9.0 / 16.0) <= 0.01 -> "9:16"
            kotlin.math.abs(ratio - 1.0) <= 0.01 -> "1:1"
            else -> null
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
    }
}
