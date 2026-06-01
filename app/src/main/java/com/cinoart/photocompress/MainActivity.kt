package com.cinoart.photocompress

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.cinoart.photocompress.ui.theme.PhotoCompressTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val PREFS_NAME = "photo_compress_settings"
private const val KEY_LANGUAGE = "language"
private const val VIDEO_PAGE_SIZE = 50

private enum class VideoSortOrder {
    SizeDesc,
    PathAsc
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoCompressTheme {
                PhotoCompressApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCompressApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appVersionLabel = remember(context) { appVersionLabel(context) }
    var language by rememberSaveable { mutableStateOf(loadAppLanguage(context)) }
    val text = remember(language) { AppText(language) }
    var showFeaturePage by rememberSaveable { mutableStateOf(false) }
    var showSettingsPage by rememberSaveable { mutableStateOf(false) }
    var showImageCompressPage by rememberSaveable { mutableStateOf(false) }
    var showVideoCompressPage by rememberSaveable { mutableStateOf(false) }
    var selectedTreeUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFolderName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideoTreeUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideoFolderName by rememberSaveable { mutableStateOf<String?>(null) }
    var qualityPreset by rememberSaveable { mutableStateOf(CompressionQualityPreset.Balanced) }
    var recursive by rememberSaveable { mutableStateOf(true) }
    var videoRecursive by rememberSaveable { mutableStateOf(true) }
    var videoSortOrder by rememberSaveable { mutableStateOf(VideoSortOrder.SizeDesc) }
    var preserveExif by rememberSaveable { mutableStateOf(true) }
    var progress by remember { mutableStateOf(CompressProgress()) }
    var videoProgress by remember { mutableStateOf(CompressProgress()) }
    var videoFiles by remember { mutableStateOf<List<VideoFileInfo>>(emptyList()) }
    var selectedVideoPaths by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var cleanupJob by remember { mutableStateOf<Job?>(null) }
    var videoRunningJob by remember { mutableStateOf<Job?>(null) }
    var videoCleanupJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var videoStatus by remember { mutableStateOf<String?>(null) }
    val isRunning = runningJob?.isActive == true
    val isCleaning = cleanupJob?.isActive == true
    val isBusy = isRunning || isCleaning
    val isVideoRunning = videoRunningJob?.isActive == true
    val isVideoCleaning = videoCleanupJob?.isActive == true
    val isVideoBusy = isVideoRunning || isVideoCleaning
    val isAnyBusy = isBusy || isVideoBusy

    if (showFeaturePage) {
        BackHandler { showFeaturePage = false }
        FeatureInfoScreen(
            appVersionLabel = appVersionLabel,
            text = text,
            onBack = { showFeaturePage = false }
        )
        return
    }

    if (showSettingsPage) {
        BackHandler { showSettingsPage = false }
        SettingsScreen(
            appVersionLabel = appVersionLabel,
            language = language,
            qualityPreset = qualityPreset,
            recursive = recursive,
            preserveExif = preserveExif,
            isRunning = isAnyBusy,
            text = text,
            onBack = { showSettingsPage = false },
            onLanguageChange = { nextLanguage ->
                language = nextLanguage
                saveAppLanguage(context, nextLanguage)
                status = null
            },
            onQualityPresetChange = { qualityPreset = it },
            onRecursiveChange = { recursive = it },
            onPreserveExifChange = { preserveExif = it }
        )
        return
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        selectedTreeUri = uri.toString()
        selectedFolderName = DocumentFile.fromTreeUri(context, uri)?.name ?: text.selectedFolder
        progress = CompressProgress()
        status = text.folderReady
    }

    val videoFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        selectedVideoTreeUri = uri.toString()
        selectedVideoFolderName = DocumentFile.fromTreeUri(context, uri)?.name ?: text.selectedFolder
        videoProgress = CompressProgress()
        videoFiles = emptyList()
        selectedVideoPaths = emptyList()
        videoStatus = text.videoScanningFiles
        scope.launch {
            scanVideoFolder(
                context = context,
                uri = uri,
                recursive = videoRecursive,
                language = language,
                onStart = {
                    videoStatus = text.videoScanningFiles
                    videoFiles = emptyList()
                    selectedVideoPaths = emptyList()
                },
                onSuccess = { files ->
                    videoFiles = files
                    selectedVideoPaths = files.map { it.relativePath }
                    videoStatus = text.videoFolderReadyWithCount(files.size)
                },
                onError = { message -> videoStatus = message }
            )
        }
    }

    if (showImageCompressPage) {
        BackHandler {
            if (!isBusy) showImageCompressPage = false
        }
        ImageCompressScreen(
            appVersionLabel = appVersionLabel,
            folderName = selectedFolderName ?: text.noFolder,
            status = status ?: text.initialStatus,
            progress = progress,
            canStart = selectedTreeUri != null && !isBusy,
            canClearBackups = selectedTreeUri != null && !isBusy,
            isRunning = isRunning,
            isCleaning = isCleaning,
            isBusy = isBusy,
            text = text,
            onBack = { if (!isBusy) showImageCompressPage = false },
            onPickFolder = { folderLauncher.launch(null) },
            onStart = {
                val uri = selectedTreeUri?.let(Uri::parse) ?: return@ImageCompressScreen
                val tree = DocumentFile.fromTreeUri(context, uri) ?: run {
                    status = text.unableOpenFolder
                    return@ImageCompressScreen
                }
                val settings = CompressSettings(
                    jpegQuality = qualityPreset.jpegQuality,
                    recursive = recursive,
                    preserveExif = preserveExif,
                    language = language
                )
                runningJob = scope.launch {
                    val runText = AppText(language)
                    status = runText.scanning
                    progress = CompressProgress()
                    try {
                        val result = withContext(Dispatchers.IO) {
                            ImageCompressor(context.applicationContext).compressTree(tree, settings) { next ->
                                withContext(Dispatchers.Main) {
                                    progress = next
                                }
                            }
                        }
                        progress = result
                        status = if (result.compressed > 0) {
                            runText.completeWithBackup(result.compressed, result.skipped, result.backupPath)
                        } else {
                            runText.completeNoNew(result.skipped)
                        }
                    } catch (_: CancellationException) {
                        status = runText.stopped
                    } catch (error: Throwable) {
                        status = runText.error(error.message ?: error.javaClass.simpleName)
                    } finally {
                        runningJob = null
                    }
                }
            },
            onStop = {
                runningJob?.cancel()
            },
            onClearBackups = {
                val uri = selectedTreeUri?.let(Uri::parse) ?: return@ImageCompressScreen
                val tree = DocumentFile.fromTreeUri(context, uri) ?: run {
                    status = text.unableOpenFolder
                    return@ImageCompressScreen
                }
                cleanupJob = scope.launch {
                    val runText = AppText(language)
                    status = runText.clearingBackups
                    try {
                        val result = withContext(Dispatchers.IO) {
                            ImageCompressor(context.applicationContext).deleteBackupImages(tree)
                        }
                        status = if (result.failed == 0) {
                            runText.clearBackupSuccess(result.deletedFiles, ImageCompressor.formatBytes(result.deletedBytes))
                        } else {
                            runText.clearBackupPartial(result.deletedFiles, result.failed)
                        }
                    } catch (error: Throwable) {
                        status = runText.clearBackupFailed(error.message ?: error.javaClass.simpleName)
                    } finally {
                        cleanupJob = null
                    }
                }
            }
        )
        return
    }

    if (showVideoCompressPage) {
        BackHandler {
            if (!isVideoBusy) showVideoCompressPage = false
        }
        VideoCompressScreen(
            appVersionLabel = appVersionLabel,
            folderName = selectedVideoFolderName ?: text.noFolder,
            status = videoStatus ?: text.videoInitialStatus,
            progress = videoProgress,
            videos = videoFiles,
            selectedVideoPaths = selectedVideoPaths,
            recursive = videoRecursive,
            sortOrder = videoSortOrder,
            canStart = selectedVideoTreeUri != null && selectedVideoPaths.isNotEmpty() && !isVideoBusy,
            canClearBackups = selectedVideoTreeUri != null && !isVideoBusy,
            isRunning = isVideoRunning,
            isCleaning = isVideoCleaning,
            isBusy = isVideoBusy,
            text = text,
            onBack = { if (!isVideoBusy) showVideoCompressPage = false },
            onPickFolder = { videoFolderLauncher.launch(null) },
            onRecursiveChange = { nextRecursive ->
                videoRecursive = nextRecursive
                val uri = selectedVideoTreeUri?.let(Uri::parse) ?: return@VideoCompressScreen
                scope.launch {
                    scanVideoFolder(
                        context = context,
                        uri = uri,
                        recursive = nextRecursive,
                        language = language,
                        onStart = {
                            videoStatus = text.videoScanningFiles
                            videoFiles = emptyList()
                            selectedVideoPaths = emptyList()
                        },
                        onSuccess = { files ->
                            videoFiles = files
                            selectedVideoPaths = files.map { it.relativePath }
                            videoStatus = text.videoFolderReadyWithCount(files.size)
                        },
                        onError = { message -> videoStatus = message }
                    )
                }
            },
            onSortOrderChange = { videoSortOrder = it },
            onToggleVideo = { path ->
                selectedVideoPaths = if (path in selectedVideoPaths) {
                    selectedVideoPaths - path
                } else {
                    (selectedVideoPaths + path).distinct()
                }
            },
            onSelectAllVideos = {
                selectedVideoPaths = videoFiles.map { it.relativePath }
            },
            onClearVideoSelection = {
                selectedVideoPaths = emptyList()
            },
            onStart = {
                val uri = selectedVideoTreeUri?.let(Uri::parse) ?: return@VideoCompressScreen
                val tree = DocumentFile.fromTreeUri(context, uri) ?: run {
                    videoStatus = text.unableOpenFolder
                    return@VideoCompressScreen
                }
                val selectedPaths = selectedVideoPaths.toSet()
                if (selectedPaths.isEmpty()) {
                    videoStatus = text.noVideoSelected
                    return@VideoCompressScreen
                }
                val settings = VideoCompressSettings(
                    videoBitrate = qualityPreset.videoBitrate,
                    recursive = videoRecursive,
                    selectedRelativePaths = selectedPaths,
                    language = language
                )
                videoRunningJob = scope.launch {
                    val runText = AppText(language)
                    videoStatus = runText.videoScanning
                    videoProgress = CompressProgress()
                    try {
                        val result = withContext(Dispatchers.IO) {
                            VideoCompressor(context.applicationContext).compressTree(tree, settings) { next ->
                                withContext(Dispatchers.Main) {
                                    videoProgress = next
                                }
                            }
                        }
                        videoProgress = result
                        videoStatus = when {
                            result.failed > 0 -> runText.videoCompleteWithFailures(result.compressed, result.skipped, result.failed)
                            result.compressed > 0 -> runText.videoCompleteWithBackup(result.compressed, result.skipped, result.backupPath)
                            else -> runText.videoCompleteNoNew(result.skipped)
                        }
                    } catch (_: CancellationException) {
                        videoStatus = runText.stopped
                    } catch (error: Throwable) {
                        videoStatus = runText.error(error.message ?: error.javaClass.simpleName)
                    } finally {
                        videoRunningJob = null
                    }
                }
            },
            onStop = {
                videoRunningJob?.cancel()
            },
            onClearBackups = {
                val uri = selectedVideoTreeUri?.let(Uri::parse) ?: return@VideoCompressScreen
                val tree = DocumentFile.fromTreeUri(context, uri) ?: run {
                    videoStatus = text.unableOpenFolder
                    return@VideoCompressScreen
                }
                videoCleanupJob = scope.launch {
                    val runText = AppText(language)
                    videoStatus = runText.clearingVideoBackups
                    try {
                        val result = withContext(Dispatchers.IO) {
                            VideoCompressor(context.applicationContext).deleteBackupVideos(tree)
                        }
                        videoStatus = if (result.failed == 0) {
                            runText.clearVideoBackupSuccess(result.deletedFiles, ImageCompressor.formatBytes(result.deletedBytes))
                        } else {
                            runText.clearVideoBackupPartial(result.deletedFiles, result.failed)
                        }
                    } catch (error: Throwable) {
                        videoStatus = runText.clearBackupFailed(error.message ?: error.javaClass.simpleName)
                    } finally {
                        videoCleanupJob = null
                    }
                }
            }
        )
        return
    }

    HomeScreen(
        appVersionLabel = appVersionLabel,
        text = text,
        onFeatureClick = { showFeaturePage = true },
        onSettingsClick = { showSettingsPage = true },
        onImageCompressClick = { showImageCompressPage = true },
        onVideoCompressClick = { showVideoCompressPage = true }
    )
}

private fun appVersionLabel(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    return "v${packageInfo.versionName ?: "-"} ($versionCode)"
}

private fun loadAppLanguage(context: Context): AppLanguage {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return AppLanguage.fromStorage(prefs.getString(KEY_LANGUAGE, null))
}

private fun saveAppLanguage(context: Context, language: AppLanguage) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_LANGUAGE, language.storageValue)
        .apply()
}

private suspend fun scanVideoFolder(
    context: Context,
    uri: Uri,
    recursive: Boolean,
    language: AppLanguage,
    onStart: () -> Unit,
    onSuccess: (List<VideoFileInfo>) -> Unit,
    onError: (String) -> Unit
) {
    val text = AppText(language)
    onStart()
    try {
        val tree = DocumentFile.fromTreeUri(context, uri) ?: run {
            onError(text.unableOpenFolder)
            return
        }
        val files = withContext(Dispatchers.IO) {
            VideoCompressor(context.applicationContext).scanVideos(
                tree,
                VideoCompressSettings(
                    recursive = recursive,
                    language = language
                )
            )
        }
        onSuccess(files)
    } catch (error: Throwable) {
        onError(text.error(error.message ?: error.javaClass.simpleName))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    appVersionLabel: String,
    text: AppText,
    onFeatureClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onImageCompressClick: () -> Unit,
    onVideoCompressClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text.appTitle)
                        Text(
                            appVersionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onFeatureClick) {
                        Icon(Icons.Filled.Info, contentDescription = text.featureInfo)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = text.settings)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeEntryButton(
                label = text.imageCompression,
                icon = Icons.Filled.Image,
                modifier = Modifier.weight(1f),
                onClick = onImageCompressClick
            )
            HomeEntryButton(
                label = text.videoCompression,
                icon = Icons.Filled.Videocam,
                modifier = Modifier.weight(1f),
                onClick = onVideoCompressClick
            )
        }
    }
}

@Composable
private fun HomeEntryButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(72.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageCompressScreen(
    appVersionLabel: String,
    folderName: String,
    status: String,
    progress: CompressProgress,
    canStart: Boolean,
    canClearBackups: Boolean,
    isRunning: Boolean,
    isCleaning: Boolean,
    isBusy: Boolean,
    text: AppText,
    onBack: () -> Unit,
    onPickFolder: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClearBackups: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isBusy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = text.back)
                    }
                },
                title = {
                    Column {
                        Text(text.imageCompression)
                        Text(
                            appVersionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FolderPanel(
                title = text.folderTitle,
                folderName = folderName,
                status = status,
                isRunning = isBusy,
                text = text,
                onPickFolder = onPickFolder
            )
            ActionPanel(
                canStart = canStart,
                canClearBackups = canClearBackups,
                isRunning = isRunning,
                isCleaning = isCleaning,
                clearBackups = text.clearBackups,
                clearingBackupsButton = text.clearingBackupsButton,
                text = text,
                onStart = onStart,
                onStop = onStop,
                onClearBackups = onClearBackups
            )
            ProgressPanel(progress = progress, text = text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoCompressScreen(
    appVersionLabel: String,
    folderName: String,
    status: String,
    progress: CompressProgress,
    videos: List<VideoFileInfo>,
    selectedVideoPaths: List<String>,
    recursive: Boolean,
    sortOrder: VideoSortOrder,
    canStart: Boolean,
    canClearBackups: Boolean,
    isRunning: Boolean,
    isCleaning: Boolean,
    isBusy: Boolean,
    text: AppText,
    onBack: () -> Unit,
    onPickFolder: () -> Unit,
    onRecursiveChange: (Boolean) -> Unit,
    onSortOrderChange: (VideoSortOrder) -> Unit,
    onToggleVideo: (String) -> Unit,
    onSelectAllVideos: () -> Unit,
    onClearVideoSelection: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClearBackups: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isBusy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = text.back)
                    }
                },
                title = {
                    Column {
                        Text(text.videoCompression)
                        Text(
                            appVersionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FolderPanel(
                title = text.videoFolderTitle,
                folderName = folderName,
                status = status,
                isRunning = isBusy,
                text = text,
                onPickFolder = onPickFolder
            )
            VideoOptionsPanel(
                recursive = recursive,
                sortOrder = sortOrder,
                isRunning = isBusy,
                text = text,
                onRecursiveChange = onRecursiveChange,
                onSortOrderChange = onSortOrderChange
            )
            VideoSelectionPanel(
                videos = videos,
                selectedVideoPaths = selectedVideoPaths,
                sortOrder = sortOrder,
                isRunning = isBusy,
                text = text,
                onToggleVideo = onToggleVideo,
                onSelectAllVideos = onSelectAllVideos,
                onClearVideoSelection = onClearVideoSelection
            )
            ActionPanel(
                canStart = canStart,
                canClearBackups = canClearBackups,
                isRunning = isRunning,
                isCleaning = isCleaning,
                clearBackups = text.clearVideoBackups,
                clearingBackupsButton = text.clearingVideoBackupsButton,
                text = text,
                onStart = onStart,
                onStop = onStop,
                onClearBackups = onClearBackups
            )
            ProgressPanel(progress = progress, text = text)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    appVersionLabel: String,
    language: AppLanguage,
    qualityPreset: CompressionQualityPreset,
    recursive: Boolean,
    preserveExif: Boolean,
    isRunning: Boolean,
    text: AppText,
    onBack: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onQualityPresetChange: (CompressionQualityPreset) -> Unit,
    onRecursiveChange: (Boolean) -> Unit,
    onPreserveExifChange: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = text.back)
                    }
                },
                title = {
                    Column {
                        Text(text.settings)
                        Text(
                            appVersionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LanguagePanel(
                language = language,
                text = text,
                isRunning = isRunning,
                onLanguageChange = onLanguageChange
            )
            SettingsPanel(
                qualityPreset = qualityPreset,
                recursive = recursive,
                preserveExif = preserveExif,
                isRunning = isRunning,
                text = text,
                onQualityPresetChange = onQualityPresetChange,
                onRecursiveChange = onRecursiveChange,
                onPreserveExifChange = onPreserveExifChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureInfoScreen(
    appVersionLabel: String,
    text: AppText,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = text.back)
                    }
                },
                title = {
                    Column {
                        Text(text.featureInfo)
                        Text(
                            appVersionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FeatureSection(
                title = text.localCompressTitle,
                lines = text.featureLocalLines
            )
            FeatureSection(
                title = text.backupTitle,
                lines = text.featureBackupLines
            )
            FeatureSection(
                title = text.exifTitle,
                lines = text.featureExifLines
            )
        }
    }
}

@Composable
private fun FolderPanel(
    title: String,
    folderName: String,
    status: String,
    isRunning: Boolean,
    text: AppText,
    onPickFolder: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = folderName,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.width(12.dp))
                OutlinedButton(enabled = !isRunning, onClick = onPickFolder) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text.choose)
                }
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VideoOptionsPanel(
    recursive: Boolean,
    sortOrder: VideoSortOrder,
    isRunning: Boolean,
    text: AppText,
    onRecursiveChange: (Boolean) -> Unit,
    onSortOrderChange: (VideoSortOrder) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text.includeSubfolders, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text.videoIncludeSubfoldersDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = recursive,
                    enabled = !isRunning,
                    onCheckedChange = onRecursiveChange
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text.videoSortTitle, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SortChoiceButton(
                        label = text.videoSortBySize,
                        selected = sortOrder == VideoSortOrder.SizeDesc,
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f),
                        onClick = { onSortOrderChange(VideoSortOrder.SizeDesc) }
                    )
                    SortChoiceButton(
                        label = text.videoSortByPath,
                        selected = sortOrder == VideoSortOrder.PathAsc,
                        enabled = !isRunning,
                        modifier = Modifier.weight(1f),
                        onClick = { onSortOrderChange(VideoSortOrder.PathAsc) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChoiceButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = modifier) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun VideoSelectionPanel(
    videos: List<VideoFileInfo>,
    selectedVideoPaths: List<String>,
    sortOrder: VideoSortOrder,
    isRunning: Boolean,
    text: AppText,
    onToggleVideo: (String) -> Unit,
    onSelectAllVideos: () -> Unit,
    onClearVideoSelection: () -> Unit
) {
    val selected = selectedVideoPaths.toSet()
    var pageIndex by rememberSaveable { mutableStateOf(0) }
    val sortedVideos = remember(videos, sortOrder) {
        when (sortOrder) {
            VideoSortOrder.SizeDesc -> videos.sortedWith(compareByDescending<VideoFileInfo> { it.size }.thenBy { it.relativePath.lowercase(Locale.US) })
            VideoSortOrder.PathAsc -> videos.sortedBy { it.relativePath.lowercase(Locale.US) }
        }
    }
    LaunchedEffect(videos, sortOrder) {
        pageIndex = 0
    }
    val pageCount = if (sortedVideos.isEmpty()) {
        1
    } else {
        ((sortedVideos.size - 1) / VIDEO_PAGE_SIZE) + 1
    }
    val currentPage = pageIndex.coerceIn(0, pageCount - 1)
    val firstIndex = currentPage * VIDEO_PAGE_SIZE
    val pagedVideos = sortedVideos.drop(firstIndex).take(VIDEO_PAGE_SIZE)
    val lastIndex = (firstIndex + pagedVideos.size).coerceAtMost(sortedVideos.size)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text.videoSelectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text.videoSelectionCount(selected.size, videos.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onSelectAllVideos,
                    enabled = !isRunning && videos.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text.selectAllVideos, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onClearVideoSelection,
                    enabled = !isRunning && selected.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text.clearVideoSelection, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (videos.isEmpty()) {
                Text(
                    text.noVideosFound,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (sortedVideos.size > VIDEO_PAGE_SIZE) {
                    Text(
                        text.videoPageInfo(currentPage + 1, pageCount, firstIndex + 1, lastIndex, sortedVideos.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { pageIndex = currentPage - 1 },
                            enabled = !isRunning && currentPage > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text.previousPage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { pageIndex = currentPage + 1 },
                            enabled = !isRunning && currentPage < pageCount - 1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text.nextPage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pagedVideos.forEach { video ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = video.relativePath in selected,
                                enabled = !isRunning,
                                onCheckedChange = { onToggleVideo(video.relativePath) }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    video.relativePath,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    ImageCompressor.formatBytes(video.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguagePanel(
    language: AppLanguage,
    text: AppText,
    isRunning: Boolean,
    onLanguageChange: (AppLanguage) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(text.languageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text.languageHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LanguageChoiceButton(
                    label = text.chinese,
                    selected = language == AppLanguage.Chinese,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f),
                    onClick = { onLanguageChange(AppLanguage.Chinese) }
                )
                LanguageChoiceButton(
                    label = text.english,
                    selected = language == AppLanguage.English,
                    enabled = !isRunning,
                    modifier = Modifier.weight(1f),
                    onClick = { onLanguageChange(AppLanguage.English) }
                )
            }
        }
    }
}

@Composable
private fun LanguageChoiceButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            Text(label)
        }
    }
}

@Composable
private fun FeatureSection(title: String, lines: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            lines.forEach { line -> FeatureLine(line) }
        }
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SettingsPanel(
    qualityPreset: CompressionQualityPreset,
    recursive: Boolean,
    preserveExif: Boolean,
    isRunning: Boolean,
    text: AppText,
    onQualityPresetChange: (CompressionQualityPreset) -> Unit,
    onRecursiveChange: (Boolean) -> Unit,
    onPreserveExifChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text.imageQuality, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text.qualityPresetDescription(qualityPreset), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompressionQualityPreset.entries.forEach { preset ->
                    QualityPresetButton(
                        label = text.qualityPresetLabel(preset),
                        selected = preset == qualityPreset,
                        enabled = !isRunning,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        onClick = { onQualityPresetChange(preset) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text.preserveExif, style = MaterialTheme.typography.bodyLarge)
                    Text(text.preserveExifDescription, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = preserveExif,
                    enabled = !isRunning,
                    onCheckedChange = onPreserveExifChange
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(text.includeSubfolders, style = MaterialTheme.typography.bodyLarge)
                    Text(text.includeSubfoldersDescription, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = recursive,
                    enabled = !isRunning,
                    onCheckedChange = onRecursiveChange
                )
            }
        }
    }
}

@Composable
private fun QualityPresetButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier
        ) {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ActionPanel(
    canStart: Boolean,
    canClearBackups: Boolean,
    isRunning: Boolean,
    isCleaning: Boolean,
    clearBackups: String,
    clearingBackupsButton: String,
    text: AppText,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClearBackups: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text.startCompress)
            }
            OutlinedButton(
                onClick = onStop,
                enabled = isRunning,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text.stop)
            }
        }
        OutlinedButton(
            onClick = onClearBackups,
            enabled = canClearBackups,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isCleaning) clearingBackupsButton else clearBackups)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressPanel(progress: CompressProgress, text: AppText) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text.progressTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${progress.processed}/${progress.total}", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(
                progress = {
                    if (progress.total == 0) 0f else progress.processed.toFloat() / progress.total
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (progress.currentPath.isNotBlank()) {
                Text(
                    progress.currentPath,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(text.success, progress.compressed.toString())
                StatChip(text.skipped, progress.skipped.toString())
                StatChip(text.failed, progress.failed.toString())
                StatChip(text.saved, "${String.format(Locale.US, "%.1f", progress.savedPercent)}%")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SizeBox(text.original, ImageCompressor.formatBytes(progress.originalBytes), Modifier.weight(1f))
                SizeBox(text.compressed, ImageCompressor.formatBytes(progress.finalBytes), Modifier.weight(1f))
                SizeBox(text.saved, ImageCompressor.formatBytes(progress.savedBytes), Modifier.weight(1f))
            }
            if (progress.backupPath.isNotBlank()) {
                Text("${text.backupDir}: ${progress.backupPath}", style = MaterialTheme.typography.bodyMedium)
            }
            if (progress.messages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    progress.messages.forEach { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    AssistChip(onClick = {}, label = { Text("$label $value") })
}

@Composable
private fun SizeBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(68.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.CenterStart) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PhotoCompressPreview() {
    PhotoCompressTheme {
        ProgressPanel(
            CompressProgress(
                total = 12,
                processed = 5,
                compressed = 4,
                skipped = 1,
                originalBytes = 8_400_000,
                finalBytes = 3_100_000,
                backupPath = "bk/20260528_220000",
                messages = listOf("完成：DCIM/IMG_001.jpg 4.80 MB -> 1.20 MB")
            ),
            text = AppText(AppLanguage.Chinese)
        )
    }
}
