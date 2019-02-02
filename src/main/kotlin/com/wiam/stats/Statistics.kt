package com.wiam.stats

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Statistics {
    private val map = HashMap<String, Int>()
    private val lock = ReentrantLock()

    fun add(key: String, amount: Int) {
        lock.withLock {
            map.let {
                it[key] = it.getOrDefault(key, 0) + amount
            }
        }
    }

    override fun toString(): String {
        lock.withLock {
            val str = StringBuilder()
            map.keys.sorted().forEach { k -> str.append("$k:\t${map[k]}\n") }
            return str.toString()
        }
    }
}
