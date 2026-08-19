package com.example.sleepcycle

import com.example.sleepcycle.update.HttpFetcher
import com.example.sleepcycle.update.UpdateCheckResult
import com.example.sleepcycle.update.UpdateChecker
import com.example.sleepcycle.update.VersionComparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun testVersionComparator() {
        // 基本三段式大小比较
        assertTrue(VersionComparator.isNewerVersion("1.1.2", "1.1.1"))
        assertTrue(VersionComparator.isNewerVersion("v1.2.0", "1.1.9"))
        assertTrue(VersionComparator.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(VersionComparator.isNewerVersion("1.10.0", "1.9.0"))

        // 相等情况
        assertEquals(0, VersionComparator.compare("1.1.1", "1.1.1"))
        assertEquals(0, VersionComparator.compare("v1.1.1", "1.1.1"))
        assertEquals(0, VersionComparator.compare("1.1.1-beta", "1.1.1"))

        // 远端落后或相等时不判定为更新
        assertFalse(VersionComparator.isNewerVersion("1.1.1", "1.1.1"))
        assertFalse(VersionComparator.isNewerVersion("1.1.0", "1.1.1"))
        assertFalse(VersionComparator.isNewerVersion("v1.0.5", "1.1.1"))

        // 补零对齐比较（例如 1.2 vs 1.2.0）
        assertEquals(0, VersionComparator.compare("1.2", "1.2.0"))
        assertTrue(VersionComparator.isNewerVersion("1.2.1", "1.2"))
    }

    @Test
    fun testParseReleaseJsonWithApkAsset() {
        val jsonPayload = """
            {
              "tag_name": "v1.2.0",
              "name": "v1.2.0 - 升级优化",
              "body": "1. 新增深色模式\n2. 优化睡眠计算性能",
              "published_at": "2026-08-19T10:00:00Z",
              "html_url": "https://github.com/michiru233/SleepCycle/releases/tag/v1.2.0",
              "assets": [
                {
                  "name": "source.zip",
                  "browser_download_url": "https://github.com/michiru233/SleepCycle/releases/download/v1.2.0/source.zip"
                },
                {
                  "name": "SleepCycle-v1.2.0.apk",
                  "browser_download_url": "https://github.com/michiru233/SleepCycle/releases/download/v1.2.0/SleepCycle-v1.2.0.apk"
                }
              ]
            }
        """.trimIndent()

        val checker = UpdateChecker(
            httpFetcher = { jsonPayload },
            ioDispatcher = Dispatchers.Unconfined
        )

        val releaseInfo = checker.parseReleaseJson(jsonPayload)
        assertEquals("v1.2.0", releaseInfo.tagName)
        assertEquals("1.2.0", releaseInfo.versionName)
        assertEquals("v1.2.0 - 升级优化", releaseInfo.title)
        assertEquals("1. 新增深色模式\n2. 优化睡眠计算性能", releaseInfo.releaseNotes)
        assertEquals("https://github.com/michiru233/SleepCycle/releases/download/v1.2.0/SleepCycle-v1.2.0.apk", releaseInfo.downloadUrl)
        assertEquals("https://github.com/michiru233/SleepCycle/releases/tag/v1.2.0", releaseInfo.htmlUrl)
    }

    @Test
    fun testParseReleaseJsonWithoutApkFallsBackToHtmlUrl() {
        val jsonPayload = """
            {
              "tag_name": "1.3.0",
              "name": "v1.3.0",
              "body": "更新说明",
              "published_at": "2026-08-19T12:00:00Z",
              "html_url": "https://github.com/michiru233/SleepCycle/releases/tag/1.3.0",
              "assets": []
            }
        """.trimIndent()

        val checker = UpdateChecker(
            httpFetcher = { jsonPayload },
            ioDispatcher = Dispatchers.Unconfined
        )

        val releaseInfo = checker.parseReleaseJson(jsonPayload)
        assertEquals("1.3.0", releaseInfo.versionName)
        assertEquals("https://github.com/michiru233/SleepCycle/releases/tag/1.3.0", releaseInfo.downloadUrl)
    }

    @Test
    fun testCheckForUpdateHasUpdate() = runBlocking {
        val jsonPayload = """
            {
              "tag_name": "v1.2.0",
              "name": "v1.2.0 - 大版本更新",
              "body": "全新升级",
              "published_at": "2026-08-19T10:00:00Z",
              "html_url": "https://github.com/michiru233/SleepCycle/releases/tag/v1.2.0",
              "assets": []
            }
        """.trimIndent()

        val checker = UpdateChecker(
            httpFetcher = { jsonPayload },
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = checker.checkForUpdate(currentVersionName = "1.1.1")
        assertTrue(result is UpdateCheckResult.HasUpdate)
        val update = result as UpdateCheckResult.HasUpdate
        assertEquals("1.2.0", update.releaseInfo.versionName)
    }

    @Test
    fun testCheckForUpdateUpToDate() = runBlocking {
        val jsonPayload = """
            {
              "tag_name": "v1.1.1",
              "name": "v1.1.1",
              "body": "当前最新",
              "published_at": "2026-08-19T10:00:00Z",
              "html_url": "https://github.com/michiru233/SleepCycle/releases/tag/v1.1.1",
              "assets": []
            }
        """.trimIndent()

        val checker = UpdateChecker(
            httpFetcher = { jsonPayload },
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = checker.checkForUpdate(currentVersionName = "1.1.1")
        assertTrue(result is UpdateCheckResult.UpToDate)
    }

    @Test
    fun testCheckForUpdateNetworkErrorHandlesGracefully() = runBlocking {
        val mockFetcher = HttpFetcher {
            throw java.io.IOException("网络连接超时")
        }

        val checker = UpdateChecker(
            httpFetcher = mockFetcher,
            ioDispatcher = Dispatchers.Unconfined
        )

        val result = checker.checkForUpdate(currentVersionName = "1.1.1")
        assertTrue(result is UpdateCheckResult.Error)
        val error = result as UpdateCheckResult.Error
        assertTrue(error.message.contains("网络连接超时"))
    }
}
