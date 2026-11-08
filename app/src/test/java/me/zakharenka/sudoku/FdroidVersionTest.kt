package me.zakharenka.sudoku

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class FdroidVersionTest {

    @Test
    fun fdroiddataVersionMatchesBuildConfig() {
        val versionFile = listOf(
            File("fdroid/version.json"),
            File("../fdroid/version.json"),
        ).firstOrNull { it.isFile }
            ?: error("fdroid/version.json not found")

        val text = versionFile.readText()
        val versionName = Regex("\"versionName\"\\s*:\\s*\"([^\"]+)\"")
            .find(text)?.groupValues?.get(1)
        val versionCode = Regex("\"versionCode\"\\s*:\\s*(\\d+)")
            .find(text)?.groupValues?.get(1)?.toInt()

        assertEquals(BuildConfig.VERSION_NAME, versionName)
        assertEquals(BuildConfig.VERSION_CODE, versionCode)
    }
}
