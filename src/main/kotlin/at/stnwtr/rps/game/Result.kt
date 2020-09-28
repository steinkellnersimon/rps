package at.stnwtr.rps.game

enum class Result(val type: String) {
    WIN("win"),
    DEFEAT("defeat"),
    DRAW("draw");

    companion object {
        fun byType(type: String) = values().firstOrNull { it.type == type }

        fun byBoolean(win: Boolean) = if (win) WIN else DEFEAT
    }
}