package com.wiam

import com.wiam.github.GithubAPIInterface
import com.wiam.github.json.Repository
import com.wiam.persistence.Types
import com.wiam.stats.Statistics
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Level
import java.util.logging.Logger


const val REPOSITORY_QUEUE_SIZE = 50
const val RELEASE_QUEUE_SIZE = 50

val log = Logger.getGlobal()!!

fun main(args: Array<String>) {
    log.level = Level.INFO
    log.parent.handlers.forEach { it.level = Level.INFO }

    val repositoryQueue = Queue<Repository>(REPOSITORY_QUEUE_SIZE)
    val releaseQueue = Queue<Release>(RELEASE_QUEUE_SIZE)

    val githubInterfaceAPI = GithubAPIInterface()

    Database.connect("jdbc:postgresql:githubparser", driver = "org.postgresql.Driver", user = "user", password = "user")

    if (args.contains("--reset")) {
        log.info("Resetting the database")
        transaction {
            SchemaUtils.drop(Types)
            SchemaUtils.create(Types)
        }
    }

    val stats = Statistics()
    val reposDiscoverer = GithubReposDiscoverer(repositoryQueue, githubInterfaceAPI, stats)
    val reposParser = GithubReposParser(repositoryQueue, releaseQueue, githubInterfaceAPI, stats)
    val classFinder = JavaClassProcessor(releaseQueue, Types, stats)

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

    while (true) {
        if (!ti.isAlive) log.warning("Indexing thread dead")
        if (!tp.isAlive) log.warning("Parsing thread dead")
        if (!tc.isAlive) log.warning("Finder thread dead")
        Thread.sleep(5000)
        System.out.println("Runtime Statistics:\n$stats")
    }
}

