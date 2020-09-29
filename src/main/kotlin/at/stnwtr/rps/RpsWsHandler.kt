package at.stnwtr.rps

import at.stnwtr.rps.game.RpsGame
import at.stnwtr.rps.packet.*
import at.stnwtr.rps.player.Player
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsHandler
import io.javalin.websocket.WsMessageContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class RpsWsHandler(val server: RpsServer) : Consumer<WsHandler> {

    private val executorService = Executors.newScheduledThreadPool(4)

    private val sessions = ConcurrentHashMap<WsContext, Player>()

    val games = mutableListOf<RpsGame>()

    private fun contextByUUID(uuid: String) = sessions.entries.firstOrNull { it.value.uuid == uuid }?.key!!

    override fun accept(handler: WsHandler) {
        handler.onMessage {
            when (PacketType.byType(JsonParser.parseString(it.message()).asJsonObject.get("type").asString)) {
                PacketType.LOGIN -> processLogin(it)
                PacketType.STATS -> processStats(it)
                PacketType.START_GAME -> processStartGame(it)
                PacketType.SELECTION -> processSelection(it)
            }
        }

        handler.onClose {
            sessions.remove(it)
        }

        handler.onError {
            sessions.remove(it)
        }
    }

    private fun processSelection(context: WsMessageContext) {
        games.firstOrNull { it.blue.first == context || it.red.first == context }?.receive(context)
    }

    private fun processStartGame(context: WsMessageContext) {
        val player = sessions[context] ?: return
        var opponent: Player? = null

        println("${player.name} + ${RpsGame.PLAYER_QUEUE}")
        println(sessions)

        if (RpsGame.PLAYER_QUEUE.size != 0) {
            opponent = RpsGame.PLAYER_QUEUE.poll()
        } else {
            RpsGame.PLAYER_QUEUE.add(player)
        }

        if (opponent != null) {
            val playerRank = server.playerDatabase.getPlayerRank(player)
            val opponentRank = server.playerDatabase.getPlayerRank(opponent)

            context.send(
                server.jsonMapper.writeValueAsString(
                    OutgoingStartGamePacket(playerRank, opponent.name, opponentRank)
                )
            )
            contextByUUID(opponent.uuid).send(
                server.jsonMapper.writeValueAsString(
                    OutgoingStartGamePacket(opponentRank, player.name, playerRank)
                )
            )

            games.add(RpsGame(this, server, context to player, contextByUUID(opponent.uuid) to opponent))

            player.isPlaying = true
            opponent.isPlaying = true
        } else {
            executorService.schedule({
                RpsGame.PLAYER_QUEUE.remove(player)
                if (player.isPlaying) {
                    return@schedule
                }

                val bot = Player(
                    UUID.randomUUID().toString(),
                    "Bot #" + (Math.random() * 10_000).toInt(),
                    Player.DEFAULT_WINS,
                    Player.DEFAULT_DEFEATS,
                    Player.DEFAULT_SCORE,
                    isBot = true,
                    isPlaying = false
                )

                context.send(
                    server.jsonMapper.writeValueAsString(
                        OutgoingStartGamePacket(
                            server.playerDatabase.getPlayerRank(player),
                            bot.name,
                            ThreadLocalRandom.current().nextInt(1_000)
                        )
                    )
                )

                games.add(RpsGame(this, server, context to player, null to bot))

                player.isPlaying = true
                bot.isPlaying = true
            }, RpsGame.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
    }

    private fun processStats(context: WsMessageContext) {
        val player = sessions[context] ?: return
        val response =
            OutgoingStatsPacket(player.wins, player.defeats, server.playerDatabase.getPlayerRank(player), player.score)
        context.send(server.jsonMapper.writeValueAsString(response))
    }

    private fun processLogin(context: WsMessageContext) {
        val packet = server.jsonMapper.readValue<IncomingLoginPacket>(context.message())

        val uuid = packet.uuid ?: UUID.randomUUID().toString()

        // TODO 28.09.2020: regex check
        try {
            UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            return
        }

        val player = sessions.computeIfAbsent(context) {
            server.playerDatabase.getPlayer(uuid)
        }
        player.name =
            if (Player.USERNAME_PATTERN.matcher(packet.username).matches()) packet.username else Player.DEFAULT_NAME

        val response = OutgoingLoginPacket(player.name, player.uuid)
        context.send(server.jsonMapper.writeValueAsString(response))

        println("${player.name} - ${RpsGame.PLAYER_QUEUE}")
    }
}