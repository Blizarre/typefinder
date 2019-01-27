package com.wiam

import java.util.logging.Level
import java.util.logging.Logger


const val REPOSITORY_QUEUE_SIZE = 50
const val RELEASE_QUEUE_SIZE = 50

val log = Logger.getLogger("Main")!!

fun main(args: Array<String>) {
    if (args.size != 1) {
        System.err.println("Error: Port number expected")
        System.exit(1)
    }

    log.level = Level.FINE
    log.parent.level = Level.FINE
    log.parent.handlers.forEach { it.level = Level.FINE }

    val repositoryQueue = Queue<Repository>(REPOSITORY_QUEUE_SIZE)
    val releaseQueue = Queue<Release>(RELEASE_QUEUE_SIZE)

    val githubInterfaceAPI = GithubAPIInterface()
    val githubInterfaceSearch = GithubAPIInterface()

    val reposDiscoverer = GithubReposDiscoverer(repositoryQueue, githubInterfaceSearch)
    val reposParser = GithubReposParser(repositoryQueue, releaseQueue, githubInterfaceAPI)
    val classFinder = JavaClassProcessor(releaseQueue)

    val ti = Thread(reposDiscoverer)
    val tp = Thread(reposParser)
    val tc = Thread(classFinder)

    val status = Thread {
        while (true) {
            log.info("Status: repos ${repositoryQueue.size}, release ${releaseQueue.size}")
            Thread.sleep(5000)
        }
    }

    log.info("Starting indexing thread ${ti.id}")
    ti.start()
    log.info("Starting Parsing thread ${tp.id}")
    tp.start()
    log.info("Starting Finder thread ${tp.id}")
    tc.start()

    log.info("Starting status thread ${status.id}")
    status.start()
    ti.join()
    tp.join()
    tc.join()
    status.join()
    log.info("End of threads")
}

