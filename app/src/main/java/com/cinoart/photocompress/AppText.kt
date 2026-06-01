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

internal enum class CompressionQualityPreset(val jpegQuality: Int, val videoBitrate: Int) {
    Smaller(78, 1_200_000),
    Balanced(86, 2_000_000),
    HigherQuality(92, 4_000_000)
}

internal class AppText(private val language: AppLanguage) {
    private val zh: Boolean
        get() = language == AppLanguage.Chinese

    val appTitle: String get() = if (zh) "本地压缩" else "Local Compress"
    val noFolder: String get() = if (zh) "未选择目录" else "No folder selected"
    val selectedFolder: String get() = if (zh) "已选择目录" else "Selected folder"
    val initialStatus: String
        get() = if (zh) {
            "选择相册目录后开始压缩。JPEG 会保留 EXIF，已处理过的图片会跳过。"
        } else {
            "Select a photo folder to start. JPEG EXIF can be preserved, and processed images are skipped."
        }
    val videoInitialStatus: String
        get() = if (zh) {
            "选择视频目录后开始压缩。支持 MP4/M4V，已处理过的视频会跳过。"
        } else {
            "Select a video folder to start. MP4/M4V files are supported, and processed videos are skipped."
        }
    val folderReady: String get() = if (zh) "目录已选择，可以开始压缩。" else "Folder selected. Ready to compress."
    val videoFolderReady: String get() = if (zh) "视频目录已选择，可以开始压缩。" else "Video folder selected. Ready to compress."
    val unableOpenFolder: String get() = if (zh) "无法打开所选目录，请重新选择。" else "Cannot open the selected folder. Please choose again."
    val scanning: String get() = if (zh) "正在扫描和压缩..." else "Scanning and compressing..."
    val videoScanning: String get() = if (zh) "正在扫描和压缩视频..." else "Scanning and compressing videos..."
    val videoScanningFiles: String get() = if (zh) "正在扫描视频文件..." else "Scanning video files..."
    val videoRescanRequired: String get() = if (zh) "包含子目录设置已变化，请重新选择视频目录。" else "Include subfolders changed. Choose the video folder again."
    val stopped: String get() = if (zh) "已停止，已完成的文件和备份会保留。" else "Stopped. Completed files and backups are kept."
    val clearingBackups: String get() = if (zh) "正在清空 bk 目录下的备份图片..." else "Deleting backup images under bk..."
    val clearingVideoBackups: String get() = if (zh) "正在清空 bk 目录下的备份视频..." else "Deleting backup videos under bk..."

    val folderTitle: String get() = if (zh) "相册目录" else "Photo folder"
    val videoFolderTitle: String get() = if (zh) "视频目录" else "Video folder"
    val choose: String get() = if (zh) "选择" else "Choose"
    val featureInfo: String get() = if (zh) "功能说明" else "Features"
    val settings: String get() = if (zh) "设置" else "Settings"
    val imageCompression: String get() = if (zh) "图片压缩" else "Image compression"
    val videoCompression: String get() = if (zh) "视频压缩" else "Video compression"
    val videoCompressionPreparing: String
        get() = if (zh) "视频压缩功能准备中。" else "Video compression is coming soon."
    val languageTitle: String get() = if (zh) "界面语言" else "Language"
    val languageHint: String get() = if (zh) "切换后会保存到本机" else "Saved on this device"
    val chinese: String get() = if (zh) "中文" else "Chinese"
    val english: String get() = if (zh) "英文" else "English"

    val localCompressTitle: String get() = if (zh) "本地压缩" else "Local compression"
    val backupTitle: String get() = if (zh) "备份与恢复空间" else "Backups and space"
    val exifTitle: String get() = if (zh) "EXIF 与跳过规则" else "EXIF and skip rules"

    val imageQuality: String get() = if (zh) "压缩质量" else "Compression quality"
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
            CompressionQualityPreset.Balanced -> if (zh) "推荐，兼顾画质、码率和体积" else "Recommended, balances image quality, video bitrate, and file size"
            CompressionQualityPreset.HigherQuality -> if (zh) "优先保留画质和视频码率，压缩幅度较小" else "Prioritizes image quality and video bitrate with lighter compression"
        }
    }
    val preserveExif: String get() = if (zh) "保留 EXIF" else "Preserve EXIF"
    val preserveExifDescription: String
        get() = if (zh) "JPEG 压缩后复制拍摄时间、机型、定位等元数据" else "Copy time, camera model, location, and other JPEG metadata after compression"
    val includeSubfolders: String get() = if (zh) "包含子目录" else "Include subfolders"
    val includeSubfoldersDescription: String
        get() = if (zh) "会跳过 bk、隐藏目录和已处理图片" else "Skips bk, hidden folders, and processed images"
    val videoIncludeSubfoldersDescription: String
        get() = if (zh) "扫描所选目录及其子目录中的视频" else "Scan videos in the selected folder and subfolders"

    val startCompress: String get() = if (zh) "开始压缩" else "Start"
    val stop: String get() = if (zh) "停止" else "Stop"
    val clearBackups: String get() = if (zh) "清空备份图片" else "Delete backup images"
    val clearingBackupsButton: String get() = if (zh) "正在清空备份..." else "Deleting backups..."
    val clearVideoBackups: String get() = if (zh) "清空备份视频" else "Delete backup videos"
    val clearingVideoBackupsButton: String get() = if (zh) "正在清空备份..." else "Deleting backups..."
    val videoSelectionTitle: String get() = if (zh) "选择要压缩的视频" else "Videos to compress"
    val videoSortTitle: String get() = if (zh) "排序" else "Sort"
    val videoSortBySize: String get() = if (zh) "按大小" else "By size"
    val videoSortByPath: String get() = if (zh) "按路径" else "By path"
    val selectAllVideos: String get() = if (zh) "全选" else "Select all"
    val clearVideoSelection: String get() = if (zh) "取消全选" else "Clear"
    val previousPage: String get() = if (zh) "上一页" else "Previous"
    val nextPage: String get() = if (zh) "下一页" else "Next"
    val noVideosFound: String get() = if (zh) "未找到支持的视频。当前支持 MP4/M4V。" else "No supported videos found. MP4/M4V are supported."
    val noVideoSelected: String get() = if (zh) "请先选择至少一个视频。" else "Select at least one video first."

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
                "图片和视频只在手机本地处理，不上传网络。",
                "图片支持 JPEG 和 PNG，视频支持 MP4 和 M4V，并提供三档压缩质量。"
            )
        } else {
            listOf(
                "Choose a photo folder with the system folder picker.",
                "Images and videos are processed on the phone and are not uploaded.",
                "Supports JPEG/PNG images and MP4/M4V videos with three compression quality presets."
            )
        }

    val featureBackupLines: List<String>
        get() = if (zh) {
            listOf(
                "压缩前会把原图或原视频复制到所选目录的 bk 文件夹。",
                "可一键清空 bk 中的备份图片或备份视频，释放手机空间。",
                "清空备份会保留处理记录，避免下次重复压缩。"
            )
        } else {
            listOf(
                "Original images or videos are copied to the bk folder before compression.",
                "Backup images or videos in bk can be deleted in one tap to free space.",
                "Processing records are kept, so files are not compressed again after cleanup."
            )
        }

    val featureExifLines: List<String>
        get() = if (zh) {
            listOf(
                "保留 EXIF 开关开启时，会复制 JPEG 的拍摄时间、机型、定位等元数据。",
                "已处理过的图片和视频会自动跳过。",
                "bk 目录和以 . 开头的隐藏目录不会进入压缩队列。"
            )
        } else {
            listOf(
                "When Preserve EXIF is on, JPEG time, camera model, location, and other metadata are copied.",
                "Already processed images and videos are skipped automatically.",
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

    fun videoCompleteWithBackup(compressed: Int, skipped: Int, backupPath: String): String {
        return if (zh) {
            "完成：$compressed 个视频压缩成功，$skipped 个跳过，备份在 $backupPath。"
        } else {
            "Done: $compressed videos compressed, $skipped skipped. Backup: $backupPath."
        }
    }

    fun videoCompleteNoNew(skipped: Int): String {
        return if (zh) "完成：没有新压缩视频，$skipped 个已跳过。" else "Done: no new videos compressed, $skipped skipped."
    }

    fun videoCompleteWithFailures(compressed: Int, skipped: Int, failed: Int): String {
        return if (zh) {
            "完成：$compressed 个视频压缩成功，$skipped 个跳过，$failed 个失败。请查看下方失败记录。"
        } else {
            "Done: $compressed videos compressed, $skipped skipped, $failed failed. Check the failure records below."
        }
    }

    fun videoFolderReadyWithCount(count: Int): String {
        return if (zh) "视频目录已选择，已扫描到 $count 个视频。" else "Video folder selected. Found $count videos."
    }

    fun videoSelectionCount(selected: Int, total: Int): String {
        return if (zh) "已选择 $selected / $total 个视频" else "$selected / $total videos selected"
    }

    fun videoPageInfo(page: Int, pageCount: Int, start: Int, end: Int, total: Int): String {
        return if (zh) {
            "第 $page / $pageCount 页，显示 $start-$end / $total"
        } else {
            "Page $page of $pageCount, showing $start-$end of $total"
        }
    }

    fun error(message: String): String = if (zh) "出错：$message" else "Error: $message"
    fun videoExportFailed(details: String): String = if (zh) "视频导出失败：$details" else "Video export failed: $details"
    fun videoMuxerFailed(details: String): String {
        return if (zh) {
            "MP4 封装失败：$details。通常是原视频的音频轨道、时间戳或系统写入器不兼容，原视频已保留。"
        } else {
            "MP4 muxing failed: $details. This is usually caused by an incompatible audio track, timestamps, or system muxer. The original video was kept."
        }
    }

    fun clearBackupSuccess(count: Int, bytes: String): String {
        return if (zh) "已删除 $count 张备份图片，释放 $bytes。" else "Deleted $count backup images and freed $bytes."
    }

    fun clearBackupPartial(count: Int, failed: Int): String {
        return if (zh) "已删除 $count 张备份图片，$failed 项删除失败。" else "Deleted $count backup images, $failed items failed."
    }

    fun clearVideoBackupSuccess(count: Int, bytes: String): String {
        return if (zh) "已删除 $count 个备份视频，释放 $bytes。" else "Deleted $count backup videos and freed $bytes."
    }

    fun clearVideoBackupPartial(count: Int, failed: Int): String {
        return if (zh) "已删除 $count 个备份视频，$failed 项删除失败。" else "Deleted $count backup videos, $failed items failed."
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
