package com.gandhasiri.app.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gandhasiri.app.data.GandhaSiriDatabase
import com.gandhasiri.app.data.entities.AlertLog
import com.gandhasiri.app.data.entities.PatrolLog
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// --- DATA CLASSES ---

data class ChecklistItem(
    val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val weight: Int
)

// --- VIEWMODEL ---

class SecurityViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GandhaSiriDatabase.getDatabase(application)
    private val alertLogDao = db.alertLogDao()
    private val patrolLogDao = db.patrolLogDao()
    private val treeDao = db.treeDao()
    private val prefs = application.getSharedPreferences("gandhasiri_security", Context.MODE_PRIVATE)

    private val physicalItems = listOf(
        ChecklistItem("physical_1", "Perimeter Fencing Installed", "Barbed wire or concrete wall around the entire plantation boundary.", "HIGH", 15),
        ChecklistItem("physical_2", "Motion Sensor Lights Installed", "Cover all entry points and tree rows with motion activated lights.", "HIGH", 15),
        ChecklistItem("physical_3", "CCTV Cameras Operational", "Minimum 4 cameras covering entry gate, tree rows, and perimeter.", "HIGH", 15),
        ChecklistItem("physical_4", "Farm Gate is Locked at Night", "Use a heavy duty padlock.", "HIGH", 10),
        ChecklistItem("physical_5", "Guard Dog Present on Farm", "A dog alerts to night movement before any human can.", "MEDIUM", 8),
        ChecklistItem("physical_6", "Thorny Hedge Around Trees", "Plant Bougainvillea or Lantana as a secondary inner barrier.", "MEDIUM", 7),
        ChecklistItem("physical_7", "Security Guard Employed at Night", "For large plantations above 50 trees, a guard is recommended.", "MEDIUM", 10),
        ChecklistItem("physical_8", "Microchip or Paint Marker on Trees", "RFID microchips or UV paint markers help prove ownership.", "LOW", 5)
    )

    private val digitalItems = listOf(
        ChecklistItem("digital_1", "All Trees Registered in This App", "Every tree must have a Tree ID, photo, and GPS in GandhaSiri.", "HIGH", 10),
        ChecklistItem("digital_2", "Plantation Registered with Forest Dept", "Visit your DFO and submit documents. Legal registration helps.", "HIGH", 10),
        ChecklistItem("digital_3", "Neighbor Emergency Contact Saved", "Save at least one neighbor's number for the panic system.", "HIGH", 8),
        ChecklistItem("digital_4", "Local Police Station Number Saved", "Save your nearest police station number for emergencies.", "MEDIUM", 5),
        ChecklistItem("digital_5", "Photos of All Trees Taken Recently", "Update tree photos every 6 months for stronger legal evidence.", "MEDIUM", 4),
        ChecklistItem("digital_6", "KSDL Farmer Registration Done", "Register at ksdl.karnataka.gov.in for theft support programs.", "LOW", 3)
    )

    private val _checklistState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val checklistState: StateFlow<Map<String, Boolean>> = _checklistState.asStateFlow()

    private val _securityScore = MutableStateFlow(0)
    val securityScore: StateFlow<Int> = _securityScore.asStateFlow()

    val patrolLogs = patrolLogDao.getLastFiveFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val alertLogs = alertLogDao.getLatestFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    private val calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    val patrolCountThisMonth = patrolLogDao.getCountThisMonth(calendar.timeInMillis).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val alertCountThisMonth = alertLogDao.getCountThisMonth(calendar.timeInMillis).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _aiSecurityAdvice = MutableStateFlow(prefs.getString("ai_security_advice", "Tap the button to get a personalized AI security assessment for your farm.") ?: "")
    val aiSecurityAdvice: StateFlow<String> = _aiSecurityAdvice.asStateFlow()

    private val _isLoadingAI = MutableStateFlow(false)
    val isLoadingAI: StateFlow<Boolean> = _isLoadingAI.asStateFlow()

    init {
        loadChecklist()
    }

    private fun loadChecklist() {
        val state = mutableMapOf<String, Boolean>()
        physicalItems.forEach { state[it.id] = prefs.getBoolean("security_item_${it.id}", false) }
        digitalItems.forEach { state[it.id] = prefs.getBoolean("security_item_${it.id}", false) }
        _checklistState.value = state
        calculateScore()
    }

    fun toggleChecklistItem(id: String) {
        val newState = _checklistState.value.toMutableMap()
        val current = newState[id] ?: false
        newState[id] = !current
        _checklistState.value = newState
        
        prefs.edit().putBoolean("security_item_$id", !current).apply()
        calculateScore()
    }

    private fun calculateScore() {
        var score = 0
        physicalItems.forEach { if (_checklistState.value[it.id] == true) score += it.weight }
        digitalItems.forEach { if (_checklistState.value[it.id] == true) score += it.weight }
        
        // Cap at 100
        val finalScore = score.coerceAtMost(100)
        _securityScore.value = finalScore
        prefs.edit().putInt("security_score", finalScore).apply()
        Log.d("GandhaSiri", "Security score recalculated with new value: $finalScore")
    }

    fun savePatrolLog(status: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val log = PatrolLog(timestamp = System.currentTimeMillis(), status = status, notes = notes)
            patrolLogDao.insert(log)
            Log.d("GandhaSiri", "Patrol log saved with status: $status")
        }
    }

    fun reportPanic(lat: Double, lng: Double, treeCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val alert = AlertLog(type = "PANIC", timestamp = System.currentTimeMillis(), note = "$lat, $lng")
            alertLogDao.insert(alert)
            Log.d("GandhaSiri", "Alert log inserted for panic at $lat, $lng")
            
            // Auto-check neighbor emergency contact saved item
            if (_checklistState.value["digital_3"] != true) {
                withContext(Dispatchers.Main) {
                    toggleChecklistItem("digital_3")
                }
            }
        }
    }

    fun getAIAdvice(treeCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingAI.value = true
            val checked = mutableListOf<String>()
            val unchecked = mutableListOf<String>()
            
            physicalItems.forEach { if (_checklistState.value[it.id] == true) checked.add(it.title) else unchecked.add(it.title) }
            digitalItems.forEach { if (_checklistState.value[it.id] == true) checked.add(it.title) else unchecked.add(it.title) }

            val prompt = """
                I am a sandalwood farmer in Karnataka India. I have $treeCount sandalwood trees on my farm. My current security score is ${_securityScore.value} out of 100. I have completed these security measures: ${checked.joinToString(", ")}. I have not yet done these: ${unchecked.joinToString(", ")}. Based on this, give me 3 specific actionable security improvements I should prioritize first for my farm size and situation. Keep the response under 120 words. Format as 3 numbered points only. Return the response as plain text with no markdown.
            """.trimIndent()

            try {
                val model = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = com.gandhasiri.app.BuildConfig.GEMINI_API_KEY,
                    requestOptions = RequestOptions(apiVersion = "v1beta")
                )
                val response = model.generateContent(prompt)
                val text = response.text ?: "Could not generate advice at this time."
                _aiSecurityAdvice.value = text
                prefs.edit().putString("ai_security_advice", text).apply()
                Log.d("GandhaSiri", "Gemini security assessment fetched with response length: ${text.length}")
            } catch (e: Exception) {
                Log.e("GandhaSiri", "Gemini API call failed", e)
            } finally {
                _isLoadingAI.value = false
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun SecurityScoreCard(score: Int) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "scoreAnimation"
    )

    val (label, color, tip) = when {
        score < 40 -> Triple("Danger", com.gandhasiri.app.ui.theme.PanicRed, "Your farm is at high risk. Complete the checklist immediately.")
        score < 70 -> Triple("Moderate", Color(0xFFF59E0B), "Good start. A few more steps will significantly reduce theft risk.")
        score < 90 -> Triple("Good", com.gandhasiri.app.ui.theme.SuccessGreen, "Your farm is well protected. Keep monitoring regularly.")
        else -> Triple("Fully Secured", Color(0xFF14532D), "Your farm is well protected. Keep monitoring regularly.")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                CircularProgressIndicator(
                    progress = animatedScore / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 12.dp,
                    trackColor = color.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$animatedScore",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                    Text(
                        text = "/ 100",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.gandhasiri.app.ui.theme.MidBrown
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (score >= 90) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = label.uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = tip,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = com.gandhasiri.app.ui.theme.DarkWood,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ChecklistSection(
    title: String,
    icon: ImageVector,
    items: List<ChecklistItem>,
    state: Map<String, Boolean>,
    onToggle: (String) -> Unit,
    onItemClick: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = com.gandhasiri.app.ui.theme.Sandalwood)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = com.gandhasiri.app.ui.theme.NearBlackBrown
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    ChecklistRow(
                        item = item,
                        isChecked = state[item.id] ?: false,
                        onToggle = { onToggle(item.id) },
                        onClick = { onItemClick?.invoke(item.id) }
                    )
                    if (index < items.size - 1) Divider(color = com.gandhasiri.app.ui.theme.WarmCream, thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun ChecklistRow(item: ChecklistItem, isChecked: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = com.gandhasiri.app.ui.theme.Sandalwood)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
            Text(item.description, fontSize = 12.sp, color = com.gandhasiri.app.ui.theme.MidBrown, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Badge(
            containerColor = when(item.priority) {
                "HIGH" -> com.gandhasiri.app.ui.theme.PanicRed.copy(alpha = 0.1f)
                "MEDIUM" -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                else -> com.gandhasiri.app.ui.theme.SuccessGreen.copy(alpha = 0.1f)
            },
            contentColor = when(item.priority) {
                "HIGH" -> com.gandhasiri.app.ui.theme.PanicRed
                "MEDIUM" -> Color(0xFFD97706)
                else -> com.gandhasiri.app.ui.theme.SuccessGreen
            }
        ) {
            Text(item.priority, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- MAIN SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onProfileClick: () -> Unit,
    viewModel: SecurityViewModel = viewModel(
        factory = SecurityViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    val score by viewModel.securityScore.collectAsState()
    val checklistState by viewModel.checklistState.collectAsState()
    val patrolLogs by viewModel.patrolLogs.collectAsState()
    val alertLogs by viewModel.alertLogs.collectAsState()
    val patrolCount by viewModel.patrolCountThisMonth.collectAsState()
    val alertCount by viewModel.alertCountThisMonth.collectAsState()
    val aiAdvice by viewModel.aiSecurityAdvice.collectAsState()
    val isLoadingAI by viewModel.isLoadingAI.collectAsState()
    
    var showPatrolSheet by remember { mutableStateOf(false) }
    var showKsdlSheet by remember { mutableStateOf(false) }
    var showPanicConfirm by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("gandhasiri_security", Context.MODE_PRIVATE)
    var neighborNumber by remember { mutableStateOf(prefs.getString("emergency_contact_number", "") ?: "") }
    var policeNumber by remember { mutableStateOf(prefs.getString("police_number", "") ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GandhaSiri Security", color = Color.White, fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.gandhasiri.app.ui.theme.DarkWood),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                }
            )
        },
        containerColor = com.gandhasiri.app.ui.theme.WarmCream
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // SECTION 1: SCORE CARD
            SecurityScoreCard(score)

            // SECTION 2: PHYSICAL
            ChecklistSection(
                title = "Physical Protection",
                icon = Icons.Default.Security,
                items = (viewModel as SecurityViewModel).let {
                    listOf(
                        ChecklistItem("physical_1", "Perimeter Fencing Installed", "Barbed wire or concrete wall around the entire plantation boundary.", "HIGH", 15),
                        ChecklistItem("physical_2", "Motion Sensor Lights Installed", "Cover all entry points and tree rows with motion activated lights.", "HIGH", 15),
                        ChecklistItem("physical_3", "CCTV Cameras Operational", "Minimum 4 cameras covering entry gate, tree rows, and perimeter.", "HIGH", 15),
                        ChecklistItem("physical_4", "Farm Gate is Locked at Night", "Use a heavy duty padlock.", "HIGH", 10),
                        ChecklistItem("physical_5", "Guard Dog Present on Farm", "A dog alerts to night movement before any human can.", "MEDIUM", 8),
                        ChecklistItem("physical_6", "Thorny Hedge Around Trees", "Plant Bougainvillea or Lantana as a secondary inner barrier.", "MEDIUM", 7),
                        ChecklistItem("physical_7", "Security Guard Employed at Night", "For large plantations above 50 trees, a guard is recommended.", "MEDIUM", 10),
                        ChecklistItem("physical_8", "Microchip or Paint Marker on Trees", "RFID microchips or UV paint markers help prove ownership.", "LOW", 5)
                    )
                },
                state = checklistState,
                onToggle = { viewModel.toggleChecklistItem(it) }
            )

            // SECTION 3: DIGITAL
            ChecklistSection(
                title = "Digital and Legal Shield",
                icon = Icons.Default.Description,
                items = listOf(
                    ChecklistItem("digital_1", "All Trees Registered in This App", "Every tree must have a Tree ID, photo, and GPS in GandhaSiri.", "HIGH", 10),
                    ChecklistItem("digital_2", "Plantation Registered with Forest Dept", "Visit your DFO and submit documents. Legal registration helps.", "HIGH", 10),
                    ChecklistItem("digital_3", "Neighbor Emergency Contact Saved", "Save at least one neighbor's number for the panic system.", "HIGH", 8),
                    ChecklistItem("digital_4", "Local Police Station Number Saved", "Save your nearest police station number for emergencies.", "MEDIUM", 5),
                    ChecklistItem("digital_5", "Photos of All Trees Taken Recently", "Update tree photos every 6 months for stronger legal evidence.", "MEDIUM", 4),
                    ChecklistItem("digital_6", "KSDL Farmer Registration Done", "Register at ksdl.karnataka.gov.in for theft support programs.", "LOW", 3)
                ),
                state = checklistState,
                onToggle = { viewModel.toggleChecklistItem(it) },
                onItemClick = { id ->
                    when(id) {
                        "digital_2" -> showKsdlSheet = true
                        "digital_3" -> scope.launch { scrollState.animateScrollTo(scrollState.maxValue) }
                        "digital_6" -> {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ksdl.karnataka.gov.in"))
                            context.startActivity(intent)
                        }
                    }
                }
            )

            // SECTION 4: PATROL LOG
            SectionHeader("Night Patrol Log", Icons.Default.DarkMode, patrolCount)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Button(
                        onClick = { showPatrolSheet = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.DarkWood)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LOG TONIGHT'S PATROL", fontWeight = FontWeight.Bold)
                    }
                    
                    if (patrolLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        patrolLogs.take(5).forEach { log ->
                            PatrolRow(log)
                        }
                    }
                }
            }

            // SECTION 5: EMERGENCY PANIC
            SectionHeader("Emergency Alert System", Icons.Default.NotificationsActive, alertCount)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = neighborNumber,
                        onValueChange = { neighborNumber = it },
                        label = { Text("Neighbor WhatsApp Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = policeNumber,
                        onValueChange = { policeNumber = it },
                        label = { Text("Local Police Station Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            prefs.edit()
                                .putString("emergency_contact_number", neighborNumber)
                                .putString("police_number", policeNumber)
                                .apply()
                            Toast.makeText(context, "Contacts Saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.Sandalwood),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SAVE CONTACTS", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    PanicButton(onClick = { showPanicConfirm = true })
                    
                    if (alertLogs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Alert History", fontWeight = FontWeight.Bold, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
                        Spacer(modifier = Modifier.height(12.dp))
                        alertLogs.take(10).forEach { alert ->
                            AlertLogItem(alert) { lat, lng ->
                                val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                context.startActivity(mapIntent)
                            }
                        }
                    }
                }
            }

            // AI ASSESSMENT
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = com.gandhasiri.app.ui.theme.PureWhite),
                border = BorderStroke(1.dp, com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = com.gandhasiri.app.ui.theme.Sandalwood, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("AI Security Assessment", fontSize = 20.sp, fontWeight = FontWeight.Black, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoadingAI) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = com.gandhasiri.app.ui.theme.Sandalwood)
                    } else {
                        aiAdvice.split("\n").filter { it.isNotBlank() }.forEach { point ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Eco, contentDescription = null, tint = com.gandhasiri.app.ui.theme.Sandalwood, modifier = Modifier.size(16.dp).padding(top = 4.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(point.trim(), fontSize = 14.sp, color = com.gandhasiri.app.ui.theme.DarkWood, lineHeight = 20.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    TextButton(
                        onClick = { viewModel.getAIAdvice(0) }, // Replace 0 with actual tree count if available
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("REGENERATE", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // BOTTOM SHEETS & DIALOGS
    if (showPatrolSheet) {
        ModalBottomSheet(onDismissRequest = { showPatrolSheet = false }) {
            PatrolBottomSheetContent { status, notes ->
                viewModel.savePatrolLog(status, notes)
                showPatrolSheet = false
            }
        }
    }

    if (showKsdlSheet) {
        ModalBottomSheet(onDismissRequest = { showKsdlSheet = false }) {
            Column(modifier = Modifier.padding(24.dp).padding(bottom = 40.dp)) {
                Text("Forest Department Contacts", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Address:", fontWeight = FontWeight.Bold)
                Text("Room 448, 4th Floor, MS Building, Bengaluru 560001")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Phone:", fontWeight = FontWeight.Bold)
                Text("080 2225 6722")
            }
        }
    }

    if (showPanicConfirm) {
        AlertDialog(
            onDismissRequest = { showPanicConfirm = false },
            title = { Text("CONFIRM EMERGENCY ALERT", color = com.gandhasiri.app.ui.theme.PanicRed, fontWeight = FontWeight.Black) },
            text = { Text("This will send a WhatsApp alert to your emergency contact and log this incident. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        showPanicConfirm = false
                        handlePanic(context, neighborNumber) { lat, lng ->
                            viewModel.reportPanic(lat, lng, 0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.PanicRed)
                ) {
                    Text("YES, SEND ALERT")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicConfirm = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, count: Int = 0) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = com.gandhasiri.app.ui.theme.Sandalwood)
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
        if (count > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Surface(color = com.gandhasiri.app.ui.theme.Sandalwood.copy(alpha = 0.1f), shape = CircleShape) {
                Text("$count this month", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = com.gandhasiri.app.ui.theme.Sandalwood)
            }
        }
    }
}

@Composable
fun PatrolRow(log: PatrolLog) {
    val date = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(log.timestamp))
    val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
    
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$date at $time", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = com.gandhasiri.app.ui.theme.NearBlackBrown)
            if (log.notes.isNotBlank()) {
                Text(log.notes, fontSize = 12.sp, color = com.gandhasiri.app.ui.theme.MidBrown, maxLines = 1)
            }
        }
        Badge(
            containerColor = if (log.status == "All Clear") com.gandhasiri.app.ui.theme.SuccessGreen.copy(alpha = 0.1f) else com.gandhasiri.app.ui.theme.PanicRed.copy(alpha = 0.1f),
            contentColor = if (log.status == "All Clear") com.gandhasiri.app.ui.theme.SuccessGreen else com.gandhasiri.app.ui.theme.PanicRed
        ) {
            Text(log.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AlertLogItem(alert: AlertLog, onMapClick: (Double, Double) -> Unit) {
    val time = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(alert.timestamp))
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(time, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(alert.note, fontSize = 11.sp, color = com.gandhasiri.app.ui.theme.Sandalwood, modifier = Modifier.clickable {
                val parts = alert.note.split(",")
                if (parts.size == 2) {
                    onMapClick(parts[0].trim().toDouble(), parts[1].trim().toDouble())
                }
            })
        }
        Badge(containerColor = com.gandhasiri.app.ui.theme.PanicRed.copy(alpha = 0.1f), contentColor = com.gandhasiri.app.ui.theme.PanicRed) {
            Text(alert.type, modifier = Modifier.padding(horizontal = 6.dp))
        }
    }
}

@Composable
fun PanicButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "panicPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.PanicRed),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Icon(Icons.Default.NotificationImportant, contentDescription = null, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text("REPORT SUSPICIOUS ACTIVITY", fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatrolBottomSheetContent(onSave: (String, String) -> Unit) {
    var status by remember { mutableStateOf("All Clear") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(24.dp).padding(bottom = 40.dp)) {
        Text("Night Patrol Log", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Time: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())}", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = status,
                onValueChange = {},
                readOnly = true,
                label = { Text("Status") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("All Clear", "Suspicious Activity Seen", "Stranger Near Farm").forEach { s ->
                    DropdownMenuItem(text = { Text(s) }, onClick = { status = s; expanded = false })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (Optional)") },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(12.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { onSave(status, notes) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = com.gandhasiri.app.ui.theme.DarkWood)
        ) {
            Text("SAVE PATROL LOG", fontWeight = FontWeight.Bold)
        }
    }
}

private fun handlePanic(context: Context, phone: String, onLogged: (Double, Double) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0
                val timestamp = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                
                val msg = "URGENT GandhaSiri Security Alert. Suspicious activity detected near my sandalwood farm. Please check immediately. Farm location: https://maps.google.com/?q=$lat,$lng. Time: $timestamp."
                val encodedMsg = Uri.encode(msg)
                
                val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=$encodedMsg"))
                try {
                    context.startActivity(whatsappIntent)
                } catch (e: Exception) {
                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                    smsIntent.putExtra("sms_body", msg)
                    context.startActivity(smsIntent)
                }
                
                // Log incident
                onLogged(lat, lng)
                
                // Show Notification
                showNotification(context)
                
                Toast.makeText(context, "Alert sent successfully", Toast.LENGTH_SHORT).show()
            }
    } catch (e: SecurityException) {
        Log.e("GandhaSiri", "GPS fetch failed", e)
    }
}

private fun showNotification(context: Context) {
    val channelId = "gandhasiri_emergency"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(channelId, "Security Alerts", android.app.NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }
    
    val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Security Alert GandhaSiri")
        .setContentText("Panic button activated. Emergency contact notified.")
        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        
    notificationManager.notify(1, builder.build())
}

// --- FACTORY ---

class SecurityViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SecurityViewModel(application) as T
    }
}
