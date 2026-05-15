package com.gandhasiri.app.ui.screens
import com.gandhasiri.app.R
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gandhasiri.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(onProfileClick: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_before_planting),
        stringResource(R.string.tab_growing_phase),
        stringResource(R.string.tab_harvest_process),
        stringResource(R.string.tab_selling_transport)
    )
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.legal_guide_title), 
                        color = Color.White, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkWood
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Filled.Person, contentDescription = "Profile", tint = Color.White)
                    }
                }
            )
        },
        containerColor = WarmCream
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Section Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkWood,
                contentColor = LightGold,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Sandalwood,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { 
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            selectedTabIndex = index 
                        },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            ) 
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.legal_guide_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = NearBlackBrown,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Official regulatory information for private farmers",
                    fontSize = 14.sp,
                    color = DarkWood.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                when (selectedTabIndex) {
                    0 -> BeforePlantingSection()
                    1 -> GrowingPhaseSection()
                    2 -> HarvestProcessSection()
                    3 -> SellingTransportSection()
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                // Disclaimer
                Card(
                    colors = CardDefaults.cardColors(containerColor = Sandalwood.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Sandalwood.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = Sandalwood, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.legal_disclaimer_title), fontWeight = FontWeight.Bold, color = NearBlackBrown)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.legal_disclaimer_text),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = DarkWood
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun BeforePlantingSection() {
    LegalAccordionCard(
        title = stringResource(R.string.legal_q1_title),
        content = stringResource(R.string.legal_q1_content)
    )
    LegalAccordionCard(
        title = stringResource(R.string.legal_q2_title),
        content = stringResource(R.string.legal_q2_content)
    )
    LegalAccordionCard(
        title = stringResource(R.string.legal_q3_title),
        content = stringResource(R.string.legal_q3_content)
    )
    LegalAccordionCard(
        title = stringResource(R.string.legal_q4_title),
        content = stringResource(R.string.legal_q4_content)
    )
}

@Composable
fun GrowingPhaseSection() {
    LegalAccordionCard(
        title = "How Long Until Harvest?",
        content = "Sandalwood is a slow growing tree. Natural growth takes 25 to 30 years. With intensive organic cultivation methods heartwood can develop in 15 to 20 years. IWST Bengaluru recommends a 15 year harvest cycle for maximum benefit. Do not cut trees before heartwood is properly formed."
    )
    LegalAccordionCard(
        title = "How to Know If Heartwood Is Ready",
        content = "Heartwood is the dark fragrant inner core of the tree. A tree is generally ready when girth at chest height reaches 60 cm or more and age is 15 years or above. You can also check by tapping — a hollow sound means heartwood has not formed. Contact your local IWST officer for a proper assessment."
    )
    LegalAccordionCard(
        title = "Host Plants Are Mandatory",
        content = "Sandalwood is semi-parasitic. Its roots absorb nutrients from nearby host plants. Without host plants the tree will not survive. Recommended host plants are Casuarina, Pongamia, Cassia siamea, and red gram. Plant host plants within 2 metres of each sandalwood sapling."
    )
    LegalAccordionCard(
        title = "Protecting Against Theft",
        content = "A single mature sandalwood tree can be worth Rs 2 to 5 lakh. Install perimeter fencing and motion sensor lights. Register every tree digitally with GPS and photo using this app. Keep your Tree IDs safe — they are your legal proof of ownership. Report any theft immediately to your local police station and Forest Range Officer."
    )
    LegalAccordionCard(
        title = "Common Diseases to Watch",
        content = "Sandalwood Spike Disease is the most dangerous — there is no cure. Affected trees must be removed immediately to prevent spread. Symptoms are small narrow leaves, short internodes, and no fruiting. Also watch for root rot in waterlogged soil and borers that attack bark. Contact your nearest forest nursery for treatment advice."
    )
}

@Composable
fun HarvestProcessSection() {
    Text(
        "Felling Permission Roadmap",
        fontWeight = FontWeight.Bold,
        color = NearBlackBrown,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    
    val harvestSteps = listOf(
        "Verify Tree Is Ready" to "Confirm the tree is at least 15 years old with a girth of 60 cm or more. Heartwood should be clearly visible and fragrant. Do not apply for permission prematurely — the FRO will reject the application if trees are not mature.",
        "Gather Your Documents" to "You will need: Land ownership certificate from Tahsildar, plantation registration receipt, Tree ID list with GPS/Photos from this app, Aadhaar card, and a written harvest application.",
        "Apply Online via SAKALA" to "Visit sakala.karnataka.gov.in or nearest Nada Kacheri. Submit Form 1 for tree felling. You will receive an application number via SMS to track progress.",
        "FRO Inspection Visit" to "After application, the Forest Range Officer will visit your farm to physically verify trees. Show your Tree ID records from this app. This typically takes 2-6 weeks.",
        "Receive Felling Permission" to "If approved, you receive a felling permission NOC in triplicate. Cutting without this NOC is a criminal offence under the Karnataka Forest Act 1963.",
        "Supervised Cutting" to "Authorized Forest Department personnel will cut the trees. You cannot cut them yourself. An FRO must be present during the entire felling process.",
        "Auction and Payment" to "Wood is auctioned at government depots. Buyers like KSDL participate. You receive 80% of auction price after a 20% processing fee. Payment takes 30-60 days."
    )

    harvestSteps.forEachIndexed { index, pair ->
        HarvestStepItem(index + 1, pair.first, pair.second, index == harvestSteps.size - 1)
    }
}

@Composable
fun SellingTransportSection() {
    LegalAccordionCard(
        title = "Who Can You Sell To?",
        content = "Since the 2022 Karnataka Forest Amendment Rules you can now sell to any authorized buyer not just government agencies. Previously only KSDL and KSHDC were authorized buyers. However all sales must still be documented and the transit permit is mandatory."
    )
    LegalAccordionCard(
        title = "Transit Permit Is Mandatory",
        content = "No sandalwood can be moved from your farm to any location without a Sandalwood Transit Permit issued by the Forest Department. Moving sandalwood without this permit is a criminal offence. The permit must accompany the wood at all times during transport."
    )
    LegalAccordionCard(
        title = "Penalties for Violations",
        content = "Cutting without permission: up to 7 years imprisonment. Transporting without permit: seizure of wood and vehicle. Selling to unauthorized buyers: criminal prosecution. Do not take shortcuts; the penalties are severe."
    )
    LegalAccordionCard(
        title = "Current Market Prices (2024-25)",
        content = "Heartwood auction rate: ₹6500 - ₹7500 per kg. Sandalwood oil: ₹1.5 lakh per kg. Sapwood: ₹45,000 - ₹60,000 per tonne. Retail rate: ₹16,000 - ₹20,000 per kg. Prices are increasing ~25% annually."
    )
    LegalAccordionCard(
        title = "Contact Authorities",
        content = "Forest Dept Karnataka: 080 2225 6722. KSDL Enquiries: 0821 2411 100. SAKALA Status: SMS 'SAKALA <application_id>' to 9243100100."
    )
}

@Composable
fun LegalAccordionCard(title: String, content: String) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotate")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .animateContentSize(tween(300)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    expanded = !expanded 
                }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = NearBlackBrown,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Sandalwood,
                    modifier = Modifier.rotate(rotation)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = FaintWood)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = content,
                    fontSize = 14.sp,
                    color = DarkWood.copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun HarvestStepItem(number: Int, title: String, content: String, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Sandalwood),
                contentAlignment = Alignment.Center
            ) {
                Text(number.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                        .background(Sandalwood.copy(alpha = 0.3f))
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(title, fontWeight = FontWeight.ExtraBold, color = NearBlackBrown, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(content, fontSize = 13.sp, color = DarkWood.copy(alpha = 0.8f), lineHeight = 18.sp)
        }
    }
}

