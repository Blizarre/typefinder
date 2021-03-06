package com.wiam.persistence

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.insert

class TypeNameTooLong(name: String) : Exception("Type $name is too long")

object Types : IntIdTable() {
    private const val MAX_TYPE_LENGTH = 128
    private val repository = text("repository")
    private val githubFileUrl = text("githubfileurl")
    private val line = integer("line")
    private val type = varchar("type", MAX_TYPE_LENGTH).index()

    fun insert(repository: String, typeName: String, fileUrl: String, lineNumber: Int) {
        if (typeName.length > MAX_TYPE_LENGTH) {
            throw TypeNameTooLong(typeName)
        }
        insert {
            it[this.repository] = repository
            it[type] = typeName
            it[line] = lineNumber
            it[githubFileUrl] = fileUrl
        }

    }
}