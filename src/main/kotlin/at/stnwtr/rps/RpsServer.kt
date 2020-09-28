package at.stnwtr.rps

import at.stnwtr.rps.player.PlayerDatabase
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class RpsContext(val cors: Boolean, val host: String, val port: Int)

class RpsServer(private val context: RpsContext) {

    val jsonMapper = jacksonObjectMapper()

    val playerDatabase = PlayerDatabase()

    private val app = Javalin.create {
        it.addStaticFiles("/www")
        if (context.cors) {
            it.enableCorsForAllOrigins()
        }
    }

    init {
        start()
        Runtime.getRuntime().addShutdownHook(Thread(::stop))
    }

    private fun start() {
        playerDatabase.open()
        app.start(context.host, context.port)

        app.ws("/ws", RpsWsHandler(this))
    }

    private fun stop() {
        app.stop()
        playerDatabase.close()
    }
}