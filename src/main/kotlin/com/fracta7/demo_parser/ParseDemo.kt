package com.fracta7.demo_parser

data class DemoOutput(
    var df: HashMap<UInt, PropColumn>,
    val gameEvents: MutableList<GameEvent>,
    val skins: MutableList<EconItem>,
    val itemDrops: MutableList<EconItem>,
    val chatMessages: MutableList<ChatMessageRecord>,
    val convars: HashMap<String, String>,
    val header: HashMap<String, String>?,
    val playerMd: MutableList<PlayerEndMetaData>,
    val gameEventsCounter: HashSet<String>,
    val projectiles: MutableList<ProjectileRecord>,
    val voiceData: MutableList<CSVCMsg_VoiceData>,
    val propController: PropController,
    val dfPerPlayer: HashMap<ULong, HashMap<UInt, PropColumn>>
)