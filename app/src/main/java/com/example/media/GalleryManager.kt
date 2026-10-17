package com.example.media

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GalleryItem(
    val id: String,
    val filePath: String,
    val timestamp: Long,
    val formattedTime: String,
    val isVideo: Boolean = false,
    val durationSeconds: Int = 0
)

class GalleryManager(private val context: Context) {
    private val galleryDir: File = File(context.filesDir, "drone_gallery").apply {
        if (!exists()) mkdirs()
    }

    val items = mutableListOf<GalleryItem>()

    init {
        loadExistingItems()
    }

    private fun loadExistingItems() {
        items.clear()
        val files = galleryDir.listFiles() ?: return
        files.sortedByDescending { it.lastModified() }.forEach { file ->
            val isVid = file.name.endsWith(".mp4")
            val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            items.add(
                GalleryItem(
                    id = file.name,
                    filePath = file.absolutePath,
                    timestamp = file.lastModified(),
                    formattedTime = sdf.format(Date(file.lastModified())),
                    isVideo = isVid
                )
            )
        }
    }

    fun savePhoto(bitmap: Bitmap): GalleryItem? {
        return try {
            val timestamp = System.currentTimeMillis()
            val fileName = "DRONE_SHOT_$timestamp.jpg"
            val file = File(galleryDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            val item = GalleryItem(
                id = fileName,
                filePath = file.absolutePath,
                timestamp = timestamp,
                formattedTime = sdf.format(Date(timestamp)),
                isVideo = false
            )
            items.add(0, item)
            item
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun recordSimulatedClip(durationSeconds: Int): GalleryItem? {
        return try {
            val timestamp = System.currentTimeMillis()
            val fileName = "DRONE_CLIP_$timestamp.mp4"
            val file = File(galleryDir, fileName)
            // Create dummy file placeholder for video clip
            file.writeText("SIMULATED_VIDEO_DATA_$timestamp")

            val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
            val item = GalleryItem(
                id = fileName,
                filePath = file.absolutePath,
                timestamp = timestamp,
                formattedTime = sdf.format(Date(timestamp)),
                isVideo = true,
                durationSeconds = durationSeconds
            )
            items.add(0, item)
            item
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteItem(item: GalleryItem) {
        val file = File(item.filePath)
        if (file.exists()) file.delete()
        items.remove(item)
    }
}
