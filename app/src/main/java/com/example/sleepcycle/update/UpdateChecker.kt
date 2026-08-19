package com.example.sleepcycle.update

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * 语义化版本号解析与比对工具
 */
object VersionComparator {

    /**
     * 规范化版本号字符串，去除前导 'v' 或 'V'，并剔除后缀
     */
    fun normalize(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }

    /**
     * 比较两个版本号的大小
     *
     * @param remoteVersion 远端版本号（如 "1.1.2", "v2.0.0"）
     * @param currentVersion 本地当前版本号（如 "1.1.1"）
     * @return 若 remoteVersion > currentVersion 返回 > 0，若相等返回 0，若小于返回 < 0
     */
    fun compare(remoteVersion: String, currentVersion: String): Int {
        val remoteClean = normalize(remoteVersion).split("-")[0].split("+")[0]
        val currentClean = normalize(currentVersion).split("-")[0].split("+")[0]

        val remoteParts = remoteClean.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) {
                return r.compareTo(c)
            }
        }
        return 0
    }

    /**
     * 判断远端版本是否高于当前版本
     */
    fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        return compare(remoteVersion, currentVersion) > 0
    }
}

/**
 * 轻量级 JSON 解析器，不依赖 Android SDK 的 org.json（可在 JVM 单元测试中直接运行）
 */
object SimpleJsonParser {

    /**
     * 提取 JSON 顶层或简单嵌套的字符串字段值
     */
    fun extractString(json: String, key: String): String {
        val regex = Regex("""\"$key\"\s*:\s*\"((?:\\.|[^\"\\])*)\"""")
        val match = regex.find(json) ?: return ""
        val raw = match.groupValues[1]
        return unescapeJson(raw)
    }

    /**
     * 从 assets 数组中查找首个 .apk 资产的 browser_download_url
     */
    fun extractApkDownloadUrl(json: String): String? {
        val assetsIdx = json.indexOf("\"assets\"")
        if (assetsIdx == -1) return null

        val assetsSubstring = json.substring(assetsIdx)
        // 查找每个 object 块
        val objectRegex = Regex("""\{[^{}]*\}""")
        for (match in objectRegex.findAll(assetsSubstring)) {
            val block = match.value
            val name = extractString(block, "name")
            val downloadUrl = extractString(block, "browser_download_url")
            if (name.endsWith(".apk", ignoreCase = true) && downloadUrl.isNotEmpty()) {
                return downloadUrl
            }
        }
        return null
    }

    private fun unescapeJson(str: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (c == '\\' && i + 1 < str.length) {
                when (val next = str[i + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'u' -> {
                        if (i + 5 < str.length) {
                            val hex = str.substring(i + 2, i + 6)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 6
                                continue
                            }
                        }
                        sb.append(c)
                    }
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}

/**
 * 网络请求客户端接口，便于单元测试 mock
 */
fun interface HttpFetcher {
    fun fetch(urlString: String): String
}

/**
 * 原生 HttpURLConnection 实现
 */
class DefaultHttpFetcher(
    private val connectTimeoutMs: Int = 8000,
    private val readTimeoutMs: Int = 8000
) : HttpFetcher {
    override fun fetch(urlString: String): String {
        val url = URL(urlString)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "SleepCycle-Android-App")
        }

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IllegalStateException("HTTP 请求失败: $responseCode ${connection.responseMessage}")
        }

        return connection.inputStream.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        }
    }
}

/**
 * GitHub Release 检查更新核心模块
 */
class UpdateChecker(
    private val httpFetcher: HttpFetcher = DefaultHttpFetcher(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val apiUrl: String = DEFAULT_GITHUB_RELEASE_API_URL
) {

    /**
     * 解析 GitHub Release API 返回的 JSON 字符串
     */
    fun parseReleaseJson(jsonString: String): ReleaseInfo {
        val tagName = SimpleJsonParser.extractString(jsonString, "tag_name").trim()
        val rawName = SimpleJsonParser.extractString(jsonString, "name")
        val title = if (rawName.isNotEmpty()) rawName else tagName
        val releaseNotes = SimpleJsonParser.extractString(jsonString, "body")
        val publishedAt = SimpleJsonParser.extractString(jsonString, "published_at")
        val htmlUrl = SimpleJsonParser.extractString(jsonString, "html_url")

        val versionName = VersionComparator.normalize(tagName)
        val apkUrl = SimpleJsonParser.extractApkDownloadUrl(jsonString)
        val downloadUrl = apkUrl ?: htmlUrl

        return ReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            title = title,
            releaseNotes = releaseNotes,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl,
            downloadUrl = downloadUrl
        )
    }

    /**
     * 异步检查是否有新版本
     *
     * @param currentVersionName 当前 App 版本名称（例如 "1.1.1"）
     * @return UpdateCheckResult
     */
    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
        return withContext(ioDispatcher) {
            try {
                val jsonString = httpFetcher.fetch(apiUrl)
                val releaseInfo = parseReleaseJson(jsonString)

                if (releaseInfo.versionName.isEmpty()) {
                    return@withContext UpdateCheckResult.Error("未获取到有效的远端版本号")
                }

                if (VersionComparator.isNewerVersion(releaseInfo.versionName, currentVersionName)) {
                    UpdateCheckResult.HasUpdate(releaseInfo)
                } else {
                    UpdateCheckResult.UpToDate
                }
            } catch (e: Exception) {
                UpdateCheckResult.Error(
                    message = e.localizedMessage ?: "检查更新失败，请稍后重试",
                    cause = e
                )
            }
        }
    }

    companion object {
        const val DEFAULT_GITHUB_RELEASE_API_URL =
            "https://api.github.com/repos/michiru233/SleepCycle/releases/latest"
    }
}
