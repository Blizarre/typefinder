package com.wiam

import com.beust.klaxon.Klaxon
import com.wiam.github.GithubAPIInterface
import com.wiam.github.RequestError
import com.wiam.github.json.Repository
import java.net.URL
import java.util.function.Consumer


class GithubReposParser(
        private val repositoryQueue: Producer<Repository>,
        private val releaseQueue: Consumer<Release>,
        private val githubInterface: GithubAPIInterface
) : Runnable {
    override fun run() {
        while (true) {
            log.info("Fetching a new repository")
            val repo = repositoryQueue.get()
            log.info("Fetching information for ${repo.full_name}")
            val url = URL(repo.releases_url.replace("{/id}", "/latest"))
            try {
                val stream = githubInterface.call(url).getInputStream()
                val release = Klaxon().parse<com.wiam.github.json.Release>(stream)
                if (release != null) {
                    log.info("Found release ${release.zipball_url}")
                    releaseQueue.accept(Release(repo, release))
                } else {
                    log.warning("Error when fetching release for ${repo.full_name}")
                }
            } catch (e: RequestError) {
                when (e.code) {
                    404 -> log.info("No release for ${repo.full_name}")
                    else -> log.warning("Error when fetching ${repo.full_name}: ${e.message}")

                }
            }
        }
    }
}

class Release(val repository: Repository, jsonRelease: com.wiam.github.json.Release) {
    val zipUrl = URL(jsonRelease.zipball_url)
    val name = jsonRelease.name
    val tagName = jsonRelease.tag_name

    fun htmlUrl(filePath: String) = "${repository.html_url}/tree/$tagName/$filePath"
}