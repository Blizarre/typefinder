package com.wiam

import com.beust.klaxon.Klaxon
import com.beust.klaxon.KlaxonException
import com.wiam.github.GithubAPIInterface
import com.wiam.github.RequestError
import com.wiam.github.json.Repository
import com.wiam.stats.Statistics
import java.io.InputStreamReader
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
            val languagesURL = URL(repo.languages_url)
            val releasesURL = URL(repo.releases_url.replace("{/id}", "/latest"))
            // TODO: heavy refactoring required
            try {
                val languagesStream = githubInterface.call(languagesURL).getInputStream()
                val languages = Klaxon().parseJsonObject(InputStreamReader(languagesStream))
                stats.add("parser.repos.processed", 1)

                if (languages.keys.contains("Java")) {
                    stats.add("parser.repos.java", 1)
                    val releaseStream = githubInterface.call(releasesURL).getInputStream()
                    val release = Klaxon().parse<com.wiam.github.json.Release>(releaseStream)
                    if (release != null) {
                        log.info("Found release ${release.zipball_url}")
                        stats.add("parser.releases.parsed", 1)
                        releaseQueue.accept(Release(repo, release))
                    } else {
                        stats.add("parser.parse.error", 1)
                        log.warning("Error when fetching release for ${repo.full_name}")
                    }

                }

            } catch (e: RequestError) {
                when (e.code) {
                    404 -> {
                        log.info("No release for ${repo.full_name}")
                        stats.add("parser.request.norrelease", 1)
                    }
                    else -> {
                        stats.add("parser.request.error", 1)
                        log.warning("Error when fetching ${repo.full_name}: ${e.message}")
                    }

                }
            } catch (e: KlaxonException) {
                stats.add("parser.klaxon.error", 1)
                log.warning("Error parsing data from $releasesURL: ${e.message}")
            }
        }
    }
}

class Release(val repository: Repository, jsonRelease: com.wiam.github.json.Release) {
    private val tagName = jsonRelease.tag_name
    val zipUrl = URL(jsonRelease.zipball_url)
    val name = jsonRelease.name

    fun htmlUrl(filePath: String) = "${repository.html_url}/tree/$tagName/$filePath"
}