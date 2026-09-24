package foo.barz.wallpaperpicker

import foo.barz.wallpaperpicker.core.database.LocalFolderImageRecord
import foo.barz.wallpaperpicker.core.model.MediaStoreAlbum
import foo.barz.wallpaperpicker.core.model.WallpaperSourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class Phase3Test {

    @Test
    fun testWallpaperSourceTypes() {
        val types = WallpaperSourceType.values()
        assertEquals(4, types.size)
        assertEquals("本地文件夹", WallpaperSourceType.LOCAL_FOLDER.displayName)
        assertEquals("系统相册", WallpaperSourceType.MEDIA_STORE.displayName)
        assertEquals("自建相册 (Immich)", WallpaperSourceType.IMMICH.displayName)
        assertEquals("通用网络图源 (HTTP)", WallpaperSourceType.HTTP_API.displayName)
    }

    @Test
    fun testMediaStoreAlbumModel() {
        val allPhotos = MediaStoreAlbum(id = null, name = "全部照片", count = 120)
        assertNull(allPhotos.id)
        assertEquals("全部照片", allPhotos.name)
        assertEquals(120, allPhotos.count)

        val cameraAlbum = MediaStoreAlbum(id = "123", name = "Camera", count = 45)
        assertEquals("123", cameraAlbum.id)
        assertEquals("Camera", cameraAlbum.name)
        assertEquals(45, cameraAlbum.count)
    }

    @Test
    fun testLocalFolderImageRecordModel() {
        val record = LocalFolderImageRecord(
            folderUri = "content://com.android.externalstorage.documents/tree/primary%3APictures",
            documentId = "primary:Pictures/photo1.jpg",
            documentUri = "content://com.android.externalstorage.documents/tree/primary%3APictures/document/primary%3APictures%2Fphoto1.jpg",
            fileName = "photo1.jpg",
            mimeType = "image/jpeg"
        )

        assertEquals("primary:Pictures/photo1.jpg", record.documentId)
        assertEquals("photo1.jpg", record.fileName)
        assertEquals("image/jpeg", record.mimeType)
        assertNotNull(record.lastIndexed)
    }
}
