package com.wiam.persistence

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.joda.time.DateTime

object ClassFinderError : IntIdTable() {
    private val repository = ClassFinderError.text("repository")
    private val githubFileUrl = ClassFinderError.text("githubfileurl")
    private val message = ClassFinderError.text("message")
    private val time = ClassFinderError.datetime("timestamp")

    fun insert(repository: String, fileUrl: String, message: String) {
        insert {
            it[this.repository] = repository
            it[this.githubFileUrl] = fileUrl
            it[this.message] = message
            it[this.time] = DateTime.now()
        }

    }

}
