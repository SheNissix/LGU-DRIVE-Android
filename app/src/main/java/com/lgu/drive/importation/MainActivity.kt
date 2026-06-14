package com.lgu.drive.importation

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "LGU-Drive Logo",
                        modifier = Modifier.size(80.dp)
                    )
                    Column {
                        Text(
                            "LGU-Drive",
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
                }
                Spacer(Modifier.height(32.dp))
                tabs.forEach { tab ->
                    NavigationDrawerItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, fontWeight = FontWeight.Medium) },
                        selected = currentRoute == tab.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = (tab.route != "form")
                                }
                                launchSingleTop = true
                                restoreState = (tab.route != "form")
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
                    "v2.5.1-PROTOTYPE",
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
                            "LGU-DRIVE",
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

@OptIn(ExperimentalMaterial3Api::class)
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

    var isMotorVehicleSelected by remember { mutableStateOf(false) }
    var isPassengerCarSelected by remember { mutableStateOf(false) }

    val motorVehicles = remember { mutableStateListOf(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1")) }
    val passengerCars = remember { mutableStateListOf(mutableMapOf(
        "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"
    )) }

    var signatureUriText by remember { mutableStateOf("No confirmation files selected") }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        signatureUriText = uri?.lastPathSegment?.let { "signatures/uploaded_signature_$it.png" } ?: "No verification file chosen"
    }

    LaunchedEffect(refreshTrigger) {
        val (dnr, dne) = DatabaseService.fetchDonorsAndDoneesDetailed()
        donors = dnr; donees = dne
    }

    fun clearFields() {
        doneeName = ""; doneeAddr = ""; doneeContact = ""; doneeTel = ""; doneeFax = ""; doneeEmail = ""; selectedDoneeId = ""; doneeStatus = "new"
        donorName = ""; donorAddr = ""; donorTel = ""; donorFax = ""; donorEmail = ""; selectedDonorId = ""; donorStatus = "new"
        isMotorVehicleSelected = false; isPassengerCarSelected = false
        motorVehicles.clear(); motorVehicles.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1"))
        passengerCars.clear(); passengerCars.add(mutableMapOf("vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
        signatureUriText = "No confirmation files selected"
        currentStep = 0
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val steps = listOf("Donee", "Donor", "Vehicles", "Review")
            steps.forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(if (index <= currentStep) MaterialTheme.colorScheme.primary else Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < currentStep) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        else Text(text = "${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = if (index <= currentStep) MaterialTheme.colorScheme.primary else Color.Gray)
                }
                if (index < steps.size - 1) {
                    HorizontalDivider(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), color = if (index < currentStep) MaterialTheme.colorScheme.primary else Color.LightGray)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (currentStep) {
                    0 -> { // Step 1: Donee
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("1. Donee / Consignee Profile", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            var searchExpanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { searchExpanded = true }) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("FIND EXISTING")
                                }
                                DropdownMenu(expanded = searchExpanded, onDismissRequest = { searchExpanded = false }) {
                                    donees.forEach { dne ->
                                        DropdownMenuItem(
                                            text = { Text("${dne["DoneeName"]} (${dne["DoneeID"]})") },
                                            onClick = {
                                                doneeStatus = "existing"; selectedDoneeId = dne["DoneeID"] ?: ""
                                                doneeName = dne["DoneeName"] ?: ""; doneeAddr = dne["DoneeAddress"] ?: ""
                                                doneeContact = dne["ContactPerson"] ?: ""; doneeTel = dne["DoneeTelNo"] ?: ""
                                                doneeFax = dne["DoneeFaxNo"] ?: ""; doneeEmail = dne["DoneeEmail"] ?: ""
                                                searchExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(value = doneeName, onValueChange = { doneeName = it; doneeStatus = "new" }, label = { Text("Donee Name *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = doneeAddr, onValueChange = { doneeAddr = it }, label = { Text("Address *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = doneeContact, onValueChange = { doneeContact = it }, label = { Text("Contact Person *") }, modifier = Modifier.fillMaxWidth())
                                Text("Contact Details (Choose at least one *):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                OutlinedTextField(value = doneeTel, onValueChange = { doneeTel = it }, label = { Text("Telephone") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = doneeFax, onValueChange = { doneeFax = it }, label = { Text("Fax Number") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = doneeEmail, onValueChange = { doneeEmail = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    1 -> { // Step 2: Donor
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("2. Donor / Supplier Profile", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            var searchExpanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { searchExpanded = true }) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("FIND EXISTING")
                                }
                                DropdownMenu(expanded = searchExpanded, onDismissRequest = { searchExpanded = false }) {
                                    donors.forEach { dnr ->
                                        DropdownMenuItem(
                                            text = { Text("${dnr["DonorName"]} (${dnr["DonorID"]})") },
                                            onClick = {
                                                donorStatus = "existing"; selectedDonorId = dnr["DonorID"] ?: ""
                                                donorName = dnr["DonorName"] ?: ""; donorAddr = dnr["DonorAddress"] ?: ""
                                                donorTel = dnr["DonorTelNo"] ?: ""; donorFax = dnr["DonorFaxNo"] ?: ""
                                                donorEmail = dnr["DonorEmail"] ?: ""
                                                searchExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(value = donorName, onValueChange = { donorName = it; donorStatus = "new" }, label = { Text("Donor Name *") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = donorAddr, onValueChange = { donorAddr = it }, label = { Text("Address *") }, modifier = Modifier.fillMaxWidth())
                                Text("Contact Details (Choose at least one *):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                OutlinedTextField(value = donorTel, onValueChange = { donorTel = it }, label = { Text("Telephone") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = donorFax, onValueChange = { donorFax = it }, label = { Text("Fax Number") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = donorEmail, onValueChange = { donorEmail = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    2 -> { // Step 3: Vehicles
                        Text("3. Vehicle Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Select Vehicle Classification(s):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isMotorVehicleSelected, onCheckedChange = { isMotorVehicleSelected = it })
                                        Text("Motor Vehicle", fontSize = 14.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isPassengerCarSelected, onCheckedChange = { isPassengerCarSelected = it })
                                        Text("Passenger Car", fontSize = 14.sp)
                                    }
                                }

                                if (isMotorVehicleSelected) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text("Motor Vehicles", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    motorVehicles.forEachIndexed { idx, item ->
                                        Text("Motor Vehicle Block #${idx + 1}:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        OutlinedTextField(value = item["desc"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("desc", it) } }, label = { Text("Vehicle Description *") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["tariffCode"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("tariffCode", it) } }, label = { Text("Vehicle Tariff Code *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["origin"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("origin", it) } }, label = { Text("Origin *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["qty"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("qty", it) } }, label = { Text("Quantity *") }, modifier = Modifier.weight(0.6f))
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    OutlinedButton(onClick = { motorVehicles.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1")) }, modifier = Modifier.align(Alignment.End)) { Text("+ Add Motor Vehicle") }
                                }

                                if (isPassengerCarSelected) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text("Passenger Cars", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    passengerCars.forEachIndexed { idx, item ->
                                        Text("Passenger Car Block #${idx + 1}:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        OutlinedTextField(value = item["vin"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("vin", it) } }, label = { Text("VIN *") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["year"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("year", it) } }, label = { Text("Year Model *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["color"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("color", it) } }, label = { Text("Color *") }, modifier = Modifier.weight(1f))
                                        }
                                        OutlinedTextField(value = item["regDate"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("regDate", it) } }, label = { Text("Registration Date (YYYY-MM-DD) *") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["weight"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("weight", it) } }, label = { Text("Vehicle Weight *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["engineNo"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("engineNo", it) } }, label = { Text("Engine Number *") }, modifier = Modifier.weight(1f))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["displacement"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("displacement", it) } }, label = { Text("Engine Displacement *") }, modifier = Modifier.weight(1f))
                                            var fuelExpanded by remember { mutableStateOf(false) }
                                            val fuelOptions = listOf("G - Gasoline", "E - Electric", "D - Diesel")
                                            val currentFuel = fuelOptions.find { it.startsWith(item["fuelType"] ?: "G") } ?: "G - Gasoline"
                                            ExposedDropdownMenuBox(expanded = fuelExpanded, onExpandedChange = { fuelExpanded = !fuelExpanded }, modifier = Modifier.weight(1f)) {
                                                OutlinedTextField(value = currentFuel, onValueChange = {}, readOnly = true, label = { Text("Fuel Type *") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                                                ExposedDropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }) {
                                                    fuelOptions.forEach { option ->
                                                        DropdownMenuItem(text = { Text(option) }, onClick = {
                                                            passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("fuelType", option.take(1)) }
                                                            fuelExpanded = false
                                                        })
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    OutlinedButton(onClick = { passengerCars.add(mutableMapOf("vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G")) }, modifier = Modifier.align(Alignment.End)) { Text("+ Add Passenger Car") }
                                }
                            }
                        }
                    }
                    3 -> { // Step 4: Review
                        Text("4. Review & Finalize", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)), border = BorderStroke(1.dp, Color(0xFFE0E0E0))) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Comprehensive Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider(color = Color(0xFFE0E0E0))

                                // Detailed Donee Summary
                                Text("Donee Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Name: ${doneeName.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Address: ${doneeAddr.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Contact Person: ${doneeContact.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Tel: ${doneeTel.ifBlank { "N/A" }} | Fax: ${doneeFax.ifBlank { "N/A" }} | Email: ${doneeEmail.ifBlank { "N/A" }}", fontSize = 12.sp)

                                Spacer(Modifier.height(4.dp))

                                // Detailed Donor Summary
                                Text("Donor Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Name: ${donorName.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Address: ${donorAddr.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Tel: ${donorTel.ifBlank { "N/A" }} | Fax: ${donorFax.ifBlank { "N/A" }} | Email: ${donorEmail.ifBlank { "N/A" }}", fontSize = 12.sp)

                                Spacer(Modifier.height(4.dp))

                                // Detailed Vehicle Summary
                                if (isMotorVehicleSelected && motorVehicles.isNotEmpty()) {
                                    Text("Motor Vehicles (${motorVehicles.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    motorVehicles.forEachIndexed { idx, mv ->
                                        Text("#${idx + 1}: ${mv["desc"]} (Tariff: ${mv["tariffCode"]}, Origin: ${mv["origin"]}, Qty: ${mv["qty"]})", fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                if (isPassengerCarSelected && passengerCars.isNotEmpty()) {
                                    Text("Passenger Cars (${passengerCars.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    passengerCars.forEachIndexed { idx, pc ->
                                        Text("#${idx + 1}: VIN: ${pc["vin"]} (Year: ${pc["year"]}, Color: ${pc["color"]}, Fuel: ${pc["fuelType"]})", fontSize = 12.sp)
                                        Text("       Reg: ${pc["regDate"]}, Wt: ${pc["weight"]}, Eng: ${pc["engineNo"]}, Displ: ${pc["displacement"]}", fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                Spacer(Modifier.height(8.dp))
                                Text("Authorized Signature:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedButton(onClick = { filePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Text("Import Signature") }
                                Text(signatureUriText, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentStep > 0) OutlinedButton(onClick = { currentStep-- }, modifier = Modifier.weight(0.6f).height(48.dp), shape = RoundedCornerShape(8.dp)) { Text("PREVIOUS") }
            Button(onClick = { clearFields() }, modifier = Modifier.weight(0.6f).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) { Text("CLEAR") }
            Button(
                onClick = {
                    if (currentStep == 0) {
                        if (doneeName.isBlank() || doneeAddr.isBlank() || doneeContact.isBlank()) { Toast.makeText(context, "Please fill in all required Donee fields.", Toast.LENGTH_SHORT).show(); return@Button }
                        if (doneeTel.isBlank() && doneeFax.isBlank() && doneeEmail.isBlank()) { Toast.makeText(context, "Please provide at least one Donee contact method.", Toast.LENGTH_SHORT).show(); return@Button }
                        currentStep++
                    } else if (currentStep == 1) {
                        if (donorName.isBlank() || donorAddr.isBlank()) { Toast.makeText(context, "Please fill in all required Donor fields.", Toast.LENGTH_SHORT).show(); return@Button }
                        if (donorTel.isBlank() && donorFax.isBlank() && donorEmail.isBlank()) { Toast.makeText(context, "Please provide at least one Donor contact method.", Toast.LENGTH_SHORT).show(); return@Button }
                        currentStep++
                    } else if (currentStep == 2) {
                        if (!isMotorVehicleSelected && !isPassengerCarSelected) { Toast.makeText(context, "Please select at least one vehicle classification.", Toast.LENGTH_SHORT).show(); return@Button }
                        if (isMotorVehicleSelected && motorVehicles.any { it.values.any { v -> v.isBlank() } }) { Toast.makeText(context, "Please fill in all fields for Motor Vehicles.", Toast.LENGTH_SHORT).show(); return@Button }
                        if (isPassengerCarSelected && passengerCars.any { it.values.any { v -> v.isBlank() } }) { Toast.makeText(context, "Please fill in all fields for Passenger Cars.", Toast.LENGTH_SHORT).show(); return@Button }
                        currentStep++
                    } else {
                        if (signatureUriText == "No confirmation files selected" || signatureUriText == "No verification file chosen") { Toast.makeText(context, "Please import an authorized signature.", Toast.LENGTH_SHORT).show(); return@Button }
                        val payload = mapOf(
                            "DoneeStatus" to doneeStatus, "ExistingDoneeID" to selectedDoneeId, "DoneeName" to doneeName, "DoneeAddress" to doneeAddr, "ContactPerson" to doneeContact, "DoneeTelNo" to doneeTel, "DoneeFaxNo" to doneeFax, "DoneeEmail" to doneeEmail,
                            "DonorStatus" to donorStatus, "ExistingDonorID" to selectedDonorId, "DonorName" to donorName, "DonorAddress" to donorAddr, "DonorTelNo" to donorTel, "DonorFaxNo" to donorFax, "DonorEmail" to donorEmail,
                            "IncludesMotorVehicles" to isMotorVehicleSelected.toString(), "IncludesPassengerCars" to isPassengerCarSelected.toString(), "DonorSignaturePath" to signatureUriText
                        )
                        scope.launch {
                            DatabaseService.submitVehicleApplication(payload, if(isMotorVehicleSelected) motorVehicles else emptyList(), if(isPassengerCarSelected) passengerCars else emptyList())
                                .onSuccess { id -> Toast.makeText(context, "Submitted!", Toast.LENGTH_LONG).show(); clearFields(); navController.navigate("history") { popUpTo("form") { inclusive = true } } }
                                .onFailure { tx -> Toast.makeText(context, "Error: ${tx.message}", Toast.LENGTH_LONG).show() }
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (currentStep < 3) "CONTINUE" else "SUBMIT", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryLogScreen(refreshTrigger: Long) {
    var logs by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var donorsDetailed by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var doneesDetailed by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var motorVehicles by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var passengerCars by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }

    var viewCategory by remember { mutableStateOf("Application") }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf("Descending") }

    var isLoading by remember { mutableStateOf(false) }
    var selectedLogForDetail by remember { mutableStateOf<List<String>?>(null) }
    var selectedEntityForDetail by remember { mutableStateOf<Map<String, String>?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        logs = DatabaseService.fetchHistory()
        val (dnrs, dnes) = DatabaseService.fetchDonorsAndDoneesDetailed()
        donorsDetailed = dnrs; doneesDetailed = dnes

        // Use the newly bulletproofed backend fetch
        val (mv, pc) = DatabaseService.fetchVehiclesDetailed()
        motorVehicles = mv
        passengerCars = pc

        isLoading = false
    }

    fun <T> sortData(list: List<T>, keyExtractor: (T) -> String): List<T> =
        if (sortOrder == "Ascending") list.sortedBy { keyExtractor(it) } else list.sortedByDescending { keyExtractor(it) }

    val hierarchicalResults = remember(searchQuery, logs, donorsDetailed, doneesDetailed, motorVehicles, passengerCars, viewCategory, sortOrder) {
        if (searchQuery.isBlank()) return@remember emptyList<Any>()
        val matchedDonors = donorsDetailed.filter { it.values.any { v -> v.contains(searchQuery, ignoreCase = true) } }
        val matchedDonees = doneesDetailed.filter { it.values.any { v -> v.contains(searchQuery, ignoreCase = true) } }
        val matchedMotors = motorVehicles.filter { it.values.any { v -> v.contains(searchQuery, ignoreCase = true) } }
        val matchedPassengers = passengerCars.filter { it.values.any { v -> v.contains(searchQuery, ignoreCase = true) } }
        val matchedDonorIds = matchedDonors.map { it["DonorID"] }
        val matchedDoneeIds = matchedDonees.map { it["DoneeID"] }
        val matchedAppIdsFromVehicles = (matchedMotors + matchedPassengers).map { it["ApplicationID"] }.distinct()

        val tiedApps = logs.filter { app -> app[0].contains(searchQuery, ignoreCase = true) || matchedDonorIds.contains(app[8]) || matchedDoneeIds.contains(app[7]) || matchedAppIdsFromVehicles.contains(app[0]) }

        val results = mutableListOf<Any>()
        results.addAll(sortData(tiedApps) { it[0] })

        when (viewCategory) {
            "Donor" -> results.addAll(sortData(matchedDonors) { it["DonorName"] ?: "" })
            "Donee" -> results.addAll(sortData(matchedDonees) { it["DoneeName"] ?: "" })
            "Motor Vehicle" -> results.addAll(sortData(matchedMotors) { it["DonateID"] ?: "" })
            "Passenger Car" -> results.addAll(sortData(matchedPassengers) { it["VIN"] ?: "" })
            else -> {
                results.addAll(sortData(matchedDonors) { it["DonorName"] ?: "" })
                results.addAll(sortData(matchedDonees) { it["DoneeName"] ?: "" })
                results.addAll(sortData(matchedMotors) { it["DonateID"] ?: "" })
                results.addAll(sortData(matchedPassengers) { it["VIN"] ?: "" })
            }
        }
        results
    }

    if (selectedLogForDetail != null) {
        val log = selectedLogForDetail!!

        // Instant data filtering using the guaranteed "ApplicationID" keys
        val fastAppVehicleDetails = remember(log[0], motorVehicles, passengerCars) {
            motorVehicles.filter { it["ApplicationID"] == log[0] } + passengerCars.filter { it["ApplicationID"] == log[0] }
        }

        AlertDialog(
            onDismissRequest = { selectedLogForDetail = null },
            confirmButton = { TextButton(onClick = { selectedLogForDetail = null }) { Text("CLOSE") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = log[0] }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("DELETE APPLICATION") } },
            title = { Text("Application Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailRow("Application ID", log[0]); DetailRow("Date Submitted", log[1])
                        DetailRow("Donor Name", log[3]); DetailRow("Donee Name", log[2])
                    }
                    HorizontalDivider()
                    Text("Included Vehicles", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    if (fastAppVehicleDetails.isEmpty()) {
                        Text("No vehicles found attached to this application.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        fastAppVehicleDetails.forEachIndexed { index, vehicle ->
                            Card(shape = RoundedCornerShape(4.dp), border = BorderStroke(0.5.dp, Color.LightGray), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Vehicle #${index + 1}: ${vehicle["CarType"]}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    DetailRow("DonateID", vehicle["DonateID"] ?: "N/A")
                                    if (vehicle["CarType"] == "Passenger Car") {
                                        DetailRow("VIN", vehicle["VIN"] ?: "N/A"); DetailRow("Year", vehicle["YearModel"] ?: "N/A")
                                        DetailRow("Color", vehicle["Color"] ?: "N/A"); DetailRow("Fuel", vehicle["FuelType"] ?: "N/A")
                                    } else {
                                        DetailRow("Description", vehicle["VehicleDescription"] ?: "N/A")
                                        DetailRow("Vehicle Tariff", vehicle["TariffCode"] ?: "N/A")
                                        DetailRow("Quantity", vehicle["Quantity"] ?: "1")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (selectedEntityForDetail != null) {
        AlertDialog(
            onDismissRequest = { selectedEntityForDetail = null },
            confirmButton = { TextButton(onClick = { selectedEntityForDetail = null }) { Text("CLOSE") } },
            title = { Text("More Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    selectedEntityForDetail!!.forEach { (key, value) -> DetailRow(key, value) }
                }
            }
        )
    }

    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete application ${showDeleteConfirmation}?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = showDeleteConfirmation!!
                    showDeleteConfirmation = null; selectedLogForDetail = null
                    scope.launch {
                        val res = snackbarHostState.showSnackbar("Deleting application $id...", "UNDO", duration = SnackbarDuration.Short)
                        if (res != SnackbarResult.ActionPerformed) {
                            DatabaseService.deleteApplication(id).onSuccess {
                                Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show()
                                logs = DatabaseService.fetchHistory()
                            }
                        }
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("DELETE") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = null }) { Text("CANCEL") } }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Previous Applications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Review and manage importation records.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }

            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search all data fields (Email, VIN, Tariff...)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), leadingIcon = { Icon(Icons.Default.Search, null) })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var filterExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(onClick = { filterExpanded = true }, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text(viewCategory)
                    }
                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                        listOf("Application", "Donor", "Donee", "Motor Vehicle", "Passenger Car").forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { viewCategory = cat; filterExpanded = false })
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { sortOrder = if (sortOrder == "Descending") "Ascending" else "Descending" }) {
                    Icon(if (sortOrder == "Descending") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null)
                }
            }

            val sideScroll = rememberScrollState()
            val useScroll = viewCategory == "Donor" || viewCategory == "Donee" || viewCategory == "Passenger Car" || viewCategory == "Motor Vehicle"

            Box(modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp)).then(if (useScroll) Modifier.horizontalScroll(sideScroll) else Modifier)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxHeight()) {
                    val finalDataList: List<Any> = if (searchQuery.isNotBlank()) hierarchicalResults else {
                        when (viewCategory) {
                            "Application" -> sortData(logs) { it[0] }
                            "Donor" -> sortData(donorsDetailed) { it["DonorID"] ?: "" }
                            "Donee" -> sortData(doneesDetailed) { it["DoneeID"] ?: "" }
                            "Motor Vehicle" -> sortData(motorVehicles) { it["DonateID"] ?: "" }
                            "Passenger Car" -> sortData(passengerCars) { it["VIN"] ?: "" }
                            else -> emptyList()
                        }
                    }

                    if (finalDataList.isEmpty()) {
                        item { Box(modifier = Modifier.fillParentMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text("No results.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = TextAlign.Center) } }
                    } else {
                        stickyHeader {
                            val headers = when (viewCategory) {
                                "Application" -> listOf("APPLICATION ID / DATE", "DONOR / DONEE", "VEHICLE TYPE")
                                "Donor" -> listOf("DONOR ID", "NAME", "ADDRESS", "TELEPHONE", "FAX", "EMAIL")
                                "Donee" -> listOf("DONEE ID", "NAME", "ADDRESS", "CONTACT PERSON", "TELEPHONE", "FAX", "EMAIL")
                                "Motor Vehicle" -> listOf("DONATE ID", "DESCRIPTION", "VEHICLE TARIFF", "ORIGIN", "QUANTITY", "APP ID")
                                "Passenger Car" -> listOf("VIN", "DONATE ID", "YEAR MODEL", "COLOR", "REG. DATE", "WEIGHT", "ENGINE NO", "DISPL.", "FUEL")
                                else -> listOf("ID", "NAME", "TYPE")
                            }
                            Row(modifier = Modifier.then(if (useScroll) Modifier.width(2200.dp) else Modifier.fillMaxWidth()).background(Color(0xFFF1F3F5)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                headers.forEachIndexed { idx, h ->
                                    val rowModifier = when (viewCategory) {
                                        "Application" -> when(idx) { 0 -> Modifier.weight(1.5f); 1 -> Modifier.weight(2f); 2 -> Modifier.weight(1.5f); else -> Modifier.width(150.dp) }
                                        "Motor Vehicle" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2.5f); 2 -> Modifier.weight(1.2f); 3 -> Modifier.weight(1.2f); 4 -> Modifier.weight(1f); 5 -> Modifier.weight(1.2f); else -> Modifier.width(150.dp) }
                                        "Passenger Car" -> when(idx) { 0 -> Modifier.weight(1.5f); 1 -> Modifier.weight(1.2f); 2 -> Modifier.weight(1f); 3 -> Modifier.weight(1f); 4 -> Modifier.weight(1.2f); 5 -> Modifier.weight(1f); 6 -> Modifier.weight(1.2f); 7 -> Modifier.weight(1f); 8 -> Modifier.weight(0.8f); else -> Modifier.width(150.dp) }
                                        else -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2.5f); 2 -> Modifier.weight(2.5f); 3 -> Modifier.weight(1.8f); 4 -> Modifier.weight(1.5f); 5 -> Modifier.weight(1.5f); 6 -> Modifier.weight(2f); else -> Modifier.width(150.dp) }
                                    }
                                    Text(h, modifier = rowModifier, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray, textAlign = if(h == "DESCRIPTION") TextAlign.Center else TextAlign.Start)
                                }
                                if (viewCategory == "Application" || viewCategory == "Passenger Car" || viewCategory == "Motor Vehicle") Spacer(modifier = Modifier.width(48.dp))
                            }
                            HorizontalDivider(color = Color(0xFFE0E0E0))
                        }

                        items(finalDataList) { item ->
                            var menuExpanded by remember { mutableStateOf(false) }
                            Column(modifier = Modifier.then(if (useScroll) Modifier.width(2200.dp) else Modifier.fillMaxWidth()).background(Color.White).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (item) {
                                        is List<*> -> {
                                            val log = item as List<String>

                                            // Blue for Mixed, Green for Passenger, Orange for Motor
                                            val isMixed = log[6] == "Motor Vehicle / Passenger Car"
                                            val isPassenger = log[6] == "Passenger Car"
                                            val badgeBgColor = if (isMixed) Color(0xFFE3F2FD) else if (isPassenger) Color(0xFFE8F5E9) else Color(0xFFFFE0B2)
                                            val badgeTextColor = if (isMixed) Color(0xFF1565C0) else if (isPassenger) Color(0xFF2E7D32) else Color(0xFFE65100)

                                            Column(modifier = Modifier.weight(1.5f)) { HighlightedText(log[0], searchQuery, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)); Text(log[1].split(" ")[0], fontSize = 10.sp, color = Color.Gray) }
                                            Column(modifier = Modifier.weight(2f)) { HighlightedText(log[3], searchQuery, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)); HighlightedText(log[2], searchQuery, style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Light)) }
                                            Box(modifier = Modifier.weight(1.5f).clip(RoundedCornerShape(4.dp)).background(badgeBgColor).padding(horizontal = 6.dp, vertical = 2.dp), contentAlignment = Alignment.CenterStart) { Text(log[6], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeTextColor) }
                                            Box { IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, null) }; DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { DropdownMenuItem(text = { Text("More Details") }, onClick = { selectedLogForDetail = log; menuExpanded = false }) } }
                                        }
                                        is Map<*, *> -> {
                                            val map = item as Map<String, String>
                                            val viewFields = when {
                                                map.containsKey("DonorID") -> listOf(map["DonorID"], map["DonorName"], map["DonorAddress"], map["DonorTelNo"], map["DonorFaxNo"], map["DonorEmail"])
                                                map.containsKey("DoneeID") -> listOf(map["DoneeID"], map["DoneeName"], map["DoneeAddress"], map["ContactPerson"], map["DoneeTelNo"], map["DoneeFaxNo"], map["DoneeEmail"])
                                                map.containsKey("VIN") -> listOf(map["VIN"], map["DonateID"], map["YearModel"], map["Color"], map["RegistrationDate"], map["VehicleWeight"], map["EngineNumber"], map["EngineDisplacement"], map["FuelType"])
                                                map.containsKey("DonateID") -> listOf(map["DonateID"], map["VehicleDescription"], map["TariffCode"], map["Origin"], map["Quantity"], map["ApplicationID"])
                                                else -> listOf("ERR: Unmapped Data")
                                            }
                                            viewFields.forEachIndexed { idx, value ->
                                                val cellModifier = when (viewCategory) {
                                                    "Motor Vehicle" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2.5f); 2 -> Modifier.weight(1.2f); 3 -> Modifier.weight(1.2f); 4 -> Modifier.weight(1f); 5 -> Modifier.weight(1.2f); else -> Modifier.width(150.dp) }
                                                    "Passenger Car" -> when(idx) { 0 -> Modifier.weight(1.5f); 1 -> Modifier.weight(1.2f); 2 -> Modifier.weight(1f); 3 -> Modifier.weight(1f); 4 -> Modifier.weight(1.2f); 5 -> Modifier.weight(1f); 6 -> Modifier.weight(1.2f); 7 -> Modifier.weight(1f); 8 -> Modifier.weight(0.8f); else -> Modifier.width(150.dp) }
                                                    else -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2.5f); 2 -> Modifier.weight(2.5f); 3 -> Modifier.weight(1.8f); 4 -> Modifier.weight(1.5f); 5 -> Modifier.weight(1.5f); 6 -> Modifier.weight(2f); else -> Modifier.width(150.dp) }
                                                }
                                                val isDescription = (viewCategory == "Motor Vehicle" && idx == 1)
                                                Box(modifier = cellModifier, contentAlignment = if (isDescription) Alignment.Center else Alignment.TopStart) {
                                                    HighlightedText(text = value ?: "", query = searchQuery, style = if(idx == 0) TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) else TextStyle(fontSize = 12.sp), textAlign = if(isDescription) TextAlign.Center else TextAlign.Start)
                                                }
                                            }
                                            if (map.containsKey("VIN")) { Box { IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, null) }; DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { DropdownMenuItem(text = { Text("More Details") }, onClick = { selectedEntityForDetail = map; menuExpanded = false }) } } }
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color(0xFFF0F0F0))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightedText(text: String, query: String, style: TextStyle, textAlign: TextAlign = TextAlign.Start) {
    if (query.isBlank()) { Text(text = text, style = style, textAlign = textAlign); return }
    val annotatedString = buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = text.indexOf(query, start, ignoreCase = true)
            if (index == -1) { append(text.substring(start)); break }
            append(text.substring(start, index))
            withStyle(SpanStyle(background = Color.Yellow.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)) { append(text.substring(index, index + query.length)) }
            start = index + query.length
        }
    }
    Text(text = annotatedString, style = style, textAlign = textAlign)
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
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
        OutlinedTextField(value = codeBufferText, onValueChange = { codeBufferText = it }, modifier = Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(8.dp), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp), colors = TextFieldDefaults.colors(unfocusedContainerColor = Color(0xFFF8F9FA), focusedContainerColor = Color(0xFFF8F9FA)))
        Button(onClick = { scope.launch { consoleStreamLogResult = DatabaseService.executeRawSql(codeBufferText) } }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("EXECUTE QUERY", fontWeight = FontWeight.Bold) }
        Text("Output Viewport:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
        Box(modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp)).background(Color.White).padding(8.dp)) {
            when (val output = consoleStreamLogResult) {
                is RawSqlResult.SelectSuccess -> {
                    val sideScroll = rememberScrollState()
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.horizontalScroll(sideScroll).background(Color(0xFFF1F3F5))) { output.headers.forEach { h -> Text(text = h, modifier = Modifier.width(150.dp).padding(8.dp), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.DarkGray) } }
                        output.rows.forEach { row -> Row(modifier = Modifier.horizontalScroll(sideScroll).border(0.5.dp, Color(0xFFF0F0F0))) { row.forEach { cell -> Text(text = cell, modifier = Modifier.width(150.dp).padding(8.dp), fontFamily = FontFamily.Monospace, fontSize = 10.sp) } } }
                    }
                }
                is RawSqlResult.UpdateSuccess -> Text("Command executed successfully.\nAffected Rows: ${output.affectedRows}", color = Color(0xFF2E7D32), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                is RawSqlResult.Error -> Text("SQL Error:\n${output.message}", color = Color(0xFFD32F2F), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                null -> Text("Ready for query input...", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}