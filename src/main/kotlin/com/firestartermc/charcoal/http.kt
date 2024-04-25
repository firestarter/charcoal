package com.firestartermc.charcoal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.absolutePathString


val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(
            Json {
                encodeDefaults = false
                ignoreUnknownKeys = true
            },
        )
    }
}

fun fetchLatestRelease() = runBlocking {
    try {
        client.get("https://api.github.com/repos/firestarter/bonfire/releases/latest") {
            header("X-GitHub-Api-Version", "2022-11-28")
        }.body<GitHubRelease>()
    } catch (x: Throwable) {
        x.printStackTrace()
        null
    }
}

@OptIn(InternalAPI::class)
fun downloadRelease(url: String): File {
    val file = File(System.getProperty("java.io.tmpdir"), "bonfire-release.tar.gz")
    runBlocking { client.get(url).content.copyAndClose(file.writeChannel()) }
    return file
}

fun extractTarball(directory: Path, tarball: File) {
    FileInputStream(tarball)
        .let(::GzipCompressorInputStream)
        .let(::TarArchiveInputStream)
        .use { tar ->
            while (true) {
                val entry = tar.nextEntry ?: break
                val extractTo = directory.resolve(entry.name)
                println("copying file to ${extractTo.absolutePathString()}..")

                if (entry.isDirectory) {
                    Files.createDirectories(extractTo)
                } else {
                    Files.copy(tar, extractTo, REPLACE_EXISTING)
                }
            }
        }
}
