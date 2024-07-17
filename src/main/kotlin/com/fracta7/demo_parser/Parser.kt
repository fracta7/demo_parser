package com.fracta7.demo_parser

import com.fracta7.demo_parser.variant.Variant
import java.util.*

data class Parser(
    val input: ParserInputs,
    val forceSinglethread: Boolean
) {
    fun parseDemo(demoBytes: ByteArray): Result<DemoOutput> {
        val firstPassParser = FirstPassParser(input)
        val firstPassOutput = firstPassParser.parseDemo(demoBytes).getOrElse { return Result.failure(it) }

        return if (checkMultithreadability(input.wantedPlayerProps) && !forceSinglethread) {
            secondPassMultiThreaded(demoBytes, firstPassOutput)
        } else {
            secondPassSingleThreaded(demoBytes, firstPassOutput)
        }
    }

    private fun secondPassSingleThreaded(
        outerBytes: ByteArray,
        firstPassOutput: FirstPassOutput
    ): Result<DemoOutput> {
        val parser = SecondPassParser(firstPassOutput.clone(), 16, true).getOrElse { return Result.failure(it) }
        parser.start(outerBytes).getOrElse { return Result.failure(it) }
        val secondPassOutput = parser.createOutput()
        val outputs = combineOutputs(mutableListOf(secondPassOutput), firstPassOutput)
        rmUnwantedTicks(outputs.df)?.let { outputs.df = it }
        addItemPurchaseSellColumn(outputs.gameEvents)
        removeItemSoldEvents(outputs.gameEvents)
        return Result.success(outputs)
    }

    private fun secondPassMultiThreaded(
        outerBytes: ByteArray,
        firstPassOutput: FirstPassOutput
    ): Result<DemoOutput> {
        val secondPassOutputs = firstPassOutput.fullpacketOffsets.parallelMap { offset ->
            val parser = SecondPassParser(firstPassOutput.clone(), offset, false).getOrElse { return@parallelMap Result.failure(it) }
            parser.start(outerBytes).getOrElse { return@parallelMap Result.failure(it) }
            Result.success(parser.createOutput())
        }

        val ok = secondPassOutputs.mapNotNull { it.getOrNull() }
        if (ok.size != secondPassOutputs.size) {
            return Result.failure(DemoParserError("Error in multi-threaded parsing"))
        }

        val outputs = combineOutputs(ok.toMutableList(), firstPassOutput)
        rmUnwantedTicks(outputs.df)?.let { outputs.df = it }
        addItemPurchaseSellColumn(outputs.gameEvents)
        removeItemSoldEvents(outputs.gameEvents)
        return Result.success(outputs)
    }

    companion object {
        fun removeItemSoldEvents(events: MutableList<GameEvent>) {
            events.removeAll { it.name == "item_sold" }
        }

        fun addItemPurchaseSellColumn(events: MutableList<GameEvent>) {
            val purchases = events.filter { it.name == "item_purchase" }
            val sells = events.filter { it.name == "item_sold" }

            val purchaseHelpers = purchases.mapNotNull { SellBackHelper.fromEvent(it) }
            val sellHelpers = sells.mapNotNull { SellBackHelper.fromEvent(it) }

            val wasSold = purchaseHelpers.map { purchase ->
                val wantedSells = sellHelpers.filter {
                    it.tick > purchase.tick && it.steamid == purchase.steamid && it.inventorySlot == purchase.inventorySlot
                }
                val wantedBuys = purchaseHelpers.filter {
                    it.tick > purchase.tick && it.steamid == purchase.steamid && it.inventorySlot == purchase.inventorySlot
                }
                val minTickSell = wantedSells.minByOrNull { it.tick }
                val minTickBuy = wantedBuys.minByOrNull { it.tick }

                minTickSell != null && minTickBuy != null && minTickSell.tick < minTickBuy.tick
            }

            var idx = 0
            events.forEach { event ->
                if (event.name == "item_purchase") {
                    event.fields.add(EventField(
                        name = "was_sold",
                        data = Variant.Bool(wasSold[idx])
                    ))
                    idx++
                }
            }
        }
    }

    private fun rmUnwantedTicks(hm: MutableMap<UInt, PropColumn>): Map<UInt, PropColumn>? {
        if (input.wantedTicks.isEmpty()) return null

        val wantedIndices = hm[TICK_ID]?.data?.let { data ->
            when (data) {
                is VarVec.I32 -> data.withIndex()
                    .filter { (_, value) -> value != null && input.wantedTicks.contains(value) }
                    .map { it.index }
                else -> null
            }
        } ?: return null

        return hm.mapValues { (_, v) -> v.sliceToNew(wantedIndices) ?: v }
    }

    private fun combineOutputs(secondPassOutputs: MutableList<SecondPassOutput>, firstPassOutput: FirstPassOutput): DemoOutput {
        secondPassOutputs.sortBy { it.ptr }
        val allDfsCombined = combineDfs(secondPassOutputs.map { it.df }.toMutableList(), false)
        val allGameEvents = secondPassOutputs.flatMap { it.gameEventsCounter }.toSet()

        val propController = firstPassOutput.propController.copy()
        firstPassOutput.addedTempProps.forEach { prop ->
            propController.wantedPlayerProps.remove(prop)
            propController.propInfos.removeAll { it.propName == prop }
        }

        val perPlayers = secondPassOutputs.map { it.dfPerPlayer }
        val allSteamids = perPlayers.flatMap { it.keys }.toSet()

        val pp = allSteamids.associateWith { steamid ->
            val v = perPlayers.mapNotNull { it[steamid] }
            combineDfs(v.toMutableList(), true)
        }

        return DemoOutput(
            propController = propController,
            chatMessages = secondPassOutputs.flatMap { it.chatMessages },
            itemDrops = secondPassOutputs.flatMap { it.itemDrops },
            playerMd = secondPassOutputs.flatMap { it.playerMd },
            gameEvents = secondPassOutputs.flatMap { it.gameEvents },
            skins = secondPassOutputs.flatMap { it.skins },
            convars = secondPassOutputs.flatMap { it.convars },
            df = allDfsCombined,
            header = firstPassOutput.header,
            gameEventsCounter = allGameEvents,
            projectiles = secondPassOutputs.flatMap { it.projectiles },
            voiceData = secondPassOutputs.flatMap { it.voiceData },
            dfPerPlayer = pp
        )
    }

    private fun combineDfs(v: MutableList<Map<UInt, PropColumn>>, removeNameAndSteamid: Boolean): Map<UInt, PropColumn> {
        if (v.size == 1) {
            val result = v.removeAt(0).toMutableMap()
            if (removeNameAndSteamid) {
                result.remove(STEAMID_ID)
                result.remove(NAME_ID)
            }
            return result
        }

        val big = mutableMapOf<UInt, PropColumn>()
        for (partDf in v) {
            for ((k, v) in partDf) {
                if (removeNameAndSteamid && (k == STEAMID_ID || k == NAME_ID)) {
                    continue
                }

                if (k in big) {
                    big[k]?.extendFrom(v)
                } else {
                    big[k] = v.clone()
                }
            }
        }
        return big
    }
}

