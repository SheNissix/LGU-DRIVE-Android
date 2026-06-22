package com.lgu.drive.importation

import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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

// Utility to restrict phone inputs to digits, singular space, and hyphen
fun sanitizePhoneInput(input: String): String {
    return input.filter { it.isDigit() || it == ' ' || it == '-' }
        .replace(Regex(" {2,}"), " ")
        .replace(Regex("-{2,}"), "-")
}

// Utility to highly reliably detect if the keyboard is open
@Composable
fun rememberKeyboardVisibility(): Boolean {
    val view = LocalView.current
    var isKeyboardOpen by remember { mutableStateOf(false) }

    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // If the keypad is larger than 15% of the screen height, it's open
            isKeyboardOpen = keypadHeight > screenHeight * 0.15
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }
    return isKeyboardOpen
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
    object PortalForm : NavigationTab("form", "New Application", Icons.Default.AddCircle)
    object LedgerArchives : NavigationTab("history", "History Logs", Icons.AutoMirrored.Filled.ListAlt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAppLayoutContainer() {
    val navController = rememberNavController()
    val tabs = listOf(NavigationTab.PortalForm, NavigationTab.LedgerArchives)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var refreshTrigger by remember { mutableLongStateOf(0L) }

    // Dynamic state to check if keyboard is visible using view tree observer
    val isKeyboardVisible = rememberKeyboardVisibility()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "LGU-Drive Logo",
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(32.dp)
                    )
                },
                title = {
                    Text(
                        "LGU-DRIVE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                },
                actions = {
                    IconButton(onClick = { refreshTrigger = System.currentTimeMillis() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!isKeyboardVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    modifier = Modifier.shadow(8.dp)
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(22.dp)) },
                            label = { Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = (tab.route != "form")
                                    }
                                    launchSingleTop = true
                                    restoreState = (tab.route != "form")
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
            NavHost(navController = navController, startDestination = "form") {
                composable("form") { RegisterVehicleScreen(navController, refreshTrigger) }
                composable("history") { HistoryLogScreen(refreshTrigger) { refreshTrigger = System.currentTimeMillis() } }
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

    val stepTitles = listOf("Donee Profile", "Donor Profile", "Vehicle Details", "Review & Finalize")
    val animatedProgress by animateFloatAsState(targetValue = (currentStep + 1) / 4f, label = "Progress")

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Compact & Scrollable Header
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text(
                    "STEP ${currentStep + 1} OF 4",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stepTitles[currentStep],
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    if (currentStep < 2) {
                        var searchExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { searchExpanded = true }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("EXISTING", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            DropdownMenu(expanded = searchExpanded, onDismissRequest = { searchExpanded = false }) {
                                if (currentStep == 0) {
                                    donees.forEach { dne ->
                                        DropdownMenuItem(
                                            text = { Text("${dne["DoneeName"]} (${dne["DoneeID"]})", fontSize = 13.sp) },
                                            onClick = {
                                                doneeStatus = "existing"; selectedDoneeId = dne["DoneeID"] ?: ""
                                                doneeName = dne["DoneeName"] ?: ""; doneeAddr = dne["DoneeAddress"] ?: ""
                                                doneeContact = dne["ContactPerson"] ?: ""; doneeTel = dne["DoneeTelNo"] ?: ""
                                                doneeFax = dne["DoneeFaxNo"] ?: ""; doneeEmail = dne["DoneeEmail"] ?: ""
                                                searchExpanded = false
                                            }
                                        )
                                    }
                                } else {
                                    donors.forEach { dnr ->
                                        DropdownMenuItem(
                                            text = { Text("${dnr["DonorName"]} (${dnr["DonorID"]})", fontSize = 13.sp) },
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
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }

            when (currentStep) {
                0 -> { // Step 1: Donee
                    ElevatedCard(
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = doneeName, onValueChange = { doneeName = it; doneeStatus = "new" }, label = { Text("Donee Name *", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = doneeAddr, onValueChange = { doneeAddr = it; doneeStatus = "new" }, label = { Text("Address *", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = doneeContact, onValueChange = { doneeContact = it; doneeStatus = "new" }, label = { Text("Contact Person *", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            Text("Contact Details (Choose at least one *):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            OutlinedTextField(value = doneeTel, onValueChange = { doneeTel = sanitizePhoneInput(it); doneeStatus = "new" }, label = { Text("Telephone", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = doneeFax, onValueChange = { doneeFax = sanitizePhoneInput(it); doneeStatus = "new" }, label = { Text("Fax Number", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = doneeEmail, onValueChange = { doneeEmail = it; doneeStatus = "new" }, label = { Text("Email Address", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                        }
                    }
                }
                1 -> { // Step 2: Donor
                    ElevatedCard(
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = donorName, onValueChange = { donorName = it; donorStatus = "new" }, label = { Text("Donor Name *", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = donorAddr, onValueChange = { donorAddr = it; donorStatus = "new" }, label = { Text("Address *", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                            Text("Contact Details (Choose at least one *):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            OutlinedTextField(value = donorTel, onValueChange = { donorTel = sanitizePhoneInput(it); donorStatus = "new" }, label = { Text("Telephone", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = donorFax, onValueChange = { donorFax = sanitizePhoneInput(it); donorStatus = "new" }, label = { Text("Fax Number", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = donorEmail, onValueChange = { donorEmail = it; donorStatus = "new" }, label = { Text("Email Address", fontSize = 13.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                        }
                    }
                }
                2 -> { // Step 3: Vehicles
                    val totalVehicles = (if (isMotorVehicleSelected) motorVehicles.size else 0) +
                            (if (isPassengerCarSelected) passengerCars.size else 0)

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Select Vehicle Classification(s):", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isMotorVehicleSelected,
                                        onCheckedChange = { checked ->
                                            if (checked && totalVehicles + motorVehicles.size > 4) {
                                                Toast.makeText(context, "Maximum of 4 vehicles reached.", Toast.LENGTH_SHORT).show()
                                            } else { isMotorVehicleSelected = checked }
                                        }
                                    )
                                    Text("Motor Vehicle", fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isPassengerCarSelected,
                                        onCheckedChange = { checked ->
                                            if (checked && totalVehicles + passengerCars.size > 4) {
                                                Toast.makeText(context, "Maximum of 4 vehicles reached.", Toast.LENGTH_SHORT).show()
                                            } else { isPassengerCarSelected = checked }
                                        }
                                    )
                                    Text("Passenger Car", fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (isMotorVehicleSelected) {
                        Text("Motor Vehicles", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

                        motorVehicles.forEachIndexed { idx, item ->
                            ElevatedCard(
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text("Block #${idx + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                                        IconButton(onClick = {
                                            motorVehicles.removeAt(idx)
                                            if (motorVehicles.isEmpty()) {
                                                isMotorVehicleSelected = false
                                                motorVehicles.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1"))
                                            }
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    OutlinedTextField(value = item["desc"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("desc", it) } }, label = { Text("Vehicle Description *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                                    OutlinedTextField(value = item["origin"] ?: "", onValueChange = { motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("origin", it) } }, label = { Text("Origin Country *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = item["tariffCode"] ?: "",
                                            onValueChange = { newVal ->
                                                val filtered = newVal.filter { it.isDigit() }.take(4)
                                                motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("tariffCode", filtered) }
                                            },
                                            label = { Text("Tariff *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
                                        )
                                        OutlinedTextField(
                                            value = item["qty"] ?: "",
                                            onValueChange = { newVal ->
                                                val filtered = newVal.filter { it.isDigit() }
                                                motorVehicles[idx] = motorVehicles[idx].toMutableMap().apply { put("qty", filtered) }
                                            },
                                            label = { Text("Qty *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
                                        )
                                    }
                                }
                            }
                        }

                        if (totalVehicles < 4) {
                            OutlinedButton(onClick = { motorVehicles.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1")) }, modifier = Modifier.align(Alignment.End).height(36.dp), shape = CircleShape) { Text("+ Motor Vehicle", fontSize = 11.sp) }
                        }
                    }

                    if (isPassengerCarSelected) {
                        Text("Passenger Car", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

                        passengerCars.forEachIndexed { idx, item ->
                            ElevatedCard(
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text("Block #${idx + 1}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                                        IconButton(onClick = {
                                            passengerCars.removeAt(idx)
                                            if (passengerCars.isEmpty()) {
                                                isPassengerCarSelected = false
                                                passengerCars.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G"))
                                            }
                                        }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    OutlinedTextField(value = item["desc"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("desc", it) } }, label = { Text("Description *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                                    OutlinedTextField(value = item["vin"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("vin", it) } }, label = { Text("VIN *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
                                    OutlinedTextField(value = item["origin"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("origin", it) } }, label = { Text("Origin *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = item["tariffCode"] ?: "",
                                            onValueChange = { newVal ->
                                                val filtered = newVal.filter { it.isDigit() }.take(4)
                                                passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("tariffCode", filtered) }
                                            },
                                            label = { Text("Tariff *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
                                        )
                                        OutlinedTextField(
                                            value = item["year"] ?: "",
                                            onValueChange = { newVal ->
                                                val filtered = newVal.filter { it.isDigit() }.take(4)
                                                passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("year", filtered) }
                                            },
                                            label = { Text("Year *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = item["color"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("color", it) } }, label = { Text("Color *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
                                        OutlinedTextField(
                                            value = item["weight"]?.filter { it.isDigit() } ?: "",
                                            onValueChange = { newVal ->
                                                val digitsOnly = newVal.filter { it.isDigit() }
                                                passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("weight", if (digitsOnly.isNotBlank()) "$digitsOnly kg" else "") }
                                            },
                                            label = { Text("Weight *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                            suffix = { Text("kg", color = Color.Gray, fontSize = 12.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
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
                                            } catch (_: Exception) { null }
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
                                            label = { Text("Registration Date *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date", modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                                    }
                                    // -----------------------------------------

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = item["engineNo"] ?: "", onValueChange = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("engineNo", it) } }, label = { Text("Engine No *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
                                        OutlinedTextField(
                                            value = item["displacement"]?.filter { it.isDigit() } ?: "",
                                            onValueChange = { newVal ->
                                                val digitsOnly = newVal.filter { it.isDigit() }
                                                passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("displacement", if (digitsOnly.isNotBlank()) "$digitsOnly cc" else "") }
                                            },
                                            label = { Text("Displ. *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                            suffix = { Text("cc", color = Color.Gray, fontSize = 12.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
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
                                            label = { Text("Fuel Type *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        ExposedDropdownMenu(
                                            expanded = fuelExpanded,
                                            onDismissRequest = { fuelExpanded = false }
                                        ) {
                                            fuelOptions.forEach { (displayStr, dbCode) ->
                                                DropdownMenuItem(
                                                    text = { Text(displayStr, fontSize = 13.sp) },
                                                    onClick = { passengerCars[idx] = passengerCars[idx].toMutableMap().apply { put("fuelType", dbCode) }; fuelExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (totalVehicles < 4 && passengerCars.isEmpty()) {
                            OutlinedButton(onClick = { passengerCars.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G")) }, modifier = Modifier.align(Alignment.End).height(36.dp), shape = CircleShape) { Text("+ Add Pass. Car", fontSize = 11.sp) }
                        }
                    }
                }
                3 -> { // Step 4: Review
                    ElevatedCard(
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Comprehensive Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                            HorizontalDivider(color = MaterialTheme.colorScheme.background)

                            Text("Donee Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Name: ${doneeName.ifBlank { "Unspecified" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Address: ${doneeAddr.ifBlank { "Unspecified" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Contact Person: ${doneeContact.ifBlank { "Unspecified" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tel: ${doneeTel.ifBlank { "N/A" }} | Fax: ${doneeFax.ifBlank { "N/A" }} | Email: ${doneeEmail.ifBlank { "N/A" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(Modifier.height(4.dp))

                            Text("Donor Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Name: ${donorName.ifBlank { "Unspecified" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Address: ${donorAddr.ifBlank { "Unspecified" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tel: ${donorTel.ifBlank { "N/A" }} | Fax: ${donorFax.ifBlank { "N/A" }} | Email: ${donorEmail.ifBlank { "N/A" }}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(Modifier.height(4.dp))

                            if (isMotorVehicleSelected && motorVehicles.isNotEmpty()) {
                                Text("Motor Vehicles (${motorVehicles.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                motorVehicles.forEachIndexed { idx, mv ->
                                    Text("#${idx + 1}: ${mv["desc"]} (Tariff: ${mv["tariffCode"]}, Origin: ${mv["origin"]}, Qty: ${mv["qty"]})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(4.dp))
                            }

                            if (isPassengerCarSelected && passengerCars.isNotEmpty()) {
                                Text("Passenger Car (${passengerCars.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                passengerCars.forEachIndexed { idx, pc ->
                                    Text("#${idx + 1}: ${pc["desc"]} (Tariff: ${pc["tariffCode"]}, Origin: ${pc["origin"]})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("    VIN: ${pc["vin"]} (Year: ${pc["year"]}, Color: ${pc["color"]}, Fuel: ${pc["fuelType"]})", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("    Reg: ${pc["regDate"]}, Wt: ${pc["weight"]}, Eng: ${pc["engineNo"]}, Displ: ${pc["displacement"]}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(4.dp))
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.background)
                            Text("Authorized Signature (Optional):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedButton(onClick = { filePickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth().height(40.dp), shape = CircleShape) { Text("Import Signature", fontSize = 12.sp) }
                            Text(signatureUriText, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        // COMPACT NAVIGATION ROW
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                IconButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                }
            }

            if (currentStep < 3) {
                OutlinedButton(
                    onClick = { clearFieldsForCurrentSection() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) { Text("CLEAR", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
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
                                    .onSuccess {
                                        Toast.makeText(context, "Submitted successfully!", Toast.LENGTH_LONG).show()
                                        globalResetAllFields()
                                        navController.navigate("history") { popUpTo("form") { inclusive = true } }
                                    }
                                    .onFailure { tx -> Toast.makeText(context, "Database Error: ${tx.message}", Toast.LENGTH_LONG).show() }
                            } catch (_: Exception) {
                                Toast.makeText(context, "Storage Save Failure.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = if (currentStep < 3) Modifier.size(40.dp) else Modifier.weight(1f).height(40.dp),
                shape = CircleShape,
                contentPadding = if (currentStep < 3) PaddingValues(0.dp) else ButtonDefaults.ContentPadding,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (currentStep < 3) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue", tint = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("SUBMIT", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryLogScreen(refreshTrigger: Long, onRefreshRequested: () -> Unit) {
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
    var isEditingEntity by remember { mutableStateOf(false) }

    // Dialog state for unsaved edits on Donor/Donee
    var showEntityCancelDialog by remember { mutableStateOf(false) }

    // Deletion states
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

    // Unsaved Changes Confirmation Dialog for Donor/Donee Edit
    if (showEntityCancelDialog) {
        AlertDialog(
            onDismissRequest = { showEntityCancelDialog = false },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(36.dp)) },
            title = { Text("Discard Unsaved Changes?", fontSize = 16.sp) },
            text = { Text("You have made changes to this record. If you leave now, all your edits will be discarded.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showEntityCancelDialog = false
                        isEditingEntity = false // Discard edits and return to view mode
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("DISCARD", fontSize = 12.sp) }
            },
            dismissButton = { TextButton(onClick = { showEntityCancelDialog = false }) { Text("KEEP EDITING", fontSize = 12.sp, color = Color.Gray) } },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large
        )
    }

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
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("DELETE") }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { isEditingLog = true }) { Text("EDIT", fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { selectedLogForDetail = null }) { Text("CLOSE") }
                        }
                    }
                },
                title = { Text("Application Details", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DetailRow("Application ID", log[0])
                            DetailRow("Date Submitted", log[1])
                            DetailRow("Donor Name", log[3])
                            DetailRow("Donee Name", log[2])
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        Text("Donated Vehicles", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        if (fastAppVehicleDetails.isEmpty()) {
                            Text("No vehicles found attached to this application.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            fastAppVehicleDetails.forEachIndexed { index, vehicle ->
                                Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Vehicle #${index + 1}: ${vehicle["CarType"]}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
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

        // Draft states for inline editing
        val originalName = entity["DonorName"] ?: entity["DoneeName"] ?: ""
        val originalAddress = entity["DonorAddress"] ?: entity["DoneeAddress"] ?: ""
        val originalContactPerson = entity["ContactPerson"] ?: ""
        val originalTel = entity["DonorTelNo"] ?: entity["DoneeTelNo"] ?: ""
        val originalFax = entity["DonorFaxNo"] ?: entity["DoneeFaxNo"] ?: ""
        val originalEmail = entity["DonorEmail"] ?: entity["DoneeEmail"] ?: ""

        var draftName by remember(entity) { mutableStateOf(originalName) }
        var draftAddress by remember(entity) { mutableStateOf(originalAddress) }
        var draftContactPerson by remember(entity) { mutableStateOf(originalContactPerson) }
        var draftTel by remember(entity) { mutableStateOf(originalTel) }
        var draftFax by remember(entity) { mutableStateOf(originalFax) }
        var draftEmail by remember(entity) { mutableStateOf(originalEmail) }
        var isSavingEntity by remember { mutableStateOf(false) }

        val hasEntityUnsavedChanges = draftName != originalName ||
                draftAddress != originalAddress ||
                (isDonee && draftContactPerson != originalContactPerson) ||
                draftTel != originalTel ||
                draftFax != originalFax ||
                draftEmail != originalEmail

        val requestEntityDismiss = {
            if (isEditingEntity && hasEntityUnsavedChanges) {
                showEntityCancelDialog = true
            } else {
                isEditingEntity = false
                selectedEntityForDetail = null
            }
        }

        AlertDialog(
            onDismissRequest = {
                if (!isSavingEntity) {
                    requestEntityDismiss()
                }
            },
            confirmButton = {
                if (isEditingEntity) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = requestEntityDismiss, enabled = !isSavingEntity) { Text("CANCEL", color = Color.Gray) }
                        Button(
                            onClick = {
                                if (draftName.isBlank() || draftAddress.isBlank() || (isDonee && draftContactPerson.isBlank())) {
                                    Toast.makeText(context, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (draftTel.isBlank() && draftFax.isBlank() && draftEmail.isBlank()) {
                                    Toast.makeText(context, "Please provide at least one contact method.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    isSavingEntity = true
                                    try {
                                        val payload = mutableMapOf<String, String>(
                                            if (isDonor) "DonorName" to draftName else "DoneeName" to draftName,
                                            if (isDonor) "DonorAddress" to draftAddress else "DoneeAddress" to draftAddress,
                                            if (isDonor) "DonorTelNo" to draftTel else "DoneeTelNo" to draftTel,
                                            if (isDonor) "DonorFaxNo" to draftFax else "DoneeFaxNo" to draftFax,
                                            if (isDonor) "DonorEmail" to draftEmail else "DoneeEmail" to draftEmail
                                        )
                                        if (isDonee) payload["ContactPerson"] = draftContactPerson

                                        // Call DatabaseService to handle the updates
                                        val res = if (isDonor) DatabaseService.updateDonor(id!!, payload) else DatabaseService.updateDonee(id!!, payload)
                                        res.onSuccess {
                                            Toast.makeText(context, "Saved changes!", Toast.LENGTH_SHORT).show()
                                            isEditingEntity = false
                                            selectedEntityForDetail = null
                                            onRefreshRequested()
                                        }.onFailure {
                                            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } catch(e: Exception) {
                                        Toast.makeText(context, "Update failed.", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSavingEntity = false
                                    }
                                }
                            },
                            enabled = !isSavingEntity,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isSavingEntity) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White) else Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDonor || isDonee) {
                            TextButton(
                                onClick = {
                                    if (matchingApps.isNotEmpty()) {
                                        val entityTypeStr = if (isDonor) "donor" else "donee"
                                        entityDeleteError = "This $entityTypeStr is attached to an existing application. Please update the application or delete it first."
                                    } else {
                                        entityToDelete = Pair(if (isDonor) "Donor" else "Donee", id!!)
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("DELETE") }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isDonor || isDonee) {
                                TextButton(onClick = { isEditingEntity = true }) { Text("EDIT", fontWeight = FontWeight.Bold) }
                            }
                            TextButton(onClick = { selectedEntityForDetail = null; isEditingEntity = false }) { Text("CLOSE") }
                        }
                    }
                }
            },
            title = {
                Text(
                    text = if (isEditingEntity) "Edit ${if (isDonor) "Donor" else "Donee"}" else "More Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                if (isEditingEntity) {
                    // EDITED VIEW FOR DONOR / DONEE
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(value = draftName, onValueChange = { draftName = it }, label = { Text("Name *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                        OutlinedTextField(value = draftAddress, onValueChange = { draftAddress = it }, label = { Text("Address *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                        if (isDonee) {
                            OutlinedTextField(value = draftContactPerson, onValueChange = { draftContactPerson = it }, label = { Text("Contact Person *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                        }
                        Text("Contact Details (Choose at least one *):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        OutlinedTextField(value = draftTel, onValueChange = { draftTel = sanitizePhoneInput(it) }, label = { Text("Telephone", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                        OutlinedTextField(value = draftFax, onValueChange = { draftFax = sanitizePhoneInput(it) }, label = { Text("Fax Number", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                        OutlinedTextField(value = draftEmail, onValueChange = { draftEmail = it }, label = { Text("Email Address", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                    }
                } else {
                    // STANDARD READ-ONLY VIEW
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                        if (isDonor || isDonee) {
                            entity.forEach { (key, value) -> DetailRow(formatDatabaseKey(key), value) }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.background)
                            Text("Associated Applications", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)

                            if (matchingApps.isEmpty()) {
                                Text("No applications found.", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                matchingApps.forEach { appLog ->
                                    Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("App ID: ${appLog[0]} (${appLog[1]})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val mvs = motorVehicles.filter { it["ApplicationID"] == appLog[0] }
                                            val pcs = passengerCars.filter { it["ApplicationID"] == appLog[0] }
                                            mvs.forEach { Text("• Motor: ${it["VehicleDescription"]} (Qty: ${it["Quantity"]})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            pcs.forEach { Text("• Pass. Car: VIN ${it["VIN"]} (${it["YearModel"]})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            if (mvs.isEmpty() && pcs.isEmpty()) Text("• No vehicles attached", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        if (isVehicle) {
                            val appId = entity["ApplicationID"]
                            DetailRow("Application ID", appId ?: "N/A")
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.background)
                            Text("Vehicle Information", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                            val keysToHide = listOf("ApplicationID", "CarType")
                            entity.filterKeys { it !in keysToHide }.forEach { (key, value) -> DetailRow(formatDatabaseKey(key), value) }

                            if (appId != null) {
                                val logEntry = logs.find { it[0] == appId }
                                if (logEntry != null) {
                                    val donor = donorsDetailed.find { it["DonorID"] == logEntry[8] }
                                    val donee = doneesDetailed.find { it["DoneeID"] == logEntry[7] }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.background)
                                    Text("Donor Details", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                    donor?.forEach { (k, v) -> DetailRow(formatDatabaseKey(k), v) }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.background)
                                    Text("Donee Details", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                    donee?.forEach { (k, v) -> DetailRow(formatDatabaseKey(k), v) }
                                }
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
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(36.dp)) },
            title = { Text("Action Denied") },
            text = { Text(entityDeleteError!!) },
            confirmButton = { TextButton(onClick = { entityDeleteError = null }) { Text("OK") } },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large
        )
    }

    if (entityToDelete != null) {
        val (type, id) = entityToDelete!!
        AlertDialog(
            onDismissRequest = { entityToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("Delete $type?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Are you sure you want to permanently delete $type $id? This will erase their record completely.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        val entityId = id
                        val entityType = type
                        entityToDelete = null
                        selectedEntityForDetail = null
                        scope.launch {
                            val res = snackbarHostState.showSnackbar("Deleting $entityType $entityId...", "UNDO", duration = SnackbarDuration.Short)
                            if (res != SnackbarResult.ActionPerformed) {
                                val dbRes = if (entityType == "Donor") DatabaseService.deleteDonor(entityId) else DatabaseService.deleteDonee(entityId)
                                dbRes.onSuccess {
                                    Toast.makeText(context, "Permanently deleted!", Toast.LENGTH_SHORT).show()
                                    onRefreshRequested()
                                }.onFailure {
                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Deletion Cancelled. Record safe.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { entityToDelete = null }) { Text("CANCEL", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large
        )
    }

    if (showDeleteConfirmation != null) {
        val id = showDeleteConfirmation!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("Delete Application?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("You are about to permanently delete application $id. This action cannot be undone once the timer runs out.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = null
                        selectedLogForDetail = null
                        scope.launch {
                            val res = snackbarHostState.showSnackbar("Deleting $id...", "UNDO", duration = SnackbarDuration.Short)
                            if (res != SnackbarResult.ActionPerformed) {
                                DatabaseService.deleteApplication(id).onSuccess {
                                    Toast.makeText(context, "Permanently deleted!", Toast.LENGTH_SHORT).show()
                                    onRefreshRequested()
                                }
                            } else {
                                Toast.makeText(context, "Deletion Cancelled. Record restored.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("DELETE", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) { Text("CANCEL", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    actionColor = MaterialTheme.colorScheme.primary,
                    actionContentColor = MaterialTheme.colorScheme.primary,
                    snackbarData = data
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("History Logs", style = MaterialTheme.typography.headlineSmall)
                    Text("Review and manage application records.", style = MaterialTheme.typography.bodyMedium)
                }
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var isSearchFocused by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(
                            width = 1.dp,
                            color = if (isSearchFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF6D6D6D),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isSearchFocused = it.isFocused }
                        )
                    }
                }

                var filterExpanded by remember { mutableStateOf(false) }
                val displayCategory = viewCategory.replace("Passenger Car", "Passenger Car").replace("Motor Vehicle", "Motor Vehicle")

                Box {
                    ElevatedCard(
                        onClick = { filterExpanded = true },
                        shape = CircleShape,
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(displayCategory, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    DropdownMenu(expanded = filterExpanded, onDismissRequest = { filterExpanded = false }) {
                        listOf("Application", "Donor", "Donee", "Motor Vehicle", "Passenger Car").forEach { cat ->
                            DropdownMenuItem(text = { Text(cat, fontSize = 13.sp) }, onClick = { viewCategory = cat; filterExpanded = false })
                        }
                    }
                }

                ElevatedCard(
                    onClick = { sortOrder = if (sortOrder == "Descending") "Ascending" else "Descending" },
                    shape = CircleShape,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(if (sortOrder == "Descending") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
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
                                item { Text("Applications", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp), fontSize = 14.sp) }
                                items(sortData(matchedApps) { it[0] }) { item -> MobileAppCard(item, searchQuery) { selectedLogForDetail = item } }
                            }
                            if (matchedDonors.isNotEmpty()) {
                                item { Text("Donors", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp), fontSize = 14.sp) }
                                items(sortData(matchedDonors) { it["DonorID"] ?: "" }) { item -> MobileEntityCard("Donor", item, searchQuery) { selectedEntityForDetail = item } }
                            }
                            if (matchedDonees.isNotEmpty()) {
                                item { Text("Donees", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp), fontSize = 14.sp) }
                                items(sortData(matchedDonees) { it["DoneeID"] ?: "" }) { item -> MobileEntityCard("Donee", item, searchQuery) { selectedEntityForDetail = item } }
                            }
                            if (matchedMotors.isNotEmpty()) {
                                item { Text("Motor Vehicles", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp), fontSize = 14.sp) }
                                items(sortData(matchedMotors) { it["DonateID"] ?: "" }) { item -> MobileEntityCard("Motor Vehicle", item, searchQuery) { selectedEntityForDetail = item } }
                            }
                            if (matchedPassengers.isNotEmpty()) {
                                item { Text("Passenger Cars", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp), fontSize = 14.sp) }
                                items(sortData(matchedPassengers) { it["DonateID"] ?: "" }) { item -> MobileEntityCard("Passenger Car", item, searchQuery) { selectedEntityForDetail = item } }
                            }
                        }
                    } else {
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
                            items(finalDataList) { item ->
                                when (item) {
                                    is List<*> -> MobileAppCard(item as List<String>, searchQuery) { selectedLogForDetail = item }
                                    is Map<*, *> -> MobileEntityCard(viewCategory, @Suppress("UNCHECKED_CAST") (item as Map<String, String>), searchQuery) { selectedEntityForDetail = item }
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
fun MobileAppCard(log: List<String>, searchQuery: String, onClick: () -> Unit) {
    val badgeText = when (log[6]) {
        "Motor Vehicle / Passenger Car" -> "Motor Vehicle / Passenger Car"
        "Passenger Car" -> "Passenger Car"
        "Motor Vehicle" -> "Motor Vehicle"
        else -> log[6]
    }

    val badgeBgColor = if (log[6].contains("/")) Color(0xFFDBEAFE) else if (log[6] == "Passenger Car") Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
    val badgeTextColor = if (log[6].contains("/")) Color(0xFF2563EB) else if (log[6] == "Passenger Car") Color(0xFF16A34A) else Color(0xFFEA580C)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                HighlightedText(log[0], searchQuery, style = TextStyle(fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary))
                Box(modifier = Modifier.clip(CircleShape).background(badgeBgColor).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(badgeText, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = badgeTextColor)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Upload, contentDescription = "Donor", modifier = Modifier.size(16.dp), tint = Color(0xFFE11D48))
                Spacer(modifier = Modifier.width(8.dp))
                HighlightedText(log[3], searchQuery, style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = "Donee", modifier = Modifier.size(16.dp), tint = Color(0xFF22C55E))
                Spacer(modifier = Modifier.width(8.dp))
                HighlightedText(log[2], searchQuery, style = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.background)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Submitted", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Text(log[1].split(" ")[0], fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MobileEntityCard(category: String, map: Map<String, String>, searchQuery: String, onClick: () -> Unit) {
    val title = map["DonorName"] ?: map["DoneeName"] ?: map["VehicleDescription"] ?: map["VIN"] ?: "Unknown Entity"
    val subtitle = map["DonorID"] ?: map["DoneeID"] ?: map["DonateID"] ?: ""

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    HighlightedText(title, searchQuery, style = TextStyle(fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "View Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (category == "Donor" || category == "Donee") {
                Spacer(modifier = Modifier.height(12.dp))
                Text(map["DonorAddress"] ?: map["DoneeAddress"] ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
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
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(36.dp)) },
            title = { Text("Discard Unsaved Changes?", fontSize = 16.sp) },
            text = { Text("You have made changes to this application. If you leave now, all your edits will be lost.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { showCancelDialog = false; onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("DISCARD", fontSize = 12.sp) }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("KEEP EDITING", fontSize = 12.sp, color = Color.Gray) } },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large
        )
    }

    // Vehicle Deletion Confirmation Dialog
    if (itemToDelete != null) {
        val typeStr = if (itemToDelete!!.first == "MV") "Motor Vehicle" else "Passenger Car"
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("Remove $typeStr?", fontSize = 16.sp) },
            text = { Text("Are you sure you want to drop this vehicle from the application? You will need to save changes to apply this.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        hasUnsavedChanges = true
                        if (itemToDelete!!.first == "MV") draftMVs.removeAt(itemToDelete!!.second)
                        else draftPCs.removeAt(itemToDelete!!.second)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("REMOVE", fontSize = 12.sp) }
            },
            dismissButton = { TextButton(onClick = { itemToDelete = null }) { Text("CANCEL", fontSize = 12.sp, color = Color.Gray) } },
            containerColor = Color.White,
            shape = MaterialTheme.shapes.large
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
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White) else Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = { TextButton(onClick = requestDismiss, enabled = !isSaving) { Text("CANCEL", fontSize = 12.sp) } },
        title = { Text("Editing $appId", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // ---- DONEE SECTION ----
                Text("Donee Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                var doneeExpanded by remember { mutableStateOf(false) }
                val currentDoneeDisplay = if (isNewDonee) "New Donee..." else doneesDetailed.find { it["DoneeID"] == doneeSelection }?.get("DoneeName") ?: "Select Donee"

                ExposedDropdownMenuBox(expanded = doneeExpanded, onExpandedChange = { doneeExpanded = it }) {
                    OutlinedTextField(
                        value = currentDoneeDisplay, onValueChange = {}, readOnly = true,
                        label = { Text("Assigned Donee", fontSize = 12.sp) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doneeExpanded) },
                        textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.menuAnchor().fillMaxWidth(), shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(expanded = doneeExpanded, onDismissRequest = { doneeExpanded = false }) {
                        doneesDetailed.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d["DoneeName"]} (${d["DoneeID"]})", fontSize = 13.sp) },
                                onClick = { doneeSelection = d["DoneeID"]!!; isNewDonee = false; doneeExpanded = false; hasUnsavedChanges = true }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add New Donee", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            onClick = { isNewDonee = true; doneeExpanded = false; hasUnsavedChanges = true }
                        )
                    }
                }
                if (isNewDonee) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = doneeName, onValueChange = { doneeName = it; hasUnsavedChanges = true }, label = { Text("New Donee Name *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = doneeAddr, onValueChange = { doneeAddr = it; hasUnsavedChanges = true }, label = { Text("Address *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = doneeContact, onValueChange = { doneeContact = it; hasUnsavedChanges = true }, label = { Text("Contact Person *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            Text("Contact Details (Choose at least one *):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            OutlinedTextField(value = doneeTel, onValueChange = { doneeTel = sanitizePhoneInput(it); hasUnsavedChanges = true }, label = { Text("Telephone", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = doneeFax, onValueChange = { doneeFax = sanitizePhoneInput(it); hasUnsavedChanges = true }, label = { Text("Fax Number", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = doneeEmail, onValueChange = { doneeEmail = it; hasUnsavedChanges = true }, label = { Text("Email Address", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                        }
                    }
                }

                HorizontalDivider()

                // ---- DONOR SECTION ----
                Text("Donor Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                var donorExpanded by remember { mutableStateOf(false) }
                val currentDonorDisplay = if (isNewDonor) "New Donor..." else donorsDetailed.find { it["DonorID"] == donorSelection }?.get("DonorName") ?: "Select Donor"

                ExposedDropdownMenuBox(expanded = donorExpanded, onExpandedChange = { donorExpanded = it }) {
                    OutlinedTextField(
                        value = currentDonorDisplay, onValueChange = {}, readOnly = true,
                        label = { Text("Assigned Donor", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = donorExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(expanded = donorExpanded, onDismissRequest = { donorExpanded = false }) {
                        donorsDetailed.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d["DonorName"]} (${d["DonorID"]})", fontSize = 13.sp) },
                                onClick = { donorSelection = d["DonorID"]!!; isNewDonor = false; donorExpanded = false; hasUnsavedChanges = true }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("+ Add New Donor", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            onClick = { isNewDonor = true; donorExpanded = false; hasUnsavedChanges = true }
                        )
                    }
                }
                if (isNewDonor) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = donorName, onValueChange = { donorName = it; hasUnsavedChanges = true }, label = { Text("New Donor Name *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = donorAddr, onValueChange = { donorAddr = it; hasUnsavedChanges = true }, label = { Text("Address *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                            Text("Contact Details (Choose at least one *):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            OutlinedTextField(value = donorTel, onValueChange = { donorTel = sanitizePhoneInput(it); hasUnsavedChanges = true }, label = { Text("Telephone", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = donorFax, onValueChange = { donorFax = sanitizePhoneInput(it); hasUnsavedChanges = true }, label = { Text("Fax Number", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                            OutlinedTextField(value = donorEmail, onValueChange = { donorEmail = it; hasUnsavedChanges = true }, label = { Text("Email Address", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                        }
                    }
                }

                HorizontalDivider()

                // ---- VEHICLES SECTION ----
                Text("Donated Vehicles", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                draftMVs.forEachIndexed { idx, mv ->
                    var isEditingThis by remember { mutableStateOf(false) }
                    Card(colors = CardDefaults.cardColors(containerColor = if(isEditingThis) Color.White else MaterialTheme.colorScheme.background), border = if(isEditingThis) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null, modifier = Modifier.fillMaxWidth()) {
                        if (!isEditingThis) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Motor Vehicle: ${mv["desc"]?.ifBlank { "Unspecified" }}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Tariff: ${mv["tariffCode"]}, Qty: ${mv["qty"]}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { isEditingThis = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { itemToDelete = Pair("MV", idx) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                            }
                        } else {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Editing Motor Vehicle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                OutlinedTextField(value = mv["desc"] ?: "", onValueChange = { draftMVs[idx] = draftMVs[idx].toMutableMap().apply{ put("desc", it) }; hasUnsavedChanges = true }, label = { Text("Description *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                                OutlinedTextField(value = mv["origin"] ?: "", onValueChange = { draftMVs[idx] = draftMVs[idx].toMutableMap().apply{ put("origin", it) }; hasUnsavedChanges = true }, label = { Text("Origin *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = mv["tariffCode"] ?: "", onValueChange = { val f = it.filter { c -> c.isDigit() }.take(4); draftMVs[idx] = draftMVs[idx].toMutableMap().apply{ put("tariffCode", f) }; hasUnsavedChanges = true }, label = { Text("Tariff *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
                                    OutlinedTextField(value = mv["qty"] ?: "", onValueChange = { val f = it.filter { c -> c.isDigit() }; draftMVs[idx] = draftMVs[idx].toMutableMap().apply{ put("qty", f) }; hasUnsavedChanges = true }, label = { Text("Qty *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
                                }
                                TextButton(onClick = { isEditingThis = false }, modifier = Modifier.align(Alignment.End)) { Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            }
                        }
                    }
                }

                draftPCs.forEachIndexed { idx, pc ->
                    var isEditingThis by remember { mutableStateOf(false) }
                    Card(colors = CardDefaults.cardColors(containerColor = if(isEditingThis) Color.White else MaterialTheme.colorScheme.background), border = if(isEditingThis) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null, modifier = Modifier.fillMaxWidth()) {
                        if (!isEditingThis) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Passenger Car: ${pc["desc"]?.ifBlank { "Unspecified" }}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("VIN: ${pc["vin"]}, Year: ${pc["year"]}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { isEditingThis = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { itemToDelete = Pair("PC", idx) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
                            }
                        } else {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Editing Passenger Car", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                OutlinedTextField(value = pc["desc"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("desc", it) }; hasUnsavedChanges = true }, label = { Text("Description *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                                OutlinedTextField(value = pc["vin"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("vin", it) }; hasUnsavedChanges = true }, label = { Text("VIN *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters), shape = MaterialTheme.shapes.medium)
                                OutlinedTextField(value = pc["origin"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("origin", it) }; hasUnsavedChanges = true }, label = { Text("Origin *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = pc["tariffCode"] ?: "", onValueChange = { val f = it.filter { c -> c.isDigit() }.take(4); draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("tariffCode", f) }; hasUnsavedChanges = true }, label = { Text("Tariff *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
                                    OutlinedTextField(value = pc["year"] ?: "", onValueChange = { val f = it.filter { c -> c.isDigit() }.take(4); draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("year", f) }; hasUnsavedChanges = true }, label = { Text("Year *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = pc["color"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("color", it) }; hasUnsavedChanges = true }, label = { Text("Color *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
                                    OutlinedTextField(
                                        value = pc["weight"]?.filter { it.isDigit() } ?: "",
                                        onValueChange = { newVal ->
                                            val digitsOnly = newVal.filter { it.isDigit() }
                                            draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("weight", if (digitsOnly.isNotBlank()) "$digitsOnly kg" else "") }
                                            hasUnsavedChanges = true
                                        },
                                        label = { Text("Weight *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                        suffix = { Text("kg", color = Color.Gray, fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
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
                                        } catch (_: Exception) { null }
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
                                        label = { Text("Registration Date *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Select Date", modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                                }
                                // ---------------------------------

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = pc["engineNo"] ?: "", onValueChange = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("engineNo", it) }; hasUnsavedChanges = true }, label = { Text("Engine No *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
                                    OutlinedTextField(
                                        value = pc["displacement"]?.filter { it.isDigit() } ?: "",
                                        onValueChange = { newVal ->
                                            val digitsOnly = newVal.filter { it.isDigit() }
                                            draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("displacement", if (digitsOnly.isNotBlank()) "$digitsOnly cc" else "") }
                                            hasUnsavedChanges = true
                                        },
                                        label = { Text("Displacement *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp),
                                        suffix = { Text("cc", color = Color.Gray, fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
                                    )
                                }

                                var fuelExpanded by remember { mutableStateOf(false) }
                                val fuelOptions = listOf("G: Gas" to "G", "D: Diesel" to "D", "LPG: Liquid Petroleum Gas" to "LPG", "E: Electric" to "E", "H: Hydrogen Fuel" to "H")
                                val currentFuelCode = pc["fuelType"] ?: "G"
                                val currentFuelDisplay = fuelOptions.find { it.second == currentFuelCode }?.first ?: "Gas"

                                ExposedDropdownMenuBox(expanded = fuelExpanded, onExpandedChange = { fuelExpanded = !fuelExpanded }, modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(value = currentFuelDisplay, onValueChange = {}, readOnly = true, label = { Text("Fuel Type *", fontSize = 12.sp) }, textStyle = TextStyle(fontSize = 13.sp), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                                    ExposedDropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }) {
                                        fuelOptions.forEach { (displayStr, dbCode) ->
                                            DropdownMenuItem(
                                                text = { Text(displayStr, fontSize = 13.sp) },
                                                onClick = { draftPCs[idx] = draftPCs[idx].toMutableMap().apply{ put("fuelType", dbCode) }; fuelExpanded = false; hasUnsavedChanges = true }
                                            )
                                        }
                                    }
                                }
                                TextButton(onClick = { isEditingThis = false }, modifier = Modifier.align(Alignment.End)) { Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            }
                        }
                    }
                }

                // Check overall vehicle count
                val totalVehicles = draftMVs.size + draftPCs.size

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (totalVehicles < 4) {
                        OutlinedButton(
                            onClick = { draftMVs.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "qty" to "1")); hasUnsavedChanges = true },
                            modifier = Modifier.weight(1f).height(36.dp), shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) { Text("+ Motor Vehicle", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (totalVehicles < 4 && draftPCs.isEmpty()) {
                        OutlinedButton(
                            onClick = { draftPCs.add(mutableMapOf("desc" to "", "tariffCode" to "", "origin" to "", "vin" to "", "year" to "", "color" to "", "regDate" to "", "weight" to "", "engineNo" to "", "displacement" to "", "fuelType" to "G")); hasUnsavedChanges = true },
                            modifier = Modifier.weight(1f).height(36.dp), shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) { Text("+ Pass. Car", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // Show limit warnings if applicable
                if (totalVehicles >= 4) {
                    Text(text = "Combined maximum of 4 vehicles reached.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (draftPCs.isNotEmpty()) {
                    Text(text = "Maximum of 1 Passenger Car allowed per application.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
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
            withStyle(SpanStyle(background = Color(0xFFFDE047).copy(alpha = 0.5f), fontWeight = FontWeight.Bold)) { append(text.substring(index, index + query.length)) }
            start = index + query.length
        }
    }
    Text(text = annotatedString, style = style, textAlign = textAlign)
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}