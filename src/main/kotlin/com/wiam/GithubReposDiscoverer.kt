package com.wiam

import com.beust.klaxon.Klaxon
import com.wiam.github.GithubAPIInterface
import com.wiam.github.json.Repository
import com.wiam.github.json.SearchResult
import com.wiam.stats.Statistics
import java.net.URL
import java.security.InvalidParameterException
import java.util.function.Consumer
import java.util.logging.Logger

class GithubReposDiscoverer(private val processQueue: Consumer<Repository>, private val api: GithubAPIInterface, private val stats: Statistics) :
    Runnable {
    private val javaReposSearch = URL("https://api.github.com/search/repositories?q=language=java")
    private val linkUrlRegex = Regex("<(.[^<>]+)>")
    private val linkRelRegex = Regex(""";\s+rel="(.*)"""")
    private val log = Logger.getLogger(this.javaClass.name)!!

    private fun findNextPage(linkHeader: String): URL {
        linkHeader.split(",").forEach {
            val url = linkUrlRegex.find(it)?.groupValues?.get(1) ?: ""
            val rel = linkRelRegex.find(it)?.groupValues?.get(1) ?: ""
            if (rel.contentEquals("next"))
                return URL(url)
        }
        log.warning("No next page found")
        throw InvalidParameterException("Cound not find nex Github Page")
    }

    override fun run() {
        var currentPage: URL = javaReposSearch
        var addedThisLoop: Int

        do {
            log.fine("Fetching $currentPage")
            val cnx = api.call(currentPage)
            val nextPage = findNextPage(cnx.getHeaderField("Link") ?: "")
            val repos = Klaxon()
                .parse<SearchResult>(cnx.getInputStream())
                ?: throw InvalidParameterException("Error parsing the response")

            addedThisLoop = repos.items.size
            log.info("Adding $addedThisLoop repos to the queue")
            repos.items.forEach(processQueue::accept)
            stats.add("discover.repos.total", addedThisLoop)
            stats.add("discover.pages", 1)
            currentPage = nextPage
        } while (addedThisLoop > 0)
        log.info("Finished indexing")
    }
}

