package com.wiam

import java.util.concurrent.ArrayBlockingQueue
import java.util.function.Consumer

class Queue<T>(capacity: Int) : Producer<T>, Consumer<T> {
    private val queueImpl = ArrayBlockingQueue<T>(capacity)

    override fun get(): T = queueImpl.take()
    override fun accept(value: T) = queueImpl.put(value)

    val size: Int
        get() = queueImpl.size

}