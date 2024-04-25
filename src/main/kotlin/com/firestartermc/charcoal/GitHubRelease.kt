package com.firestartermc.charcoal

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    val tagName: String,
    val tarballUrl: String,
    val publishedAt: Instant,
)
