package com.wiam.generator

interface ImageGenerator {
    fun getImage(seed: Int): IntArray
}