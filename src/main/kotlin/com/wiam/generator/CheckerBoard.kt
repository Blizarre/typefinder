package com.wiam.generator


class CheckerBoard(width: Int, height: Int) : AbstractDefaultImageGenerator<CheckerBoard.CheckerParam>(width, height) {
    class CheckerParam(seed: Int) {
        val resX = seed.and(0xF) * 5 + 5
        val resY = seed.shr(4).and(0xF) * 5 + 5
    }

    override fun getParam(seed: Int) = CheckerParam(seed)
    override fun getPixel(parameter: CheckerParam, i: Int, j: Int): Int {
        return if ((i / parameter.resX + j / parameter.resY) % 2 == 0) 255 else 0
    }

}
