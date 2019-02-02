package com.wiam.persistence

import org.jetbrains.exposed.dao.IntIdTable

object Types : IntIdTable() {
    val githubFileUrl = text("githubfileurl")
    val line = integer("line")
    val type = varchar("type", 128)
}