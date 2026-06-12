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

    suspend fun fetchDonorsAndDonees(): Pair<List<Map<String, String>>, List<Map<String, String>>> = withContext(Dispatchers.IO) {
        val donors = mutableListOf<Map<String, String>>()
        val donees = mutableListOf<Map<String, String>>()
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT DonorID, DonorName FROM donor").use { rs ->
                        while (rs.next()) donors.add(mapOf("id" to rs.getString("DonorID"), "name" to rs.getString("DonorName")))
                    }
                    stmt.executeQuery("SELECT DoneeID, DoneeName FROM donee").use { rs ->
                        while (rs.next()) donees.add(mapOf("id" to rs.getString("DoneeID"), "name" to rs.getString("DoneeName")))
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
                   dv.DonateID, dv.VehicleDescription, dv.CarType, dv.Quantity, COALESCE(pc.VIN, 'General Cargo')
            FROM application a
            JOIN donee dn ON a.DoneeID = dn.DoneeID
            JOIN donor dr ON a.DonorID = dr.DonorID
            JOIN donatedvehicle dv ON a.ApplicationID = dv.ApplicationID
            LEFT JOIN passengercar pc ON dv.DonateID = pc.DonateID
            ORDER BY a.ApplicationID DESC
        """
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(query).use { rs ->
                        while (rs.next()) {
                            records.add(listOf(
                                rs.getString(1) ?: "", rs.getString(2) ?: "", rs.getString(3) ?: "",
                                rs.getString(4) ?: "", rs.getString(5) ?: "", rs.getString(6) ?: "",
                                rs.getString(7) ?: "", rs.getString(8) ?: "", rs.getString(9) ?: ""
                            ))
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        records
    }

    suspend fun submitVehicleApplication(formData: Map<String, Any>, motorVehicles: List<Map<String, String>>): Result<String> = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection()
            conn.autoCommit = false

            val doneeStatus = formData["DoneeStatus"] as String
            val doneeId = if (doneeStatus == "existing") formData["ExistingDoneeID"] as String else {
                val id = "DON${Random.nextInt(1000, 9999)}"
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
                val id = "DOR${Random.nextInt(1000, 9999)}"
                conn.prepareStatement("INSERT INTO donor VALUES (?,?,?,?,?,?)").use { ps ->
                    ps.setString(1, id); ps.setString(2, formData["DonorName"] as String)
                    ps.setString(3, formData["DonorAddress"] as String); ps.setString(4, formData["DonorTelNo"] as String)
                    ps.setString(5, formData["DonorFaxNo"] as String); ps.setString(6, formData["DonorEmail"] as String)
                    ps.executeUpdate()
                }
                id
            }

            var appCount = 0
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT COUNT(*) FROM application").use { rs -> if (rs.next()) appCount = rs.getInt(1) }
            }
            val appId = String.format("APP-%04d", appCount + Random.nextInt(100, 999))

            conn.prepareStatement("INSERT INTO application VALUES (?,?,?,?,?)").use { appPs ->
                appPs.setString(1, appId); appPs.setString(2, doneeId); appPs.setString(3, donorId)
                appPs.setDate(4, java.sql.Date(System.currentTimeMillis()))
                appPs.setString(5, formData["DonorSignaturePath"] as? String ?: "[sig_mobile.png]")
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

            val pDesc = formData["PassengerDesc"] as String
            if (pDesc.isNotBlank()) {
                val donateId = "CAR${Random.nextInt(10000, 99999)}"
                conn.prepareStatement("INSERT INTO donatedvehicle VALUES (?,?,?,?,?,?,?)").use { psV ->
                    psV.setString(1, donateId); psV.setString(2, pDesc)
                    psV.setString(3, formData["PassengerTariff"]?.toString()?.ifBlank { "8703" } ?: "8703")
                    psV.setString(4, formData["PassengerOrigin"]?.toString()?.ifBlank { "Japan" } ?: "Japan")
                    psV.setInt(5, 1); psV.setString(6, appId); psV.setString(7, "Passenger Car")
                    psV.executeUpdate()
                }

                conn.prepareStatement("INSERT INTO passengercar VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)") .use { psC ->
                    psC.setString(1, (formData["VIN"] as String).uppercase().trim()); psC.setString(2, donateId)
                    psC.setInt(3, formData["YearModel"]?.toString()?.toIntOrNull() ?: 0)
                    psC.setString(4, formData["Color"]?.toString()?.ifBlank { "N/A" } ?: "N/A")
                    try { psC.setDate(5, java.sql.Date.valueOf(formData["RegistrationDate"] as String)) } catch (e: Exception) { psC.setNull(5, java.sql.Types.DATE) }
                    psC.setString(6, formData["VehicleWeight"]?.toString()?.ifBlank { "0" } ?: "0")
                    psC.setString(7, formData["EngineNumber"]?.toString()?.ifBlank { "N/A" } ?: "N/A")
                    psC.setString(8, formData["EngineDisplacement"]?.toString()?.ifBlank { "N/A" } ?: "N/A")
                    psC.setString(9, formData["FuelType"] as String); psC.executeUpdate()
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