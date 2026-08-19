package com.example.sleepcycle.update

/**
 * 发布版本信息实体
 *
 * @property tagName 标签名称（例如 "v1.1.2" 或 "1.1.2"）
 * @property versionName 解析后的纯版本号（例如 "1.1.2"）
 * @property title Release 标题
 * @property releaseNotes Release 更新日志说明
 * @property publishedAt 发布时间（ISO 字符串）
 * @property htmlUrl Release 网页地址
 * @property downloadUrl APK 资产直接下载地址（若 Release 资产中包含 .apk），若无则为 htmlUrl
 */
data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val publishedAt: String,
    val htmlUrl: String,
    val downloadUrl: String
)

/**
 * 检查更新结果定义
 */
sealed class UpdateCheckResult {
    data class HasUpdate(val releaseInfo: ReleaseInfo) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Error(val message: String, val cause: Throwable? = null) : UpdateCheckResult()
}
