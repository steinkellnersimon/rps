package at.stnwtr.rps.game

import at.stnwtr.rps.RpsServer
import at.stnwtr.rps.RpsWsHandler
import at.stnwtr.rps.packet.IncomingSelectionPacket
import at.stnwtr.rps.packet.OutgoingFinishPacket
import at.stnwtr.rps.packet.OutgoingMoveResultPacket
import at.stnwtr.rps.packet.OutgoingStatsPacket
import at.stnwtr.rps.player.Player
import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow
import kotlin.math.roundToInt

class RpsGame(val handler: RpsWsHandler, val server: RpsServer, val blue: Pair<WsContext, Player>, val red: Pair<WsContext?, Player>) {

    private fun Double.clamp(min: Double, max: Double) = if (this < min) min else (if (this > max) max else this)

    // only red may be bot
    private var bluePoints = 0
    private var redPoints = 0

    private var blueSelection: Hand? = null
    private var redSelection: Hand? = null

    fun receive(context: WsMessageContext) {
        val hand = Hand.byType(server.jsonMapper.readValue<IncomingSelectionPacket>(context.message()).value)
        when (context) {
            blue.first -> {
                if (blueSelection == null) {
                    blueSelection = hand
                    if (red.second.isBot) {
                        handleBot()
                    }
                    if (redSelection != null) {
                        update()
                    }
                }
            }
            red.first -> {
                if (redSelection == null) {
                    redSelection = hand
                    if (blueSelection != null) {
                        update()
                    }
                }
            }
        }
    }

    private fun score(winnerScore: Int, loserScore: Int, wins: Int, defeats: Int): Int {
        val multiplier = ((loserScore + 1_000.0).pow(2) / (winnerScore + 1_000.0).pow(2)).clamp(0.1, 10.0)
        return (multiplier * (wins - defeats) * 10).roundToInt()
    }

    private fun update() {
        var winner: Player? = null
        if (blueSelection!!.beats(redSelection!!)) {
            winner = blue.second
            bluePoints += 1
        } else if (redSelection!!.beats(blueSelection!!)) {
            winner = red.second
            redPoints += 1
        }

        val bluePacket = OutgoingMoveResultPacket(
            blueSelection!!.type,
            redSelection!!.type,
            if (winner == null) Result.DRAW.type else Result.byBoolean(winner == blue.second).type,
            bluePoints,
            redPoints
        )

        val redPacket = OutgoingMoveResultPacket(
            redSelection!!.type,
            blueSelection!!.type,
            if (winner == null) Result.DRAW.type else Result.byBoolean(winner == red.second).type,
            redPoints,
            bluePoints
        )

        blue.first.send(server.jsonMapper.writeValueAsString(bluePacket))
        red.first?.send(server.jsonMapper.writeValueAsString(redPacket))

        if (bluePoints == 3 || redPoints == 3) {
            var blueFinishPacket: OutgoingFinishPacket? = null
            var redFinishPacket: OutgoingFinishPacket? = null
            val score: Int
            val blueStatsPacket: OutgoingStatsPacket?
            val redStatsPacket: OutgoingStatsPacket?

            if (bluePoints == 3) {
                blueFinishPacket = OutgoingFinishPacket(Result.WIN.type)
                redFinishPacket = OutgoingFinishPacket(Result.DEFEAT.type)
                score = score(blue.second.score, red.second.score, bluePoints, redPoints)
                blue.second.wins += 1
                blue.second.score += score
                red.second.defeats += 1
                red.second.score -= score
            } else if (redPoints == 3) {
                blueFinishPacket = OutgoingFinishPacket(Result.DEFEAT.type)
                redFinishPacket = OutgoingFinishPacket(Result.WIN.type)
                score = score(red.second.score, blue.second.score, bluePoints, redPoints)
                red.second.wins += 1
                red.second.score += score
                blue.second.defeats += 1
                blue.second.score -= score
            }

            server.playerDatabase.updatePlayer(blue.second)
            server.playerDatabase.updatePlayer(red.second)

            blueStatsPacket = OutgoingStatsPacket(
                blue.second.wins,
                blue.second.defeats,
                server.playerDatabase.getPlayerRank(blue.second),
                blue.second.score
            )

            redStatsPacket = OutgoingStatsPacket(
                red.second.wins,
                red.second.defeats,
                server.playerDatabase.getPlayerRank(red.second),
                red.second.score
            )

            blue.first.send(server.jsonMapper.writeValueAsString(blueFinishPacket))
            red.first?.send(server.jsonMapper.writeValueAsString(redFinishPacket))

            blue.first.send(server.jsonMapper.writeValueAsString(blueStatsPacket))
            red.first?.send(server.jsonMapper.writeValueAsString(redStatsPacket))

            handler.games.remove(this)

            blue.second.isPlaying = false
            red.second.isPlaying = false
        }

        blueSelection = null
        redSelection = null
    }

    private fun handleBot() {
        redSelection = Hand.values()[ThreadLocalRandom.current().nextInt(Hand.values().size)]
    }

    companion object {
        val PLAYER_QUEUE = ConcurrentLinkedQueue<Player>()
        const val TIMEOUT_SECONDS: Long = 5
    }
}