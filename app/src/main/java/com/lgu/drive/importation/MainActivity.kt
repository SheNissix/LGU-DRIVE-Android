package com.lgu.drive.importation

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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

// Utility to space out database keys
fun formatDatabaseKey(key: String): String {
    return key.replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace("ID", " ID")
        .replace("  ", " ")
        .trim()
}

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
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("/")
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
                        selected = currentRoute == tab.route.substringBefore("/"),
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
                    "v3.2.0-PROTOTYPE",
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
                            fontSize = 16.sp,
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
            Box(modifier = Modifier.padding(innerPadding).background(Color(0xFFF8F9FA))) {
                NavHost(navController = navController, startDestination = "form") {
                    composable("form") { VehicleApplicationForm(navController, null, refreshTrigger) }
                    composable("edit/{appId}") { backStackEntry ->
                        val appId = backStackEntry.arguments?.getString("appId")
                        VehicleApplicationForm(navController, appId, refreshTrigger)
                    }
                    composable("history") { HistoryLogScreen(navController, refreshTrigger) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleApplicationForm(navController: NavHostController, appId: String?, refreshTrigger: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(0) }
    var isDataLoaded by remember { mutableStateOf(appId == null) }

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

    val motorVehicles = remember { mutableStateListOf(mutableMapOf("DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1")) }
    val passengerCars = remember { mutableStateListOf(mutableMapOf(
        "DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"
    )) }

    var signatureUriText by remember { mutableStateOf("No confirmation files selected") }
    var actualFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            actualFileUri = uri
            signatureUriText = uri.lastPathSegment?.let { "signatures/uploaded_signature_$it.png" } ?: "Verification file chosen"
        } else {
            signatureUriText = "No confirmation files selected"
            actualFileUri = null
        }
    }

    LaunchedEffect(refreshTrigger, appId) {
        val (dnr, dne) = DatabaseService.fetchDonorsAndDoneesDetailed()
        donors = dnr; donees = dne

        if (appId != null && !isDataLoaded) {
            val logs = DatabaseService.fetchHistory()
            val targetLog = logs.find { it[0] == appId }

            if (targetLog != null) {
                // Populate Donee
                val dId = targetLog[7]
                val existingDonee = donees.find { it["DoneeID"] == dId }
                if (existingDonee != null) {
                    doneeStatus = "existing"
                    selectedDoneeId = dId
                    doneeName = existingDonee["DoneeName"] ?: ""
                    doneeAddr = existingDonee["DoneeAddress"] ?: ""
                    doneeContact = existingDonee["ContactPerson"] ?: ""
                    doneeTel = existingDonee["DoneeTelNo"] ?: ""
                    doneeFax = existingDonee["DoneeFaxNo"] ?: ""
                    doneeEmail = existingDonee["DoneeEmail"] ?: ""
                }

                // Populate Donor
                val donorId = targetLog[8]
                val existingDonor = donors.find { it["DonorID"] == donorId }
                if (existingDonor != null) {
                    donorStatus = "existing"
                    selectedDonorId = donorId
                    donorName = existingDonor["DonorName"] ?: ""
                    donorAddr = existingDonor["DonorAddress"] ?: ""
                    donorTel = existingDonor["DonorTelNo"] ?: ""
                    donorFax = existingDonor["DonorFaxNo"] ?: ""
                    donorEmail = existingDonor["DonorEmail"] ?: ""
                }

                // Populate Vehicles
                val (mvs, pcs) = DatabaseService.fetchVehiclesDetailed()

                val appMVs = mvs.filter { it["ApplicationID"] == appId }
                if (appMVs.isNotEmpty()) {
                    isMotorVehicleSelected = true
                    motorVehicles.clear()
                    appMVs.forEach { mv ->
                        motorVehicles.add(mutableMapOf(
                            "DonateID" to (mv["DonateID"] ?: ""),
                            "desc" to (mv["VehicleDescription"] ?: ""),
                            "tariffCode" to (mv["TariffCode"] ?: ""),
                            "origin" to (mv["Origin"] ?: ""),
                            "qty" to (mv["Quantity"] ?: "")
                        ))
                    }
                }

                val appPCs = pcs.filter { it["ApplicationID"] == appId }
                if (appPCs.isNotEmpty()) {
                    isPassengerCarSelected = true
                    passengerCars.clear()
                    appPCs.forEach { pc ->
                        passengerCars.add(mutableMapOf(
                            "DonateID" to (pc["DonateID"] ?: ""),
                            "desc" to (pc["VehicleDescription"] ?: ""),
                            "tariffCode" to (pc["TariffCode"] ?: ""),
                            "origin" to (pc["Origin"] ?: ""),
                            "vin" to (pc["VIN"] ?: ""),
                            "year" to (pc["YearModel"] ?: ""),
                            "color" to (pc["Color"] ?: ""),
                            "regDate" to (pc["RegistrationDate"] ?: ""),
                            "weight" to (pc["VehicleWeight"] ?: ""),
                            "engineNo" to (pc["EngineNumber"] ?: ""),
                            "displacement" to (pc["EngineDisplacement"] ?: ""),
                            "fuelType" to (pc["FuelType"] ?: "G")
                        ))
                    }
                }
            }
            isDataLoaded = true
        }
    }

    fun clearFieldsForCurrentSection() {
        when (currentStep) {
            0 -> {
                doneeName = ""; doneeAddr = ""; doneeContact = ""; doneeTel = ""; doneeFax = ""; doneeEmail = ""
                selectedDoneeId = ""; doneeStatus = "new"
            }
            1 -> {
                donorName = ""; donorAddr = ""; donorTel = ""; donorFax = ""; donorEmail = ""
                selectedDonorId = ""; donorStatus = "new"
            }
            2 -> {
                isMotorVehicleSelected = false; isPassengerCarSelected = false
                motorVehicles.clear()
                motorVehicles.add(mutableMapOf("DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1"))
                passengerCars.clear()
                passengerCars.add(mutableMapOf("DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
            }
            3 -> {
                signatureUriText = "No confirmation files selected"
                actualFileUri = null
            }
        }
    }

    fun globalResetAllFields() {
        doneeName = ""; doneeAddr = ""; doneeContact = ""; doneeTel = ""; doneeFax = ""; doneeEmail = ""; selectedDoneeId = ""; doneeStatus = "new"
        donorName = ""; donorAddr = ""; donorTel = ""; donorFax = ""; donorEmail = ""; selectedDonorId = ""; donorStatus = "new"
        isMotorVehicleSelected = false; isPassengerCarSelected = false
        motorVehicles.clear(); motorVehicles.add(mutableMapOf("DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1"))
        passengerCars.clear(); passengerCars.add(mutableMapOf("DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
        signatureUriText = "No confirmation files selected"
        actualFileUri = null
        currentStep = 0
    }

    if (!isDataLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (appId != null) {
            Text(
                "Editing Application: $appId",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

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
                        ElevatedCard(
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(value = doneeName, onValueChange = { doneeName = it; doneeStatus = "new" }, label = { Text("Donee Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                                OutlinedTextField(value = doneeAddr, onValueChange = { doneeAddr = it; doneeStatus = "new" }, label = { Text("Address *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                                OutlinedTextField(value = doneeContact, onValueChange = { doneeContact = it; doneeStatus = "new" }, label = { Text("Contact Person *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                                Text("Contact Details (Choose at least one *):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                OutlinedTextField(value = doneeTel, onValueChange = { doneeTel = it; doneeStatus = "new" }, label = { Text("Telephone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                                OutlinedTextField(value = doneeFax, onValueChange = { doneeFax = it; doneeStatus = "new" }, label = { Text("Fax Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                                OutlinedTextField(value = doneeEmail, onValueChange = { doneeEmail = it; doneeStatus = "new" }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
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
                        ElevatedCard(
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(value = donorName, onValueChange = { donorName = it; donorStatus = "new" }, label = { Text("Donor Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                                OutlinedTextField(value = donorAddr, onValueChange = { donorAddr = it; donorStatus = "new" }, label = { Text("Address *") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                                Text("Contact Details (Choose at least one *):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                OutlinedTextField(value = donorTel, onValueChange = { donorTel = it; donorStatus = "new" }, label = { Text("Telephone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                                OutlinedTextField(value = donorFax, onValueChange = { donorFax = it; donorStatus = "new" }, label = { Text("Fax Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                                OutlinedTextField(value = donorEmail, onValueChange = { donorEmail = it; donorStatus = "new" }, label = { Text("Email Address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                            }
                        }
                    }
                    2 -> { // Step 3: Vehicles
                        val totalVehicles = (if (isMotorVehicleSelected) motorVehicles.size else 0) +
                                (if (isPassengerCarSelected) passengerCars.size else 0)

                        Text("3. Vehicle Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        ElevatedCard(
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Select Vehicle Classification(s):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isMotorVehicleSelected,
                                            onCheckedChange = { checked ->
                                                if (checked && totalVehicles + motorVehicles.size > 4) {
                                                    Toast.makeText(context, "Combined maximum of 4 vehicles reached.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    isMotorVehicleSelected = checked
                                                }
                                            }
                                        )
                                        Text("Motor Vehicle", fontSize = 14.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isPassengerCarSelected,
                                            onCheckedChange = { checked ->
                                                if (checked && totalVehicles + passengerCars.size > 4) {
                                                    Toast.makeText(context, "Combined maximum of 4 vehicles reached.", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    isPassengerCarSelected = checked
                                                }
                                            }
                                        )
                                        Text("Passenger Car", fontSize = 14.sp)
                                    }
                                }

                                if (isMotorVehicleSelected) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text("Motor Vehicles", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)

                                    motorVehicles.forEachIndexed { idx, item ->
                                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Block #${idx + 1}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                                                    IconButton(onClick = {
                                                        motorVehicles.removeAt(idx)
                                                        if (motorVehicles.isEmpty()) {
                                                            isMotorVehicleSelected = false
                                                            motorVehicles.add(mutableMapOf("DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1"))
                                                        }
                                                    }, modifier = Modifier.size(24.dp)) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                                    }
                                                }

                                                OutlinedTextField(value = item["desc"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("desc", it) } }, label = { Text("Vehicle Description (e.g., Freightliner) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))

                                                OutlinedTextField(
                                                    value = item["origin"] ?: "",
                                                    onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("origin", it) } },
                                                    label = { Text("Origin Country *") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(
                                                        value = item["tariffCode"] ?: "",
                                                        onValueChange = { newVal ->
                                                            val filtered = newVal.filter { it.isDigit() }.take(4)
                                                            motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("tariffCode", filtered) }
                                                        },
                                                        label = { Text("Tariff (4 Digits) *") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = item["qty"] ?: "",
                                                        onValueChange = { newVal ->
                                                            val filtered = newVal.filter { it.isDigit() }
                                                            motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("qty", filtered) }
                                                        },
                                                        label = { Text("Quantity *") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (totalVehicles < 4) {
                                        OutlinedButton(onClick = { motorVehicles.add(mutableMapOf("DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1")) }, modifier = Modifier.align(Alignment.End), shape = RoundedCornerShape(8.dp)) { Text("+ Add Motor Vehicle") }
                                    } else {
                                        Text(text = "Combined maximum of 4 vehicles reached.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End).padding(top = 8.dp))
                                    }
                                }

                                if (isPassengerCarSelected) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text("Passenger Car", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    passengerCars.forEachIndexed { idx, item ->
                                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                                    Text("Block #${idx + 1}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                                                    IconButton(onClick = {
                                                        passengerCars.removeAt(idx)
                                                        if (passengerCars.isEmpty()) {
                                                            isPassengerCarSelected = false
                                                            passengerCars.add(mutableMapOf("DonateID" to "", "desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
                                                        }
                                                    }, modifier = Modifier.size(24.dp)) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                                                    }
                                                }

                                                OutlinedTextField(value = item["desc"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("desc", it) } }, label = { Text("Vehicle Description (e.g., Toyota Camry) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                                                OutlinedTextField(value = item["vin"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("vin", it) } }, label = { Text("VIN *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))

                                                OutlinedTextField(
                                                    value = item["origin"] ?: "",
                                                    onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("origin", it) } },
                                                    label = { Text("Origin Country *") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(
                                                        value = item["tariffCode"] ?: "",
                                                        onValueChange = { newVal ->
                                                            val filtered = newVal.filter { it.isDigit() }.take(4)
                                                            passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("tariffCode", filtered) }
                                                        },
                                                        label = { Text("Tariff (4 Digits) *") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = item["year"] ?: "",
                                                        onValueChange = { newVal ->
                                                            val filtered = newVal.filter { it.isDigit() }.take(4)
                                                            passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("year", filtered) }
                                                        },
                                                        label = { Text("Year Model *") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(value = item["color"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("color", it) } }, label = { Text("Color *") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
                                                    OutlinedTextField(
                                                        value = item["weight"]?.replace(Regex("(?i)\\s*kg$"), "") ?: "",
                                                        onValueChange = {
                                                            val cleanWeight = it.replace(Regex("(?i)\\s*kg$"), "")
                                                            passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("weight", if (cleanWeight.isNotBlank()) "$cleanWeight kg" else "") }
                                                        },
                                                        label = { Text("Weight *") },
                                                        suffix = { Text("kg", color = Color.Gray) },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                }

                                                OutlinedTextField(value = item["regDate"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("regDate", it) } }, label = { Text("Registration Date (YYYY-MM-DD) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(value = item["engineNo"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("engineNo", it) } }, label = { Text("Engine Number *") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
                                                    OutlinedTextField(
                                                        value = item["displacement"]?.replace(Regex("(?i)\\s*cc$"), "") ?: "",
                                                        onValueChange = {
                                                            val cleanDisp = it.replace(Regex("(?i)\\s*cc$"), "")
                                                            passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("displacement", if (cleanDisp.isNotBlank()) "$cleanDisp cc" else "") }
                                                        },
                                                        label = { Text("Displacement *") },
                                                        suffix = { Text("cc", color = Color.Gray) },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                }

                                                var fuelExpanded by remember { mutableStateOf(false) }
                                                val fuelOptions = listOf("G: Gas" to "G", "D: Diesel" to "D", "LPG: Liquid Petroleum Gas" to "LPG", "E: Electric" to "E", "H: Hydrogen Fuel" to "H")
                                                val currentFuelCode = item["fuelType"] ?: "G"
                                                val currentFuelDisplay = fuelOptions.find { it.second == currentFuelCode }?.first ?: "Gas"

                                                ExposedDropdownMenuBox(
                                                    expanded = fuelExpanded,
                                                    onExpandedChange = { fuelExpanded = !fuelExpanded },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    OutlinedTextField(
                                                        value = currentFuelDisplay,
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        label = { Text("Fuel Type *") },
                                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    ExposedDropdownMenu(
                                                        expanded = fuelExpanded,
                                                        onDismissRequest = { fuelExpanded = false }
                                                    ) {
                                                        fuelOptions.forEach { (displayStr, dbCode) ->
                                                            DropdownMenuItem(
                                                                text = { Text(displayStr) },
                                                                onClick = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("fuelType", dbCode) }; fuelExpanded = false }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (totalVehicles < 4) {
                                        Text(text = "Maximum of 1 Passenger Car allowed per application.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End).padding(top = 8.dp))
                                    } else {
                                        Text(text = "Combined maximum of 4 vehicles reached.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End).padding(top = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Step 4: Review
                        Text("4. Review & Finalize", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        ElevatedCard(
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Comprehensive Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider(color = Color(0xFFE0E0E0))

                                Text("Donee Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Name: ${doneeName.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Address: ${doneeAddr.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Contact Person: ${doneeContact.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Tel: ${doneeTel.ifBlank { "N/A" }} | Fax: ${doneeFax.ifBlank { "N/A" }} | Email: ${doneeEmail.ifBlank { "N/A" }}", fontSize = 12.sp)

                                Spacer(Modifier.height(4.dp))

                                Text("Donor Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Name: ${donorName.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Address: ${donorAddr.ifBlank { "Unspecified" }}", fontSize = 12.sp)
                                Text("Tel: ${donorTel.ifBlank { "N/A" }} | Fax: ${donorFax.ifBlank { "N/A" }} | Email: ${donorEmail.ifBlank { "N/A" }}", fontSize = 12.sp)

                                Spacer(Modifier.height(4.dp))

                                if (isMotorVehicleSelected && motorVehicles.isNotEmpty()) {
                                    Text("Motor Vehicles (${motorVehicles.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    motorVehicles.forEachIndexed { idx, mv ->
                                        Text("#${idx + 1}: ${mv["desc"]} (Tariff: ${mv["tariffCode"]}, Origin: ${mv["origin"]}, Qty: ${mv["qty"]})", fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                if (isPassengerCarSelected && passengerCars.isNotEmpty()) {
                                    Text("Passenger Car (${passengerCars.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    passengerCars.forEachIndexed { idx, pc ->
                                        Text("#${idx + 1}: ${pc["desc"]} (Tariff: ${pc["tariffCode"]}, Origin: ${pc["origin"]})", fontSize = 12.sp)
                                        Text("    VIN: ${pc["vin"]} (Year: ${pc["year"]}, Color: ${pc["color"]}, Fuel: ${pc["fuelType"]})", fontSize = 12.sp)
                                        Text("    Reg: ${pc["regDate"]}, Wt: ${pc["weight"]}, Eng: ${pc["engineNo"]}, Displ: ${pc["displacement"]}", fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }

                                Spacer(Modifier.height(8.dp))
                                Text("Authorized Signature (Optional):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                OutlinedButton(onClick = { filePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Text("Import Signature") }
                                Text(signatureUriText, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentStep > 0) OutlinedButton(onClick = { currentStep-- }, modifier = Modifier.weight(0.6f).height(48.dp), shape = RoundedCornerShape(8.dp)) { Text("PREVIOUS") }

            if (currentStep < 3) {
                Button(onClick = { clearFieldsForCurrentSection() }, modifier = Modifier.weight(0.6f).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black)) { Text("CLEAR") }
            }

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

                        if (isMotorVehicleSelected) {
                            for ((idx, mv) in motorVehicles.withIndex()) {
                                if (mv["desc"].isNullOrBlank() || mv["tariffCode"].isNullOrBlank() || mv["origin"].isNullOrBlank() || mv["qty"].isNullOrBlank()) {
                                    Toast.makeText(context, "Motor Vehicle #${idx + 1} has missing required fields.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val tariff = mv["tariffCode"] ?: ""
                                if (tariff.length != 4) {
                                    Toast.makeText(context, "Motor Vehicle #${idx + 1} Tariff Code must be exactly 4 digits.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val qty = mv["qty"]?.toIntOrNull()
                                if (qty == null || qty <= 0) {
                                    Toast.makeText(context, "Motor Vehicle #${idx + 1} must have a valid quantity greater than 0.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }
                        }

                        if (isPassengerCarSelected) {
                            for ((idx, pc) in passengerCars.withIndex()) {
                                if (pc["desc"].isNullOrBlank() || pc["tariffCode"].isNullOrBlank() || pc["origin"].isNullOrBlank() ||
                                    pc["vin"].isNullOrBlank() || pc["year"].isNullOrBlank() || pc["color"].isNullOrBlank() ||
                                    pc["regDate"].isNullOrBlank() || pc["weight"].isNullOrBlank() || pc["engineNo"].isNullOrBlank() ||
                                    pc["displacement"].isNullOrBlank() || pc["fuelType"].isNullOrBlank()) {
                                    Toast.makeText(context, "Passenger Car #${idx + 1} has missing required fields.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val tariff = pc["tariffCode"] ?: ""
                                if (tariff.length != 4) {
                                    Toast.makeText(context, "Passenger Car #${idx + 1} Tariff Code must be exactly 4 digits.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val year = pc["year"]?.toIntOrNull()
                                if (year == null || year < 1900 || year > 2100) {
                                    Toast.makeText(context, "Passenger Car #${idx + 1} must have a valid 4-digit Year Model.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!pc["regDate"]!!.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                    Toast.makeText(context, "Passenger Car #${idx + 1} Registration Date must be in YYYY-MM-DD format.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }
                        }

                        val submittedTotal = (if (isMotorVehicleSelected) motorVehicles.size else 0) + (if (isPassengerCarSelected) passengerCars.size else 0)
                        if (submittedTotal > 4) {
                            Toast.makeText(context, "You cannot submit more than 4 combined vehicles.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        currentStep++
                    } else {
                        scope.launch {
                            try {
                                val localSavedPath = if (actualFileUri != null) {
                                    Toast.makeText(context, "Saving verification image locally...", Toast.LENGTH_SHORT).show()
                                    StorageService.saveSignatureLocally(context, actualFileUri!!)
                                } else {
                                    "NULL"
                                }

                                val payload = mapOf(
                                    "ApplicationID" to (appId ?: ""), // Sent to backend if updating
                                    "DoneeStatus" to doneeStatus, "ExistingDoneeID" to selectedDoneeId, "DoneeName" to doneeName, "DoneeAddress" to doneeAddr, "ContactPerson" to doneeContact, "DoneeTelNo" to doneeTel, "DoneeFaxNo" to doneeFax, "DoneeEmail" to doneeEmail,
                                    "DonorStatus" to donorStatus, "ExistingDonorID" to selectedDonorId, "DonorName" to donorName, "DonorAddress" to donorAddr, "DonorTelNo" to donorTel, "DonorFaxNo" to donorFax, "DonorEmail" to donorEmail,
                                    "IncludesMotorVehicles" to isMotorVehicleSelected.toString(), "IncludesPassengerCars" to isPassengerCarSelected.toString(),
                                    "DonorSignaturePath" to localSavedPath
                                )

                                if (appId == null) {
                                    DatabaseService.submitVehicleApplication(payload, if(isMotorVehicleSelected) motorVehicles else emptyList(), if(isPassengerCarSelected) passengerCars else emptyList())
                                        .onSuccess { id ->
                                            Toast.makeText(context, "Submitted successfully!", Toast.LENGTH_LONG).show()
                                            globalResetAllFields()
                                            navController.navigate("history") { popUpTo("form") { inclusive = true } }
                                        }
                                        .onFailure { tx -> Toast.makeText(context, "Database Error: ${tx.message}", Toast.LENGTH_LONG).show() }
                                } else {
                                    // Requires updateVehicleApplication configured in DatabaseService to handle edits using the provided ApplicationID
                                    DatabaseService.updateVehicleApplication(appId, payload, if(isMotorVehicleSelected) motorVehicles else emptyList(), if(isPassengerCarSelected) passengerCars else emptyList())
                                        .onSuccess {
                                            Toast.makeText(context, "Application updated successfully!", Toast.LENGTH_LONG).show()
                                            globalResetAllFields()
                                            navController.navigate("history") { popUpTo("form") { inclusive = true } }
                                        }
                                        .onFailure { tx -> Toast.makeText(context, "Update Error: ${tx.message}", Toast.LENGTH_LONG).show() }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Storage Save Failure: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (currentStep < 3) "CONTINUE" else if (appId != null) "UPDATE" else "SUBMIT", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryLogScreen(navController: NavHostController, refreshTrigger: Long) {
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
            "Passenger Car" -> results.addAll(sortData(matchedPassengers) { it["DonateID"] ?: "" })
            else -> {
                results.addAll(sortData(matchedDonors) { it["DonorName"] ?: "" })
                results.addAll(sortData(matchedDonees) { it["DoneeName"] ?: "" })
                results.addAll(sortData(matchedMotors) { it["DonateID"] ?: "" })
                results.addAll(sortData(matchedPassengers) { it["DonateID"] ?: "" })
            }
        }
        results
    }

    if (selectedLogForDetail != null) {
        val log = selectedLogForDetail!!

        val fastAppVehicleDetails = remember(log[0], motorVehicles, passengerCars) {
            val mvs = motorVehicles.filter { it["ApplicationID"] == log[0] }
            val pcs = passengerCars.filter { it["ApplicationID"] == log[0] }
            mvs + pcs
        }

        AlertDialog(
            onDismissRequest = { selectedLogForDetail = null },
            confirmButton = { TextButton(onClick = { selectedLogForDetail = null }) { Text("CLOSE") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = log[0] }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("DELETE APPLICATION") } },
            title = { Text("Application Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailRow("Application ID", log[0])
                        DetailRow("Date Submitted", log[1])
                        DetailRow("Donor Name", log[3])
                        DetailRow("Donee Name", log[2])

                        val signaturePath = log.getOrElse(9) { "" }
                        if (signaturePath.isNotBlank() && signaturePath != "NULL") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Signature File", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                                Text(
                                    text = "Saved Locally",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            Toast.makeText(context, "Stored at: $signaturePath", Toast.LENGTH_LONG).show()
                                        }
                                )
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Signature File", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                                Text("Not Provided", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                        }
                    }
                    HorizontalDivider()
                    Text("Donated Vehicles", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    if (fastAppVehicleDetails.isEmpty()) {
                        Text("No vehicles found attached to this application.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        fastAppVehicleDetails.forEachIndexed { index, vehicle ->
                            Card(shape = RoundedCornerShape(4.dp), border = BorderStroke(0.5.dp, Color.LightGray), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Vehicle #${index + 1}: ${vehicle["CarType"]}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                    vehicle.filterKeys { it != "CarType" && it != "ApplicationID" }.forEach { (k, v) ->
                                        if (v.isNotBlank() && v != "NULL" && v != "N/A") {
                                            DetailRow(formatDatabaseKey(k), v)
                                        }
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
        val entity = selectedEntityForDetail!!
        AlertDialog(
            onDismissRequest = { selectedEntityForDetail = null },
            confirmButton = { TextButton(onClick = { selectedEntityForDetail = null }) { Text("CLOSE") } },
            title = { Text("More Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    val isDonor = entity.containsKey("DonorID")
                    val isDonee = entity.containsKey("DoneeID")
                    val isVehicle = entity.containsKey("DonateID")

                    if (isDonor || isDonee) {
                        entity.forEach { (key, value) -> DetailRow(formatDatabaseKey(key), value) }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                        Text("Associated Applications", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        val id = if (isDonor) entity["DonorID"] else entity["DoneeID"]
                        val matchingApps = logs.filter { if (isDonor) it[8] == id else it[7] == id }

                        if (matchingApps.isEmpty()) {
                            Text("No applications found.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            matchingApps.forEach { appLog ->
                                Card(shape = RoundedCornerShape(4.dp), border = BorderStroke(0.5.dp, Color.LightGray), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("App ID: ${appLog[0]} (${appLog[1]})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        val mvs = motorVehicles.filter { it["ApplicationID"] == appLog[0] }
                                        val pcs = passengerCars.filter { it["ApplicationID"] == appLog[0] }
                                        mvs.forEach { Text("• Motor: ${it["VehicleDescription"]} (Qty: ${it["Quantity"]})", fontSize = 11.sp, color = Color.DarkGray) }
                                        pcs.forEach { Text("• Pass. Car: VIN ${it["VIN"]} (${it["YearModel"]})", fontSize = 11.sp, color = Color.DarkGray) }
                                        if (mvs.isEmpty() && pcs.isEmpty()) Text("• No vehicles attached", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }

                    if (isVehicle) {
                        val appId = entity["ApplicationID"]
                        DetailRow("Application ID", appId ?: "N/A")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                        Text("Vehicle Information", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        val keysToHide = listOf("ApplicationID", "CarType")
                        entity.filterKeys { it !in keysToHide }.forEach { (key, value) -> DetailRow(formatDatabaseKey(key), value) }

                        if (appId != null) {
                            val logEntry = logs.find { it[0] == appId }
                            if (logEntry != null) {
                                val donor = donorsDetailed.find { it["DonorID"] == logEntry[8] }
                                val donee = doneesDetailed.find { it["DoneeID"] == logEntry[7] }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                                Text("Donor Details", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                donor?.forEach { (k, v) -> DetailRow(formatDatabaseKey(k), v) }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                                Text("Donee Details", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                donee?.forEach { (k, v) -> DetailRow(formatDatabaseKey(k), v) }
                            }
                        }
                    }
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
                                val (dnrs, dnes) = DatabaseService.fetchDonorsAndDoneesDetailed()
                                donorsDetailed = dnrs; doneesDetailed = dnes
                                val (mv, pc) = DatabaseService.fetchVehiclesDetailed()
                                motorVehicles = mv; passengerCars = pc
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )

                var filterExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { filterExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.FilterList, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(viewCategory)
                    }
                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                        listOf("Application", "Donor", "Donee", "Motor Vehicle", "Passenger Car").forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { viewCategory = cat; filterExpanded = false })
                        }
                    }
                }

                IconButton(onClick = { sortOrder = if (sortOrder == "Descending") "Ascending" else "Descending" }) {
                    Icon(if (sortOrder == "Descending") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null)
                }
            }

            val sideScroll = rememberScrollState()

            val useScroll = viewCategory == "Donor" || viewCategory == "Donee" || viewCategory == "Passenger Car"

            val rowWidth = when (viewCategory) {
                "Passenger Car" -> 1600.dp
                "Donor", "Donee" -> 1600.dp
                else -> 2200.dp
            }

            Box(modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp)).then(if (useScroll) Modifier.horizontalScroll(sideScroll) else Modifier)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxHeight()) {
                    val finalDataList: List<Any> = if (searchQuery.isNotBlank()) hierarchicalResults else {
                        when (viewCategory) {
                            "Application" -> sortData(logs) { it[0] }
                            "Donor" -> sortData(donorsDetailed) { it["DonorID"] ?: "" }
                            "Donee" -> sortData(doneesDetailed) { it["DoneeID"] ?: "" }
                            "Motor Vehicle" -> sortData(motorVehicles) { it["DonateID"] ?: "" }
                            "Passenger Car" -> sortData(passengerCars) { it["DonateID"] ?: "" }
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
                                "Motor Vehicle" -> listOf("DONATE ID", "DESCRIPTION", "VEHICLE TARIFF", "ORIGIN", "QUANTITY")
                                "Passenger Car" -> listOf("DONATE ID / VIN", "REG. DATE / YEAR MODEL", "COLOR", "WEIGHT", "ENGINE NO", "DISPLACEMENT", "FUEL")
                                else -> listOf("ID", "NAME", "TYPE")
                            }
                            Row(modifier = Modifier.then(if (useScroll) Modifier.width(rowWidth) else Modifier.fillMaxWidth()).background(Color(0xFFF1F3F5)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                headers.forEachIndexed { idx, h ->
                                    val rowModifier = when (viewCategory) {
                                        "Application" -> when(idx) { 0 -> Modifier.weight(1.5f); 1 -> Modifier.weight(2f); 2 -> Modifier.weight(1.5f); else -> Modifier.width(150.dp) }
                                        "Motor Vehicle" -> when(idx) { 0 -> Modifier.weight(0.8f); 1 -> Modifier.weight(1.2f); 2 -> Modifier.weight(1f); 3 -> Modifier.weight(1f); 4 -> Modifier.weight(0.6f); else -> Modifier.width(150.dp) }
                                        "Passenger Car" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(0.8f); 2 -> Modifier.weight(0.8f); 3 -> Modifier.weight(0.8f); 4 -> Modifier.weight(0.8f); 5 -> Modifier.weight(0.8f); 6 -> Modifier.weight(0.8f); else -> Modifier.width(150.dp) }
                                        "Donor" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2.5f); 2 -> Modifier.weight(2.5f); 3 -> Modifier.weight(1.8f); 4 -> Modifier.weight(1.5f); 5 -> Modifier.weight(2f); else -> Modifier.width(150.dp) }
                                        "Donee" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2f); 2 -> Modifier.weight(2.5f); 3 -> Modifier.weight(1.5f); 4 -> Modifier.weight(1.5f); 5 -> Modifier.weight(1.5f); 6 -> Modifier.weight(2f); else -> Modifier.width(150.dp) }
                                        else -> Modifier.width(150.dp)
                                    }
                                    Text(h, modifier = rowModifier, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.DarkGray, textAlign = TextAlign.Start)
                                }
                                Spacer(modifier = Modifier.width(48.dp))
                            }
                            HorizontalDivider(color = Color(0xFFE0E0E0))
                        }

                        items(finalDataList) { item ->
                            var menuExpanded by remember { mutableStateOf(false) }
                            Column(modifier = Modifier.then(if (useScroll) Modifier.width(rowWidth) else Modifier.fillMaxWidth()).background(Color.White).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (item) {
                                        is List<*> -> {
                                            val log = item as List<String>
                                            val isMixed = log[6] == "Motor Vehicle / Passenger Car"
                                            val isPassenger = log[6] == "Passenger Car"
                                            val badgeBgColor = if (isMixed) Color(0xFFE3F2FD) else if (isPassenger) Color(0xFFE8F5E9) else Color(0xFFFFE0B2)
                                            val badgeTextColor = if (isMixed) Color(0xFF1565C0) else if (isPassenger) Color(0xFF2E7D32) else Color(0xFFE65100)

                                            Column(modifier = Modifier.weight(1.5f)) { HighlightedText(log[0], searchQuery, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)); Text(log[1].split(" ")[0], fontSize = 10.sp, color = Color.Gray) }
                                            Column(modifier = Modifier.weight(2f)) { HighlightedText(log[3], searchQuery, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)); HighlightedText(log[2], searchQuery, style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Light)) }
                                            Box(modifier = Modifier.weight(1.5f).clip(RoundedCornerShape(4.dp)).background(badgeBgColor).padding(horizontal = 6.dp, vertical = 2.dp), contentAlignment = Alignment.CenterStart) { Text(log[6], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeTextColor) }
                                            Box(modifier = Modifier.width(48.dp)) {
                                                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, null) }
                                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                                    DropdownMenuItem(
                                                        text = { Text("More Details") },
                                                        onClick = { selectedLogForDetail = log; menuExpanded = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Edit Application") },
                                                        onClick = {
                                                            menuExpanded = false
                                                            navController.navigate("edit/${log[0]}")
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        is Map<*, *> -> {
                                            val map = item as Map<String, String>

                                            val isDonor = map.containsKey("DonorID")
                                            val isDonee = map.containsKey("DoneeID")
                                            val isVehicle = map.containsKey("DonateID")
                                            val isPassenger = map.containsKey("VIN") && map["VIN"] != "NULL"

                                            val viewFields = when {
                                                isDonor -> listOf(map["DonorID"], map["DonorName"], map["DonorAddress"], map["DonorTelNo"], map["DonorFaxNo"], map["DonorEmail"])
                                                isDonee -> listOf(map["DoneeID"], map["DoneeName"], map["DoneeAddress"], map["ContactPerson"], map["DoneeTelNo"], map["DoneeFaxNo"], map["DoneeEmail"])
                                                isPassenger -> listOf(
                                                    map["DonateID"],
                                                    map["RegistrationDate"],
                                                    map["Color"],
                                                    map["VehicleWeight"],
                                                    map["EngineNumber"],
                                                    map["EngineDisplacement"],
                                                    map["FuelType"]
                                                )
                                                isVehicle -> listOf(map["DonateID"], map["VehicleDescription"], map["TariffCode"], map["Origin"], map["Quantity"])
                                                else -> listOf("ERR: Unmapped Data")
                                            }

                                            viewFields.forEachIndexed { idx, value ->
                                                val cellModifier = when (viewCategory) {
                                                    "Motor Vehicle" -> when(idx) { 0 -> Modifier.weight(0.8f); 1 -> Modifier.weight(1.2f); 2 -> Modifier.weight(1f); 3 -> Modifier.weight(1f); 4 -> Modifier.weight(0.6f); else -> Modifier.width(150.dp) }
                                                    "Passenger Car" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(0.8f); 2 -> Modifier.weight(0.8f); 3 -> Modifier.weight(0.8f); 4 -> Modifier.weight(0.8f); 5 -> Modifier.weight(0.8f); 6 -> Modifier.weight(0.8f); else -> Modifier.width(150.dp) }
                                                    "Donor" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2.5f); 2 -> Modifier.weight(2.5f); 3 -> Modifier.weight(1.8f); 4 -> Modifier.weight(1.5f); 5 -> Modifier.weight(2f); else -> Modifier.width(150.dp) }
                                                    "Donee" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2f); 2 -> Modifier.weight(2.5f); 3 -> Modifier.weight(1.5f); 4 -> Modifier.weight(1.5f); 5 -> Modifier.weight(1.5f); 6 -> Modifier.weight(2f); else -> Modifier.width(150.dp) }
                                                    else -> Modifier.width(150.dp)
                                                }

                                                Box(modifier = cellModifier, contentAlignment = Alignment.TopStart) {
                                                    if (viewCategory == "Passenger Car") {
                                                        when (idx) {
                                                            0 -> {
                                                                Column {
                                                                    HighlightedText(text = map["DonateID"] ?: "N/A", query = searchQuery, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary))
                                                                    Text(text = map["VIN"] ?: "N/A", fontSize = 10.sp, color = Color.Gray)
                                                                }
                                                            }
                                                            1 -> {
                                                                Column {
                                                                    HighlightedText(text = map["RegistrationDate"] ?: "N/A", query = searchQuery, style = TextStyle(fontSize = 12.sp))
                                                                    Text(text = map["YearModel"] ?: "N/A", fontSize = 10.sp, color = Color.Gray)
                                                                }
                                                            }
                                                            else -> {
                                                                HighlightedText(text = value ?: "", query = searchQuery, style = TextStyle(fontSize = 12.sp))
                                                            }
                                                        }
                                                    } else {
                                                        HighlightedText(text = value ?: "", query = searchQuery, style = if(idx == 0) TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary) else TextStyle(fontSize = 12.sp))
                                                    }
                                                }
                                            }

                                            Box(modifier = Modifier.width(48.dp)) { IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, null) }; DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { DropdownMenuItem(text = { Text("More Details") }, onClick = { selectedEntityForDetail = map; menuExpanded = false }) } }
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