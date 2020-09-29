package at.stnwtr.rps.packet

class OutgoingLoginPacket(
    val username: String,
    val uuid: String
) : OutgoingPacket(PacketType.LOGIN)

class OutgoingStatsPacket(
    val wins: Int,
    val defeats: Int,
    val rank: Int,
    val score: Int
) : OutgoingPacket(PacketType.STATS)

class OutgoingStartGamePacket(
    val ownRank: Int,
    val enemyName: String,
    val enemyRank: Int
) : OutgoingPacket(PacketType.START_GAME)

class OutgoingMoveResultPacket(
    val ownMove: String,
    val enemyMove: String,
    val result: String,
    val ownPoints: Int,
    val enemyPoints: Int
) : OutgoingPacket(PacketType.MOVE_RESULT)

class OutgoingFinishPacket(
    val result: String
) : OutgoingPacket(PacketType.FINISH)
