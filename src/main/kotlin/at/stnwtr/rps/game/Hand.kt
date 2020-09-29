package at.stnwtr.rps.game

enum class Hand(val type: String) {
    ROCK("rock"),
    PAPER("paper"),
    SCISSORS("scissors");

    fun beats(other: Hand) = when (this) {
        ROCK -> other == SCISSORS
        PAPER -> other == ROCK
        SCISSORS -> other == PAPER
    }

    companion object {
        fun byType(type: String) = values().firstOrNull { it.type == type }
    }
}