package com.wiam

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.wiam.github.GithubAPIInterface
import com.wiam.github.RequestError
import com.wiam.github.json.Repository
import com.wiam.stats.Statistics
import java.net.URL
import java.util.function.Consumer


class GithubReposParser(
        private val repositoryQueue: Producer<Repository>,
        private val releaseQueue: Consumer<Release>,
        private val githubInterface: GithubAPIInterface,
        private val stats: Statistics
) : Runnable {
    override fun run() {
        while (true) {
            log.info("Fetching a new repository")
            val repo = repositoryQueue.get()
            stats.add("parser.processed.total", 1)
            log.info("Fetching information for ${repo.full_name}")
            val url = URL(repo.releases_url.replace("{/id}", "/latest"))
            try {

                val stream = githubInterface.call(url).getInputStream()
                val release = Klaxon().parse<com.wiam.github.json.Release>(stream)
                if (release != null) {
                    log.info("Found release ${release.zipball_url}")
                    stats.add("parser.releases.found", 1)
                    releaseQueue.accept(Release(repo, release))
                } else {
                    log.warning("Error when fetching release for ${repo.full_name}")
                }
            } catch (e: RequestError) {
                stats.add("parser.request.errors", 1)
                when (e.code) {
                    404 -> log.info("No release for ${repo.full_name}")
                    else -> log.warning("Error when fetching ${repo.full_name}: ${e.message}")

                }
            } catch (e: KlaxonException) {
                stats.add("parser.klaxon.error", 1)
                log.warning("Error parsing data from $url: ${e.message}")
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