package com.wiam

import com.beust.klaxon.Klaxon
import java.net.URL
import java.security.InvalidParameterException
import java.util.function.Consumer
import java.util.logging.Logger

class GithubReposDiscoverer(private val processQueue: Consumer<Repository>, private val api: GithubAPIInterface) :
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
            currentPage = nextPage
        } while (addedThisLoop > 0)
        log.info("Finished indexing")
    }
}

class SearchResult(
    val total_count: Int,
    val items: List<Repository>
)

data class Repository(
        val id: Long,
        val full_name: String,
        val releases_url: String,
        val name: String,
        val html_url: String
)