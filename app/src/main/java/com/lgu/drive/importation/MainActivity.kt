package com.lgu.drive.importation

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LguDriveTheme {
                ModernAppLayoutContainer()
            }
        }
    }
}

sealed class NavigationTab(val route: String, val label: String, val icon: ImageVector) {
    object PortalForm : NavigationTab("form", "New Application", Icons.Default.Description)
    object LedgerArchives : NavigationTab("history", "Previous Applications", Icons.Default.Inventory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAppLayoutContainer() {
    val navController = rememberNavController()
    val tabs = listOf(NavigationTab.PortalForm, NavigationTab.LedgerArchives)
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var refreshTrigger by remember { mutableLongStateOf(0L) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 0.dp
            ) {
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "LGU-DRIVE",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Vehicle Importation Registry",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Spacer(Modifier.height(32.dp))
                tabs.forEach { tab ->
                    NavigationDrawerItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, fontWeight = FontWeight.Medium) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            selectedIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "v2.4.0-PROTOTYPE",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    title = {
                        Text(
                            "LGU-DRIVE CONSOLE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = { refreshTrigger = System.currentTimeMillis() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Server Data")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.border(0.5.dp, Color(0xFFEEEEEE))
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
                NavHost(navController = navController, startDestination = "form") {
                    composable("form") { RegisterVehicleScreen(navController, refreshTrigger) }
                    composable("history") { HistoryLogScreen(refreshTrigger) }
                }
            }
        }
    }
}

@Composable
fun RegisterVehicleScreen(navController: NavHostController, refreshTrigger: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(0) }

    var donors by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var donees by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    var doneeStatus by remember { mutableStateOf("new") }
    var selectedDoneeId by remember { mutableStateOf("") }
    var doneeName by remember { mutableStateOf("") }
    var doneeAddr by remember { mutableStateOf("") }
    var doneeContact by remember { mutableStateOf("") }
    var doneeTel by remember { mutableStateOf("") }
    var doneeFax by remember { mutableStateOf("") }
    var doneeEmail by remember { mutableStateOf("") }

    var donorStatus by remember { mutableStateOf("new") }
    var selectedDonorId by remember { mutableStateOf("") }
    var donorName by remember { mutableStateOf("") }
    var donorAddr by remember { mutableStateOf("") }
    var donorTel by remember { mutableStateOf("") }
    var donorFax by remember { mutableStateOf("") }
    var donorEmail by remember { mutableStateOf("") }

    var passengerDesc by remember { mutableStateOf("") }
    var passengerTariff by remember { mutableStateOf("") }
    var passengerOrigin by remember { mutableStateOf("") }
    var vin by remember { mutableStateOf("") }
    var modelYear by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var regDate by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var engineNo by remember { mutableStateOf("") }
    var displacement by remember { mutableStateOf("") }
    var fuelType by remember { mutableStateOf("G") }

    val motorVehicles = remember { mutableStateListOf(mutableMapOf("desc" to "", "tariff" to "", "origin" to "", "qty" to "1")) }
    var signatureUriText by remember { mutableStateOf("No confirmation files selected") }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        signatureUriText = uri?.lastPathSegment?.let { "signatures/uploaded_signature_$it.png" } ?: "No verification file chosen"
    }

    LaunchedEffect(refreshTrigger) {
        val (dnr, dne) = DatabaseService.fetchDonorsAndDonees()
        donors = dnr; donees = dne
        if (dnr.isNotEmpty()) selectedDonorId = dnr.first()["id"] ?: ""
        if (dne.isNotEmpty()) selectedDoneeId = dne.first()["id"] ?: ""
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Step Indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val steps = listOf("Consignee", "Donor", "Vehicles", "Review")
            steps.forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (index <= currentStep) MaterialTheme.colorScheme.primary else Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < currentStep) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(text = "${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (index <= currentStep) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                if (index < steps.size - 1) {
                    HorizontalDivider(modifier = Modifier.weight(1f).padding(start = 8.dp, end = 8.dp), color = if (index < currentStep) MaterialTheme.colorScheme.primary else Color.LightGray)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (currentStep) {
                    0 -> { // Step 1: Donee
                        Text("1. Donee / Consignee Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = doneeStatus == "new", onClick = { doneeStatus = "new" })
                                        Text("New Profile", fontSize = 14.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = doneeStatus == "existing", onClick = { doneeStatus = "existing" })
                                        Text("Search Existing", fontSize = 14.sp)
                                    }
                                }
                                if (doneeStatus == "new") {
                                    OutlinedTextField(value = doneeName, onValueChange = { doneeName = it }, label = { Text("Donee Name") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = doneeAddr, onValueChange = { doneeAddr = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = doneeContact, onValueChange = { doneeContact = it }, label = { Text("Contact Person") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = doneeTel, onValueChange = { doneeTel = it }, label = { Text("Telephone") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = doneeFax, onValueChange = { doneeFax = it }, label = { Text("Fax Number") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = doneeEmail, onValueChange = { doneeEmail = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                                } else {
                                    donees.forEach { donee ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                            RadioButton(selected = selectedDoneeId == donee["id"], onClick = { selectedDoneeId = donee["id"] ?: "" })
                                            Text("${donee["name"]} [${donee["id"]}]", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Step 2: Donor
                        Text("2. Donor / Supplier Entity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = donorStatus == "new", onClick = { donorStatus = "new" })
                                        Text("New Profile", fontSize = 14.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = donorStatus == "existing", onClick = { donorStatus = "existing" })
                                        Text("Search Existing", fontSize = 14.sp)
                                    }
                                }
                                if (donorStatus == "new") {
                                    OutlinedTextField(value = donorName, onValueChange = { donorName = it }, label = { Text("Donor Name") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = donorAddr, onValueChange = { donorAddr = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = donorTel, onValueChange = { donorTel = it }, label = { Text("Telephone") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = donorFax, onValueChange = { donorFax = it }, label = { Text("Fax Number") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = donorEmail, onValueChange = { donorEmail = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                                } else {
                                    donors.forEach { donor ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                            RadioButton(selected = selectedDonorId == donor["id"], onClick = { selectedDonorId = donor["id"] ?: "" })
                                            Text("${donor["name"]} [${donor["id"]}]", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // Step 3: Vehicles
                        Text("3. Vehicle Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Vehicle Specification", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Type:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = fuelType == "G", onClick = { fuelType = "G" }) // Using fuelType as temporary vehicle type toggle for now to minimize state changes
                                        Text("Motor Vehicle", fontSize = 14.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = fuelType == "D", onClick = { fuelType = "D" })
                                        Text("Passenger Car", fontSize = 14.sp)
                                    }
                                }

                                if (fuelType == "G") {
                                    motorVehicles.forEachIndexed { idx, item ->
                                        Text("Vehicle Block #${idx + 1}:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        OutlinedTextField(value = item["desc"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("desc", it) } }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["tariff"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("tariff", it) } }, label = { Text("Tariff") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["origin"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("origin", it) } }, label = { Text("Origin") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["qty"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("qty", it) } }, label = { Text("Qty") }, modifier = Modifier.weight(0.6f))
                                        }
                                    }
                                    OutlinedButton(onClick = { motorVehicles.add(mutableMapOf("desc" to "", "tariff" to "", "origin" to "", "qty" to "1")) }, modifier = Modifier.align(Alignment.End)) {
                                        Text("+ Add Row")
                                    }
                                } else {
                                    // Passenger Car Mode
                                    OutlinedTextField(value = passengerDesc, onValueChange = { passengerDesc = it }, label = { Text("Car Description") }, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = passengerTariff, onValueChange = { passengerTariff = it }, label = { Text("Tariff Code") }, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = passengerOrigin, onValueChange = { passengerOrigin = it }, label = { Text("Origin") }, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = "1", onValueChange = {}, label = { Text("Qty") }, modifier = Modifier.weight(0.6f), enabled = false)
                                    }
                                    OutlinedTextField(value = vin, onValueChange = { vin = it }, label = { Text("VIN") }, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = modelYear, onValueChange = { modelYear = it }, label = { Text("Year") }, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = color, onValueChange = { color = it }, label = { Text("Color") }, modifier = Modifier.weight(1f))
                                    }
                                    OutlinedTextField(value = regDate, onValueChange = { regDate = it }, label = { Text("Reg Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight") }, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = engineNo, onValueChange = { engineNo = it }, label = { Text("Engine No") }, modifier = Modifier.weight(1f))
                                    }
                                    OutlinedTextField(value = displacement, onValueChange = { displacement = it }, label = { Text("Displacement") }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                    3 -> { // Step 4: Review
                        Text("4. Review & Finalize", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Summary of Application", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider(color = Color(0xFFE0E0E0))
                                Text("Donee: ${if (doneeStatus == "new") doneeName else "ID: $selectedDoneeId"}", fontSize = 13.sp)
                                Text("Donor: ${if (donorStatus == "new") donorName else "ID: $selectedDonorId"}", fontSize = 13.sp)
                                if (fuelType == "G") {
                                    Text("Vehicles: ${motorVehicles.size} line(s) defined", fontSize = 13.sp)
                                } else {
                                    Text("Vehicle: Passenger Car ($passengerDesc)", fontSize = 13.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Authorized Signature:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedButton(onClick = { filePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Import Signature")
                                }
                                Text(signatureUriText, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("PREVIOUS")
                }
            }
            Button(
                onClick = {
                    if (currentStep < 3) {
                        currentStep++
                    } else {
                        // Submit logic
                        if (doneeStatus == "new" && doneeTel.isBlank() && doneeFax.isBlank() && doneeEmail.isBlank()) {
                            Toast.makeText(context, "Validation: Donee contact required.", Toast.LENGTH_LONG).show(); return@Button
                        }
                        val payload = mapOf(
                            "DoneeStatus" to doneeStatus, "ExistingDoneeID" to selectedDoneeId, "DoneeName" to doneeName, "DoneeAddress" to doneeAddr, "ContactPerson" to doneeContact, "DoneeTelNo" to doneeTel, "DoneeFaxNo" to doneeFax, "DoneeEmail" to doneeEmail,
                            "DonorStatus" to donorStatus, "ExistingDonorID" to selectedDonorId, "DonorName" to donorName, "DonorAddress" to donorAddr, "DonorTelNo" to donorTel, "DonorFaxNo" to donorFax, "DonorEmail" to donorEmail,
                            "PassengerDesc" to passengerDesc, "PassengerTariff" to passengerTariff, "PassengerOrigin" to passengerOrigin, "VIN" to vin, "YearModel" to modelYear, "Color" to color, "RegistrationDate" to regDate, "VehicleWeight" to weight, "EngineNumber" to engineNo, "EngineDisplacement" to displacement, "FuelType" to fuelType,
                            "DonorSignaturePath" to signatureUriText
                        )
                        scope.launch {
                            DatabaseService.submitVehicleApplication(payload, motorVehicles)
                                .onSuccess { id ->
                                    Toast.makeText(context, "Application submitted: $id", Toast.LENGTH_LONG).show()
                                    navController.navigate("history")
                                }
                                .onFailure { tx -> Toast.makeText(context, "Error: ${tx.message}", Toast.LENGTH_LONG).show() }
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (currentStep < 3) "CONTINUE" else "SUBMIT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryLogScreen(refreshTrigger: Long) {
    var logs by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var queryStr by remember { mutableStateOf("") }
    var vehicleTypeFilter by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf("Newest") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger) { 
        isLoading = true
        logs = DatabaseService.fetchHistory()
        isLoading = false
    }

    val filteredLogs = logs.filter { list -> 
        (vehicleTypeFilter == "All" || list[6] == vehicleTypeFilter) &&
        list.any { str -> str.contains(queryStr, ignoreCase = true) }
    }

    val computedLogs = if (sortOrder == "Newest") filteredLogs else filteredLogs.reversed()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Previous Applications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Review and filter submitted vehicle importation records.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
            // ... rest of the existing UI
        Column {
            Text("Previous Applications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Review and filter submitted vehicle importation records.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        OutlinedTextField(
            value = queryStr,
            onValueChange = { queryStr = it },
            placeholder = { Text("Search by ID, Donee, or VIN...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Simplified Filter UI (Using AssistChips as stand-ins for full dropdowns for brevity)
            FilterChip(
                selected = vehicleTypeFilter == "All",
                onClick = { vehicleTypeFilter = "All" },
                label = { Text("All") }
            )
            FilterChip(
                selected = vehicleTypeFilter == "Motor Vehicle",
                onClick = { vehicleTypeFilter = "Motor Vehicle" },
                label = { Text("Motor") }
            )
            FilterChip(
                selected = vehicleTypeFilter == "Passenger Car",
                onClick = { vehicleTypeFilter = "Passenger Car" },
                label = { Text("Passenger") }
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { sortOrder = if (sortOrder == "Newest") "Oldest" else "Newest" }) {
                Icon(if (sortOrder == "Newest") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = "Sort")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp))) {
            stickyHeader {
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F3F5)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("REF ID / DATE", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                    Text("DONEE", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                    Text("VEHICLE TYPE", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                }
                HorizontalDivider(color = Color(0xFFE0E0E0))
            }
            items(computedLogs) { line ->
                Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(line[0], fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text(line[1].split(" ")[0], fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(line[2], modifier = Modifier.weight(1.5f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(if (line[6] == "Passenger Car") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(line[6], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (line[6] == "Passenger Car") Color(0xFF2E7D32) else Color(0xFF1565C0))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Asset: ${line[5]} (Qty: ${line[7]})", fontSize = 11.sp, color = Color.Gray)
                    if (line[8] != "General Cargo") {
                        Text("VIN: ${line[8]}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

@Composable
fun SqlConsoleScreen() {
    var codeBufferText by remember { mutableStateOf("SELECT * FROM application ORDER BY ApplicationID DESC LIMIT 5;") }
    var consoleStreamLogResult by remember { mutableStateOf<RawSqlResult?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("Relational SQL Console", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Execute administrative queries against the cloud database instance.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        OutlinedTextField(
            value = codeBufferText,
            onValueChange = { codeBufferText = it },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF8F9FA),
                focusedContainerColor = Color(0xFFF8F9FA)
            )
        )

        Button(
            onClick = { scope.launch { consoleStreamLogResult = DatabaseService.executeRawSql(codeBufferText) } },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("EXECUTE QUERY", fontWeight = FontWeight.Bold)
        }

        Text("Output Viewport:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)

        Box(modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp)).background(Color.White).padding(8.dp)) {
            when (val output = consoleStreamLogResult) {
                is RawSqlResult.SelectSuccess -> {
                    val sideScroll = rememberScrollState()
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.horizontalScroll(sideScroll).background(Color(0xFFF1F3F5))) {
                            output.headers.forEach { h ->
                                Text(text = h, modifier = Modifier.width(150.dp).padding(8.dp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.DarkGray)
                            }
                        }
                        output.rows.forEach { row ->
                            Row(modifier = Modifier.horizontalScroll(sideScroll).border(0.5.dp, Color(0xFFF0F0F0))) {
                                row.forEach { cell ->
                                    Text(text = cell, modifier = Modifier.width(150.dp).padding(8.dp), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                is RawSqlResult.UpdateSuccess -> {
                    Text("Command executed successfully.\nAffected Rows: ${output.affectedRows}", color = Color(0xFF2E7D32), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                is RawSqlResult.Error -> {
                    Text("SQL Error:\n${output.message}", color = Color(0xFFD32F2F), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                null -> {
                    Text("Ready for query input...", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
    }
}
