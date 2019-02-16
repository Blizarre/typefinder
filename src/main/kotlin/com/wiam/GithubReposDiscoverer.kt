package com.wiam

import com.wiam.github.listreleases.JavaReposReleases
import com.wiam.github.listreleases.Release
import com.wiam.stats.Statistics
import java.util.function.Consumer
import java.util.logging.Logger

class GithubReposDiscoverer(private val processQueue: Consumer<Release>, private val api: JavaReposReleases, private val stats: Statistics) :
        Runnable {
    private val log = Logger.getLogger(this.javaClass.name)!!


    override fun run() {
        var addedThisLoop: Int

        do {
            log.fine("Fetching repositories...")
            val releases = api.listReleases()
            addedThisLoop = releases.size
            log.info("Adding $addedThisLoop repos to the queue")
            stats.add("discover.repos.total", addedThisLoop)
            releases.forEach(processQueue::accept)
            stats.add("discover.pages", 1)
        } while (addedThisLoop > 0)
        log.info("Finished indexing")
    }
}


