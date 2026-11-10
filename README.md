# SunnyWeather

Kotlin + Jetpack Compose 空项目，使用 Kotlin DSL 和 TOML 版本目录。

```text
SunnyWeather/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── kotlin/com/pyn/sunnyweather/
        │   │   ├── MainActivity.kt
        │   │   └── ui/theme/
        │   └── res/
        ├── test/kotlin/
        └── androidTest/kotlin/
```

构建配置参考 Android CLI 的 `empty-activity` 模板，使用 JDK 17、Gradle 9.1.0、AGP 9.0.1 和 Kotlin 2.3.20。依赖和插件版本统一在 `gradle/libs.versions.toml` 中管理，Compose 库版本由 BOM 管理。

最低支持 API 24，目标 API 36，编译 SDK 36.1。CLI 模板默认要求的 SDK 37 安装时提示不可用，因此采用本机已有的 SDK 36.1。

在 Android Studio 打开项目根目录，选择 JDK 17 并同步 Gradle。

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug

# 连接设备或启动模拟器后运行
android run --apks app/build/outputs/apk/debug/app-debug.apk
```
