# PhotoCompress

PhotoCompress 是一个本地运行的 Android 图片压缩工具。它面向手机相册目录批量处理图片，支持压缩、备份、保留元数据、跳过已处理文件，并且不会把图片上传到网络。

当前版本：`v1.1.7 (9)`

## 功能特性

- 本地选择相册目录，通过 Android 系统文件夹选择器授权访问。
- 支持批量处理 `JPEG/JPG` 和 `PNG` 图片。
- 提供三档图片压缩质量：小体积、均衡、高画质。
- 压缩前自动把原图备份到所选目录下的 `bk/` 文件夹。
- JPEG 可选择是否保留 EXIF 元数据，例如拍摄时间、设备型号、定位信息等。
- 自动记录已处理图片，再次选择同一目录时会跳过未变化的图片。
- 自动跳过 `bk/` 备份目录和以 `.` 开头的隐藏目录。
- 支持一键删除 `bk/` 目录下的备份图片，处理记录会保留。
- 支持中文 / English 界面切换，并保存到本机。
- App 内置功能说明页面和版本号显示，便于确认安装版本。

## 使用方式

1. 安装并打开 App。
2. 点击“选择”，授权一个相册或图片目录。
3. 按需设置图片压缩质量、是否保留 EXIF、是否包含子目录。
4. 点击“开始压缩”。
5. 如需释放备份占用空间，可点击“清空备份图片”。

压缩或清理过程中，设置项会被锁定，避免处理中途修改参数。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Android Storage Access Framework
- AndroidX DocumentFile
- AndroidX ExifInterface

## 构建与安装

环境要求：

- Android Studio
- Android SDK 34
- JDK 17 或兼容当前 Gradle 配置的 JDK

常用命令：

```bash
./gradlew :app:assembleDebug
```

构建 Debug APK。

```bash
./gradlew :app:installDebug
```

安装到已连接的 Android 设备。

```bash
adb shell dumpsys package com.cinoart.photocompress | rg 'versionCode|versionName'
```

检查设备上安装的版本。

## 项目结构

```text
app/src/main/java/com/cinoart/photocompress/
├── MainActivity.kt       # Compose 界面、状态管理和用户操作
├── ImageCompressor.kt    # 图片扫描、备份、压缩、EXIF 和处理记录
├── AppText.kt            # 中英文界面文案和压缩质量预设
└── ui/theme/             # Compose 主题配置
```

## 注意事项

- 图片处理全部在手机本地完成，不依赖云端压缩服务。
- PNG 的压缩收益取决于原图内容；如果重编码后没有变小，App 会跳过并记录。
- 备份目录为所选目录下的 `bk/`，清理备份前请确认不再需要原图。
- 当前仓库尚未指定开源许可证，公开发布前建议补充 `LICENSE` 文件。
