import java.io.FileInputStream
import java.lang.StringBuilder
import java.sql.*
import java.util.*
import java.sql.SQLException
import java.time.LocalDate

fun main() {
    val properties = Properties()
    val propertiesFile = System.getProperty("user.dir") + "\\file.properties"
    val inputStream = FileInputStream(propertiesFile)
    properties.load(inputStream)
    val username = properties.getProperty("username")
    val password = properties.getProperty("password")
    lateinit var conn: Connection
    val connectionProps = Properties()
    connectionProps["user"] = username
    connectionProps["password"] = password
    try {
        Class.forName("org.postgresql.Driver")
        conn = DriverManager.getConnection(
            "jdbc:" + "postgresql" + "://" +
                    properties.getProperty("database_address") + "/" +
                    properties.getProperty("database_name"),
            connectionProps
        )
    } catch (ex: SQLException) {
        ex.printStackTrace()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }

    val start = System.currentTimeMillis()
    //Добавление клиентов, количество - параметр программы
    createClientEntry(conn, properties.getProperty("amount_of_clients").toInt())

    //Выборка id всех клиентов
    val clientIds = mutableListOf<String>()
    val statement = conn.createStatement()
    var resultSet = statement.executeQuery("SELECT id from client")
    while (resultSet.next()) {
        clientIds.add(resultSet.getString("id"))
    }

    //Добавление игр, количество - параметр программы
    createGameEntry(conn, properties.getProperty("amount_of_games").toInt())

    //Выборка id всех игр в таблице
    val gameIds = mutableListOf<String>()
    resultSet = statement.executeQuery("SELECT id from game")
    while (resultSet.next()) {
        gameIds.add(resultSet.getString("id"))
    }

    //Добавление компьютеров, количество - параметр программы
    createMachineEntry(conn, properties.getProperty("amount_of_machines").toInt())

    //Выборка id и power_tier всех компьютеров
    val machineIds = mutableListOf<String>()
    resultSet = statement.executeQuery("SELECT id, power_tier from machine")
    while (resultSet.next()) {
        machineIds.add(resultSet.getString("id"))
    }

    //Добавление жанров, справочная таблица, количество фиксированное
    createGenreEntry(conn)

    //Выборка id всех жанров
    val genreIds = mutableListOf<String>()
    resultSet = statement.executeQuery("SELECT id from genre")
    while (resultSet.next()) {
        genreIds.add(resultSet.getString("id"))
    }

    //Добавление приобретенных игр, количество - параметр программы
    createOwnedGameEntry(conn, properties.getProperty("amount_of_owned_games").toInt(), gameIds, clientIds)

    //Выборка игр без жанров
    val gameWithoutGenreIds = mutableListOf<String>()
    resultSet = statement.executeQuery("SELECT id from game WHERE id NOT IN (SELECT game_id from game_genre)")
    while (resultSet.next()) {
        gameWithoutGenreIds.add(resultSet.getString("id"))
    }

    //Добавления жанров к играм, параметра нет, так как жанры приписываются к каждой игре, у которой их еще нет
    createGameGenreEntries(conn, genreIds, gameWithoutGenreIds)

    //Добавление подписочных планов, справочная таблица, количество фиксированное
    createSubscriptionPlanEntry(conn)

    //Выборка компьютеров, не добавленных в подписочнйый план и их power_tier
    val machineWithoutPlanIds = mutableListOf<String>()
    val machinePowerTiers = mutableListOf<String>()
    resultSet =
        statement.executeQuery("SELECT id, power_tier from machine WHERE id NOT IN (SELECT machine_id from available_machine_tier)")
    while (resultSet.next()) {
        machineWithoutPlanIds.add(resultSet.getString("id"))
        machinePowerTiers.add(resultSet.getString("power_tier"))
    }
    //Добавление доступных компьютеров, параметра нет, так как добавляются все компьютеры, которые еще не доабвлены
    createAvailableMachinesEntry(conn, machineWithoutPlanIds, machinePowerTiers)

    //Добавление установленных игр, количество - параметр программы
    createInstalledGameEntries(conn, properties.getProperty("amount_of_installed_games").toInt(), gameIds, machineIds)

    //Добавление планов подписки пользователям, количество - параметр программы
    createClientSubscriptionEntry(
        conn,
        properties.getProperty("amount_of_client_subscription_plans").toInt(),
        clientIds
    )

    //Выборка подписочных планов пользователей
    val clientSubPlans = mutableMapOf<String, MutableList<Triple<String, String, String>>>()
    resultSet =
        statement.executeQuery("SELECT subscription_plan_id, client_id, active_from, active_to from client_subscription_plan")
    while (resultSet.next()) {
        if (clientSubPlans[resultSet.getString("client_id")] == null) {
            clientSubPlans[resultSet.getString("client_id")] = mutableListOf(
                Triple(
                    resultSet.getString("subscription_plan_id"),
                    resultSet.getString("active_from"),
                    resultSet.getString("active_to")
                )
            )
        } else {
            clientSubPlans[resultSet.getString("client_id")]!!.add(
                Triple(
                    resultSet.getString("subscription_plan_id"),
                    resultSet.getString("active_from"),
                    resultSet.getString("active_to")
                )
            )
        }
    }


    //Выборка игр без цены
    val gameWithoutPriceIds = mutableListOf<String>()
    resultSet = statement.executeQuery("SELECT id from game WHERE id NOT IN (SELECT game_id from game_price)")
    while (resultSet.next()) {
        gameWithoutPriceIds.add(resultSet.getString("id"))
    }

    //Добавление цен на игры, количество - параметр программы
    createGamePriceEntries(conn, gameWithoutPriceIds)

    //Выборка доступности машин
    val machineAvailability = mutableMapOf<String, String>()
    resultSet = statement.executeQuery("SELECT machine_id, subscription_plan_id  from available_machine_tier")
    while (resultSet.next()) {
        machineAvailability[resultSet.getString("machine_id")] = resultSet.getString("subscription_plan_id")
    }

    //Добавление использования компьютеров, количество - параметр программы
    createMachineUsageEntry(
        conn,
        properties.getProperty("amount_of_machine_usages").toInt(),
        machineIds,
        machineAvailability,
        clientSubPlans
    )
    System.out.println("Generation took: " + (System.currentTimeMillis() - start) + " miliseconds")
}

fun createClientEntry(conn: Connection, amount: Int) {
    var amountLeft = amount
    val statement = conn.createStatement()
    val resultSet = statement.executeQuery("SELECT email from client")
    val clientEmails = mutableListOf<String>()
    while (resultSet.next()) {
        clientEmails.add(resultSet.getString("email"))
    }

    val insertTableSQL = StringBuilder(
        "INSERT INTO client"
                + "(nickname, hash, email) " + "VALUES "
    )


    while (amountLeft != 0) {
        val nicknameLen = (1..29).random()
        var emailNameLen = (1..24).random()
        var emailDomenLen = (1..(25 - emailNameLen)).random()

        val nickname = generateRandomString(nicknameLen)
        var email = generateRandomString(emailNameLen) + "@" + generateRandomString(emailDomenLen) + ".com"

        val hash = (1..Int.MAX_VALUE).random()


        while (clientEmails.contains(email)) {
            emailNameLen = (1..24).random()
            emailDomenLen = (1..25 - emailNameLen).random()
            email = generateRandomString(emailNameLen) + "@" + generateRandomString(emailDomenLen) + ".com"
        }
        if (amountLeft == amount) {
            insertTableSQL.append("('$nickname', $hash, '$email')")
        } else {
            insertTableSQL.append(", ('$nickname', $hash, '$email')")
        }
        clientEmails.add(email)
        amountLeft--
    }
    statement.executeUpdate(insertTableSQL.toString())
}

fun createGameEntry(conn: Connection, amount: Int) {
    var amountLeft = amount

    val statement = conn.createStatement()
    val resultSet = statement.executeQuery("SELECT title from game")
    val gameTitles = mutableListOf<String>()
    while (resultSet.next()) {
        gameTitles.add(resultSet.getString("title"))
    }

    val insertTableSQL = StringBuilder(
        "INSERT INTO game"
                + "(title) " + "VALUES "
    )
    while (amountLeft != 0) {
        var gameNameLen = (1..30).random()
        var gameTitle = generateRandomString(gameNameLen)

        while (gameTitles.contains(gameTitle)) {
            gameNameLen = (1..30).random()
            gameTitle = generateRandomString(gameNameLen)
        }
        if (amountLeft == amount) {
            insertTableSQL.append("('$gameTitle')")
        } else {
            insertTableSQL.append(", ('$gameTitle')")
        }
        gameTitles.add(gameTitle)
        amountLeft--
    }
    statement.executeUpdate(insertTableSQL.toString())
}

fun createMachineEntry(conn: Connection, amount: Int) {
    var amountLeft = amount

    val insertTableSQL = StringBuilder(
        "INSERT INTO machine"
                + "(power_tier) " + "VALUES "
    )
    val statement = conn.createStatement()

    while (amountLeft != 0) {
        val powerTierProbability = (1..10).random()

        val powerTier = if (powerTierProbability >= 6) 3 else if (powerTierProbability >= 3) 2 else 1

        if (amountLeft == amount) {
            insertTableSQL.append("('$powerTier')")
        } else {
            insertTableSQL.append(", ('$powerTier')")
        }
        amountLeft--
    }
    statement.executeUpdate(insertTableSQL.toString())
}

fun createGenreEntry(conn: Connection) {
    val genres = listOf(
        "Platformer",
        "FPS",
        "TPS",
        "Fighting",
        "Beat em up",
        "Survival",
        "Stealth",
        "Rhythm",
        "Horror",
        "Adventure",
        "Visual Novel",
        "RPG",
        "Tactic",
        "JRPG",
        "Simulator",
        "RTS",
        "Multiplayer",
        "TTS",
        "Arcade",
        "Sports",
        "Racing",
        "Puzzle"
    )

    val statement = conn.createStatement()
    var isAnythingAdded = false

    val resultSet = statement.executeQuery("SELECT name from genre")
    val genreInDB = mutableListOf<String>()

    while (resultSet.next()) {
        genreInDB.add(resultSet.getString("name"))
    }
    val insertTableSQL = StringBuilder(
        "INSERT INTO genre"
                + "(name) " + "VALUES "
    )
    for (genre in genres) {
        if (!genreInDB.contains(genre)) {
            if (isAnythingAdded) {
                insertTableSQL.append("($genre)")
                isAnythingAdded = true
            } else {
                insertTableSQL.append(", ($genre)")
            }
        }
    }
    if (isAnythingAdded) {
        statement.executeUpdate(insertTableSQL.toString())
    }
}

fun createOwnedGameEntry(conn: Connection, amount: Int, gameIds: List<String>, clientIds: List<String>) {
    val statement = conn.createStatement()
    val resultSet = statement.executeQuery(
        "SELECT game_id, client_id from owned_game"
    )
    val ownedGamesByClients = mutableListOf<Pair<String, String>>()

    while (resultSet.next()) {
        ownedGamesByClients.add(Pair(resultSet.getString("client_id"), resultSet.getString("game_id")))
    }
    var amountLeft = amount

    val insertTableSQL = StringBuilder(
        "INSERT INTO owned_game"
                + "(client_id, game_id, purchase_date) " + "VALUES "
    )


    while (amountLeft != 0) {
        val clientIndex = (0 until clientIds.size).random()
        val gameIndex = (0 until gameIds.size).random()


        if (!ownedGamesByClients.contains(Pair(clientIds[clientIndex], gameIds[gameIndex]))) {
            if (amount == amountLeft) {
                insertTableSQL.append(
                    "(${clientIds[clientIndex]}, ${gameIds[gameIndex]}, '${generateRandomDate(
                        "2010-01-01",
                        "2019-12-31"
                    )}')"
                )
            } else {
                insertTableSQL.append(
                    ", (${clientIds[clientIndex]}, ${gameIds[gameIndex]}, '${generateRandomDate(
                        "2010-01-01",
                        "2019-12-31"
                    )}')"
                )
            }
            ownedGamesByClients.add(Pair(clientIds[clientIndex], gameIds[gameIndex]))
            amountLeft--;
        }
    }
    statement.executeUpdate(insertTableSQL.toString())
}

fun createGameGenreEntries(conn: Connection, genreIds: List<String>, gameIds: List<String>) {
    val statement = conn.createStatement()

    val insertTableSQL = StringBuilder(
        "INSERT INTO game_genre"
                + "(game_id, genre_id) " + "VALUES "
    )

    for (gameId in gameIds) {
        val amountOfGenres = (1..4).random()

        val selectedGenreIds = (0 until genreIds.size).shuffled().take(amountOfGenres)

        for (selectedGenreId in selectedGenreIds) {
            if (gameId == gameIds.first() && selectedGenreId == selectedGenreIds.first()) {
                insertTableSQL.append("($gameId, ${genreIds[selectedGenreId]})")
            } else {
                insertTableSQL.append(", ($gameId, ${genreIds[selectedGenreId]})")
            }
        }
    }

    statement.executeUpdate(insertTableSQL.toString())
}

fun createSubscriptionPlanEntry(conn: Connection) {
    val planNames = listOf("low-tier plan", "middle-tier plan", "high-tier plan")
    val planPrices = listOf(400, 800, 1200)

    val statement = conn.createStatement()

    for (i in 0..2) {
        val resultSet = statement.executeQuery("SELECT id from subscription_plan WHERE (name = '${planNames[i]}')")
        if (!resultSet.next()) {
            val insertTableSQL = ("INSERT INTO subscription_plan"
                    + "(name, price) " + "VALUES "
                    + "('${planNames[i]}', ${planPrices[i]})")
            statement.executeUpdate(insertTableSQL)
        }
    }
}

fun createAvailableMachinesEntry(conn: Connection, machineIds: List<String>, machinePowerTiers: List<String>) {
    val statement = conn.createStatement()
    val insertTableSQL = StringBuilder(
        "INSERT INTO available_machine_tier"
                + "(subscription_plan_id, machine_id) " + "VALUES "
    )
    for (i in 0 until machineIds.size) {
        if (i == 0) {
            insertTableSQL.append("(${machinePowerTiers[i]}, ${machineIds[i]})")
        } else {
            insertTableSQL.append(", (${machinePowerTiers[i]}, ${machineIds[i]})")
        }
    }
    statement.executeUpdate(insertTableSQL.toString())
}

fun createClientSubscriptionEntry(conn: Connection, amount: Int, clientIds: List<String>) {
    val statement = conn.createStatement()

    var amountLeft = amount

    val subPlanDates = mutableMapOf<Pair<String, String>, MutableList<Pair<String, String>>>()
    val resultSet =
        statement.executeQuery("SELECT client_id, subscription_plan_id, active_from, active_to from client_subscription_plan")
    while (resultSet.next()) {
        if (subPlanDates[Pair(resultSet.getString("client_id"), resultSet.getString("subscription_plan_id"))] == null) {
            subPlanDates[Pair(resultSet.getString("client_id"), resultSet.getString("subscription_plan_id"))] =
                mutableListOf(
                    Pair(
                        resultSet.getString("active_from"),
                        resultSet.getString("active_to")
                    )
                )
        } else {
            subPlanDates[Pair(resultSet.getString("client_id"), resultSet.getString("subscription_plan_id"))]!!.add(
                Pair(
                    resultSet.getString("active_from"),
                    resultSet.getString("active_to")
                )
            )
        }
    }
    val insertTableSQL = StringBuilder(
        "INSERT INTO client_subscription_plan"
                + "(client_id, subscription_plan_id, active_from, active_to) " + "VALUES "
    )

    while (amountLeft != 0) {
        val clientId = (0 until clientIds.size).random()
        val planId = (1..3).random()
        val startDate: String = generateRandomDate("2010-01-01", "2019-12-31")
        val endDate = generateRandomDate(startDate, "2023-12-31")

        var isPossibleToAdd = true
        val clientSubPlanDates = subPlanDates[Pair(clientIds[clientId], planId.toString())]
        if (clientSubPlanDates != null) {
            for (pair in clientSubPlanDates) {
                if ((!compareDates(pair.second, startDate) && compareDates(
                        endDate, pair.second
                    )) || (!compareDates(pair.first, startDate) && compareDates(
                        endDate, pair.first
                    ))
                ) {
                    isPossibleToAdd = false
                    break
                }
            }
        }
        if (isPossibleToAdd) {
            if (amountLeft == amount) {
                insertTableSQL.append("(${clientIds[clientId]}, $planId, '$startDate', '$endDate')")
            } else {
                insertTableSQL.append(", (${clientIds[clientId]}, $planId, '$startDate', '$endDate')")
            }
            if (clientSubPlanDates != null) {
                subPlanDates[Pair(clientIds[clientId], planId.toString())]!!.add(Pair(startDate, endDate))
            } else {
                subPlanDates[Pair(clientIds[clientId], planId.toString())] = mutableListOf(Pair(startDate, endDate))
            }
            amountLeft--
        }
    }
    statement.executeUpdate(insertTableSQL.toString())
}

//Если первая дата позже, возвращает true, иначе false
fun compareDates(firstDate: String, secondDate: String): Boolean {
    val firstDateParts = firstDate.split("-")
    val secondDateParts = secondDate.split("-")
    return if (secondDateParts[0].toInt() > firstDateParts[0].toInt())
        false
    else if (secondDateParts[1].toInt() > firstDateParts[1].toInt())
        false
    else !(secondDateParts[1].toInt() == firstDateParts[1].toInt() && secondDateParts[2].toInt() > firstDateParts[2].toInt())
}

fun createInstalledGameEntries(conn: Connection, amount: Int, gameIds: List<String>, machineIds: List<String>) {
    val statement = conn.createStatement()

    var amountLeft = amount

    val resultSet =
        statement.executeQuery("SELECT game_id, machine_id from installed_game")
    val gameMachinePairs = mutableListOf<Pair<String, String>>()
    while (resultSet.next()) {
        gameMachinePairs.add(Pair(resultSet.getString("machine_id"), resultSet.getString("game_id")))
    }

    val insertTableSQL = StringBuilder(
        "INSERT INTO installed_game"
                + "(machine_id, game_id) " + "VALUES "
    )

    while (amountLeft != 0) {
        val gameId = (1 until gameIds.size).random()
        val machineId = (1 until machineIds.size).random()

        if (!gameMachinePairs.contains(Pair(machineIds[machineId], gameIds[gameId]))) {
            if (amount == amountLeft) {
                insertTableSQL.append("(${machineIds[machineId]}, ${gameIds[gameId]})")
            } else {
                insertTableSQL.append(", (${machineIds[machineId]}, ${gameIds[gameId]})")
            }
            gameMachinePairs.add(Pair(machineIds[machineId], gameIds[gameId]))
            amountLeft--
        }
    }
    statement.executeUpdate(insertTableSQL.toString())
}

fun createGamePriceEntries(conn: Connection, gameIds: List<String>) {
    val statement = conn.createStatement()

    val insertTableSQL = StringBuilder(
        "INSERT INTO game_price"
                + "(game_id, price, price_set_date) " + "VALUES "
    )

    for (gameId in gameIds) {
        val price = (20..800).random() * 10
        if (gameId == gameIds.first()) {
            insertTableSQL.append("($gameId, $price, '${generateRandomDate("2010-01-01", "2019-12-31")}')")
        } else {
            insertTableSQL.append(", ($gameId, $price, '${generateRandomDate("2010-01-01", "2019-12-31")}')")
        }
    }
    statement.executeUpdate(insertTableSQL.toString())
}

fun createMachineUsageEntry(
    conn: Connection,
    amount: Int,
    machineIds: List<String>,
    machineAvailability: Map<String, String>,
    clientSubPlans: MutableMap<String, MutableList<Triple<String, String, String>>>
) {
    val ownedGameIds = mutableListOf<String>()
    val ownedGamePurchaseDates = mutableListOf<String>()
    val ownedGameClientIds = mutableListOf<String>()

    val statement = conn.createStatement()

    var resultSet = statement.executeQuery("SELECT id, client_id, purchase_date from owned_game")
    while (resultSet.next()) {
        ownedGameIds.add(resultSet.getString("id"))
        ownedGamePurchaseDates.add(resultSet.getString("purchase_date"))
        ownedGameClientIds.add(resultSet.getString("client_id"))
    }

    val occupiedDays = mutableMapOf<String, MutableList<Pair<String, String>>>()

    resultSet = statement.executeQuery("SELECT machine_id, in_use_from, in_use_to from machine_usage")
    while (resultSet.next()) {
        if (occupiedDays[resultSet.getString("machine_id")] == null) {
            occupiedDays[resultSet.getString("machine_id")] = mutableListOf(
                Pair(
                    resultSet.getString("in_use_from"),
                    resultSet.getString("in_use_to")
                )
            )
        } else {
            occupiedDays[resultSet.getString("machine_id")]!!.add(
                Pair(
                    resultSet.getString("in_use_from"),
                    resultSet.getString("in_use_to")
                )
            )
        }
    }

    val insertTableSQL = StringBuilder(
        "INSERT INTO machine_usage"
                + "(owned_game_id, machine_id, in_use_from, in_use_to) " + "VALUES "
    )

    var amountLeft = amount
    while (amountLeft > 0) {
        val ownedGameId = (0 until ownedGameIds.size).random()
        var date: String
        val randomClientSubs = clientSubPlans[ownedGameClientIds[ownedGameId]]
        if (randomClientSubs != null) {
            val randomPlanId = (0 until clientSubPlans[ownedGameClientIds[ownedGameId]]!!.size).random()
            date = generateRandomDate(
                randomClientSubs[randomPlanId].second,
                randomClientSubs[randomPlanId].third
            )

            val startTime = (0..86399900).random()
            val endTime = (startTime..86400000).random()

            val convertedStartTime =
                " %02d:%02d:%02d-%02d".format(
                    startTime / 3600000,
                    startTime % 3600000 / 60000,
                    startTime % 3600000 % 60000 / 1000,
                    startTime % 3600000 % 60000 % 1000 / 100
                )

            val convertedEndTime =
                " %02d:%02d:%02d-%02d".format(
                    endTime / 3600000,
                    endTime % 3600000 / 60000,
                    endTime % 3600000 % 60000 / 1000,
                    endTime % 3600000 % 60000 % 1000 / 100
                )

            var randomMachineId = (0 until machineIds.size).random() - 1
            var isMachineIdSuitable = true

            do {
                if (randomMachineId != machineIds.size - 1) {
                    randomMachineId++
                } else {
                    randomMachineId = 0
                }
                if (machineAvailability.getValue(machineIds[randomMachineId]).toInt() > randomClientSubs[randomPlanId].first.toInt()) {
                    continue
                }
                val occupiedDaysCurrentMachine = occupiedDays[machineIds[randomMachineId]]
                if (occupiedDaysCurrentMachine != null) {
                    isMachineIdSuitable = true
                    for (date_pair in occupiedDaysCurrentMachine) {
                        if (date_pair.first.take(10) == date || date_pair.second.take(10) == date) {
                            isMachineIdSuitable = false
                        }
                    }
                    if (isMachineIdSuitable) {
                        occupiedDays[machineIds[randomMachineId]]!!.add(
                            Pair(
                                date + convertedStartTime,
                                date + convertedEndTime
                            )
                        )
                    }
                } else {
                    occupiedDays[machineIds[randomMachineId]] =
                        mutableListOf(Pair(date + convertedStartTime, date + convertedEndTime))
                }
            } while (!isMachineIdSuitable)
            if (amountLeft != amount) {
                insertTableSQL.append(", ")
            }
            insertTableSQL.append(
                "(${ownedGameIds[ownedGameId]}, ${machineIds[randomMachineId]}, '${StringBuilder(date).append(
                    convertedStartTime
                )}', '${StringBuilder(
                    date
                ).append(convertedEndTime)}')"
            )
        } else {
            continue
        }
        amountLeft--
    }
    statement.executeUpdate(insertTableSQL.toString())
}

fun generateRandomString(len: Int): String {
    val sb = StringBuilder("")
    val AlphaNumericString = ("ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "0123456789"
            + "abcdefghijklmnopqrstuvxyz")

    for (i in 0 until len) {
        val index = (0..AlphaNumericString.length - 1).random()
        sb.append(AlphaNumericString[index])
    }
    return sb.toString()
}

fun generateRandomDate(dateFromString: String, dateToString: String): String {
    var parts = dateToString.split("-")
    val dateTo = LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    parts = dateFromString.split("-")
    val dateFrom = LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    val randomDate =
        LocalDate.ofEpochDay(dateFrom.toEpochDay() + (1..dateTo.toEpochDay() - dateFrom.toEpochDay() + 1).random())
    return StringBuilder("%04d".format(randomDate.year)).append("-")
        .append("%02d".format(randomDate.monthValue))
        .append("-").append("%02d".format(randomDate.dayOfMonth)).toString()
}