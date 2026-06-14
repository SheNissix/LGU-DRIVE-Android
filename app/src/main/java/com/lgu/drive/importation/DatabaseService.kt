package com.lgu.drive.importation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

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

    private var sharedConn: Connection? = null

    private suspend fun getConnection(): Connection {
        return withContext(Dispatchers.IO) {
            try {
                if (sharedConn == null || sharedConn!!.isClosed || !sharedConn!!.isValid(2)) {
                    val props = Properties().apply {
                        put("user", USER)
                        put("password", PASS)
                        put("connectTimeout", "10000")
                    }
                    sharedConn = DriverManager.getConnection(URL, props)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sharedConn!!
        }
    }

    suspend fun fetchDonorsAndDoneesDetailed(): Pair<List<Map<String, String>>, List<Map<String, String>>> = withContext(Dispatchers.IO) {
        val donors = mutableListOf<Map<String, String>>()
        val donees = mutableListOf<Map<String, String>>()
        try {
            val conn = getConnection()
            conn.createStatement().use { stmt ->
                // Migration: Ensure Donor table has ContactPerson
                try { stmt.execute("ALTER TABLE donor ADD COLUMN ContactPerson VARCHAR(255) AFTER DonorAddress") } catch (e: Exception) {}
                
                stmt.executeQuery("SELECT DonorID, DonorName, DonorAddress, DonorTelNo, DonorFaxNo, DonorEmail FROM donor").use { rs ->
                    while (rs.next()) {
                        donors.add(mapOf(
                            "DonorID" to (rs.getString(1) ?: ""), "DonorName" to (rs.getString(2) ?: ""),
                            "DonorAddress" to (rs.getString(3) ?: ""), "DonorTelNo" to (rs.getString(4) ?: ""),
                            "DonorFaxNo" to (rs.getString(5) ?: ""), "DonorEmail" to (rs.getString(6) ?: "")
                        ))
                    }
                }
                stmt.executeQuery("SELECT DoneeID, DoneeName, DoneeAddress, ContactPerson, DoneeTelNo, DoneeFaxNo, DoneeEmail FROM donee").use { rs ->
                    while (rs.next()) {
                        donees.add(mapOf(
                            "DoneeID" to (rs.getString(1) ?: ""), "DoneeName" to (rs.getString(2) ?: ""),
                            "DoneeAddress" to (rs.getString(3) ?: ""), "ContactPerson" to (rs.getString(4) ?: ""),
                            "DoneeTelNo" to (rs.getString(5) ?: ""), "DoneeFaxNo" to (rs.getString(6) ?: ""),
                            "DoneeEmail" to (rs.getString(7) ?: "")
                        ))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        Pair(donors, donees)
    }

    suspend fun fetchHistory(): List<List<String>> = withContext(Dispatchers.IO) {
        val records = mutableListOf<List<String>>()
        val query = """
            SELECT a.ApplicationID, a.ApplicationDate, dn.DoneeName, dr.DonorName, 
                   GROUP_CONCAT(dv.CarType SEPARATOR ' / ') as CombinedType,
                   GROUP_CONCAT(dv.VehicleDescription SEPARATOR ' | ') as CombinedDesc,
                   dn.DoneeID, dr.DonorID
            FROM application a
            JOIN donee dn ON a.DoneeID = dn.DoneeID
            JOIN donor dr ON a.DonorID = dr.DonorID
            LEFT JOIN donatedvehicle dv ON a.ApplicationID = dv.ApplicationID
            GROUP BY a.ApplicationID
            ORDER BY a.ApplicationID DESC
        """
        try {
            val conn = getConnection()
            conn.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        val rawType = rs.getString(5) ?: ""
                        val types = rawType.split(" / ").distinct().filter { it.isNotBlank() }
                        val displayType = if (types.size > 1) "Motor Vehicle / Passenger Car" else if(types.isEmpty()) "Unknown" else types.first()

                        records.add(listOf(
                            rs.getString(1) ?: "", // 0: ApplicationID
                            rs.getString(2) ?: "", // 1: ApplicationDate
                            rs.getString(3) ?: "", // 2: DoneeName
                            rs.getString(4) ?: "", // 3: DonorName
                            "", // 4: Placeholder
                            rs.getString(6) ?: "", // 5: CombinedDesc
                            displayType, // 6: DisplayType (Badge)
                            rs.getString(7) ?: "", // 7: DoneeID
                            rs.getString(8) ?: ""  // 8: DonorID
                        ))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        records
    }

    suspend fun fetchApplicationDetails(appId: String): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val details = mutableListOf<Map<String, String>>()
        val query = """
            SELECT dv.DonateID, dv.VehicleDescription, dv.TariffCode, dv.Origin, dv.Quantity, dv.CarType,
                   pc.VIN, pc.YearModel, pc.Color, pc.RegistrationDate, pc.VehicleWeight, pc.EngineNumber, pc.EngineDisplacement, pc.FuelType 
            FROM donatedvehicle dv 
            LEFT JOIN passengercar pc ON dv.DonateID = pc.DonateID
            WHERE dv.ApplicationID = ?
        """
        try {
            val conn = getConnection()
            conn.prepareStatement(query).use { ps ->
                ps.setString(1, appId)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        details.add(mapOf(
                            "DonateID" to (rs.getString(1) ?: ""), "VehicleDescription" to (rs.getString(2) ?: ""),
                            "TariffCode" to (rs.getString(3) ?: ""), "Origin" to (rs.getString(4) ?: ""),
                            "Quantity" to (rs.getString(5) ?: "1"), "CarType" to (rs.getString(6) ?: ""),
                            "VIN" to (rs.getString(7) ?: "N/A"), "YearModel" to (rs.getString(8) ?: "N/A"),
                            "Color" to (rs.getString(9) ?: "N/A"), "RegistrationDate" to (rs.getString(10) ?: "N/A"),
                            "VehicleWeight" to (rs.getString(11) ?: "N/A"), "EngineNumber" to (rs.getString(12) ?: "N/A"),
                            "EngineDisplacement" to (rs.getString(13) ?: "N/A"), "FuelType" to (rs.getString(14) ?: "N/A")
                        ))
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
        try {
            val conn = getConnection()
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

                conn.prepareStatement("INSERT INTO donor (DonorID, DonorName, DonorAddress, DonorTelNo, DonorFaxNo, DonorEmail) VALUES (?,?,?,?,?,?)").use { ps ->
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

            for (mv in motorVehicles) {
                val desc = mv["desc"] ?: ""
                if (desc.isBlank()) continue
                
                var maxAssetNum = 0
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT MAX(CAST(SUBSTRING(DonateID, 4) AS UNSIGNED)) FROM donatedvehicle").use { rs ->
                        if (rs.next()) maxAssetNum = rs.getInt(1)
                    }
                }
                val donateId = String.format("CAR%05d", maxAssetNum + 1)
                
                conn.prepareStatement("INSERT INTO donatedvehicle VALUES (?,?,?,?,?,?,?)").use { ps ->
                    ps.setString(1, donateId); ps.setString(2, desc)
                    ps.setString(3, mv["tariffCode"]?.ifBlank { "0" } ?: "0")
                    ps.setString(4, mv["origin"]?.ifBlank { "N/A" } ?: "N/A")
                    ps.setInt(5, mv["qty"]?.toIntOrNull() ?: 1); ps.setString(6, appId)
                    ps.setString(7, "Motor Vehicle"); ps.executeUpdate()
                }
            }

            for (pc in passengerCars) {
                var maxAssetNum = 0
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT MAX(CAST(SUBSTRING(DonateID, 4) AS UNSIGNED)) FROM donatedvehicle").use { rs ->
                        if (rs.next()) maxAssetNum = rs.getInt(1)
                    }
                }
                val donateId = String.format("CAR%05d", maxAssetNum + 1)

                conn.prepareStatement("INSERT INTO donatedvehicle VALUES (?,?,?,?,?,?,?)").use { psV ->
                    psV.setString(1, donateId); psV.setString(2, "Passenger Car")
                    psV.setString(3, "8703"); psV.setString(4, "Japan")
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
                    psC.setString(9, pc["fuelType"]?.ifBlank { "G" } ?: "G"); psC.executeUpdate()
                }
            }

            conn.commit()
            Result.success(appId)
        } catch (e: Exception) {
            sharedConn?.rollback()
            Result.failure(e)
        }
    }

    suspend fun deleteApplication(appId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conn = getConnection()
            conn.autoCommit = false

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
            conn.prepareStatement("DELETE FROM application WHERE ApplicationID = ?").use { ps ->
                ps.setString(1, appId); ps.executeUpdate()
            }

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
            sharedConn?.rollback()
            Result.failure(e)
        }
    }

    suspend fun fetchVehiclesDetailed(): Pair<List<Map<String, String>>, List<Map<String, String>>> = withContext(Dispatchers.IO) {
        val motor = mutableListOf<Map<String, String>>()
        val passenger = mutableListOf<Map<String, String>>()
        try {
            val conn = getConnection()
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT DonateID, VehicleDescription, TariffCode, Origin, Quantity, CarType, ApplicationID FROM donatedvehicle WHERE CarType = 'Motor Vehicle'").use { rs ->
                    while (rs.next()) {
                        motor.add(mapOf(
                            "DonateID" to (rs.getString(1) ?: ""), "VehicleDescription" to (rs.getString(2) ?: ""),
                            "TariffCode" to (rs.getString(3) ?: ""), "Origin" to (rs.getString(4) ?: ""),
                            "Quantity" to (rs.getString(5) ?: ""), "CarType" to (rs.getString(6) ?: ""),
                            "ApplicationID" to (rs.getString(7) ?: "")
                        ))
                    }
                }
                stmt.executeQuery("""
                    SELECT dv.DonateID, dv.VehicleDescription, dv.TariffCode, dv.Origin, dv.Quantity, dv.CarType, dv.ApplicationID,
                           pc.VIN, pc.YearModel, pc.Color, pc.RegistrationDate, pc.VehicleWeight, pc.EngineNumber, pc.EngineDisplacement, pc.FuelType 
                    FROM donatedvehicle dv 
                    JOIN passengercar pc ON dv.DonateID = pc.DonateID
                """).use { rs ->
                    while (rs.next()) {
                        passenger.add(mapOf(
                            "DonateID" to (rs.getString(1) ?: ""), "VehicleDescription" to (rs.getString(2) ?: ""),
                            "TariffCode" to (rs.getString(3) ?: ""), "Origin" to (rs.getString(4) ?: ""),
                            "Quantity" to (rs.getString(5) ?: ""), "CarType" to (rs.getString(6) ?: ""),
                            "ApplicationID" to (rs.getString(7) ?: ""), "VIN" to (rs.getString(8) ?: ""),
                            "YearModel" to (rs.getString(9) ?: ""), "Color" to (rs.getString(10) ?: ""),
                            "RegistrationDate" to (rs.getString(11) ?: ""), "VehicleWeight" to (rs.getString(12) ?: ""),
                            "EngineNumber" to (rs.getString(13) ?: ""), "EngineDisplacement" to (rs.getString(14) ?: ""),
                            "FuelType" to (rs.getString(15) ?: "")
                        ))
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
            val conn = getConnection()
            conn.createStatement().use { stmt ->
                val isResultSet = stmt.execute(cleanQuery)
                if (isResultSet) {
                    stmt.resultSet.use { rs ->
                        val metaData = rs.metaData
                        val columnCount = metaData.columnCount
                        val headers = (1..columnCount).map { metaData.getColumnLabel(it) }
                        val rows = mutableListOf<List<String>>()
                        while (rs.next()) {
                            rows.add((1..columnCount).map { rs.getString(it) ?: "NULL" })
                        }
                        RawSqlResult.SelectSuccess(headers, rows)
                    }
                } else RawSqlResult.UpdateSuccess(stmt.updateCount)
            }
        } catch (e: Exception) { RawSqlResult.Error(e.message ?: "SQL Execution Error Exception.") }
    }
}

sealed class RawSqlResult {
    data class SelectSuccess(val headers: List<String>, val rows: List<List<String>>) : RawSqlResult()
    data class UpdateSuccess(val affectedRows: Int) : RawSqlResult()
    data class Error(val message: String) : RawSqlResult()
}
