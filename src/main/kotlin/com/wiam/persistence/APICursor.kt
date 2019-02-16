package com.wiam.persistence

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object APICursor : IntIdTable() {
    private val cursor = text("cursor")

    fun insert(newCursor: String) {
        insert {
            it[this.cursor] = newCursor
        }
    }

    fun getLast(): String? {
        val results = transaction {
            APICursor
                    .selectAll()
                    .orderBy(APICursor.id, false)
                    .limit(1).toList()
        }
        return results.firstOrNull()?.get(APICursor.cursor)
    }

}
