package com.fracta7.demo_parser.variant

data class Variant(
    val bool: Boolean,
    val u32: UInt,
    val i32: Int,
    val i16: Short,
    val f32: Float,
    val u64: ULong,
    val u8: UByte,
    val string: String,
    val vecXY: VecXY,
    val vecXYZ: VecXYZ,
    val stringVec: List<String>,
    val u32Vec: List<UInt>,
    val u64Vec: List<ULong>,
    val stickerVec: List<Sticker>
)