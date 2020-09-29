package at.stnwtr.rps.player

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class PlayerDatabase {

    companion object {
        const val TABLE_STRING =
            "create table if not exists `players` (`uuid` varchar(36) not null primary key," +
                    "`wins` int not null default 0,`defeats` int not null default 0, `score` int not null default 0);"

        init {
            Class.forName("org.sqlite.JDBC")
        }
    }

    private lateinit var connection: Connection

    fun open() {
        if (!this::connection.isInitialized) {
            connection = DriverManager.getConnection("jdbc:sqlite:players.db")
            createTable()
        }
    }

    fun close() {
        connection.close()
    }

    private fun createTable() {
        connection.prepareStatement(TABLE_STRING).executeUpdate()
    }

    fun insertPlayer(player: Player) {
        try {
            val statement =
                connection.prepareStatement("insert into `players` (`uuid`, `wins`, `defeats`, `score`) values (?, ?, ?, ?);")
            statement.setString(1, player.uuid)
            statement.setInt(2, player.wins)
            statement.setInt(3, player.defeats)
            statement.setInt(4, player.score)
            statement.executeUpdate()
        } catch (e: SQLException) {
            println("Could not write stats for player $player")
            e.printStackTrace()
        }
    }

    fun getPlayer(uuid: String): Player {
        return try {
            val statement = connection.prepareStatement("select * from `players` where `uuid` = ?;")
            statement.setString(1, uuid)

            val result = statement.executeQuery()
            result.next()

            Player(
                result.getString("uuid"),
                wins = result.getInt("wins"),
                defeats = result.getInt("defeats"),
                score = result.getInt("score")
            )
        } catch (e: SQLException) {
            insertPlayer(
                Player(
                    uuid,
                    Player.DEFAULT_NAME,
                    Player.DEFAULT_WINS,
                    Player.DEFAULT_DEFEATS,
                    Player.DEFAULT_SCORE
                )
            )
            getPlayer(uuid)
        }
    }

    fun updatePlayer(player: Player) {
        try {
            val statement =
                connection.prepareStatement("update `players` set `wins`=?, `defeats`=?, `score`=? where `uuid`=?;")
            println("inserting .... $player")
            statement.setInt(1, player.wins)
            statement.setInt(2, player.defeats)
            statement.setInt(3, if (player.score < 0) 0 else player.score)
            statement.setString(4, player.uuid)
            statement.executeUpdate()
        } catch (e: SQLException) {
            println("Could not update stats for player $player")
            e.printStackTrace()
        }
    }

    fun getPlayerRank(player: Player): Int {
        return try {
            val statement = connection.prepareStatement("select count(*) from `players` where `score` > ?")
            statement.setInt(1, player.score)

            val result = statement.executeQuery()
            result.getInt("count(*)") + 1
        } catch (e: SQLException) {
            Player.DEFAULT_RANK
        }
    }

    fun getAll(): List<Player> {
        val players = mutableListOf<Player>()
        try {
            val statement = connection.prepareStatement("select * from `players`")
            val result = statement.executeQuery()

            while (result.next()) {
                players.add(
                    Player(
                        result.getString("uuid"),
                        Player.DEFAULT_NAME,
                        result.getInt("wins"),
                        result.getInt("defeats"),
                        result.getInt("score")
                    )
                )
            }
        } catch (e: SQLException) {
            println("Could not get all")
        }
        return players
    }
}