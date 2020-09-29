package at.stnwtr.rps.packet

enum class PacketType(val type: String) {
    LOGIN("login"),
    STATS("stats"),
    START_GAME("start-game"),
    SELECTION("selection"),
    MOVE_RESULT("move-result"),
    FINISH("finish"),
    INVALID("invalid");

    companion object {
        fun byType(type: String) = values().firstOrNull { it.type == type } ?: INVALID
    }
}

abstract class Packet(val type: String)

abstract class IncomingPacket(type: PacketType) : Packet(type.type)

abstract class OutgoingPacket(type: PacketType) : Packet(type.type)
