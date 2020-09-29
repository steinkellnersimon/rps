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
import java.util.concurrent.*
import java.util.function.Consumer

class RpsWsHandler(val server: RpsServer) : Consumer<WsHandler> {

    private val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(16)

    private val sessions = ConcurrentHashMap<WsContext, Player>()

    val games: MutableList<RpsGame> = Collections.synchronizedList(mutableListOf())!!

    private fun contextByUUID(uuid: String) = sessions.entries.firstOrNull { it.value.uuid == uuid }?.key!!

    private fun stopGame(context: WsContext) {
        sessions[context]?.isPlaying = false
        games.firstOrNull { it.blue.first == context || it.red.first == context }?.stop()
    }

    override fun accept(handler: WsHandler) {
        handler.onMessage {
            try {
                val result = JsonParser.parseString(it.message()).asJsonObject?.get("type")?.asString ?: return@onMessage
                when (PacketType.byType(result)) {
                    PacketType.LOGIN -> processLogin(it)
                    PacketType.STATS -> processStats(it)
                    PacketType.START_GAME -> processStartGame(it)
                    PacketType.SELECTION -> processSelection(it)
                }
            } catch (ignored: IllegalStateException) {
            }
        }

        handler.onClose {
            stopGame(it)
            sessions.remove(it)
            println("${it.status()} - ${it.reason()}")
        }

        handler.onError {
            stopGame(it)
            sessions.remove(it)
            it.error()?.printStackTrace()
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
            }, RpsGame.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
        println("[start-game] $player")
    }

    private fun processStats(context: WsMessageContext) {
        val player = sessions[context] ?: return
        val response =
            OutgoingStatsPacket(player.wins, player.defeats, server.playerDatabase.getPlayerRank(player), player.score)
        context.send(server.jsonMapper.writeValueAsString(response))
        println("[stats] $player")
    }

    private fun processLogin(context: WsMessageContext) {
        val packet = server.jsonMapper.readValue<IncomingLoginPacket>(context.message())

        val uuid = packet.uuid ?: UUID.randomUUID().toString()

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
        println("[login] $player")
    }
}