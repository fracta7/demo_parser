package com.fracta7.demo_parser

import com.fracta7.demo_parser.variant.Variant

data class SellBackHelper(
    val tick: Int,
    val steamId: ULong,
    val inventorySlot: UInt
) {
    companion object{
        fun fromEvent(event: GameEvent) : SellBackHelper? {

        }
        fun extractField(name: String, fields: List<EventField>) : Variant? {

        }
    }

}