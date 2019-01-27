package com.wiam

interface Producer<T> {
    fun get(): T
}
