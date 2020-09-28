package at.stnwtr.rps.packet

class IncomingLoginPacket(
    val username: String,
    val uuid: String?
) : IncomingPacket(PacketType.LOGIN)

class IncomingStatsPacket : IncomingPacket(PacketType.STATS)

class IncomingStartGamePacket : IncomingPacket(PacketType.START_GAME)

class IncomingSelectionPacket(
    val value: String
) : IncomingPacket(PacketType.SELECTION)
