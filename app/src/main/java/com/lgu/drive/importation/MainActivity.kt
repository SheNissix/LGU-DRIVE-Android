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
import androidx.compose.ui.text.AnnotatedString
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
import kotlinx.coroutines.delay
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
                    "v2.4.1-PROTOTYPE",
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

    // Checkboxes for Vehicle Types
    var isMotorVehicleSelected by remember { mutableStateOf(false) }
    var isPassengerCarSelected by remember { mutableStateOf(false) }

    // Dynamic Lists for Multiple Vehicles
    val motorVehicles = remember { mutableStateListOf(mutableMapOf("desc" to "", "tariff" to "", "origin" to "", "qty" to "1")) }
    val passengerCars = remember { mutableStateListOf(mutableMapOf(
        "desc" to "", "tariff" to "", "origin" to "", "vin" to "", "year" to "", "color" to "",
        "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"
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
        motorVehicles.clear(); motorVehicles.add(mutableMapOf("desc" to "", "tariff" to "", "origin" to "", "qty" to "1"))
        passengerCars.clear(); passengerCars.add(mutableMapOf("desc" to "", "tariff" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
        signatureUriText = "No confirmation files selected"
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Step Indicator
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val steps = listOf("Donee", "Donor", "Vehicles", "Review")
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
                                        OutlinedTextField(value = item["desc"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("desc", it) } }, label = { Text("Description *") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["tariff"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("tariff", it) } }, label = { Text("Tariff *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["origin"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("origin", it) } }, label = { Text("Origin *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["qty"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("qty", it) } }, label = { Text("Qty *") }, modifier = Modifier.weight(0.6f))
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    OutlinedButton(onClick = { motorVehicles.add(mutableMapOf("desc" to "", "tariff" to "", "origin" to "", "qty" to "1")) }, modifier = Modifier.align(Alignment.End)) {
                                        Text("+ Add Motor Vehicle")
                                    }
                                }

                                if (isPassengerCarSelected) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text("Passenger Cars", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    passengerCars.forEachIndexed { idx, item ->
                                        Text("Passenger Car Block #${idx + 1}:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        OutlinedTextField(value = item["desc"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("desc", it) } }, label = { Text("Car Description *") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["tariff"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("tariff", it) } }, label = { Text("Tariff Code *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["origin"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("origin", it) } }, label = { Text("Origin *") }, modifier = Modifier.weight(1f))
                                        }
                                        OutlinedTextField(value = item["vin"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("vin", it) } }, label = { Text("VIN *") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["year"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("year", it) } }, label = { Text("Year *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["color"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("color", it) } }, label = { Text("Color *") }, modifier = Modifier.weight(1f))
                                        }
                                        OutlinedTextField(value = item["regDate"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("regDate", it) } }, label = { Text("Reg Date (YYYY-MM-DD) *") }, modifier = Modifier.fillMaxWidth())
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["weight"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("weight", it) } }, label = { Text("Weight *") }, modifier = Modifier.weight(1f))
                                            OutlinedTextField(value = item["engineNo"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("engineNo", it) } }, label = { Text("Engine No *") }, modifier = Modifier.weight(1f))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = item["displacement"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("displacement", it) } }, label = { Text("Displacement *") }, modifier = Modifier.weight(1f))
                                            
                                            var fuelExpanded by remember { mutableStateOf(false) }
                                            val fuelOptions = listOf("G - Gasoline", "E - Electric", "D - Diesel")
                                            val currentFuel = fuelOptions.find { it.startsWith(item["fuelType"] ?: "G") } ?: "G - Gasoline"
                                            
                                            ExposedDropdownMenuBox(
                                                expanded = fuelExpanded,
                                                onExpandedChange = { fuelExpanded = !fuelExpanded },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                OutlinedTextField(
                                                    value = currentFuel,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Fuel Type *") },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = fuelExpanded,
                                                    onDismissRequest = { fuelExpanded = false }
                                                ) {
                                                    fuelOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option) },
                                                            onClick = {
                                                                passengerCars[idx] = passengerCars[idx].toMutableMap().apply { 
                                                                    put("fuelType", option.take(1)) 
                                                                }
                                                                fuelExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    OutlinedButton(onClick = {
                                        passengerCars.add(mutableMapOf("desc" to "", "tariff" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "Gasoline"))
                                    }, modifier = Modifier.align(Alignment.End)) {
                                        Text("+ Add Passenger Car")
                                    }
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
                                Text("Donee: ${doneeName.ifBlank { "Unspecified" }}", fontSize = 13.sp)
                                Text("Donor: ${donorName.ifBlank { "Unspecified" }}", fontSize = 13.sp)

                                if (isMotorVehicleSelected) {
                                    Text("Motor Vehicles: ${motorVehicles.size} defined", fontSize = 13.sp)
                                }
                                if (isPassengerCarSelected) {
                                    Text("Passenger Cars: ${passengerCars.size} defined", fontSize = 13.sp)
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
                    modifier = Modifier.weight(0.6f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("PREVIOUS")
                }
            }
            
            Button(
                onClick = { clearFields() },
                modifier = Modifier.weight(0.6f).height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)
            ) {
                Text("CLEAR")
            }

            Button(
                onClick = {
                    // VALIDATION LOGIC
                    if (currentStep == 0) {
                        if (doneeName.isBlank() || doneeAddr.isBlank() || doneeContact.isBlank()) {
                            Toast.makeText(context, "Please fill in all required Donee fields.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (doneeTel.isBlank() && doneeFax.isBlank() && doneeEmail.isBlank()) {
                            Toast.makeText(context, "Please provide at least one Donee contact method.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        currentStep++
                    }
                    else if (currentStep == 1) {
                        if (donorName.isBlank() || donorAddr.isBlank()) {
                            Toast.makeText(context, "Please fill in all required Donor fields.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (donorTel.isBlank() && donorFax.isBlank() && donorEmail.isBlank()) {
                            Toast.makeText(context, "Please provide at least one Donor contact method.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        currentStep++
                    }
                    else if (currentStep == 2) {
                        if (!isMotorVehicleSelected && !isPassengerCarSelected) {
                            Toast.makeText(context, "Please select at least one vehicle classification.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isMotorVehicleSelected) {
                            val hasEmpty = motorVehicles.any { it.values.any { v -> v.isBlank() } }
                            if (hasEmpty) {
                                Toast.makeText(context, "Please fill in all fields for your Motor Vehicles.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }
                        if (isPassengerCarSelected) {
                            val hasEmpty = passengerCars.any { it.values.any { v -> v.isBlank() } }
                            if (hasEmpty) {
                                Toast.makeText(context, "Please fill in all fields for your Passenger Cars.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                        }
                        currentStep++
                    }
                    else {
                        // VALIDATION: Signature is mandatory
                        if (signatureUriText == "No confirmation files selected" || signatureUriText == "No verification file chosen") {
                            Toast.makeText(context, "Please import an authorized signature before submitting.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Submit logic
                        val payload = mapOf(
                            "DoneeStatus" to doneeStatus, "ExistingDoneeID" to selectedDoneeId, "DoneeName" to doneeName, "DoneeAddress" to doneeAddr, "ContactPerson" to doneeContact, "DoneeTelNo" to doneeTel, "DoneeFaxNo" to doneeFax, "DoneeEmail" to doneeEmail,
                            "DonorStatus" to donorStatus, "ExistingDonorID" to selectedDonorId, "DonorName" to donorName, "DonorAddress" to donorAddr, "DonorTelNo" to donorTel, "DonorFaxNo" to donorFax, "DonorEmail" to donorEmail,
                            "IncludesMotorVehicles" to isMotorVehicleSelected.toString(),
                            "IncludesPassengerCars" to isPassengerCarSelected.toString(),
                            "DonorSignaturePath" to signatureUriText
                        )

                        scope.launch {
                            val passengerCarsCleaned = passengerCars.map { car ->
                                car.toMutableMap().apply {
                                    put("fuelType", (get("fuelType") ?: "G").take(1))
                                }
                            }
                            DatabaseService.submitVehicleApplication(payload, if(isMotorVehicleSelected) motorVehicles else emptyList(), if(isPassengerCarSelected) passengerCarsCleaned else emptyList())
                                .onSuccess { id ->
                                    Toast.makeText(context, "Application submitted: $id", Toast.LENGTH_LONG).show()
                                    clearFields() // Reset UI state immediately
                                    currentStep = 0 // Go back to start
                                    navController.navigate("history") {
                                        popUpTo("form") { inclusive = true }
                                    }
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
    var donorsDetailed by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var doneesDetailed by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var motorVehicles by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var passengerCars by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    
    var viewCategory by remember { mutableStateOf("ALL") }
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
        donorsDetailed = dnrs
        doneesDetailed = dnes
        val (mv, pc) = DatabaseService.fetchVehiclesDetailed()
        motorVehicles = mv
        passengerCars = pc
        isLoading = false
    }

    // Helper for universal sorting
    fun <T> sortData(list: List<T>, keyExtractor: (T) -> String): List<T> {
        return if (sortOrder == "Ascending") list.sortedBy { keyExtractor(it) } else list.sortedByDescending { keyExtractor(it) }
    }

    // Advanced Search Logic (Hierarchical and Category-Scoped)
    val hierarchicalResults = remember(searchQuery, logs, donorsDetailed, doneesDetailed, motorVehicles, passengerCars, viewCategory) {
        if (searchQuery.isBlank()) return@remember emptyList<Any>()
        
        val tiedApps = logs.filter { app ->
            app[0].contains(searchQuery, ignoreCase = true) || // App ID
            app[2].contains(searchQuery, ignoreCase = true) || // Donee Name
            app[3].contains(searchQuery, ignoreCase = true) || // Donor Name
            app[4].contains(searchQuery, ignoreCase = true) || // Asset ID
            app[8].contains(searchQuery, ignoreCase = true)    // VIN
        }

        val matchedDonors = donorsDetailed.filter { it["DonorID"]?.equals(searchQuery, ignoreCase = true) == true || it["DonorName"]?.contains(searchQuery, ignoreCase = true) == true }
        val matchedDonees = doneesDetailed.filter { it["DoneeID"]?.equals(searchQuery, ignoreCase = true) == true || it["DoneeName"]?.contains(searchQuery, ignoreCase = true) == true }
        val matchedMotor = motorVehicles.filter { it["DonateID"]?.equals(searchQuery, ignoreCase = true) == true }
        val matchedPassenger = passengerCars.filter { it["VIN"]?.equals(searchQuery, ignoreCase = true) == true }

        when (viewCategory) {
            "ALL" -> tiedApps + matchedDonors + matchedDonees + matchedMotor + matchedPassenger
            "Application" -> tiedApps
            "Donor" -> matchedDonors
            "Donee" -> matchedDonees
            "Motor Vehicle" -> matchedMotor
            "Passenger Car" -> matchedPassenger
            else -> emptyList()
        }
    }

    // Application Details Dialog
    if (selectedLogForDetail != null) {
        AlertDialog(
            onDismissRequest = { selectedLogForDetail = null },
            confirmButton = {
                TextButton(onClick = { selectedLogForDetail = null }) { Text("CLOSE") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = selectedLogForDetail!![0] },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("DELETE APPLICATION") }
            },
            title = { Text("Application Details", fontWeight = FontWeight.Bold) },
            text = {
                val log = selectedLogForDetail!!
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Application ID", log[0])
                    DetailRow("Date Submitted", log[1])
                    DetailRow("Donor Name", log[3])
                    DetailRow("Donee Name", log[2])
                    DetailRow("Asset ID", log[4])
                    DetailRow("Description", log[5])
                    DetailRow("Vehicle Type", log[6])
                    DetailRow("Quantity", log[7])
                    if (log[8] != "General Cargo") DetailRow("VIN", log[8])
                }
            }
        )
    }

    // Entity/Vehicle Details Dialog
    if (selectedEntityForDetail != null) {
        val entity = selectedEntityForDetail!!
        AlertDialog(
            onDismissRequest = { selectedEntityForDetail = null },
            confirmButton = {
                TextButton(onClick = { selectedEntityForDetail = null }) { Text("CLOSE") }
            },
            title = { Text("More Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    entity.forEach { (key, value) ->
                        DetailRow(key, value)
                    }
                }
            }
        )
    }

    if (showDeleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete application ${showDeleteConfirmation}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idToDelete = showDeleteConfirmation!!
                        showDeleteConfirmation = null
                        selectedLogForDetail = null
                        
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Deleting application $idToDelete...",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                Toast.makeText(context, "Deletion cancelled.", Toast.LENGTH_SHORT).show()
                            } else {
                                DatabaseService.deleteApplication(idToDelete)
                                    .onSuccess {
                                        Toast.makeText(context, "Application deleted.", Toast.LENGTH_SHORT).show()
                                        logs = DatabaseService.fetchHistory() 
                                    }
                                    .onFailure { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show() }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) { Text("CANCEL") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Previous Applications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Review and manage importation records.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            // UX: Search Bar (Hierarchical)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search ID, Donor, or Donee...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) } }
            )

            // Filters Section
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var filterExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { filterExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(viewCategory)
                    }
                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                        listOf("ALL", "Application", "Donor", "Donee", "Motor Vehicle", "Passenger Car").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { viewCategory = cat; filterExpanded = false }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.weight(1f))
                
                // Sort Toggle
                IconButton(onClick = { sortOrder = if (sortOrder == "Descending") "Ascending" else "Descending" }) {
                    Icon(if (sortOrder == "Descending") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, contentDescription = "Sort")
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp))) {
                val finalDataList: List<Any> = if (searchQuery.isNotBlank()) {
                    hierarchicalResults
                } else {
                    when (viewCategory) {
                        "ALL" -> {
                            val all = mutableListOf<Any>()
                            all.addAll(sortData(logs) { it[0] })
                            all.addAll(sortData(donorsDetailed) { it["DonorID"] ?: "" })
                            all.addAll(sortData(doneesDetailed) { it["DoneeID"] ?: "" })
                            all.addAll(sortData(motorVehicles) { it["DonateID"] ?: "" })
                            all.addAll(sortData(passengerCars) { it["VIN"] ?: "" })
                            all
                        }
                        "Application" -> sortData(logs) { it[0] }
                        "Donor" -> sortData(donorsDetailed) { it["DonorID"] ?: "" }
                        "Donee" -> sortData(doneesDetailed) { it["DoneeID"] ?: "" }
                        "Motor Vehicle" -> sortData(motorVehicles) { it["DonateID"] ?: "" }
                        "Passenger Car" -> sortData(passengerCars) { it["VIN"] ?: "" }
                        else -> emptyList()
                    }
                }

                if (finalDataList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No results.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    stickyHeader {
                        val headers = when (viewCategory) {
                            "Application" -> listOf("REF ID / DATE", "DONOR/DONEE", "VEHICLE TYPE")
                            "Donor" -> listOf("DONOR ID", "NAME", "CONTACT")
                            "Donee" -> listOf("DONEE ID", "NAME", "PERSON")
                            "Motor Vehicle" -> listOf("ASSET ID", "DESCRIPTION", "ORIGIN")
                            "Passenger Car" -> listOf("VIN", "DESCRIPTION", "YEAR")
                            else -> listOf("ID / NAME", "DONOR / DONEE", "CATEGORY")
                        }
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F3F5)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(headers[0], modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                            Text(headers[1], modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                            Text(headers[2], modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.width(48.dp))
                        }
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                    }

                    items(finalDataList) { item ->
                        var menuExpanded by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (item) {
                                    is List<*> -> { // Application Row
                                        val log = item as List<String>
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            HighlightedText(log[0], searchQuery, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary))
                                            Text(log[1].split(" ")[0], fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            HighlightedText(log[3], searchQuery, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)) // Donor
                                            HighlightedText(log[2], searchQuery, style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Light)) // Donee (Thin)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (log[6] == "Passenger Car") Color(0xFFE8F5E9) else Color(0xFFFFE0B2)) // Orange-y background
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                log[6], 
                                                fontSize = 10.sp, 
                                                fontWeight = FontWeight.Bold, 
                                                color = if (log[6] == "Passenger Car") Color(0xFF2E7D32) else Color(0xFFE65100) // Deep orange text
                                            )
                                        }
                                        Box {
                                            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options") }
                                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                                DropdownMenuItem(text = { Text("More Details") }, onClick = { selectedLogForDetail = log; menuExpanded = false })
                                            }
                                        }
                                    }
                                    is Map<*, *> -> { // Entity Row
                                        val map = item as Map<String, String>
                                        val (id, name, summary) = when {
                                            map.containsKey("DonorID") -> listOf(map["DonorID"], map["DonorName"], map["DonorEmail"])
                                            map.containsKey("DoneeID") -> listOf(map["DoneeID"], map["DoneeName"], map["ContactPerson"])
                                            map.containsKey("DonateID") && map["CarType"] == "Motor Vehicle" -> listOf(map["DonateID"], map["VehicleDescription"], map["Origin"])
                                            map.containsKey("VIN") -> listOf(map["VIN"], map["VehicleDescription"], map["YearModel"])
                                            else -> listOf("N/A", "N/A", "N/A")
                                        }
                                        Box(modifier = Modifier.weight(1.2f)) {
                                            HighlightedText(id ?: "", searchQuery, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary))
                                        }
                                        Box(modifier = Modifier.weight(1.5f)) {
                                            HighlightedText(name ?: "", searchQuery, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium))
                                        }
                                        Box(modifier = Modifier.weight(1f)) {
                                            HighlightedText(summary ?: "", searchQuery, style = TextStyle(fontSize = 11.sp, color = Color.Gray))
                                        }
                                        Box {
                                            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Options") }
                                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                                DropdownMenuItem(text = { Text("More Details") }, onClick = { selectedEntityForDetail = map; menuExpanded = false })
                                            }
                                        }
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

@Composable
fun HighlightedText(text: String, query: String, style: TextStyle) {
    if (query.isBlank()) {
        Text(text = text, style = style)
        return
    }

    val annotatedString = buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = text.indexOf(query, start, ignoreCase = true)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(style = SpanStyle(background = Color.Yellow.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
        }
    }
    Text(text = annotatedString, style = style)
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
