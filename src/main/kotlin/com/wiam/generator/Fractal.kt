package com.wiam.generator


class Fractal(private val width: Int, private val height: Int) :
    AbstractDefaultImageGenerator<Fractal.Parameters>(width, height) {
    override fun getParam(seed: Int) = Parameters(seed)

    class Parameters(seed: Int) {
        val originX = seed.and(0xF) * 8 + 30
        val resY = seed.shr(4).and(0xF) * 8 + 30
    }

    override fun getPixel(parameter: Parameters, i: Int, j: Int): Int {
        val res = 256
        return fractal(
            -(width - parameter.originX) / (2.0F * res) + i.toFloat() / res + 0.3F,
            -(height - parameter.resY) / (2.0F * res) + j.toFloat() / res - 0.6F
        )
    }

    private fun fractal(x: Float, y: Float): Int {
        val c = Complex(x, y)
        var z = Complex(x, y)
        for (i in 0..255) {
            if (z.norm2() > 4096) {
                return (350 * Math.log(i.toDouble()) / Math.log(255.0)).coerceIn(0.0, 255.0).toInt()
            } else {
                z = z.square() + c
            }
        }
        return 0
    }

    class Complex(var x: Float, var y: Float) {
        operator fun plus(other: Complex): Complex {
            return Complex(x + other.x, y + other.y)
        }

        fun square(): Complex {
            return Complex(x * x - y * y, 2 * x * y)
        }

        fun norm2(): Float {
            return x * x + y * y
        }
    }
}
