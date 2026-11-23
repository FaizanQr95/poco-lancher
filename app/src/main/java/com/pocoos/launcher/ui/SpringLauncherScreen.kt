package com.pocoos.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocoos.launcher.LauncherConfig
import com.pocoos.launcher.categorizer.AppCategory
import com.pocoos.launcher.categorizer.CategorizedApp
import com.pocoos.launcher.search.SpotlightSearchEngine
import com.pocoos.launcher.widgets.AppWidgetHostManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpringLauncherScreen(
    categorizedApps: Map<AppCategory, List<CategorizedApp>>,
    widgetHostManager: AppWidgetHostManager,
    onOpenApp: (String) -> Unit
) {
    val allApps = remember(categorizedApps) { categorizedApps.values.flatten() }
    val dockApps = remember(allApps) { allApps.take(4) }
    val homeApps = remember(allApps) { allApps.drop(4) }

    val pagerState = rememberPagerState(pageCount = { 3 }) // Page 0, Page 1, Page 2 (App Library)
    var showSpotlight by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val searchResults = remember(searchQuery, allApps) {
        SpotlightSearchEngine.query(searchQuery, allApps)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D19))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 28f) showSpotlight = true
                    if (dragAmount < -28f && showSpotlight) showSpotlight = false
                }
            }
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(34.dp)) // Space for Dynamic Island & Status Bar

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> HomeGridPage(
                        apps = homeApps.take(16),
                        showWidgets = true,
                        widgetHostManager = widgetHostManager,
                        onOpenApp = onOpenApp
                    )
                    1 -> HomeGridPage(
                        apps = homeApps.drop(16),
                        showWidgets = false,
                        widgetHostManager = widgetHostManager,
                        onOpenApp = onOpenApp
                    )
                    2 -> AppLibraryPage(
                        categorizedApps = categorizedApps,
                        onOpenApp = onOpenApp
                    )
                }
            }

            if (LauncherConfig.SHOW_SPOTLIGHT_SEARCH_PILL) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable { showSpotlight = true }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Search", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // Frosted iOS Dock Bar
            FrostedDockBar(dockApps = dockApps, onOpenApp = onOpenApp)
        }

        // System-wide Spotlight Search Overlay
        AnimatedVisibility(
            visible = showSpotlight,
            enter = fadeIn(spring(stiffness = LauncherConfig.SPRING_STIFFNESS)) + scaleIn(initialScale = 0.96f),
            exit = fadeOut()
        ) {
            SpotlightOverlay(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                results = searchResults,
                onDismiss = { showSpotlight = false },
                onLaunchApp = { pkg ->
                    showSpotlight = false
                    onOpenApp(pkg)
                }
            )
        }
    }
}

@Composable
private fun HomeGridPage(
    apps: List<CategorizedApp>,
    showWidgets: Boolean,
    widgetHostManager: AppWidgetHostManager,
    onOpenApp: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        if (showWidgets) {
            // 2x2 Custom Widget Slot (Weather + Poco X3 Pro Stats / Android AppWidgetHost)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f).height(142.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF1E3A5F).copy(alpha = 0.72f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("Lahore · Clear", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("24°", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                        Text("H:27°  L:15°", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f).height(142.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF141824).copy(alpha = 0.78f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text("POCO X3 PRO", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("120Hz Active", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("SD860 · 5160mAh", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(LauncherConfig.GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(apps, key = { it.packageName }) { app ->
                SpringAppIcon(app = app, onClick = { onOpenApp(app.packageName) })
            }
        }
    }
}

@Composable
private fun SpringAppIcon(app: CategorizedApp, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = LauncherConfig.SPRING_DAMPING_RATIO,
            stiffness = LauncherConfig.SPRING_STIFFNESS
        ),
        label = "iconSpring"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable {
                pressed = true
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(LauncherConfig.ICON_CORNER_RADIUS_DP.dp))
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(2).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FrostedDockBar(dockApps: List<CategorizedApp>, onOpenApp: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dockApps.forEach { app ->
                SpringAppIcon(app = app, onClick = { onOpenApp(app.packageName) })
            }
        }
    }
}

@Composable
private fun AppLibraryPage(
    categorizedApps: Map<AppCategory, List<CategorizedApp>>,
    onOpenApp: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categorizedApps.entries.toList()) { (category, apps) ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(category.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    apps.take(4).forEach { app ->
                        Text(
                            text = app.label,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenApp(app.packageName) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<CategorizedApp>,
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable { onDismiss() }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 42.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search apps, contacts, settings...") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            results.take(8).forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onLaunchApp(app.packageName) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(app.category.title, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
                }
            }
        }
    }
}
