package at.stnwtr.rps

import at.stnwtr.rps.player.PlayerDatabase
import io.javalin.Javalin

data class RpsContext(val cors: Boolean, val host: String, val port: Int)

class RpsServer(private val context: RpsContext) {

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