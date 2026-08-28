package com.arena.marketradar.data.model

/** A single release as returned by the GitHub Releases API. */
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val publishedAt: Long,
    val body: String?,
    val assets: List<GitHubAsset>,
    val versionCode: Int,
)

data class GitHubAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
)
