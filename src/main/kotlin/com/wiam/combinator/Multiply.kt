package com.wiam.combinator

fun multiply(a: IntArray, b: IntArray): IntArray {
    require(a.size == b.size)
    val ret = IntArray(a.size)
    for (i in 0 until a.size) {
        ret[i] = (a[i] * b[i] / 255)
    }
    return ret
}