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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
                    "v3.9.0-PROTOTYPE",
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
                    composable("form") { RegisterVehicleScreen(navController, refreshTrigger) }
                    composable("history") { HistoryLogScreen(navController, refreshTrigger) { refreshTrigger = System.currentTimeMillis() } }
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
        "desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"
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

    LaunchedEffect(refreshTrigger) {
        val (dnr, dne) = DatabaseService.fetchDonorsAndDoneesDetailed()
        donors = dnr; donees = dne
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
                motorVehicles.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1"))
                passengerCars.clear()
                passengerCars.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
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
        motorVehicles.clear(); motorVehicles.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1"))
        passengerCars.clear(); passengerCars.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
        signatureUriText = "No confirmation files selected"
        actualFileUri = null
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
                                                            motorVehicles.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1"))
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
                                        OutlinedButton(onClick = { motorVehicles.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1")) }, modifier = Modifier.align(Alignment.End), shape = RoundedCornerShape(8.dp)) { Text("+ Add Motor Vehicle") }
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
                                                            passengerCars.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
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
                                                        value = item["weight"]?.filter { it.isDigit() } ?: "",
                                                        onValueChange = { newVal ->
                                                            val digitsOnly = newVal.filter { it.isDigit() }
                                                            passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("weight", if (digitsOnly.isNotBlank()) "$digitsOnly kg" else "") }
                                                        },
                                                        label = { Text("Weight *") },
                                                        suffix = { Text("kg", color = Color.Gray) },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.weight(1f),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                }

                                                // --- DATE PICKER FOR REGISTRATION DATE ---
                                                var showDatePicker by remember { mutableStateOf(false) }

                                                if (showDatePicker) {
                                                    val initialMillis = remember {
                                                        try {
                                                            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                            format.timeZone = TimeZone.getTimeZone("UTC")
                                                            item["regDate"]?.takeIf { it.isNotBlank() }?.let { format.parse(it)?.time }
                                                        } catch (e: Exception) { null }
                                                    }
                                                    val datePickerState = rememberDatePickerState(
                                                        initialSelectedDateMillis = initialMillis,
                                                        selectableDates = object : SelectableDates {
                                                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                                                return utcTimeMillis <= System.currentTimeMillis()
                                                            }
                                                        }
                                                    )

                                                    DatePickerDialog(
                                                        onDismissRequest = { showDatePicker = false },
                                                        confirmButton = {
                                                            TextButton(onClick = {
                                                                datePickerState.selectedDateMillis?.let { millis ->
                                                                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                                                                    val formattedDate = formatter.format(Date(millis))
                                                                    passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("regDate", formattedDate) }
                                                                }
                                                                showDatePicker = false
                                                            }) { Text("OK") }
                                                        },
                                                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") } }
                                                    ) {
                                                        DatePicker(state = datePickerState)
                                                    }
                                                }

                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    OutlinedTextField(
                                                        value = item["regDate"] ?: "",
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        label = { Text("Registration Date *") },
                                                        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                                                }
                                                // -----------------------------------------

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedTextField(value = item["engineNo"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("engineNo", it) } }, label = { Text("Engine Number *") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(8.dp))
                                                    OutlinedTextField(
                                                        value = item["displacement"]?.filter { it.isDigit() } ?: "",
                                                        onValueChange = { newVal ->
                                                            val digitsOnly = newVal.filter { it.isDigit() }
                                                            passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("displacement", if (digitsOnly.isNotBlank()) "$digitsOnly cc" else "") }
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
                                    "DoneeStatus" to doneeStatus, "ExistingDoneeID" to selectedDoneeId, "DoneeName" to doneeName, "DoneeAddress" to doneeAddr, "ContactPerson" to doneeContact, "DoneeTelNo" to doneeTel, "DoneeFaxNo" to doneeFax, "DoneeEmail" to doneeEmail,
                                    "DonorStatus" to donorStatus, "ExistingDonorID" to selectedDonorId, "DonorName" to donorName, "DonorAddress" to donorAddr, "DonorTelNo" to donorTel, "DonorFaxNo" to donorFax, "DonorEmail" to donorEmail,
                                    "IncludesMotorVehicles" to isMotorVehicleSelected.toString(), "IncludesPassengerCars" to isPassengerCarSelected.toString(),
                                    "DonorSignaturePath" to localSavedPath
                                )

                                DatabaseService.submitVehicleApplication(payload, if(isMotorVehicleSelected) motorVehicles else emptyList(), if(isPassengerCarSelected) passengerCars else emptyList())
                                    .onSuccess { id ->
                                        Toast.makeText(context, "Submitted successfully!", Toast.LENGTH_LONG).show()
                                        globalResetAllFields()
                                        navController.navigate("history") { popUpTo("form") { inclusive = true } }
                                    }
                                    .onFailure { tx -> Toast.makeText(context, "Database Error: ${tx.message}", Toast.LENGTH_LONG).show() }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Storage Save Failure: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
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
fun HistoryLogScreen(navController: NavHostController, refreshTrigger: Long, onRefreshRequested: () -> Unit) {
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

    var entityDeleteError by remember { mutableStateOf<String?>(null) }
    var entityToDelete by remember { mutableStateOf<Pair<String, String>?>(null) }

    // State for the inline edit dialog
    var isEditingLog by remember { mutableStateOf(false) }

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

    val isSearching = searchQuery.isNotBlank()

    if (selectedLogForDetail != null) {
        val log = selectedLogForDetail!!

        if (isEditingLog) {
            EditApplicationDialog(
                appId = log[0],
                initialLog = log,
                donorsDetailed = donorsDetailed,
                doneesDetailed = doneesDetailed,
                motorVehicles = motorVehicles,
                passengerCars = passengerCars,
                onDismiss = { isEditingLog = false },
                onSave = {
                    isEditingLog = false
                    selectedLogForDetail = null
                    onRefreshRequested()
                }
            )
        } else {
            val fastAppVehicleDetails = remember(log[0], motorVehicles, passengerCars) {
                val mvs = motorVehicles.filter { it["ApplicationID"] == log[0] }
                val pcs = passengerCars.filter { it["ApplicationID"] == log[0] }
                mvs + pcs
            }

            AlertDialog(
                onDismissRequest = { selectedLogForDetail = null },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showDeleteConfirmation = log[0] },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) { Text("DELETE") }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { isEditingLog = true }) { Text("EDIT", fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { selectedLogForDetail = null }) { Text("CLOSE") }
                        }
                    }
                },
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
    }

    if (selectedEntityForDetail != null) {
        val entity = selectedEntityForDetail!!
        val isDonor = entity.containsKey("DonorID")
        val isDonee = entity.containsKey("DoneeID")
        val isVehicle = entity.containsKey("DonateID")

        val id = if (isDonor) entity["DonorID"] else if (isDonee) entity["DoneeID"] else null
        val matchingApps = if (id != null) logs.filter { if (isDonor) it[8] == id else it[7] == id } else emptyList()

        AlertDialog(
            onDismissRequest = { selectedEntityForDetail = null },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDonor || isDonee) {
                        val typeStr = if (isDonor) "donor" else "donee"
                        TextButton(
                            onClick = {
                                if (matchingApps.isNotEmpty()) {
                                    entityDeleteError = "Cannot be deleted. This $typeStr is attached to existing applications. Please change the $typeStr of the attached applications first before deletion."
                                } else {
                                    entityToDelete = Pair(if (isDonor) "Donor" else "Donee", id!!)
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                        ) { Text("DELETE") }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = { selectedEntityForDetail = null }) { Text("CLOSE") }
                }
            },
            title = { Text("More Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (isDonor || isDonee) {
                        entity.forEach { (key, value) -> DetailRow(formatDatabaseKey(key), value) }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                        Text("Associated Applications", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)

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

    if (entityDeleteError != null) {
        AlertDialog(
            onDismissRequest = { entityDeleteError = null },
            title = { Text("Action Denied") },
            text = { Text(entityDeleteError!!) },
            confirmButton = { TextButton(onClick = { entityDeleteError = null }) { Text("OK") } }
        )
    }

    if (entityToDelete != null) {
        val (type, id) = entityToDelete!!
        AlertDialog(
            onDismissRequest = { entityToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete $type $id?") },
            confirmButton = {
                TextButton(onClick = {
                    val entityId = id
                    val entityType = type
                    entityToDelete = null
                    selectedEntityForDetail = null
                    scope.launch {
                        val res = snackbarHostState.showSnackbar("Deleting $entityType $entityId...", "UNDO", duration = SnackbarDuration.Short)
                        if (res != SnackbarResult.ActionPerformed) {
                            val dbRes = if (entityType == "Donor") DatabaseService.deleteDonor(entityId) else DatabaseService.deleteDonee(entityId)
                            dbRes.onSuccess {
                                Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show()
                                onRefreshRequested()
                            }.onFailure {
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("DELETE") }
            },
            dismissButton = { TextButton(onClick = { entityToDelete = null }) { Text("CANCEL") } }
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
                                onRefreshRequested()
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

            val useScroll = isSearching || viewCategory == "Donor" || viewCategory == "Donee" || viewCategory == "Passenger Car"

            val rowWidth = if (isSearching) 2200.dp else when (viewCategory) {
                "Passenger Car" -> 1600.dp
                "Donor", "Donee" -> 1600.dp
                else -> 2200.dp
            }

            Box(modifier = Modifier.fillMaxSize().border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp)).then(if (useScroll) Modifier.horizontalScroll(sideScroll) else Modifier)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.fillMaxHeight()) {

                    if (isSearching) {
                        val matchedApps = logs.filter { app -> app.any { it.contains(searchQuery, ignoreCase = true) } }
                        val matchedDonors = donorsDetailed.filter { it.values.any { v -> v.contains(searchQuery, ignoreCase = true) } }
                        val matchedDonees = doneesDetailed.filter { it.values.any { v -> v.contains(searchQuery, ignoreCase = true) } }
                        val matchedMotors = motorVehicles.filter { it.values.any { v -> v.contains(searchQuery, ignoreCase = true) } }
                        val matchedPassengers = passengerCars.filter { it.values.any { v -> v.contains(searchQuery, ignoreCase = true) } }

                        val totalMatches = matchedApps.size + matchedDonors.size + matchedDonees.size + matchedMotors.size + matchedPassengers.size

                        if (totalMatches == 0) {
                            item { Box(modifier = Modifier.fillParentMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text("No results.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = TextAlign.Center) } }
                        } else {
                            if (matchedApps.isNotEmpty()) {
                                stickyHeader {
                                    SectionHeader("Applications", useScroll, rowWidth)
                                    TableHeader("Application", useScroll, rowWidth)
                                }
                                items(sortData(matchedApps) { it[0] }) { item ->
                                    AppRow(item, searchQuery, useScroll, rowWidth) { selectedLogForDetail = item }
                                }
                            }
                            if (matchedDonors.isNotEmpty()) {
                                stickyHeader {
                                    SectionHeader("Donors", useScroll, rowWidth)
                                    TableHeader("Donor", useScroll, rowWidth)
                                }
                                items(sortData(matchedDonors) { it["DonorID"] ?: "" }) { item ->
                                    EntityRow("Donor", item, searchQuery, useScroll, rowWidth) { selectedEntityForDetail = item }
                                }
                            }
                            if (matchedDonees.isNotEmpty()) {
                                stickyHeader {
                                    SectionHeader("Donees", useScroll, rowWidth)
                                    TableHeader("Donee", useScroll, rowWidth)
                                }
                                items(sortData(matchedDonees) { it["DoneeID"] ?: "" }) { item ->
                                    EntityRow("Donee", item, searchQuery, useScroll, rowWidth) { selectedEntityForDetail = item }
                                }
                            }
                            if (matchedMotors.isNotEmpty()) {
                                stickyHeader {
                                    SectionHeader("Motor Vehicles", useScroll, rowWidth)
                                    TableHeader("Motor Vehicle", useScroll, rowWidth)
                                }
                                items(sortData(matchedMotors) { it["DonateID"] ?: "" }) { item ->
                                    EntityRow("Motor Vehicle", item, searchQuery, useScroll, rowWidth) { selectedEntityForDetail = item }
                                }
                            }
                            if (matchedPassengers.isNotEmpty()) {
                                stickyHeader {
                                    SectionHeader("Passenger Cars", useScroll, rowWidth)
                                    TableHeader("Passenger Car", useScroll, rowWidth)
                                }
                                items(sortData(matchedPassengers) { it["DonateID"] ?: "" }) { item ->
                                    EntityRow("Passenger Car", item, searchQuery, useScroll, rowWidth) { selectedEntityForDetail = item }
                                }
                            }
                        }
                    } else {
                        // Normal non-searching view
                        val finalDataList: List<Any> = when (viewCategory) {
                            "Application" -> sortData(logs) { it[0] }
                            "Donor" -> sortData(donorsDetailed) { it["DonorID"] ?: "" }
                            "Donee" -> sortData(doneesDetailed) { it["DoneeID"] ?: "" }
                            "Motor Vehicle" -> sortData(motorVehicles) { it["DonateID"] ?: "" }
                            "Passenger Car" -> sortData(passengerCars) { it["DonateID"] ?: "" }
                            else -> emptyList()
                        }

                        if (finalDataList.isEmpty()) {
                            item { Box(modifier = Modifier.fillParentMaxSize().padding(32.dp), contentAlignment = Alignment.Center) { Text("No records.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray, textAlign = TextAlign.Center) } }
                        } else {
                            stickyHeader {
                                TableHeader(viewCategory, useScroll, rowWidth)
                            }
                            items(finalDataList) { item ->
                                when (item) {
                                    is List<*> -> AppRow(item as List<String>, searchQuery, useScroll, rowWidth) { selectedLogForDetail = item }
                                    is Map<*, *> -> EntityRow(viewCategory, item as Map<String, String>, searchQuery, useScroll, rowWidth) { selectedEntityForDetail = item }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, useScroll: Boolean, rowWidth: Dp) {
    Box(modifier = Modifier.then(if(useScroll) Modifier.width(rowWidth) else Modifier.fillMaxWidth()).background(Color(0xFFE3E8EE)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
    }
}

@Composable
fun TableHeader(category: String, useScroll: Boolean, rowWidth: Dp) {
    val headers = when (category) {
        "Application" -> listOf("APPLICATION ID / DATE", "DONOR / DONEE", "VEHICLE TYPE")
        "Donor" -> listOf("DONOR ID", "NAME", "ADDRESS", "TELEPHONE", "FAX", "EMAIL")
        "Donee" -> listOf("DONEE ID", "NAME", "ADDRESS", "CONTACT PERSON", "TELEPHONE", "FAX", "EMAIL")
        "Motor Vehicle" -> listOf("DONATE ID", "DESCRIPTION", "VEHICLE TARIFF", "ORIGIN", "QUANTITY")
        "Passenger Car" -> listOf("DONATE ID / VIN", "REG. DATE / YEAR MODEL", "COLOR", "WEIGHT", "ENGINE NO", "DISPLACEMENT", "FUEL")
        else -> listOf("ID", "NAME", "TYPE")
    }
    Column(modifier = Modifier.then(if (useScroll) Modifier.width(rowWidth) else Modifier.fillMaxWidth()).background(Color(0xFFF1F3F5))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            headers.forEachIndexed { idx, h ->
                val rowModifier = when (category) {
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
}

@Composable
fun AppRow(log: List<String>, searchQuery: String, useScroll: Boolean, rowWidth: Dp, onMoreDetails: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isMixed = log[6] == "Motor Vehicle / Passenger Car"
    val isPassenger = log[6] == "Passenger Car"
    val badgeBgColor = if (isMixed) Color(0xFFE3F2FD) else if (isPassenger) Color(0xFFE8F5E9) else Color(0xFFFFE0B2)
    val badgeTextColor = if (isMixed) Color(0xFF1565C0) else if (isPassenger) Color(0xFF2E7D32) else Color(0xFFE65100)

    Column(modifier = Modifier.then(if (useScroll) Modifier.width(rowWidth) else Modifier.fillMaxWidth()).background(Color.White).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1.5f)) { HighlightedText(log[0], searchQuery, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)); Text(log[1].split(" ")[0], fontSize = 10.sp, color = Color.Gray) }
            Column(modifier = Modifier.weight(2f)) { HighlightedText(log[3], searchQuery, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)); HighlightedText(log[2], searchQuery, style = TextStyle(fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Light)) }
            Box(modifier = Modifier.weight(1.5f).clip(RoundedCornerShape(4.dp)).background(badgeBgColor).padding(horizontal = 6.dp, vertical = 2.dp), contentAlignment = Alignment.CenterStart) { Text(log[6], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeTextColor) }
            Box(modifier = Modifier.width(48.dp)) {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("More Details") }, onClick = { onMoreDetails(); menuExpanded = false })
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color(0xFFF0F0F0))
    }
}

@Composable
fun EntityRow(category: String, map: Map<String, String>, searchQuery: String, useScroll: Boolean, rowWidth: Dp, onMoreDetails: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
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

    Column(modifier = Modifier.then(if (useScroll) Modifier.width(rowWidth) else Modifier.fillMaxWidth()).background(Color.White).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            viewFields.forEachIndexed { idx, value ->
                val cellModifier = when (category) {
                    "Motor Vehicle" -> when(idx) { 0 -> Modifier.weight(0.8f); 1 -> Modifier.weight(1.2f); 2 -> Modifier.weight(1f); 3 -> Modifier.weight(1f); 4 -> Modifier.weight(0.6f); else -> Modifier.width(150.dp) }
                    "Passenger Car" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(0.8f); 2 -> Modifier.weight(0.8f); 3 -> Modifier.weight(0.8f); 4 -> Modifier.weight(0.8f); 5 -> Modifier.weight(0.8f); 6 -> Modifier.weight(0.8f); else -> Modifier.width(150.dp) }
                    "Donor" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2.5f); 2 -> Modifier.weight(2.5f); 3 -> Modifier.weight(1.8f); 4 -> Modifier.weight(1.5f); 5 -> Modifier.weight(2f); else -> Modifier.width(150.dp) }
                    "Donee" -> when(idx) { 0 -> Modifier.weight(1.2f); 1 -> Modifier.weight(2f); 2 -> Modifier.weight(2.5f); 3 -> Modifier.weight(1.5f); 4 -> Modifier.weight(1.5f); 5 -> Modifier.weight(1.5f); 6 -> Modifier.weight(2f); else -> Modifier.width(150.dp) }
                    else -> Modifier.width(150.dp)
                }

                Box(modifier = cellModifier, contentAlignment = Alignment.TopStart) {
                    if (category == "Passenger Car") {
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

            Box(modifier = Modifier.width(48.dp)) {
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, null) }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("More Details") }, onClick = { onMoreDetails(); menuExpanded = false })
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Color(0xFFF0F0F0))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditApplicationDialog(
    appId: String,
    initialLog: List<String>,
    donorsDetailed: List<Map<String, String>>,
    doneesDetailed: List<Map<String, String>>,
    motorVehicles: List<Map<String, String>>,
    passengerCars: List<Map<String, String>>,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    // Track unsaved modifications
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Pair<String, Int>?>(null) } // Tracks "MV" or "PC" and index to delete

    val originalSigPath = initialLog.getOrElse(9) { "" }

    // Donee State
    var doneeSelection by remember { mutableStateOf(initialLog[7]) }
    var isNewDonee by remember { mutableStateOf(false) }
    var doneeName by remember { mutableStateOf("") }
    var doneeAddr by remember { mutableStateOf("") }
    var doneeContact by remember { mutableStateOf("") }
    var doneeTel by remember { mutableStateOf("") }
    var doneeFax by remember { mutableStateOf("") }
    var doneeEmail by remember { mutableStateOf("") }

    // Donor State
    var donorSelection by remember { mutableStateOf(initialLog[8]) }
    var isNewDonor by remember { mutableStateOf(false) }
    var donorName by remember { mutableStateOf("") }
    var donorAddr by remember { mutableStateOf("") }
    var donorTel by remember { mutableStateOf("") }
    var donorFax by remember { mutableStateOf("") }
    var donorEmail by remember { mutableStateOf("") }

    // Vehicles State
    val draftMVs = remember { mutableStateListOf<MutableMap<String, String>>() }
    val draftPCs = remember { mutableStateListOf<MutableMap<String, String>>() }

    LaunchedEffect(Unit) {
        draftMVs.addAll(motorVehicles.filter { it["ApplicationID"] == appId }.map {
            mutableMapOf("desc" to (it["VehicleDescription"] ?: ""), "tariffCode" to (it["TariffCode"] ?: ""), "origin" to (it["Origin"] ?: ""), "qty" to (it["Quantity"] ?: ""))
        })
        draftPCs.addAll(passengerCars.filter { it["ApplicationID"] == appId }.map {
            mutableMapOf(
                "desc" to (it["VehicleDescription"] ?: ""), "tariffCode" to (it["TariffCode"] ?: ""), "origin" to (it["Origin"] ?: ""),
                "vin" to (it["VIN"] ?: ""), "year" to (it["YearModel"] ?: ""), "color" to (it["Color"] ?: ""),
                "regDate" to (it["RegistrationDate"] ?: ""), "weight" to (it["VehicleWeight"] ?: ""),
                "engineNo" to (it["EngineNumber"] ?: ""), "displacement" to (it["EngineDisplacement"] ?: ""), "fuelType" to (it["FuelType"] ?: "G")
            )
        })
    }

    val requestDismiss = {
        if (hasUnsavedChanges) showCancelDialog = true else onDismiss()
    }

    // Unsaved Changes Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = { showCancelDialog = false; onDismiss() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("DISCARD") }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("KEEP EDITING") } }
        )
    }

    // Vehicle Deletion Confirmation Dialog
    if (itemToDelete != null) {
        val typeStr = if (itemToDelete!!.first == "MV") "Motor Vehicle" else "Passenger Car"
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete $typeStr") },
            text = { Text("Are you sure you want to remove this vehicle from the application?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        hasUnsavedChanges = true
                        if (itemToDelete!!.first == "MV") draftMVs.removeAt(itemToDelete!!.second)
                        else draftPCs.removeAt(itemToDelete!!.second)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("DELETE") }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("CANCEL") } }
        )
    }

    AlertDialog(
        onDismissRequest = requestDismiss,
        confirmButton = {
            Button(
                onClick = {
                    // Start Validation Checks before saving
                    if (isNewDonee) {
                        if (doneeName.isBlank() || doneeAddr.isBlank() || doneeContact.isBlank()) { Toast.makeText(context, "Please fill in all required Donee fields.", Toast.LENGTH_SHORT).show(); return@Button }
                        if (doneeTel.isBlank() && doneeFax.isBlank() && doneeEmail.isBlank()) { Toast.makeText(context, "Please provide at least one Donee contact method.", Toast.LENGTH_SHORT).show(); return@Button }
                    }
                    if (isNewDonor) {
                        if (donorName.isBlank() || donorAddr.isBlank()) { Toast.makeText(context, "Please fill in all required Donor fields.", Toast.LENGTH_SHORT).show(); return@Button }
                        if (donorTel.isBlank() && donorFax.isBlank() && donorEmail.isBlank()) { Toast.makeText(context, "Please provide at least one Donor contact method.", Toast.LENGTH_SHORT).show(); return@Button }
                    }
                    if (draftMVs.isEmpty() && draftPCs.isEmpty()) { Toast.makeText(context, "Please include at least one vehicle in the application.", Toast.LENGTH_SHORT).show(); return@Button }

                    for ((idx, mv) in draftMVs.withIndex()) {
                        if (mv["desc"].isNullOrBlank() || mv["tariffCode"].isNullOrBlank() || mv["origin"].isNullOrBlank() || mv["qty"].isNullOrBlank()) {
                            Toast.makeText(context, "Motor Vehicle #${idx + 1} has missing required fields.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (mv["tariffCode"]?.length != 4) { Toast.makeText(context, "Motor Vehicle #${idx + 1} Tariff Code must be 4 digits.", Toast.LENGTH_SHORT).show(); return@Button }
                        val qty = mv["qty"]?.toIntOrNull()
                        if (qty == null || qty <= 0) { Toast.makeText(context, "Motor Vehicle #${idx + 1} must have a valid quantity greater than 0.", Toast.LENGTH_SHORT).show(); return@Button }
                    }

                    for ((idx, pc) in draftPCs.withIndex()) {
                        if (pc["desc"].isNullOrBlank() || pc["tariffCode"].isNullOrBlank() || pc["origin"].isNullOrBlank() ||
                            pc["vin"].isNullOrBlank() || pc["year"].isNullOrBlank() || pc["color"].isNullOrBlank() ||
                            pc["regDate"].isNullOrBlank() || pc["weight"].isNullOrBlank() || pc["engineNo"].isNullOrBlank() ||
                            pc["displacement"].isNullOrBlank() || pc["fuelType"].isNullOrBlank()) {
                            Toast.makeText(context, "Passenger Car #${idx + 1} has missing required fields.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (pc["tariffCode"]?.length != 4) { Toast.makeText(context, "Passenger Car #${idx + 1} Tariff Code must be 4 digits.", Toast.LENGTH_SHORT).show(); return@Button }
                        val year = pc["year"]?.toIntOrNull()
                        if (year == null || year < 1900 || year > 2100) { Toast.makeText(context, "Passenger Car #${idx + 1} must have a valid 4-digit Year.", Toast.LENGTH_SHORT).show(); return@Button }
                    }

                    // Passed validation, execute save
                    scope.launch {
                        isSaving = true
                        val payload = mapOf(
                            "DoneeStatus" to if (isNewDonee) "new" else "existing",
                            "ExistingDoneeID" to doneeSelection,
                            "DoneeName" to doneeName, "DoneeAddress" to doneeAddr, "ContactPerson" to doneeContact, "DoneeTelNo" to doneeTel, "DoneeFaxNo" to doneeFax, "DoneeEmail" to doneeEmail,
                            "DonorStatus" to if (isNewDonor) "new" else "existing",
                            "ExistingDonorID" to donorSelection,
                            "DonorName" to donorName, "DonorAddress" to donorAddr, "DonorTelNo" to donorTel, "DonorFaxNo" to donorFax, "DonorEmail" to donorEmail,
                            "DonorSignaturePath" to originalSigPath
                        )
                        DatabaseService.updateVehicleApplication(appId, payload, draftMVs, draftPCs).onSuccess {
                            Toast.makeText(context, "Saved changes!", Toast.LENGTH_SHORT).show()
                            onSave()
                        }.onFailure {
                            Toast.makeText(context, "Error updating: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                        isSaving = false
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White) else Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = requestDismiss, enabled = !isSaving) { Text("CANCEL") } },
        title = { Text("Editing $appId Details...", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ---- DONEE SECTION ----
                Text("Donee Details", fontWeight = FontWeight.Bold)
                var doneeExpanded by remember { mutableStateOf(false) }
                val currentDoneeDisplay = if (isNewDonee) "New Donee..." else doneesDetailed.find { it["DoneeID"] == doneeSelection }?.get("DoneeName") ?: "Select Donee"

                ExposedDropdownMenuBox(expanded = doneeExpanded, onExpandedChange = { doneeExpanded = it }) {
                    OutlinedTextField(
                        value = currentDoneeDisplay, onValueChange = {}, readOnly = true,
                        label = { Text("Assigned Donee") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doneeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = doneeExpanded, onDismissRequest = { doneeExpanded = false }) {
                        doneesDetailed.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d["DoneeName"]} (${d["DoneeID"]})") },
                                onClick = { doneeSelection = d["DoneeID"]!!; isNewDonee = false; doneeExpanded = false; hasUnsavedChanges = true }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add New Donee", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = { isNewDonee = true; doneeExpanded = false; hasUnsavedChanges = true }
                        )
                    }
                }
                if (isNewDonee) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = doneeName, onValueChange = { doneeName = it; hasUnsavedChanges = true }, label = { Text("New Donee Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = doneeAddr, onValueChange = { doneeAddr = it; hasUnsavedChanges = true }, label = { Text("Address *") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = doneeContact, onValueChange = { doneeContact = it; hasUnsavedChanges = true }, label = { Text("Contact Person *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            Text("Contact Details (Choose at least one *):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            OutlinedTextField(value = doneeTel, onValueChange = { doneeTel = it; hasUnsavedChanges = true }, label = { Text("Telephone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = doneeFax, onValueChange = { doneeFax = it; hasUnsavedChanges = true }, label = { Text("Fax Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = doneeEmail, onValueChange = { doneeEmail = it; hasUnsavedChanges = true }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                    }
                }

                HorizontalDivider()

                // ---- DONOR SECTION ----
                Text("Donor Details", fontWeight = FontWeight.Bold)
                var donorExpanded by remember { mutableStateOf(false) }
                val currentDonorDisplay = if (isNewDonor) "New Donor..." else donorsDetailed.find { it["DonorID"] == donorSelection }?.get("DonorName") ?: "Select Donor"

                ExposedDropdownMenuBox(expanded = donorExpanded, onExpandedChange = { donorExpanded = it }) {
                    OutlinedTextField(
                        value = currentDonorDisplay, onValueChange = {}, readOnly = true,
                        label = { Text("Assigned Donor") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = donorExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = donorExpanded, onDismissRequest = { donorExpanded = false }) {
                        donorsDetailed.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d["DonorName"]} (${d["DonorID"]})") },
                                onClick = { donorSelection = d["DonorID"]!!; isNewDonor = false; donorExpanded = false; hasUnsavedChanges = true }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add New Donor", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = { isNewDonor = true; donorExpanded = false; hasUnsavedChanges = true }
                        )
                    }
                }
                if (isNewDonor) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = donorName, onValueChange = { donorName = it; hasUnsavedChanges = true }, label = { Text("New Donor Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = donorAddr, onValueChange = { donorAddr = it; hasUnsavedChanges = true }, label = { Text("Address *") }, modifier = Modifier.fillMaxWidth())
                            Text("Contact Details (Choose at least one *):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            OutlinedTextField(value = donorTel, onValueChange = { donorTel = it; hasUnsavedChanges = true }, label = { Text("Telephone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = donorFax, onValueChange = { donorFax = it; hasUnsavedChanges = true }, label = { Text("Fax Number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(value = donorEmail, onValueChange = { donorEmail = it; hasUnsavedChanges = true }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                    }
                }

                HorizontalDivider()

                // ---- VEHICLES SECTION ----
                Text("Donated Vehicles", fontWeight = FontWeight.Bold)

                draftMVs.forEachIndexed { idx, mv ->
                    var isEditingThis by remember { mutableStateOf(false) }
                    Card(colors = CardDefaults.cardColors(containerColor = if(isEditingThis) Color(0xFFF0F4F8) else Color(0xFFF9F9F9)), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.fillMaxWidth()) {
                        if (!isEditingThis) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Motor Vehicle: ${mv["desc"]?.ifBlank { "Unspecified" }}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("Tariff: ${mv["tariffCode"]}, Qty: ${mv["qty"]}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { isEditingThis = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { itemToDelete = Pair("MV", idx) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = Color.Red) }
                            }
                        } else {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Editing Motor Vehicle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                OutlinedTextField(value = mv["desc"] ?: "", onValueChange = { draftMVs[idx] = draftMVs[idx].toMutableMap().apply{ put("desc", it) }; hasUnsavedChanges = true }, label = { Text("Description *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(value = mv["origin"] ?: "", onValueChange = { draftMVs[idx] = draftMVs[idx].toMutableMap().apply{ put("origin", it) }; hasUnsavedChanges = true }, label = { Text("Origin *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = mv["tariffCode"] ?: "", onValueChange = { val f = it.filter { c -> c.isDigit() }.take(4); draftMVs[idx] = draftMVs[idx].toMutableMap().apply{ put("tariffCode", f) }; hasUnsavedChanges = true }, label = { Text("Tariff *") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = mv["qty"] ?: "", onValueChange = { val f = it.filter { c -> c.isDigit() }; draftMVs[idx] = draftMVs[idx].toMutableMap().apply{ put("qty", f) }; hasUnsavedChanges = true }, label = { Text("Qty *") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                TextButton(onClick = { isEditingThis = false }, modifier = Modifier.align(Alignment.End)) { Text("Done") }
                            }
                        }
                    }
                }

                draftPCs.forEachIndexed { idx, pc ->
                    var isEditingThis by remember { mutableStateOf(false) }
                    Card(colors = CardDefaults.cardColors(containerColor = if(isEditingThis) Color(0xFFF0F4F8) else Color(0xFFF9F9F9)), border = BorderStroke(1.dp, Color(0xFFEEEEEE)), modifier = Modifier.fillMaxWidth()) {
                        if (!isEditingThis) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Passenger Car: ${pc["desc"]?.ifBlank { "Unspecified" }}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("VIN: ${pc["vin"]}, Year: ${pc["year"]}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { isEditingThis = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { itemToDelete = Pair("PC", idx) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = Color.Red) }
                            }
                        } else {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Editing Passenger Car", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                OutlinedTextField(value = pc["desc"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("desc", it) }; hasUnsavedChanges = true }, label = { Text("Description *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(value = pc["vin"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("vin", it) }; hasUnsavedChanges = true }, label = { Text("VIN *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(value = pc["origin"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("origin", it) }; hasUnsavedChanges = true }, label = { Text("Origin *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = pc["tariffCode"] ?: "", onValueChange = { val f = it.filter { c -> c.isDigit() }.take(4); draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("tariffCode", f) }; hasUnsavedChanges = true }, label = { Text("Tariff *") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = pc["year"] ?: "", onValueChange = { val f = it.filter { c -> c.isDigit() }.take(4); draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("year", f) }; hasUnsavedChanges = true }, label = { Text("Year *") }, modifier = Modifier.weight(1f), singleLine = true)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = pc["color"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("color", it) }; hasUnsavedChanges = true }, label = { Text("Color *") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(
                                        value = pc["weight"]?.filter { it.isDigit() } ?: "",
                                        onValueChange = { newVal ->
                                            val digitsOnly = newVal.filter { it.isDigit() }
                                            draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("weight", if (digitsOnly.isNotBlank()) "$digitsOnly kg" else "") }
                                            hasUnsavedChanges = true
                                        },
                                        label = { Text("Weight *") },
                                        suffix = { Text("kg", color = Color.Gray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                // --- DATE PICKER FOR EDIT MODE ---
                                var showDatePicker by remember { mutableStateOf(false) }

                                if (showDatePicker) {
                                    val initialMillis = remember {
                                        try {
                                            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                            format.timeZone = TimeZone.getTimeZone("UTC")
                                            pc["regDate"]?.takeIf { it.isNotBlank() }?.let { format.parse(it)?.time }
                                        } catch (e: Exception) { null }
                                    }
                                    val datePickerState = rememberDatePickerState(
                                        initialSelectedDateMillis = initialMillis,
                                        selectableDates = object : SelectableDates {
                                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                                return utcTimeMillis <= System.currentTimeMillis()
                                            }
                                        }
                                    )

                                    DatePickerDialog(
                                        onDismissRequest = { showDatePicker = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let { millis ->
                                                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                                                    val formattedDate = formatter.format(Date(millis))
                                                    draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("regDate", formattedDate) }
                                                    hasUnsavedChanges = true
                                                }
                                                showDatePicker = false
                                            }) { Text("OK") }
                                        },
                                        dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("CANCEL") } }
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = pc["regDate"] ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Registration Date *") },
                                        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                                }
                                // ---------------------------------

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = pc["engineNo"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("engineNo", it) }; hasUnsavedChanges = true }, label = { Text("Engine No *") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(
                                        value = pc["displacement"]?.filter { it.isDigit() } ?: "",
                                        onValueChange = { newVal ->
                                            val digitsOnly = newVal.filter { it.isDigit() }
                                            draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("displacement", if (digitsOnly.isNotBlank()) "$digitsOnly cc" else "") }
                                            hasUnsavedChanges = true
                                        },
                                        label = { Text("Displacement *") },
                                        suffix = { Text("cc", color = Color.Gray) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                var fuelExpanded by remember { mutableStateOf(false) }
                                val fuelOptions = listOf("G: Gas" to "G", "D: Diesel" to "D", "LPG: Liquid Petroleum Gas" to "LPG", "E: Electric" to "E", "H: Hydrogen Fuel" to "H")
                                val currentFuelCode = pc["fuelType"] ?: "G"
                                val currentFuelDisplay = fuelOptions.find { it.second == currentFuelCode }?.first ?: "Gas"

                                ExposedDropdownMenuBox(expanded = fuelExpanded, onExpandedChange = { fuelExpanded = !fuelExpanded }, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(value = currentFuelDisplay, onValueChange = {}, readOnly = true, label = { Text("Fuel Type *") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                                    ExposedDropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }) {
                                        fuelOptions.forEach { (displayStr, dbCode) ->
                                            DropdownMenuItem(
                                                text = { Text(displayStr) },
                                                onClick = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("fuelType", dbCode) }; fuelExpanded = false; hasUnsavedChanges = true }
                                            )
                                        }
                                    }
                                }
                                TextButton(onClick = { isEditingThis = false }, modifier = Modifier.align(Alignment.End)) { Text("Done") }
                            }
                        }
                    }
                }

                // Check overall vehicle count
                val totalVehicles = draftMVs.size + draftPCs.size

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (totalVehicles < 4) {
                        OutlinedButton(
                            onClick = { draftMVs.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1")); hasUnsavedChanges = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("+ Motor Vehicle", fontSize = 11.sp) }
                    }
                    if (totalVehicles < 4 && draftPCs.isEmpty()) {
                        OutlinedButton(
                            onClick = { draftPCs.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G")); hasUnsavedChanges = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("+ Pass. Car", fontSize = 11.sp) }
                    }
                }

                // Show limit warnings if applicable
                if (totalVehicles >= 4) {
                    Text(text = "Combined maximum of 4 vehicles reached.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                } else if (draftPCs.isNotEmpty()) {
                    Text(text = "Maximum of 1 Passenger Car allowed per application.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                }
            }
        }
    )
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