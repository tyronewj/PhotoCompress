package com.cinoart.photocompress

enum class AppLanguage(val storageValue: String) {
    Chinese("zh"),
    English("en");

    companion object {
        fun fromStorage(value: String?): AppLanguage {
            return entries.firstOrNull { it.storageValue == value } ?: Chinese
        }
    }
}

internal enum class CompressionQualityPreset(val jpegQuality: Int) {
    Smaller(78),
    Balanced(86),
    HigherQuality(92)
}

internal class AppText(private val language: AppLanguage) {
    private val zh: Boolean
        get() = language == AppLanguage.Chinese

    val appTitle: String get() = if (zh) "本地图片压缩" else "Local Photo Compress"
    val noFolder: String get() = if (zh) "未选择目录" else "No folder selected"
    val selectedFolder: String get() = if (zh) "已选择目录" else "Selected folder"
    val initialStatus: String
        get() = if (zh) {
            "选择相册目录后开始压缩。JPEG 会保留 EXIF，已处理过的图片会跳过。"
        } else {
            "Select a photo folder to start. JPEG EXIF can be preserved, and processed images are skipped."
        }
    val folderReady: String get() = if (zh) "目录已选择，可以开始压缩。" else "Folder selected. Ready to compress."
    val unableOpenFolder: String get() = if (zh) "无法打开所选目录，请重新选择。" else "Cannot open the selected folder. Please choose again."
    val scanning: String get() = if (zh) "正在扫描和压缩..." else "Scanning and compressing..."
    val stopped: String get() = if (zh) "已停止，已完成的文件和备份会保留。" else "Stopped. Completed files and backups are kept."
    val clearingBackups: String get() = if (zh) "正在清空 bk 目录下的备份图片..." else "Deleting backup images under bk..."

    val folderTitle: String get() = if (zh) "相册目录" else "Photo folder"
    val choose: String get() = if (zh) "选择" else "Choose"
    val featureInfo: String get() = if (zh) "功能说明" else "Features"
    val languageTitle: String get() = if (zh) "界面语言" else "Language"
    val languageHint: String get() = if (zh) "切换后会保存到本机" else "Saved on this device"
    val chinese: String get() = if (zh) "中文" else "Chinese"
    val english: String get() = if (zh) "英文" else "English"

    val localCompressTitle: String get() = if (zh) "本地压缩" else "Local compression"
    val backupTitle: String get() = if (zh) "备份与恢复空间" else "Backups and space"
    val exifTitle: String get() = if (zh) "EXIF 与跳过规则" else "EXIF and skip rules"

    val imageQuality: String get() = if (zh) "图片压缩质量" else "Image compression quality"
    fun qualityPresetLabel(preset: CompressionQualityPreset): String {
        return when (preset) {
            CompressionQualityPreset.Smaller -> if (zh) "小体积" else "Smaller"
            CompressionQualityPreset.Balanced -> if (zh) "均衡" else "Balanced"
            CompressionQualityPreset.HigherQuality -> if (zh) "高画质" else "Higher quality"
        }
    }
    fun qualityPresetDescription(preset: CompressionQualityPreset): String {
        return when (preset) {
            CompressionQualityPreset.Smaller -> if (zh) "优先减小文件体积，适合释放更多空间" else "Prioritizes smaller files to free more space"
            CompressionQualityPreset.Balanced -> if (zh) "推荐，兼顾画质和体积" else "Recommended, balances quality and file size"
            CompressionQualityPreset.HigherQuality -> if (zh) "优先保留画质，压缩幅度较小" else "Prioritizes image quality with lighter compression"
        }
    }
    val preserveExif: String get() = if (zh) "保留 EXIF" else "Preserve EXIF"
    val preserveExifDescription: String
        get() = if (zh) "JPEG 压缩后复制拍摄时间、机型、定位等元数据" else "Copy time, camera model, location, and other JPEG metadata after compression"
    val includeSubfolders: String get() = if (zh) "包含子目录" else "Include subfolders"
    val includeSubfoldersDescription: String
        get() = if (zh) "会跳过 bk、隐藏目录和已处理图片" else "Skips bk, hidden folders, and processed images"

    val startCompress: String get() = if (zh) "开始压缩" else "Start"
    val stop: String get() = if (zh) "停止" else "Stop"
    val clearBackups: String get() = if (zh) "清空备份图片" else "Delete backup images"
    val clearingBackupsButton: String get() = if (zh) "正在清空备份..." else "Deleting backups..."

    val progressTitle: String get() = if (zh) "处理进度" else "Progress"
    val success: String get() = if (zh) "成功" else "Done"
    val skipped: String get() = if (zh) "跳过" else "Skipped"
    val failed: String get() = if (zh) "失败" else "Failed"
    val saved: String get() = if (zh) "节省" else "Saved"
    val original: String get() = if (zh) "原始" else "Original"
    val compressed: String get() = if (zh) "压缩后" else "Compressed"
    val backupDir: String get() = if (zh) "备份目录" else "Backup folder"
    val back: String get() = if (zh) "返回" else "Back"

    val featureLocalLines: List<String>
        get() = if (zh) {
            listOf(
                "通过系统文件夹选择器选择相册目录。",
                "图片只在手机本地处理，不上传网络。",
                "支持 JPEG 和 PNG，并提供三档图片压缩质量。"
            )
        } else {
            listOf(
                "Choose a photo folder with the system folder picker.",
                "Images are processed on the phone and are not uploaded.",
                "Supports JPEG and PNG with three image compression quality presets."
            )
        }

    val featureBackupLines: List<String>
        get() = if (zh) {
            listOf(
                "压缩前会把原图复制到所选目录的 bk 文件夹。",
                "可一键清空 bk 中的备份图片，释放手机空间。",
                "清空备份会保留处理记录，避免下次重复压缩。"
            )
        } else {
            listOf(
                "Original files are copied to the bk folder before compression.",
                "Backup images in bk can be deleted in one tap to free space.",
                "Processing records are kept, so files are not compressed again after cleanup."
            )
        }

    val featureExifLines: List<String>
        get() = if (zh) {
            listOf(
                "保留 EXIF 开关开启时，会复制 JPEG 的拍摄时间、机型、定位等元数据。",
                "已处理过的图片会自动跳过。",
                "bk 目录和以 . 开头的隐藏目录不会进入压缩队列。"
            )
        } else {
            listOf(
                "When Preserve EXIF is on, JPEG time, camera model, location, and other metadata are copied.",
                "Already processed images are skipped automatically.",
                "The bk folder and hidden folders starting with . are excluded."
            )
        }

    fun completeWithBackup(compressed: Int, skipped: Int, backupPath: String): String {
        return if (zh) {
            "完成：$compressed 张压缩成功，$skipped 张跳过，备份在 $backupPath。"
        } else {
            "Done: $compressed compressed, $skipped skipped. Backup: $backupPath."
        }
    }

    fun completeNoNew(skipped: Int): String {
        return if (zh) "完成：没有新压缩图片，$skipped 张已跳过。" else "Done: no new images compressed, $skipped skipped."
    }

    fun error(message: String): String = if (zh) "出错：$message" else "Error: $message"

    fun clearBackupSuccess(count: Int, bytes: String): String {
        return if (zh) "已删除 $count 张备份图片，释放 $bytes。" else "Deleted $count backup images and freed $bytes."
    }

    fun clearBackupPartial(count: Int, failed: Int): String {
        return if (zh) "已删除 $count 张备份图片，$failed 项删除失败。" else "Deleted $count backup images, $failed items failed."
    }

    fun clearBackupFailed(message: String): String = if (zh) "清空备份失败：$message" else "Failed to delete backups: $message"

    fun recordFailed(path: String, message: String): String = if (zh) "记录失败：$path - $message" else "Record failed: $path - $message"
    fun itemFailed(path: String, message: String): String = if (zh) "失败：$path - $message" else "Failed: $path - $message"
    fun skipNoName(path: String): String = if (zh) "跳过：$path 无文件名" else "Skipped: $path has no file name"
    fun skipUnsupported(path: String): String = if (zh) "跳过：$path 格式不支持" else "Skipped: $path format is not supported"
    fun skipProcessed(path: String): String = if (zh) "跳过：$path 已处理过" else "Skipped: $path already processed"
    fun skipDecode(path: String): String = if (zh) "跳过：$path 无法解码" else "Skipped: $path could not be decoded"
    fun encodeFailed(): String = if (zh) "压缩编码失败" else "Compression encoding failed"
    fun skipNotSmaller(path: String): String = if (zh) "跳过：$path 未变小，已记录" else "Skipped: $path was not smaller, recorded"
    fun doneFile(path: String, originalSize: String, finalSize: String): String = if (zh) {
        "完成：$path $originalSize -> $finalSize"
    } else {
        "Done: $path $originalSize -> $finalSize"
    }
    fun createBackupDirFailed(name: String): String = if (zh) "无法创建备份目录：$name" else "Cannot create backup folder: $name"
    fun createBackupRunDirFailed(path: String): String = if (zh) "无法创建本次备份目录：$path" else "Cannot create this backup folder: $path"
    fun createBackupSubdirFailed(name: String): String = if (zh) "无法创建备份子目录：$name" else "Cannot create backup subfolder: $name"
    fun createBackupFileFailed(path: String): String = if (zh) "无法创建备份文件：$path" else "Cannot create backup file: $path"
    fun openBackupStreamFailed(): String = if (zh) "无法打开备份流" else "Cannot open backup stream"
    fun openWriteStreamFailed(): String = if (zh) "无法打开写入流" else "Cannot open write stream"
    fun createMarkerFileFailed(name: String): String = if (zh) "无法创建处理记录文件：$name" else "Cannot create processed marker file: $name"
    fun writeMarkerFileFailed(name: String): String = if (zh) "无法写入处理记录文件：$name" else "Cannot write processed marker file: $name"
}
