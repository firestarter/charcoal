package com.firestartermc.charcoal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    namingStrategy = JsonNamingStrategy.SnakeCase
}

@Serializable
data class PackDescription(
    val packName: String,
    val packVersion: String,
    val githubRepo: String,
    val serverAddress: String,
    val overrideTitleMenu: Boolean,
)

@OptIn(ExperimentalSerializationApi::class)
fun readPackDescription(directory: Path): PackDescription? {
    return Files.newInputStream(Path(directory.absolutePathString(), "pack.json")).use {
        json.decodeFromStream(it)
    }
}
