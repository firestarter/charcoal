package com.firestartermc.charcoal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.logging.log4j.LogManager
import java.awt.Dimension
import java.awt.FlowLayout
import java.nio.file.Path
import javax.swing.*
import kotlin.io.path.*

@OptIn(ExperimentalSerializationApi::class)
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = false
                namingStrategy = JsonNamingStrategy.SnakeCase
                ignoreUnknownKeys = true
            },
        )
    }

    install(HttpTimeout) {
        requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
    }
}

/**
 * Fetches the latest release from the GitHub API.
 */
suspend fun fetchLatestRelease(): GitHubRelease? = try {
    val response = client.get("https://api.github.com/repos/firestarter/bonfire/releases/latest") {
        header("X-GitHub-Api-Version", "2022-11-28")
    }

    response.body<GitHubRelease>()
} catch (x: Throwable) {
    x.printStackTrace()
    null
}

private val log = LogManager.getLogger()

@OptIn(ExperimentalPathApi::class)
suspend fun autoUpdate(directory: Path) {
    println(directory)

    val release = fetchLatestRelease()
                  ?: return

    log.info("Latest release is ${release.tagName}.")

    val response = JOptionPane.showConfirmDialog(
    null,
    "Bonfire version ${release.tagName} has been released! Would you like to automatically update your game now to this version?\n\nThis is required to keep playing on our official server!",
    )

    if (response != 0) {
        log.info("User declined to update to ${release.tagName}.")
        return
    }

    log.info("User accepted to update to ${release.tagName}.")

    val frame = JFrame("Updating Bonfire")
    frame.size = Dimension(500, 100)
    frame.layout = FlowLayout()
    frame.setLocationRelativeTo(null)

    val textArea = JTextArea(2, 20)
    textArea.wrapStyleWord = true
    textArea.lineWrap = true
    textArea.isOpaque = false
    textArea.isEditable = false
    textArea.isFocusable = false
    textArea.background = UIManager.getColor("Label.background")
    textArea.font = UIManager.getFont("Label.font")
    textArea.border = UIManager.getBorder("Label.border")

    fun updateLabel(text: String) {
        textArea.text = text
    }


    updateLabel("Downloading ${release.tagName} tarball...")
    frame.add(textArea)
    frame.isVisible = true

    // stream the tarball
    val tarball = client.get("https://codeload.github.com/firestarter/bonfire/legacy.tar.gz/refs/tags/2.3-SNAPSHOT")
        .bodyAsChannel()
        .toInputStream()
        .let(::GzipCompressorInputStream)
        .let(::TarArchiveInputStream)

    updateLabel("Extracting tarball...")

    var root: Path? = null
    while (true) {
        val entry = tarball.nextTarEntry
                    ?: break

        val extract = directory.resolve(entry.name)
        updateLabel("Extracting ${entry.name} to $extract...")

        // find the root directory.
        if (root == null && entry.name.startsWith("firestarter-bonfire") && entry.isDirectory) {
            root = extract
        }

        //
        if (entry.isDirectory) extract.createDirectories() else extract.createFile()
    }

    requireNotNull(root) { "Root directory not found in tarball." }
    log.info("Extracted to $root, copying files to $directory.")

    for (entry in root.listDirectoryEntries()) entry.copyToRecursively(
        directory.resolve(entry.name),
        followLinks = false,
        overwrite = true,
    )

    root.deleteRecursively()
}

fun autoUpdateBlocking(directory: Path) = runBlocking {
    try {
        autoUpdate(directory)
    } catch (ex: Throwable) {
        log.error("Failed to auto-update.", ex)
    }
}
