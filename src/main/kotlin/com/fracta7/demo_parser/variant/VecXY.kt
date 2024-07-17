package com.fracta7.demo_parser.variant

data class VecXY(val coordinates: FloatArray) {
    constructor(x: Float, y: Float) : this(floatArrayOf(x, y))

    val x: Float
        get() = coordinates[0]

    val y: Float
        get() = coordinates[1]
}