import hep.dataforge.meta.buildMeta
import kotlinx.coroutines.*
import kotlinx.html.currentTimeMillis
import scientifik.plotly.Plotly
import scientifik.plotly.models.Trace
import scientifik.plotly.server.serve
import java.io.File
import java.lang.Math.*
import java.lang.StringBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import kotlin.coroutines.*

class Attacker(private val uiContext: CoroutineContext = newFixedThreadPoolContext(1000, "many threads context")) : CoroutineScope {
    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = uiContext + job

    private val queries = listOf(
        "SELECT game.title, game_price.price\n" +
                "FROM game_price\n" +
                "INNER JOIN game ON game.id = game_price.id AND game_price.price_end_date IS NULL\n" +
                "WHERE game.title = ",
        "SELECT game.title\n" +
                "FROM owned_game\n" +
                "RIGHT JOIN machine_usage ON owned_game.id = machine_usage.owned_game_id AND in_use_from > current_date - interval '1 month'\n" +
                "INNER JOIN game ON owned_game.game_id = game.id\n" +
                "GROUP BY game.title HAVING SUM((date_part('hour', machine_usage.in_use_to) * 60 + date_part('minute', machine_usage.in_use_to) - date_part('hour', machine_usage.in_use_from) * 60 - date_part('minute', machine_usage.in_use_from))) / 60 > ",
        "SELECT COUNT(game.title), client_id\n" +
                "FROM owned_game\n" +
                "INNER JOIN game ON owned_game.game_id = game.id\n" +
                "WHERE client_id = ",
        "SELECT game.title, COUNT(client_id) as amount_sold\n" +
                "FROM owned_game\n" +
                "INNER JOIN game ON owned_game.game_id = game.id\n" +
                "WHERE game.title = ",
        "SELECT client.nickname, subscription_plan.name, client_subscription_plan.active_from, client_subscription_plan.active_to\n" +
                "FROM client_subscription_plan\n" +
                "INNER JOIN client ON client_id = client.id\n" +
                "INNER JOIN subscription_plan ON subscription_plan_id = subscription_plan.id\n" +
                "WHERE client.email = "
    )
    private val queryPlanNames =
        listOf(
            "game_prices_plan",
            "game_played_plan",
            "games_owned_by_client_plan",
            "game_owned_by_clients_plan",
            "client_sub_plans_plan"
        )
    val indexesNames = listOf(
        "idx_game_title",
        "idx_client_email",
        "idx_owned_game_ids",
        "idx_client_sub_plan_ids",
        "idx_machine_usage_owned_game_ids"
    )

    private val gameTitles = mutableListOf<String>()
    private val clientIds = mutableListOf<String>()
    private val clientEmails = mutableListOf<String>()

    var isPrepare = false
    var isIndex = false

    fun attackBd(conn: Connection, amountOfThreads: Int, amountOfQueries: Int): List<Deferred<Long>> {
        val results = mutableListOf<Deferred<Long>>()

        val statement = conn.createStatement()
        var resultSet = statement.executeQuery("SELECT title FROM game")
        while (resultSet.next()) {
            gameTitles.add(resultSet.getString("title"))
        }

        resultSet = statement.executeQuery("SELECT id, email FROM client")
        while (resultSet.next()) {
            clientIds.add(resultSet.getString("id"))
            clientEmails.add(resultSet.getString("email"))
        }

        if (isIndex) {
            statement.executeUpdate("CREATE INDEX ${indexesNames[0]} ON game(title)")
            statement.executeUpdate("CREATE INDEX ${indexesNames[1]} ON client(email)")
            statement.executeUpdate("CREATE INDEX ${indexesNames[2]} ON owned_game(game_id, client_id)")
            statement.executeUpdate("CREATE INDEX ${indexesNames[3]} ON client_subscription_plan(client_id, subscription_plan_id)")
            statement.executeUpdate("CREATE INDEX ${indexesNames[4]} ON machine_usage(owned_game_id)")
            isIndex = false
        }

        val connections = mutableListOf<Connection>()
        for (i in 1..amountOfThreads) {
            val username = "postgres"
            val password = "r177"
            lateinit var conn2: Connection
            val connectionProps = Properties()
            connectionProps["user"] = username
            connectionProps["password"] = password
            try {
                conn2 = DriverManager.getConnection(
                    "jdbc:" + "postgresql" + "://" +
                            "127.0.0.1:5432" + "/" +
                            "streaming_service",
                    connectionProps
                )
            } catch (ex: SQLException) {
                ex.printStackTrace()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            connections.add(conn2)
        }
        for (i in 1..amountOfThreads) {
            results.add(async(coroutineContext) {
                oneThreadAttack(amountOfQueries, connections[i - 1])
            })
        }

        return results
    }

    private fun oneThreadAttack(amountOfQueries: Int, conn: Connection): Long {
        val statement = conn.createStatement()
        if (isPrepare) {
            for (i in 0 until queries.size) {
                when (i) {
                    0, 4 -> statement.executeUpdate("PREPARE " + queryPlanNames[i] + "(text) AS\n" + queries[i] + "$1")
                    1 -> statement.executeUpdate("PREPARE " + queryPlanNames[i] + "(int) AS\n" + queries[i] + "$1")
                    2 -> statement.executeUpdate("PREPARE " + queryPlanNames[i] + "(int) AS\n" + queries[i] + "$1\nGROUP BY client_id")
                    else -> statement.executeUpdate("PREPARE " + queryPlanNames[i] + "(text) AS\n" + queries[i] + "$1\nGROUP BY game.title")
                }
            }
        }
        val timesList = mutableListOf<Long>()
        for (j in 1..amountOfQueries) {
            val query: String = if (!isPrepare) {
                when (j % queries.size) {
                    0 -> queries[j % queries.size] + "'" + gameTitles[(0 until gameTitles.size).random()] + "'"
                    1 -> queries[j % queries.size] + (0..100).random().toString()
                    2 -> queries[j % queries.size] + clientIds[(0 until clientIds.size).random()] + "\nGROUP BY client_id"
                    3 -> queries[j % queries.size] + "'" + gameTitles[(0 until gameTitles.size).random()] + "'\nGROUP BY game.title"
                    else -> queries[j % queries.size] + "'" + clientEmails[(0 until clientEmails.size).random()] + "'"
                }
            } else {
                "EXECUTE " + when (j % queries.size) {
                    0, 3 -> queryPlanNames[j % queries.size] + "('" + gameTitles[(0 until gameTitles.size).random()] + "')"
                    1 -> queryPlanNames[j % queries.size] + "(" + (0..100).random().toString() + ")"
                    else -> queryPlanNames[j % queries.size] + "(" + clientIds[(0 until clientIds.size).random()] + ")"
                }
            }
            val start = currentTimeMillis()
            statement.executeQuery(query)
            timesList.add(currentTimeMillis() - start)
        }
        conn.close()
        return timesList.average().toLong()
    }
}

suspend fun main() {
    val username = "postgres"
    val password = "r177"
    lateinit var conn: Connection
    val connectionProps = Properties()
    connectionProps["user"] = username
    connectionProps["password"] = password
    try {
        conn = DriverManager.getConnection(
            "jdbc:" + "postgresql" + "://" +
                    "127.0.0.1:5432" + "/" +
                    "streaming_service",
            connectionProps
        )
    } catch (ex: SQLException) {
        ex.printStackTrace()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }

    val serverMeta = buildMeta {
        "update" to {
            "enabled" to true
        }
    }
    val attacker = Attacker()

    val amountOfQueries = 300
    val server = Plotly.serve(serverMeta) {
        val xThreads = (1..20).map { it * 2 }
        val xThreadsSquare = (1..7).map { it * it }
        val xThreadsExp = (1..7).map { exp(it - 1.0).toInt() }
        val yThreads = xThreads.map { 0 }
        val yThreadsSquare = xThreadsSquare.map { 0 }
        val yThreadsExp = xThreadsExp.map { 0 }

        val traceAmountOfThreadsExp = Trace.build(x = xThreadsExp, y = yThreadsExp) { name = "Thread graph" }

        plot(1, 12) {
            trace(traceAmountOfThreadsExp)
            layout {
                title = "Экспоненциальный рост"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Длительность выполнения запроса(мс)" }
            }
        }

        val traceAmountOfThreadsSquare = Trace.build(x = xThreadsSquare, y = yThreadsSquare) { name = "Thread graph" }

        plot(2, 12) {
            trace(traceAmountOfThreadsSquare)
            layout {
                title = "Квадратичный рост"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Длительность выполнения запроса(мс)" }
            }
        }

        val traceAmountOfThreads = Trace.build(x = xThreads, y = yThreads) { name = "Thread graph" }

        plot(3, 12) {
            trace(traceAmountOfThreads)
            layout {
                title = "Линейный рост"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Длительность выполнения запроса(мс)" }
            }
        }


        val traceAmountOfThreadsIndex = Trace.build(x = xThreads, y = yThreads) { name = "Thread graph" }

        plot(4, 6) {
            trace(traceAmountOfThreadsIndex)
            layout {
                title =
                    "With CREATE INDEX"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Длительность выполнения запроса(мс)" }
            }
        }

        val traceAmountOfThreadsIndexDiff = Trace.build(x = xThreads, y = yThreads) { name = "Thread graph" }

        plot(4, 6) {
            trace(traceAmountOfThreadsIndexDiff)
            layout {
                title = "Normal - optimized"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Времени сохранено(мс)" }
            }
        }

        val traceAmountOfThreadsPrepared = Trace.build(x = xThreads, y = yThreads) { name = "Thread graph" }

        plot(5, 6) {
            trace(traceAmountOfThreadsPrepared)
            layout {
                title =
                    "With PREPARE"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Длительность выполнения запроса(мс)" }
            }
        }

        val traceAmountOfThreadsPreparedDiff = Trace.build(x = xThreads, y = yThreads) { name = "Thread graph" }

        plot(5, 6) {
            trace(traceAmountOfThreadsPreparedDiff)
            layout {
                title = "Normal - oprimized"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Времени сохранено(мс)" }
            }
        }

        val traceAmountOfThreadsBoth = Trace.build(x = xThreads, y = yThreads) { name = "Thread graph" }

        plot(6, 6) {
            trace(traceAmountOfThreadsBoth)
            layout {
                title =
                    "With PREPARE and CREATE INDEX"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Длительность выполнения запроса(мс)" }
            }
        }

        val traceAmountOfThreadsBothDiff = Trace.build(x = xThreads, y = yThreads) { name = "Thread graph" }

        plot(6, 6) {
            trace(traceAmountOfThreadsBothDiff)
            layout {
                title = "Normal - optimized"
                xaxis { title = "Количество потоков(шт)" }
                yaxis { title = "Времени сохранено(мс)" }
            }
        }

        launch {
            val statement = conn.createStatement()
            for (indexName in attacker.indexesNames) {
                statement.executeUpdate("DROP INDEX IF EXISTS $indexName")
            }
            //Графики без оптимизации
            //Экспоненциальный рост
            var amountOfThreads = 1
            var dynamicY = Array(xThreadsExp.size) { _ -> 0.0 }

            while (amountOfThreads <= xThreadsExp.size) {
                val listOfTimes =
                    attacker.attackBd(conn, exp(amountOfThreads - 1.0).toInt(), amountOfQueries).awaitAll()
                dynamicY[amountOfThreads - 1] =
                    BigDecimal(listOfTimes.average()).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                val expMap = mutableMapOf<Int, Double>()
                for (i in 0 until xThreadsExp.size) {
                    expMap[xThreadsExp[i]] = dynamicY[i]
                }
                traceAmountOfThreadsExp.y = xThreadsExp.map { i -> expMap[i] }
                amountOfThreads++
            }
            //Квадратичный рост
            amountOfThreads = 1
            dynamicY = Array(xThreadsSquare.size) { _ -> 0.0 }

            while (amountOfThreads <= xThreadsSquare.size) {
                val listOfTimes = attacker.attackBd(conn, amountOfThreads * amountOfThreads, amountOfQueries).awaitAll()
                dynamicY[amountOfThreads - 1] =
                    BigDecimal(listOfTimes.average()).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                traceAmountOfThreadsSquare.y = xThreadsSquare.map { i -> dynamicY[sqrt(i.toDouble()).toInt() - 1] }
                amountOfThreads++
            }

            //Линейный рост
            amountOfThreads = 1
            dynamicY = Array(20) { _ -> 0.0 }

            while (amountOfThreads <= 20) {
                val listOfTimes = attacker.attackBd(conn, amountOfThreads * 2, amountOfQueries).awaitAll()
                dynamicY[amountOfThreads - 1] =
                    BigDecimal(listOfTimes.average()).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                traceAmountOfThreads.y = xThreads.map { i -> dynamicY[i / 2 - 1] }
                amountOfThreads++
            }
            //Запись значений в файл
            var resultsStr = StringBuilder("")
            for (dynamicYelement in dynamicY) {
                resultsStr.append("$dynamicYelement\n")

            }
            File("linear.txt").writeText(resultsStr.toString())

            //Оптимизация CREATE INDEX
            attacker.isIndex = true

            val normalValues = dynamicY
            amountOfThreads = 1
            dynamicY = Array(20) { _ -> 0.0 }

            while (amountOfThreads <= 20) {
                val listOfTimes = attacker.attackBd(conn, amountOfThreads * 2, amountOfQueries).awaitAll()
                dynamicY[amountOfThreads - 1] =
                    BigDecimal(listOfTimes.average()).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                traceAmountOfThreadsIndex.y = xThreads.map { i -> dynamicY[i / 2 - 1] }
                traceAmountOfThreadsIndexDiff.y = xThreads.map { i -> normalValues[i / 2 - 1] - dynamicY[i / 2 - 1] }
                amountOfThreads++
            }
            //Запись значений в файл
            resultsStr = StringBuilder("")
            for (dynamicYelement in dynamicY) {
                resultsStr.append("$dynamicYelement\n")

            }
            File("index.txt").writeText(resultsStr.toString())

            for (indexName in attacker.indexesNames) {
                statement.executeUpdate("DROP INDEX IF EXISTS $indexName")
            }

            //Оптимизация PREPARE
            attacker.isPrepare = true

            amountOfThreads = 1
            dynamicY = Array(20) { _ -> 0.0 }

            while (amountOfThreads <= 20) {
                val listOfTimes = attacker.attackBd(conn, amountOfThreads * 2, amountOfQueries).awaitAll()
                dynamicY[amountOfThreads - 1] =
                    BigDecimal(listOfTimes.average()).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                traceAmountOfThreadsPrepared.y = xThreads.map { i -> dynamicY[i / 2 - 1] }
                traceAmountOfThreadsPreparedDiff.y = xThreads.map { i -> normalValues[i / 2 - 1] - dynamicY[i / 2 - 1] }
                amountOfThreads++
            }
            //Запись значений в файл
            resultsStr = StringBuilder("")
            for (dynamicYelement in dynamicY) {
                resultsStr.append("$dynamicYelement\n")

            }
            File("prepare.txt").writeText(resultsStr.toString())

            statement.executeUpdate("DEALLOCATE ALL")
            attacker.isPrepare = false

            //Оптимизация и INDEX и PREPARE
            attacker.isPrepare = true
            attacker.isIndex = true

            amountOfThreads = 1
            dynamicY = Array(20) { _ -> 0.0 }

            while (amountOfThreads <= 20) {
                val listOfTimes = attacker.attackBd(conn, amountOfThreads * 2, amountOfQueries).awaitAll()
                dynamicY[amountOfThreads - 1] =
                    BigDecimal(listOfTimes.average()).setScale(2, RoundingMode.HALF_EVEN).toDouble()
                traceAmountOfThreadsBoth.y = xThreads.map { i -> dynamicY[i / 2 - 1] }
                traceAmountOfThreadsBothDiff.y = xThreads.map { i -> normalValues[i / 2 - 1] - dynamicY[i / 2 - 1] }
                amountOfThreads++
            }
            //Запись значений в файл
            resultsStr = StringBuilder("")
            for (dynamicYelement in dynamicY) {
                resultsStr.append("$dynamicYelement\n")

            }
            File("both.txt").writeText(resultsStr.toString())

            statement.executeUpdate("DEALLOCATE ALL")
            attacker.isPrepare = false

            for (indexName in attacker.indexesNames) {
                statement.executeUpdate("DROP INDEX IF EXISTS $indexName")
            }
        }

    }

    System.out.println("Press enter to stop server")
    readLine()
    conn.close()

    server.stop()

}