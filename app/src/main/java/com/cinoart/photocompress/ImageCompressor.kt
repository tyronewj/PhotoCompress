package com.cinoart.photocompress

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CompressSettings(
    val jpegQuality: Int = 86,
    val recursive: Boolean = true,
    val preserveExif: Boolean = true,
    val backupFolderName: String = "bk"
)

data class CompressProgress(
    val total: Int = 0,
    val processed: Int = 0,
    val compressed: Int = 0,
    val skipped: Int = 0,
    val failed: Int = 0,
    val originalBytes: Long = 0,
    val finalBytes: Long = 0,
    val currentPath: String = "",
    val backupPath: String = "",
    val messages: List<String> = emptyList()
) {
    val savedBytes: Long
        get() = (originalBytes - finalBytes).coerceAtLeast(0)

    val savedPercent: Float
        get() = if (originalBytes <= 0L) 0f else savedBytes * 100f / originalBytes
}

class ImageCompressor(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun compressTree(
        root: DocumentFile,
        settings: CompressSettings,
        onProgress: suspend (CompressProgress) -> Unit
    ): CompressProgress {
        val runFolderName = timestamp()
        val backupRoot = root.findFile(settings.backupFolderName)
            ?: root.createDirectory(settings.backupFolderName)
            ?: error("无法创建备份目录：${settings.backupFolderName}")
        val markerStore = ProcessMarkerStore(backupRoot)
        val processedMarkers = markerStore.load().toMutableMap()
        val entries = collectImages(root, settings)
        var backupRunRoot: DocumentFile? = null

        fun ensureBackupRunRoot(): DocumentFile {
            backupRunRoot?.let { return it }
            val created = backupRoot.createDirectory(runFolderName)
                ?: error("无法创建本次备份目录：${settings.backupFolderName}/$runFolderName")
            backupRunRoot = created
            return created
        }

        var progress = CompressProgress(
            total = entries.size,
            backupPath = settings.backupFolderName
        )
        onProgress(progress)

        for (entry in entries) {
            currentCoroutineContext().ensureActive()
            val originalSize = entry.file.length().coerceAtLeast(0)
            val currentPath = entry.relativePath
            progress = progress.copy(currentPath = currentPath)
            onProgress(progress)

            val result = runCatching {
                compressOne(entry, processedMarkers, ::ensureBackupRunRoot, settings, originalSize)
            }

            progress = result.fold(
                onSuccess = { outcome ->
                    outcome.marker?.let { marker ->
                        processedMarkers[entry.relativePath] = marker
                    }
                    val markerMessage = outcome.marker?.let {
                        runCatching { markerStore.save(processedMarkers) }
                            .exceptionOrNull()
                            ?.let { error -> "记录失败：${entry.relativePath} - ${error.message ?: error.javaClass.simpleName}" }
                    }
                    val messages = if (markerMessage == null) {
                        progress.messages + outcome.message
                    } else {
                        progress.messages + outcome.message + markerMessage
                    }
                    progress.copy(
                        processed = progress.processed + 1,
                        compressed = progress.compressed + if (outcome.compressed) 1 else 0,
                        skipped = progress.skipped + if (outcome.compressed) 0 else 1,
                        originalBytes = progress.originalBytes + originalSize,
                        finalBytes = progress.finalBytes + outcome.finalSize,
                        backupPath = backupRunRoot?.let { "${settings.backupFolderName}/$runFolderName" } ?: settings.backupFolderName,
                        messages = messages.takeLast(MAX_MESSAGES)
                    )
                },
                onFailure = { error ->
                    val message = "失败：$currentPath - ${error.message ?: error.javaClass.simpleName}"
                    progress.copy(
                        processed = progress.processed + 1,
                        failed = progress.failed + 1,
                        originalBytes = progress.originalBytes + originalSize,
                        finalBytes = progress.finalBytes + originalSize,
                        messages = (progress.messages + message).takeLast(MAX_MESSAGES)
                    )
                }
            )
            onProgress(progress)
        }

        return progress.copy(currentPath = "")
    }

    private fun collectImages(root: DocumentFile, settings: CompressSettings): List<ImageEntry> {
        val result = mutableListOf<ImageEntry>()

        fun walk(dir: DocumentFile, relativeDir: String, depth: Int) {
            for (child in dir.listFiles()) {
                val name = child.name ?: continue
                if (child.isDirectory) {
                    if (depth == 0 && name == settings.backupFolderName) continue
                    if (settings.recursive) {
                        val childPath = joinPath(relativeDir, name)
                        walk(child, childPath, depth + 1)
                    }
                    continue
                }

                if (child.isFile && child.canRead() && isSupportedImage(name)) {
                    result += ImageEntry(child, joinPath(relativeDir, name))
                }
            }
        }

        walk(root, "", 0)
        return result.sortedBy { it.relativePath.lowercase(Locale.US) }
    }

    private fun compressOne(
        entry: ImageEntry,
        processedMarkers: Map<String, ProcessedMarker>,
        ensureBackupRunRoot: () -> DocumentFile,
        settings: CompressSettings,
        originalSize: Long
    ): CompressOutcome {
        val source = entry.file
        val sourceName = source.name ?: return CompressOutcome(false, originalSize, "跳过：${entry.relativePath} 无文件名")
        val format = imageFormat(sourceName)
            ?: return CompressOutcome(false, originalSize, "跳过：${entry.relativePath} 格式不支持")

        if (processedMarkers[entry.relativePath]?.matches(source) == true) {
            return CompressOutcome(
                compressed = false,
                finalSize = originalSize,
                message = "跳过：${entry.relativePath} 已处理过"
            )
        }

        val tempFile = File.createTempFile("photo-compress-", ".${format.extension}", context.cacheDir)
        try {
            val bitmap = resolver.openInputStream(source.uri).use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return CompressOutcome(false, originalSize, "跳过：${entry.relativePath} 无法解码")

            bitmap.use {
                FileOutputStream(tempFile).use { output ->
                    val ok = when (format) {
                        ImageFormat.Jpeg -> it.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, output)
                        ImageFormat.Png -> it.compress(Bitmap.CompressFormat.PNG, 100, output)
                    }
                    if (!ok) error("压缩编码失败")
                }
            }

            if (format == ImageFormat.Jpeg && settings.preserveExif) {
                copyJpegExif(source.uri, tempFile)
            }

            val compressedSize = tempFile.length()
            if (compressedSize <= 0L || compressedSize >= originalSize) {
                return CompressOutcome(
                    compressed = false,
                    finalSize = originalSize,
                    message = "跳过：${entry.relativePath} 未变小，已记录",
                    marker = source.toProcessedMarker(entry.relativePath)
                )
            }

            val backupRunRoot = ensureBackupRunRoot()
            val backupFile = createBackupFile(backupRunRoot, entry.relativePath, sourceName, source.type)
            copyDocument(source.uri, backupFile.uri)
            writeFileToDocument(tempFile, source.uri)

            return CompressOutcome(
                compressed = true,
                finalSize = compressedSize,
                message = "完成：${entry.relativePath} ${formatBytes(originalSize)} -> ${formatBytes(compressedSize)}",
                marker = source.toProcessedMarker(entry.relativePath, fallbackSize = compressedSize)
            )
        } finally {
            tempFile.delete()
        }
    }

    private fun copyJpegExif(sourceUri: Uri, targetFile: File) {
        val sourceExif = resolver.openInputStream(sourceUri).use { input ->
            if (input == null) null else ExifInterface(input)
        } ?: return
        val targetExif = ExifInterface(targetFile.absolutePath)

        for (tag in JPEG_EXIF_TAGS) {
            sourceExif.getAttribute(tag)?.let { targetExif.setAttribute(tag, it) }
        }
        targetExif.saveAttributes()
    }

    private fun createBackupFile(
        backupRunRoot: DocumentFile,
        relativePath: String,
        fileName: String,
        mimeType: String?
    ): DocumentFile {
        val parentPath = relativePath.substringBeforeLast("/", missingDelimiterValue = "")
        val parent = parentPath
            .split("/")
            .filter { it.isNotBlank() }
            .fold(backupRunRoot) { dir, segment ->
                dir.findFile(segment)?.takeIf { it.isDirectory }
                    ?: dir.createDirectory(segment)
                    ?: error("无法创建备份子目录：$segment")
            }
        return parent.createFile(mimeType ?: mimeFor(fileName), fileName)
            ?: error("无法创建备份文件：$relativePath")
    }

    private fun copyDocument(from: Uri, to: Uri) {
        resolver.openInputStream(from).use { input ->
            resolver.openOutputStream(to, "wt").use { output ->
                if (input == null || output == null) error("无法打开备份流")
                input.copyTo(output)
            }
        }
    }

    private fun writeFileToDocument(from: File, to: Uri) {
        from.inputStream().use { input ->
            resolver.openOutputStream(to, "wt").use { output ->
                if (output == null) error("无法写入原图")
                input.copyTo(output)
            }
        }
    }

    private data class ImageEntry(
        val file: DocumentFile,
        val relativePath: String
    )

    private data class CompressOutcome(
        val compressed: Boolean,
        val finalSize: Long,
        val message: String,
        val marker: ProcessedMarker? = null
    )

    private data class ProcessedMarker(
        val relativePath: String,
        val size: Long,
        val lastModified: Long,
        val markedAt: Long
    ) {
        fun matches(file: DocumentFile): Boolean {
            val currentSize = file.length().coerceAtLeast(0)
            val currentLastModified = file.lastModified().coerceAtLeast(0)
            val lastModifiedMatches = lastModified == 0L ||
                currentLastModified == 0L ||
                lastModified == currentLastModified
            return currentSize == size && lastModifiedMatches
        }
    }

    private inner class ProcessMarkerStore(private val backupRoot: DocumentFile) {
        fun load(): Map<String, ProcessedMarker> {
            val markerFile = backupRoot.findFile(PROCESS_MARKER_FILE) ?: return emptyMap()
            val text = resolver.openInputStream(markerFile.uri).use { input ->
                input?.bufferedReader()?.readText().orEmpty()
            }
            return text.lineSequence()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.split('\t')
                    if (parts.size < 4) return@mapNotNull null
                    val relativePath = Uri.decode(parts[0]) ?: return@mapNotNull null
                    val size = parts[1].toLongOrNull() ?: return@mapNotNull null
                    val lastModified = parts[2].toLongOrNull() ?: 0L
                    val markedAt = parts[3].toLongOrNull() ?: 0L
                    relativePath to ProcessedMarker(relativePath, size, lastModified, markedAt)
                }
                .toMap()
        }

        fun save(markers: Map<String, ProcessedMarker>) {
            val markerFile = backupRoot.findFile(PROCESS_MARKER_FILE)
                ?: backupRoot.createFile("text/tab-separated-values", PROCESS_MARKER_FILE)
                ?: error("无法创建处理记录文件：$PROCESS_MARKER_FILE")
            val text = buildString {
                appendLine("# PhotoCompress processed marker v1")
                markers.values
                    .sortedBy { it.relativePath.lowercase(Locale.US) }
                    .forEach { marker ->
                        append(Uri.encode(marker.relativePath))
                        append('\t')
                        append(marker.size)
                        append('\t')
                        append(marker.lastModified)
                        append('\t')
                        append(marker.markedAt)
                        appendLine()
                    }
            }
            resolver.openOutputStream(markerFile.uri, "wt").use { output ->
                if (output == null) error("无法写入处理记录文件：$PROCESS_MARKER_FILE")
                output.write(text.toByteArray(Charsets.UTF_8))
            }
        }
    }

    private enum class ImageFormat(val extension: String) {
        Jpeg("jpg"),
        Png("png")
    }

    private fun imageFormat(name: String): ImageFormat? {
        return when (name.substringAfterLast('.', "").lowercase(Locale.US)) {
            "jpg", "jpeg" -> ImageFormat.Jpeg
            "png" -> ImageFormat.Png
            else -> null
        }
    }

    private fun isSupportedImage(name: String): Boolean = imageFormat(name) != null

    private fun mimeFor(name: String): String {
        return when (imageFormat(name)) {
            ImageFormat.Jpeg -> "image/jpeg"
            ImageFormat.Png -> "image/png"
            null -> "application/octet-stream"
        }
    }

    private fun joinPath(parent: String, child: String): String {
        return if (parent.isBlank()) child else "$parent/$child"
    }

    private fun DocumentFile.toProcessedMarker(relativePath: String, fallbackSize: Long? = null): ProcessedMarker {
        val size = length().takeIf { it > 0L } ?: fallbackSize ?: 0L
        return ProcessedMarker(
            relativePath = relativePath,
            size = size,
            lastModified = lastModified().coerceAtLeast(0),
            markedAt = System.currentTimeMillis()
        )
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }

    companion object {
        private const val MAX_MESSAGES = 8
        private const val PROCESS_MARKER_FILE = ".photo_compress_processed.tsv"

        private val JPEG_EXIF_TAGS = arrayOf(
            ExifInterface.TAG_APERTURE_VALUE,
            ExifInterface.TAG_ARTIST,
            ExifInterface.TAG_BITS_PER_SAMPLE,
            ExifInterface.TAG_BRIGHTNESS_VALUE,
            ExifInterface.TAG_COLOR_SPACE,
            ExifInterface.TAG_COPYRIGHT,
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
            ExifInterface.TAG_EXPOSURE_MODE,
            ExifInterface.TAG_EXPOSURE_PROGRAM,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_FLASHPIX_VERSION,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_IMAGE_DESCRIPTION,
            ExifInterface.TAG_IMAGE_LENGTH,
            ExifInterface.TAG_IMAGE_WIDTH,
            ExifInterface.TAG_ISO_SPEED_RATINGS,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_PIXEL_X_DIMENSION,
            ExifInterface.TAG_PIXEL_Y_DIMENSION,
            ExifInterface.TAG_RESOLUTION_UNIT,
            ExifInterface.TAG_SCENE_CAPTURE_TYPE,
            ExifInterface.TAG_SOFTWARE,
            ExifInterface.TAG_SUBSEC_TIME,
            ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
            ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_X_RESOLUTION,
            ExifInterface.TAG_Y_RESOLUTION
        )

        fun formatBytes(bytes: Long): String {
            if (bytes < 1024L) return "$bytes B"
            val kb = bytes / 1024f
            if (kb < 1024f) return String.format(Locale.US, "%.1f KB", kb)
            return String.format(Locale.US, "%.2f MB", kb / 1024f)
        }
    }
}

private inline fun Bitmap.use(block: (Bitmap) -> Unit) {
    try {
        block(this)
    } finally {
        recycle()
    }
}
