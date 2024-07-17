package com.fracta7.demo_parser.variant

data class VecXYZ(val coordinates: FloatArray) {
    constructor(x: Float, y: Float, z: Float) : this(floatArrayOf(x, y, z))

    val x: Float
        get() = coordinates[0]

    val y: Float
        get() = coordinates[1]

    val z: Float
        get() = coordinates[2]
}