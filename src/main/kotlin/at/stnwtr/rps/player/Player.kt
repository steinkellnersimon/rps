package at.stnwtr.rps.player

import java.util.regex.Pattern

data class Player(
    val uuid: String,
    var name: String = DEFAULT_NAME,
    var wins: Int = DEFAULT_WINS,
    var defeats: Int = DEFAULT_DEFEATS,
    var score: Int = DEFAULT_SCORE,
    val isBot: Boolean = false,
    var isPlaying: Boolean = false
) {
    companion object {
        val USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9öäüÖÄÜßẞ_\\s]{1,32}$")!!
        const val DEFAULT_NAME = "just no"
        const val DEFAULT_WINS = 0
        const val DEFAULT_DEFEATS = 0
        const val DEFAULT_SCORE = 1_000
        const val DEFAULT_RANK = 0
    }
}