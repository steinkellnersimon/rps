package at.stnwtr.rps.player

import java.sql.*

class PlayerDatabase {

    companion object {
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
        var statement: PreparedStatement? = null
        try {
            statement = connection.prepareStatement(
                "create table if not exists `players` (`uuid` varchar(36) not null primary key," +
                        "`wins` int not null default 0,`defeats` int not null default 0, `score` int not null default 0);"
            )
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            statement?.close()
        }
    }

    private fun insertPlayer(player: Player) {
        var statement: PreparedStatement? = null
        try {
            statement =
                connection.prepareStatement("insert into `players` (`uuid`, `wins`, `defeats`, `score`) values (?, ?, ?, ?);")
            statement.setString(1, player.uuid)
            statement.setInt(2, player.wins)
            statement.setInt(3, player.defeats)
            statement.setInt(4, player.score)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            statement?.close()
        }
    }

    fun getPlayer(uuid: String): Player {
        var statement: PreparedStatement? = null
        var result: ResultSet? = null
        return try {
            statement = connection.prepareStatement("select * from `players` where `uuid` = ?;")
            statement.setString(1, uuid)

            result = statement.executeQuery()
            result.next()

            Player(
                result.getString("uuid"),
                wins = result.getInt("wins"),
                defeats = result.getInt("defeats"),
                score = result.getInt("score")
            )
        } catch (e: SQLException) {
            e.printStackTrace()
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
        } finally {
            result?.close()
            statement?.close()
        }
    }

    fun updatePlayer(player: Player) {
        var statement: PreparedStatement? = null
        try {
            statement =
                connection.prepareStatement("update `players` set `wins`=?, `defeats`=?, `score`=? where `uuid`=?;")
            statement.setInt(1, player.wins)
            statement.setInt(2, player.defeats)
            statement.setInt(3, if (player.score < 0) 0 else player.score)
            statement.setString(4, player.uuid)
            statement.executeUpdate()
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            statement?.close()
        }
    }

    fun getPlayerRank(player: Player): Int {
        var statement: PreparedStatement? = null
        var result: ResultSet? = null

        return try {
            statement = connection.prepareStatement("select count(*) from `players` where `score` > ?")
            statement.setInt(1, player.score)

            result = statement.executeQuery()
            result.getInt("count(*)") + 1
        } catch (e: SQLException) {
            e.printStackTrace()
            Player.DEFAULT_RANK
        } finally {
            result?.close()
            statement?.clearParameters()
        }
    }

    fun getAll(): List<Player> {
        var statement: PreparedStatement? = null
        var result: ResultSet? = null

        val players = mutableListOf<Player>()

        try {
            statement = connection.prepareStatement("select * from `players`")
            result = statement.executeQuery()

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
            e.printStackTrace()
        } finally {
            result?.close()
            statement?.close()
        }
        return players
    }
}