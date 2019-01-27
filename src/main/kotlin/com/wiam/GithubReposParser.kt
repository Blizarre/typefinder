package com.wiam

import com.beust.klaxon.Klaxon
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
                val release = Klaxon().parse<ReleaseDeserialized>(stream)
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

data class ReleaseDeserialized(
    val zipball_url: String,
    val name: String
)

class Release(val repository: Repository, releaseDeserialized: ReleaseDeserialized) {
    val url = URL(releaseDeserialized.zipball_url)
    val name = releaseDeserialized.name
}