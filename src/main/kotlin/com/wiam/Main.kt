package com.wiam

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.logging.Level
import java.util.logging.Logger


const val REPOSITORY_QUEUE_SIZE = 50
const val RELEASE_QUEUE_SIZE = 50

val log = Logger.getGlobal()!!

object Types : IntIdTable() {
    val githubFileUrl = text("githubfileurl")
    val line = integer("line")
    val type = varchar("type", 128)
}

fun main(args: Array<String>) {
    log.level = Level.INFO
    log.parent.handlers.forEach { it.level = Level.INFO }

    val repositoryQueue = Queue<Repository>(REPOSITORY_QUEUE_SIZE)
    val releaseQueue = Queue<Release>(RELEASE_QUEUE_SIZE)

    val githubInterfaceAPI = GithubAPIInterface()
    val githubInterfaceSearch = GithubAPIInterface()

    Database.connect("jdbc:postgresql:githubparser", driver = "org.postgresql.Driver", user = "user", password = "user")

    if (args.contains("--reset")) {
        log.info("Resetting the database")
        transaction {
            SchemaUtils.drop(Types)
            SchemaUtils.create(Types)
        }
    }


    val reposDiscoverer = GithubReposDiscoverer(repositoryQueue, githubInterfaceSearch)
    val reposParser = GithubReposParser(repositoryQueue, releaseQueue, githubInterfaceAPI)
    val classFinder = JavaClassProcessor(releaseQueue, Types)

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

