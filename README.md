# SunnyWeather

基于 Kotlin 和 Jetpack Compose 的原生 Android 天气应用，使用彩云天气 API 展示城市天气与预报。

## 功能

- 城市搜索：支持中文、拼音、拼音片段和首字母，例如 `深圳`、`shenzhen`、`shen`、`sz`。
- 天气查看：当前温度、天气状况、逐小时和逐日预报，以及体感温度、湿度和风速。
- 下拉刷新：显示加载状态，网络失败时提供提示与重试。
- 本地缓存：记住上次选择的城市和最近一次成功获取的天气，断网时保留已有数据并标明更新时间。
- 界面适配：支持浅色、深色主题与系统字体缩放。

首次启动默认显示深圳，可点击城市名称切换地点。

## 运行环境

- Android Studio，支持 AGP 9.0.1。
- JDK 17、Android SDK 36.1。
- Android 7.0（API 24）及以上的模拟器或设备。

Gradle Wrapper 已包含在工程中，无需单独安装 Gradle。

## 快速开始

1. 克隆或下载项目，用 Android Studio 打开包含 `settings.gradle.kts` 的根目录。
2. 根据提示安装所需 SDK，等待 Gradle 同步完成。
3. 选择 `app`，连接设备或启动模拟器，点击 Run。

工程已包含 `weather-demo.properties` 演示配置，无需另外填写天气 Key。演示 Key 共用 API 配额；若接口提示鉴权失败或额度不足，可配置自己的 Key。

命令行构建：

```bash
./gradlew assembleDebug
```

Windows 使用 `gradlew.bat assembleDebug`。APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 使用自己的 API Key

在本机 `local.properties` 中保留 Android Studio 生成的 SDK 路径，追加：

```properties
CAIYUN_API_TOKEN=YOUR_CAIYUN_API_TOKEN
```

本地配置优先于演示配置。`local.properties` 已被 Git 忽略，无需提交本机 SDK 路径。

## 技术与结构

采用单模块 MVVM，使用协程与 StateFlow 管理页面状态。

- UI：Jetpack Compose、Material 3。
- 网络：Retrofit、OkHttp。
- JSON：Moshi。
- 缓存：DataStore。

```text
app/src/main/kotlin/com/pyn/sunnyweather/
├── data/       # 网络、城市数据、缓存、映射与仓库
├── model/      # 城市及天气模型
└── ui/         # 天气页、搜索页、组件与主题
```

Repository 负责网络与缓存的一致性，ViewModel 处理用户操作和页面状态，展示转换层准备日期标签与预报数据，Compose 负责渲染。

## 数据来源与范围

天气来自彩云 v2.6 API，城市数据基于[彩云 2024-06-17 城市表](https://docs.caiyunapp.com/weather-api/20240617-adcode.csv)。内置 3,184 条中国大陆城市／区县，使用城市中心点查询天气；不包含海外、港澳台、街道搜索或设备定位。

拼音检索采用随包索引，支持连续关键词匹配，不提供任意错拼纠正。索引由 pypinyin 生成，相关[许可证](app/src/main/assets/PINYIN_LICENSE.txt)随项目保留。分层思路参考[郭霖的 SunnyWeather 教学项目](https://github.com/guolindev/SunnyWeather)。

预报数量以接口实际返回和有效时间为准。应用只保存最近一次成功天气快照，不提供多城市离线收藏。已在 Android 17 模拟器验证主要流程，实体设备及最低系统版本尚未完成运行验证。
