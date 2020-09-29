package at.stnwtr.rps

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

// TODO 27.09.2020: Close Connection / Statement / ResultSet instances
// TODO 28.09.2020: einer spielt lange nicht ... timeout
// TODO 28.09.2020: während game rausgehen ... anderer gewinnt

private fun configPairs(args: Array<String>) = try {
    Files.readAllLines(Path.of(args[0]))
        .map { it.split("=") }
        .filter { it.size == 2 }
        .map { Pair(it[0], it[1]) }
} catch (e: IOException) {
    emptyList()
} catch (e: ArrayIndexOutOfBoundsException) {
    emptyList()
}

private fun <T> List<Pair<String, String>>.configOption(path: String, transformer: String?.() -> T?, default: T) =
    transformer(this.firstOrNull { it.first == path }?.second) ?: default

fun main(args: Array<String>) {
    val entries = configPairs(args)

    val cors = entries.configOption("cors", String?::toBoolean, false)
    val host = entries.configOption("host", { this }, "0.0.0.0")
    val port = entries.configOption("port", { this?.toIntOrNull() }, 7435)

    RpsServer(RpsContext(cors, host, port))
}
