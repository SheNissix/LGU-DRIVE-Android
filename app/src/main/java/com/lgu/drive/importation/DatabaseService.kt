package com.lgu.drive.importation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.random.Random

object DatabaseService {
    private const val URL = "jdbc:mariadb://zephyr.proxy.rlwy.net:37168/importationform"
    private const val USER = "root"
    private const val PASS = "aZLUGLIFlwAzyDqsEsEGIavWAqsnJaxc"

    init {
        try {
            Class.forName("org.mariadb.jdbc.Driver")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getConnection(): Connection {
        val props = Properties().apply {
            put("user", USER)
            put("password", PASS)
            put("connectTimeout", "15000")
        }
        return DriverManager.getConnection(URL, props)
    }

    suspend fun fetchDonorsAndDoneesDetailed(): Pair<List<Map<String, String>>, List<Map<String, String>>> = withContext(Dispatchers.IO) {
        val donors = mutableListOf<Map<String, String>>()
        val donees = mutableListOf<Map<String, String>>()
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT * FROM donor").use { rs ->
                        val md = rs.metaData
                        while (rs.next()) {
                            val map = mutableMapOf<String, String>()
                            for (i in 1..md.columnCount) map[md.getColumnName(i)] = rs.getString(i) ?: ""
                            donors.add(map)
                        }
                    }
                    stmt.executeQuery("SELECT * FROM donee").use { rs ->
                        val md = rs.metaData
                        while (rs.next()) {
                            val map = mutableMapOf<String, String>()
                            for (i in 1..md.columnCount) map[md.getColumnName(i)] = rs.getString(i) ?: ""
                            donees.add(map)
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        Pair(donors, donees)
    }

    suspend fun fetchHistory(): List<List<String>> = withContext(Dispatchers.IO) {
        val records = mutableListOf<List<String>>()
        // Updated query to group vehicles by application
        val query = """
            SELECT a.ApplicationID, a.ApplicationDate, dn.DoneeName, dr.DonorName, 
                   GROUP_CONCAT(dv.CarType SEPARATOR ' / ') as CombinedType,
                   GROUP_CONCAT(dv.VehicleDescription SEPARATOR ' | ') as CombinedDesc
            FROM application a
            JOIN donee dn ON a.DoneeID = dn.DoneeID
            JOIN donor dr ON a.DonorID = dr.DonorID
            JOIN donatedvehicle dv ON a.ApplicationID = dv.ApplicationID
            GROUP BY a.ApplicationID
            ORDER BY a.ApplicationID DESC
        """
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(query).use { rs ->
                        while (rs.next()) {
                            // Logic for combined category
                            val rawType = rs.getString(5) ?: ""
                            val types = rawType.split(" / ").distinct()
                            val displayType = if (types.size > 1) "Motor Vehicle / Passenger Car" else types.first()

                            records.add(listOf(
                                rs.getString(1) ?: "", // ApplicationID
                                rs.getString(2) ?: "", // ApplicationDate
                                rs.getString(3) ?: "", // DoneeName
                                rs.getString(4) ?: "", // DonorName
                                "", // Legacy AssetID placeholder
                                rs.getString(6) ?: "", // CombinedDesc
                                displayType, // DisplayType (Badge)
                                "", // Legacy Qty placeholder
                                ""  // Legacy VIN placeholder
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        records
    }

    // New function to fetch all specific vehicle details for a single application
    suspend fun fetchApplicationDetails(appId: String): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val details = mutableListOf<Map<String, String>>()
        val query = """
            SELECT dv.*, pc.VIN, pc.YearModel, pc.Color, pc.RegistrationDate, pc.VehicleWeight, pc.EngineNumber, pc.EngineDisplacement, pc.FuelType 
            FROM donatedvehicle dv 
            LEFT JOIN passengercar pc ON dv.DonateID = pc.DonateID
            WHERE dv.ApplicationID = ?
        """
        try {
            getConnection().use { conn ->
                conn.prepareStatement(query).use { ps ->
                    ps.setString(1, appId)
                    ps.executeQuery().use { rs ->
                        val md = rs.metaData
                        while (rs.next()) {
                            val map = mutableMapOf<String, String>()
                            for (i in 1..md.columnCount) {
                                val colName = md.getColumnName(i)
                                map[colName] = rs.getString(i) ?: "N/A"
                            }
                            details.add(map)
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        details
    }

    suspend fun submitVehicleApplication(
        formData: Map<String, Any>, 
        motorVehicles: List<Map<String, String>>, 
        passengerCars: List<Map<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection()
            conn.autoCommit = false

            val doneeStatus = formData["DoneeStatus"] as String
            val doneeId = if (doneeStatus == "existing") formData["ExistingDoneeID"] as String else {
                var maxNum = 0
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT MAX(CAST(SUBSTRING(DoneeID, 4) AS UNSIGNED)) FROM donee").use { rs ->
                        if (rs.next()) maxNum = rs.getInt(1)
                    }
                }
                val id = String.format("DON%04d", maxNum + 1)
                
                conn.prepareStatement("INSERT INTO donee VALUES (?,?,?,?,?,?,?)").use { ps ->
                    ps.setString(1, id); ps.setString(2, formData["DoneeName"] as String)
                    ps.setString(3, formData["DoneeAddress"] as String); ps.setString(4, formData["ContactPerson"] as String)
                    ps.setString(5, formData["DoneeTelNo"] as String); ps.setString(6, formData["DoneeFaxNo"] as String)
                    ps.setString(7, formData["DoneeEmail"] as String); ps.executeUpdate()
                }
                id
            }

            val donorStatus = formData["DonorStatus"] as String
            val donorId = if (donorStatus == "existing") formData["ExistingDonorID"] as String else {
                var maxNum = 0
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT MAX(CAST(SUBSTRING(DonorID, 4) AS UNSIGNED)) FROM donor").use { rs ->
                        if (rs.next()) maxNum = rs.getInt(1)
                    }
                }
                val id = String.format("DOR%04d", maxNum + 1)

                conn.prepareStatement("INSERT INTO donor VALUES (?,?,?,?,?,?)").use { ps ->
                    ps.setString(1, id); ps.setString(2, formData["DonorName"] as String)
                    ps.setString(3, formData["DonorAddress"] as String); ps.setString(4, formData["DonorTelNo"] as String)
                    ps.setString(5, formData["DonorFaxNo"] as String); ps.setString(6, formData["DonorEmail"] as String)
                    ps.executeUpdate()
                }
                id
            }

            var maxAppNum = 0
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT MAX(CAST(SUBSTRING(ApplicationID, 5) AS UNSIGNED)) FROM application").use { rs ->
                    if (rs.next()) maxAppNum = rs.getInt(1)
                }
            }
            val appId = String.format("APP-%04d", maxAppNum + 1)

            conn.prepareStatement("INSERT INTO application VALUES (?,?,?,?,?)").use { appPs ->
                appPs.setString(1, appId); appPs.setString(2, doneeId); appPs.setString(3, donorId)
                appPs.setDate(4, java.sql.Date(System.currentTimeMillis()))
                appPs.setString(5, formData["DonorSignaturePath"] as String)
                appPs.executeUpdate()
            }

            var assetInserted = false
            for (mv in motorVehicles) {
                val desc = mv["desc"] ?: ""
                if (desc.isBlank()) continue
                val donateId = "CAR${Random.nextInt(10000, 99999)}"
                conn.prepareStatement("INSERT INTO donatedvehicle VALUES (?,?,?,?,?,?,?)").use { ps ->
                    ps.setString(1, donateId); ps.setString(2, desc)
                    ps.setString(3, mv["tariff"]?.ifBlank { "0" } ?: "0")
                    ps.setString(4, mv["origin"]?.ifBlank { "N/A" } ?: "N/A")
                    ps.setInt(5, mv["qty"]?.toIntOrNull() ?: 1); ps.setString(6, appId)
                    ps.setString(7, "Motor Vehicle"); ps.executeUpdate()
                }
                assetInserted = true
            }

            for (pc in passengerCars) {
                val pDesc = pc["desc"] ?: ""
                if (pDesc.isBlank()) continue

                val donateId = "CAR${Random.nextInt(10000, 99999)}"
                conn.prepareStatement("INSERT INTO donatedvehicle VALUES (?,?,?,?,?,?,?)").use { psV ->
                    psV.setString(1, donateId); psV.setString(2, pDesc)
                    psV.setString(3, pc["tariff"]?.ifBlank { "8703" } ?: "8703")
                    psV.setString(4, pc["origin"]?.ifBlank { "Japan" } ?: "Japan")
                    psV.setInt(5, 1); psV.setString(6, appId); psV.setString(7, "Passenger Car")
                    psV.executeUpdate()
                }

                conn.prepareStatement("INSERT INTO passengercar VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)") .use { psC ->
                    psC.setString(1, (pc["vin"] ?: "").uppercase().trim()); psC.setString(2, donateId)
                    psC.setInt(3, pc["year"]?.toIntOrNull() ?: 0)
                    psC.setString(4, pc["color"]?.ifBlank { "N/A" } ?: "N/A")
                    try { psC.setDate(5, java.sql.Date.valueOf(pc["regDate"] ?: "")) } catch (e: Exception) { psC.setNull(5, java.sql.Types.DATE) }
                    psC.setString(6, pc["weight"]?.ifBlank { "0" } ?: "0")
                    psC.setString(7, pc["engineNo"]?.ifBlank { "N/A" } ?: "N/A")
                    psC.setString(8, pc["displacement"]?.ifBlank { "N/A" } ?: "N/A")
                    psC.setString(9, pc["fuelType"]?.ifBlank { "Gasoline" } ?: "Gasoline"); psC.executeUpdate()
                }
                assetInserted = true
            }

            if (!assetInserted) throw Exception("Validation Error: Please describe at least one vehicle asset profile.")

            conn.commit()
            Result.success(appId)
        } catch (e: Exception) {
            conn?.rollback()
            Result.failure(e)
        } finally { conn?.close() }
    }

    suspend fun deleteApplication(appId: String): Result<Unit> = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection()
            conn.autoCommit = false

            // 1. Get DoneeID and DonorID for this application
            var doneeId: String? = null
            var donorId: String? = null
            conn.prepareStatement("SELECT DoneeID, DonorID FROM application WHERE ApplicationID = ?").use { ps ->
                ps.setString(1, appId)
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        doneeId = rs.getString("DoneeID")
                        donorId = rs.getString("DonorID")
                    }
                }
            }

            if (doneeId == null || donorId == null) throw Exception("Application not found.")

            // 2. Delete linked vehicles (PassengerCar and DonatedVehicle)
            val donateIds = mutableListOf<String>()
            conn.prepareStatement("SELECT DonateID FROM donatedvehicle WHERE ApplicationID = ?").use { ps ->
                ps.setString(1, appId)
                ps.executeQuery().use { rs -> while (rs.next()) donateIds.add(rs.getString(1)) }
            }

            for (did in donateIds) {
                conn.prepareStatement("DELETE FROM passengercar WHERE DonateID = ?").use { ps ->
                    ps.setString(1, did); ps.executeUpdate()
                }
            }
            conn.prepareStatement("DELETE FROM donatedvehicle WHERE ApplicationID = ?").use { ps ->
                ps.setString(1, appId); ps.executeUpdate()
            }

            // 3. Delete the Application record
            conn.prepareStatement("DELETE FROM application WHERE ApplicationID = ?").use { ps ->
                ps.setString(1, appId); ps.executeUpdate()
            }

            // 4. Conditional deletion of Donee
            var doneeUsage = 0
            conn.prepareStatement("SELECT COUNT(*) FROM application WHERE DoneeID = ?").use { ps ->
                ps.setString(1, doneeId)
                ps.executeQuery().use { rs -> if (rs.next()) doneeUsage = rs.getInt(1) }
            }
            if (doneeUsage == 0) {
                conn.prepareStatement("DELETE FROM donee WHERE DoneeID = ?").use { ps ->
                    ps.setString(1, doneeId); ps.executeUpdate()
                }
            }

            // 5. Conditional deletion of Donor
            var donorUsage = 0
            conn.prepareStatement("SELECT COUNT(*) FROM application WHERE DonorID = ?").use { ps ->
                ps.setString(1, donorId)
                ps.executeQuery().use { rs -> if (rs.next()) donorUsage = rs.getInt(1) }
            }
            if (donorUsage == 0) {
                conn.prepareStatement("DELETE FROM donor WHERE DonorID = ?").use { ps ->
                    ps.setString(1, donorId); ps.executeUpdate()
                }
            }

            conn.commit()
            Result.success(Unit)
        } catch (e: Exception) {
            conn?.rollback()
            Result.failure(e)
        } finally { conn?.close() }
    }

    suspend fun fetchVehiclesDetailed(): Pair<List<Map<String, String>>, List<Map<String, String>>> = withContext(Dispatchers.IO) {
        val motor = mutableListOf<Map<String, String>>()
        val passenger = mutableListOf<Map<String, String>>()
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT * FROM donatedvehicle WHERE CarType = 'Motor Vehicle'").use { rs ->
                        val md = rs.metaData
                        while (rs.next()) {
                            val map = mutableMapOf<String, String>()
                            for (i in 1..md.columnCount) map[md.getColumnName(i)] = rs.getString(i) ?: ""
                            motor.add(map)
                        }
                    }
                    stmt.executeQuery("""
                        SELECT dv.*, pc.VIN, pc.YearModel, pc.Color, pc.RegistrationDate, pc.VehicleWeight, pc.EngineNumber, pc.EngineDisplacement, pc.FuelType 
                        FROM donatedvehicle dv 
                        JOIN passengercar pc ON dv.DonateID = pc.DonateID
                    """).use { rs ->
                        val md = rs.metaData
                        while (rs.next()) {
                            val map = mutableMapOf<String, String>()
                            for (i in 1..md.columnCount) map[md.getColumnName(i)] = rs.getString(i) ?: ""
                            passenger.add(map)
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        Pair(motor, passenger)
    }

    suspend fun executeRawSql(query: String): RawSqlResult = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext RawSqlResult.Error("Query string command buffer is empty.")
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    val isResultSet = stmt.execute(cleanQuery)
                    if (isResultSet) {
                        stmt.resultSet.use { rs ->
                            val metaData = rs.metaData
                            val columnCount = metaData.columnCount
                            val headers = (1..columnCount).map { metaData.getColumnName(it) }
                            val rows = mutableListOf<List<String>>()
                            while (rs.next()) {
                                rows.add((1..columnCount).map { rs.getString(it) ?: "NULL" })
                            }
                            RawSqlResult.SelectSuccess(headers, rows)
                        }
                    } else RawSqlResult.UpdateSuccess(stmt.updateCount)
                }
            }
        } catch (e: Exception) { RawSqlResult.Error(e.message ?: "SQL Execution Error Exception.") }
    }
}

sealed class RawSqlResult {
    data class SelectSuccess(val headers: List<String>, val rows: List<List<String>>) : RawSqlResult()
    data class UpdateSuccess(val affectedRows: Int) : RawSqlResult()
    data class Error(val message: String) : RawSqlResult()
}
