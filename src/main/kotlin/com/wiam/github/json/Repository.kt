package com.wiam.github.json

data class Repository(
        val full_name: String,
        val releases_url: String,
        val languages_url: String,
        val name: String,
        val html_url: String
)