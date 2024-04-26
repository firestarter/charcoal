package com.firestartermc.charcoal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.logging.log4j.LogManager
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JTextArea
import javax.swing.UIManager
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.name

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
suspend fun fetchLatestRelease(description: PackDescription): GitHubRelease? = try {
    val response = client.get("https://api.github.com/repos/${description.githubRepo}/releases/latest") {
        header("X-GitHub-Api-Version", "2022-11-28")
    }

    response.body<GitHubRelease>()
} catch (x: Throwable) {
    x.printStackTrace()
    null
}

private val log = LogManager.getLogger()

@OptIn(ExperimentalPathApi::class)
suspend fun autoUpdate(directory: Path): Boolean {
    val packDescription = readPackDescription(directory) ?: return false
    val release = fetchLatestRelease(packDescription) ?: return false

    /* check to see if a new update is available */
    if (packDescription.packVersion == release.tagName) {
        log.info("we are running the latest available pack version (${release.tagName}).")
        return false
    } else {
        log.warn("we are outdated; pack version ${release.tagName} is available for installation.")
    }

    /* let the user decide whether they would like to update their game */
    val response = JOptionPane.showConfirmDialog(null, "${packDescription.packName} version ${release.tagName} has been released! Would you like to automatically update your game now to this version?\n\nThis is required to keep playing on our official server!",)
    if (response != 0) {
        log.info("user declined the available pack update.")
        return false
    }

    log.info("user accepted the available pack update.")

    val frame = JFrame("Updating ${packDescription.packName} to version ${release.tagName}")
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


    updateLabel("Downloading ${packDescription.packName} ${release.tagName} tarball..")
    frame.add(textArea)
    frame.isVisible = true

    // stream the tarball
    val tarball = client.get(release.tarballUrl)
        .bodyAsChannel()
        .toInputStream()
        .let(::GzipCompressorInputStream)
        .let(::TarArchiveInputStream)

    updateLabel("Extracting tarball..")

    var root: Path? = null
    while (true) {
        val entry = tarball.nextTarEntry ?: break

        val extract = directory.resolve(entry.name)
        updateLabel("Extracting ${entry.name} to $extract...")

        // find the root directory.
        if (root == null && entry.name.startsWith("firestarter-bonfire") && entry.isDirectory) {
            root = extract
        }

        // copy the file into the desired directory
        if (entry.isDirectory) {
            extract.createDirectories()
        } else {
            Files.copy(tarball, extract, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    requireNotNull(root) { "Root directory not found in tarball." }
    log.info("extracted to $root, copying files to $directory.")

    /* completely remote top-level directories that are included in the update */
    for (topLevelDirectory in root.toFile().listFiles()!!) {
        log.info("deleted top-level directory ${topLevelDirectory.name} as it was included in the update file.")
        File(directory.toFile(), topLevelDirectory.name).deleteRecursively()
    }

    /* copy in the new update contents */
    for (entry in root.listDirectoryEntries()) {
        entry.moveTo(
            directory.resolve(entry.name),
            overwrite = true,
        )
    }

    root.deleteRecursively()
    JOptionPane.showMessageDialog(null, "${packDescription.packName} has been updated to the latest version. The game will now close and you may relaunch it immediately to continue playing!")
    return true
}

fun autoUpdateBlocking(directory: Path) = runBlocking {
    try {
       return@runBlocking autoUpdate(directory)
    } catch (ex: Throwable) {
        log.error("failed to auto-update.", ex)
    }

    false
}
