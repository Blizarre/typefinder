package com.wiam.persistence

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.joda.time.DateTime

object ClassFinderError : IntIdTable() {
    private val repository = text("repository")
    private val githubFileUrl = text("githubfileurl")
    private val message = text("message")
    private val time = datetime("timestamp")

    fun insert(repository: String, fileUrl: String, message: String) {
        insert {
            it[this.repository] = repository
            it[this.githubFileUrl] = fileUrl
            it[this.message] = message
            it[this.time] = DateTime.now()
        }

    }

}
