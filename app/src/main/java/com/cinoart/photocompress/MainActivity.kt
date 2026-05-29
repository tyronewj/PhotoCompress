package com.cinoart.photocompress

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var selectedTreeUri by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFolderName by rememberSaveable { mutableStateOf("未选择目录") }
    var jpegQuality by rememberSaveable { mutableIntStateOf(86) }
    var recursive by rememberSaveable { mutableStateOf(true) }
    var preserveExif by rememberSaveable { mutableStateOf(true) }
    var progress by remember { mutableStateOf(CompressProgress()) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf("选择相册目录后开始压缩。JPEG 会保留 EXIF，已处理过的图片会跳过。") }
    val isRunning = runningJob?.isActive == true

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        selectedTreeUri = uri.toString()
        selectedFolderName = DocumentFile.fromTreeUri(context, uri)?.name ?: "已选择目录"
        progress = CompressProgress()
        status = "目录已选择，可以开始压缩。"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("本地图片压缩")
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
                folderName = selectedFolderName,
                status = status,
                isRunning = isRunning,
                onPickFolder = { folderLauncher.launch(null) }
            )

            SettingsPanel(
                quality = jpegQuality,
                recursive = recursive,
                preserveExif = preserveExif,
                isRunning = isRunning,
                onQualityChange = { jpegQuality = it },
                onRecursiveChange = { recursive = it },
                onPreserveExifChange = { preserveExif = it }
            )

            ActionPanel(
                canStart = selectedTreeUri != null && !isRunning,
                isRunning = isRunning,
                onStart = {
                    val uri = selectedTreeUri?.let(Uri::parse) ?: return@ActionPanel
                    val tree = DocumentFile.fromTreeUri(context, uri) ?: run {
                        status = "无法打开所选目录，请重新选择。"
                        return@ActionPanel
                    }
                    val settings = CompressSettings(
                        jpegQuality = jpegQuality,
                        recursive = recursive,
                        preserveExif = preserveExif
                    )
                    runningJob = scope.launch {
                        status = "正在扫描和压缩..."
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
                                "完成：${result.compressed} 张压缩成功，${result.skipped} 张跳过，备份在 ${result.backupPath}。"
                            } else {
                                "完成：没有新压缩图片，${result.skipped} 张已跳过。"
                            }
                        } catch (_: CancellationException) {
                            status = "已停止，已完成的文件和备份会保留。"
                        } catch (error: Throwable) {
                            status = "出错：${error.message ?: error.javaClass.simpleName}"
                        } finally {
                            runningJob = null
                        }
                    }
                },
                onStop = {
                    runningJob?.cancel()
                }
            )

            ProgressPanel(progress = progress)
        }
    }
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

@Composable
private fun FolderPanel(
    folderName: String,
    status: String,
    isRunning: Boolean,
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
            Text("相册目录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    Text("选择")
                }
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsPanel(
    quality: Int,
    recursive: Boolean,
    preserveExif: Boolean,
    isRunning: Boolean,
    onQualityChange: (Int) -> Unit,
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
                    Text("JPEG 质量", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$quality，数值越高画质越接近原图", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Slider(
                value = quality.toFloat(),
                onValueChange = { onQualityChange(it.toInt()) },
                enabled = !isRunning,
                valueRange = 70f..95f,
                steps = 24
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("保留 EXIF", style = MaterialTheme.typography.bodyLarge)
                    Text("JPEG 压缩后复制拍摄时间、机型、定位等元数据", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = preserveExif,
                    enabled = !isRunning,
                    onCheckedChange = onPreserveExifChange
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("包含子目录", style = MaterialTheme.typography.bodyLarge)
                    Text("会跳过 bk 备份目录和已处理图片", style = MaterialTheme.typography.bodyMedium)
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
private fun ActionPanel(
    canStart: Boolean,
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("开始压缩")
        }
        OutlinedButton(
            onClick = onStop,
            enabled = isRunning,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("停止")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressPanel(progress: CompressProgress) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("处理进度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                StatChip("成功", progress.compressed.toString())
                StatChip("跳过", progress.skipped.toString())
                StatChip("失败", progress.failed.toString())
                StatChip("节省", "${String.format(Locale.US, "%.1f", progress.savedPercent)}%")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SizeBox("原始", ImageCompressor.formatBytes(progress.originalBytes), Modifier.weight(1f))
                SizeBox("压缩后", ImageCompressor.formatBytes(progress.finalBytes), Modifier.weight(1f))
                SizeBox("节省", ImageCompressor.formatBytes(progress.savedBytes), Modifier.weight(1f))
            }
            if (progress.backupPath.isNotBlank()) {
                Text("备份目录：${progress.backupPath}", style = MaterialTheme.typography.bodyMedium)
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
            )
        )
    }
}
