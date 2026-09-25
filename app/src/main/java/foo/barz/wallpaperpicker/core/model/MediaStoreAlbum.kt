package foo.barz.wallpaperpicker.core.model

import android.net.Uri

/**
 * Represents a photo album or bucket queried from the system MediaStore.
 */
data class MediaStoreAlbum(
    val id: String?,
    val name: String,
    val count: Int,
    val coverUri: Uri? = null
)
