package com.fracta7.demo_parser.variant

data class VarVec(
    val u32: List<UInt?>,
    val bool: List<Boolean?>,
    val u64: List<UInt?>,
    val f32: List<Float?>,
    val i32: List<Int?>,
    val string: List<String?>,
    val stringVec: List<List<String>>,
    val u64Vec: List<List<ULong>>,
    val u32Vec: List<List<UInt>>,
    val xyVec: List<VecXY?>,
    val xyzVec: List<VecXYZ?>,
    val stickers: List<List<Sticker>>
)