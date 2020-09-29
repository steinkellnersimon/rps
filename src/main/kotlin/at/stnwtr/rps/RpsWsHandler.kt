package at.stnwtr.rps

import at.stnwtr.rps.game.RpsGame
import at.stnwtr.rps.packet.*
import at.stnwtr.rps.player.Player
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.JsonParser
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsHandler
import io.javalin.websocket.WsMessageContext
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer

class RpsWsHandler(val server: RpsServer) : Consumer<WsHandler> {

    private val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(8)

    val jsonMapper = jacksonObjectMapper()

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

        if (RpsGame.PLAYER_QUEUE.size != 0 && !RpsGame.PLAYER_QUEUE.contains(player)) {
            opponent = RpsGame.PLAYER_QUEUE.poll()
        } else {
            RpsGame.PLAYER_QUEUE.add(player)
        }

        if (opponent != null) {
            val playerRank = server.playerDatabase.getPlayerRank(player)
            val opponentRank = server.playerDatabase.getPlayerRank(opponent)

            context.send(
                jsonMapper.writeValueAsString(
                    OutgoingStartGamePacket(playerRank, opponent.name, opponentRank)
                )
            )
            contextByUUID(opponent.uuid).send(
                jsonMapper.writeValueAsString(
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
                    jsonMapper.writeValueAsString(
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
    }

    private fun processStats(context: WsMessageContext) {
        val player = sessions[context] ?: return
        val response =
            OutgoingStatsPacket(player.wins, player.defeats, server.playerDatabase.getPlayerRank(player), player.score)
        context.send(jsonMapper.writeValueAsString(response))
    }

    private fun processLogin(context: WsMessageContext) {
        val packet = jsonMapper.readValue<IncomingLoginPacket>(context.message())

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
        context.send(jsonMapper.writeValueAsString(response))
    }
}