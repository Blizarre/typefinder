package com.wiam.generator

import java.time.Duration
import java.time.Instant

abstract class AbstractDefaultImageGenerator<Param>(private val width: Int, private val height: Int) : ImageGenerator {
    abstract fun getPixel(parameter: Param, i: Int, j: Int): Int
    abstract fun getParam(seed: Int): Param

    final override fun getImage(seed: Int): IntArray {
        val timer = Instant.now()

        val p = getParam(seed)

        val bi = IntArray(width * height)

        for (i in 0 until width) {
            for (j in 0 until height) {
                bi[i + j * width] =
                        getPixel(p, i, j)
            }
        }
        val elapsed = Duration.between(timer, Instant.now())
        System.out.println("${this.javaClass.name} generated in ${elapsed.toMillis()} ms.")
        return bi
    }

}