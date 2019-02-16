package com.wiam

import com.wiam.github.listreleases.JavaReposReleases
import com.wiam.github.listreleases.Release
import com.wiam.persistence.ClassFinderError
import com.wiam.persistence.Types
import com.wiam.stats.Statistics
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Level
import java.util.logging.Logger


const val RELEASE_QUEUE_SIZE = 50

val log = Logger.getGlobal()!!

fun main(args: Array<String>) {
    log.level = Level.INFO
    log.parent.handlers.forEach { it.level = Level.INFO }

    val releaseQueue = Queue<Release>(RELEASE_QUEUE_SIZE)

    val githubInterfaceAPI = JavaReposReleases()

    // TODO: Improve data model with a separate Repository table
    // TODO: Add persistence for the discoverer
    Database.connect("jdbc:postgresql:githubparser", driver = "org.postgresql.Driver", user = "user", password = "user")

    if (args.contains("--reset")) {
        log.info("Resetting the database")
        transaction {
            SchemaUtils.drop(Types)
            SchemaUtils.create(Types)

            SchemaUtils.drop(ClassFinderError)
            SchemaUtils.create(ClassFinderError)
        }
    }

    val stats = Statistics()
    val reposDiscoverer = GithubReposDiscoverer(releaseQueue, githubInterfaceAPI, stats)
    val classFinders = listOf(
            JavaClassProcessor(releaseQueue, stats),
            JavaClassProcessor(releaseQueue, stats),
            JavaClassProcessor(releaseQueue, stats),
            JavaClassProcessor(releaseQueue, stats))

    val ti = Thread(reposDiscoverer)
    val tFinders = classFinders.map {
        Thread(it)
    }

    log.info("Starting indexing thread ${ti.id}")
    ti.start()
    log.info("Starting Finder threads: ${tFinders.joinToString { it.id.toString() }}")
    tFinders.forEach { it.start() }

    while (true) {
        if (!ti.isAlive) log.warning("Indexing thread dead")
        if (tFinders.any { !it.isAlive }) log.warning("Finder thread dead")
        Thread.sleep(5000)
        System.out.println("Runtime Statistics:\n$stats")
    }
}

