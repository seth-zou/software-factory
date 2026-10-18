package com.bmwelectric.snake

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bmwelectric.snake.game.BEST_SCORE_KEY
import com.bmwelectric.snake.game.Direction
import com.bmwelectric.snake.game.Point
import com.bmwelectric.snake.game.RoundState
import com.bmwelectric.snake.game.SCORE_HISTORY_KEY
import com.bmwelectric.snake.game.ScoreEntry
import com.bmwelectric.snake.game.SnakeGameController
import com.bmwelectric.snake.game.SnakeGameState
import com.bmwelectric.snake.game.SpeedProfile
import com.bmwelectric.snake.game.VehicleState
import com.bmwelectric.snake.game.decodeScoreHistory
import com.bmwelectric.snake.game.encodeScoreHistory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val demoState = intent.getStringExtra(DEMO_STATE_EXTRA).takeIf { isDebuggableBuild() }
        setContent {
            SnakeAutomotiveApp(initialDemoState = demoState)
        }
    }

    private fun isDebuggableBuild(): Boolean = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

@Composable
fun SnakeAutomotiveApp(initialDemoState: String? = null) {
    val context = LocalContext.current
    val prefs = remember { createScorePreferences(context) }
    val storedScores = remember(prefs) { loadStoredScores(prefs) }
    var storageIssue by remember { mutableStateOf(prefs == null || storedScores.readFailed) }
    val controller = remember {
        SnakeGameController(
            bestScore = storedScores.bestScore,
            scoreHistory = storedScores.history,
            persistBestScore = {
                if (!prefs.persistBestScore(it)) storageIssue = true
            },
            persistScoreHistory = {
                if (!prefs.persistScoreHistory(it)) storageIssue = true
            },
        ).applyDemoState(initialDemoState)
    }
    var gameState by remember { mutableStateOf(controller.state) }
    var dayMode by remember { mutableStateOf(false) }
    val freezeDemoTicks = initialDemoState.equals("playing", ignoreCase = true)
    val palette = if (dayMode) CockpitPalette.day() else CockpitPalette.dark()
    val focusRequester = remember { FocusRequester() }

    fun updateState(action: SnakeGameController.() -> Unit) {
        controller.action()
        gameState = controller.state
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(gameState.roundState, gameState.speedProfile) {
        if (freezeDemoTicks) return@LaunchedEffect
        while (gameState.roundState == RoundState.Playing && gameState.vehicleState == VehicleState.Parked) {
            delay(gameState.speedProfile.tickMillis)
            controller.tick()
            gameState = controller.state
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.canvas,
        ) {
            SnakeCockpitScreen(
                state = gameState,
                storageIssue = storageIssue,
                dayMode = dayMode,
                palette = palette,
                focusRequester = focusRequester,
                onThemeToggle = { dayMode = !dayMode },
                onVehicleState = { updateState { setVehicleState(it) } },
                onSpeed = { updateState { setSpeedProfile(it) } },
                onStart = { updateState { start() } },
                onPauseContinue = { updateState { pauseOrContinue() } },
                onRestart = { updateState { restart() } },
                onExit = { updateState { exit() } },
                onDirection = { updateState { inputDirection(it) } },
            )
        }
    }
}

private const val DEMO_STATE_EXTRA = "demo_state"

private data class StoredScores(
    val bestScore: Int,
    val history: List<ScoreEntry>,
    val readFailed: Boolean,
)

private fun createScorePreferences(context: Context): SharedPreferences? = runCatching {
    context.createDeviceProtectedStorageContext()
        .getSharedPreferences("snake-local-score", Context.MODE_PRIVATE)
}.getOrNull()

private fun loadStoredScores(prefs: SharedPreferences?): StoredScores {
    if (prefs == null) return StoredScores(bestScore = 0, history = emptyList(), readFailed = true)

    var readFailed = false
    val storedBest = runCatching { prefs.getInt(BEST_SCORE_KEY, 0) }.getOrElse {
        readFailed = true
        0
    }
    val rawHistory = runCatching { prefs.getString(SCORE_HISTORY_KEY, null) }.getOrElse {
        readFailed = true
        null
    }
    val history = decodeScoreHistory(rawHistory)
    val nonBlankRows = rawHistory?.lineSequence()?.count { it.isNotBlank() } ?: 0
    val recoveredHistory = rawHistory.isNullOrBlank() || nonBlankRows == history.size
    val bestFromHistory = history.maxOfOrNull { it.score } ?: 0

    return StoredScores(
        bestScore = maxOf(storedBest, bestFromHistory).coerceAtLeast(0),
        history = history,
        readFailed = readFailed || !recoveredHistory,
    )
}

private fun SharedPreferences?.persistBestScore(bestScore: Int): Boolean = this != null && runCatching {
    edit().putInt(BEST_SCORE_KEY, bestScore.coerceAtLeast(0)).apply()
}.isSuccess

private fun SharedPreferences?.persistScoreHistory(scoreHistory: List<ScoreEntry>): Boolean = this != null && runCatching {
    edit().putString(SCORE_HISTORY_KEY, encodeScoreHistory(scoreHistory)).apply()
}.isSuccess

private fun SnakeGameController.applyDemoState(demoState: String?): SnakeGameController {
    when (demoState?.lowercase()) {
        "playing" -> start()
        "paused" -> {
            start()
            pauseOrContinue()
        }
        "driving", "driving_locked" -> setVehicleState(VehicleState.Driving)
    }
    return this
}

@Composable
private fun SnakeCockpitScreen(
    state: SnakeGameState,
    storageIssue: Boolean,
    dayMode: Boolean,
    palette: CockpitPalette,
    focusRequester: FocusRequester,
    onThemeToggle: () -> Unit,
    onVehicleState: (VehicleState) -> Unit,
    onSpeed: (SpeedProfile) -> Unit,
    onStart: () -> Unit,
    onPauseContinue: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onDirection: (Direction) -> Unit,
) {
    val keyboardHandler: (Key) -> Boolean = { key ->
        when (key) {
            Key.DirectionUp, Key.W -> {
                onDirection(Direction.Up)
                true
            }
            Key.DirectionDown, Key.S -> {
                onDirection(Direction.Down)
                true
            }
            Key.DirectionLeft, Key.A -> {
                onDirection(Direction.Left)
                true
            }
            Key.DirectionRight, Key.D -> {
                onDirection(Direction.Right)
                true
            }
            Key.Spacebar -> {
                onPauseContinue()
                true
            }
            else -> false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && keyboardHandler(event.key)
            }
            .background(palette.canvas)
            .padding(24.dp)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = state.statusAnnouncement
            },
    ) {
        TopBar(dayMode, palette, onThemeToggle)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FunctionRail(palette)
            MainGameArea(
                state = state,
                storageIssue = storageIssue,
                palette = palette,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            ControlRail(
                state = state,
                palette = palette,
                onVehicleState = onVehicleState,
                onSpeed = onSpeed,
                onStart = onStart,
                onPauseContinue = onPauseContinue,
                onRestart = onRestart,
                onExit = onExit,
                onDirection = onDirection,
                modifier = Modifier
                    .width(432.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun TopBar(dayMode: Boolean, palette: CockpitPalette, onThemeToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.panel)
                .border(1.dp, palette.line, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("BMW", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text("Snake Parked Mode", color = palette.text, fontSize = 28.sp, maxLines = 1)
            Text("Android Automotive cockpit game", color = palette.muted, fontSize = 16.sp, maxLines = 1)
        }
        StatusChip("Battery 76%", palette)
        StatusChip("Range 318 km", palette)
        StatusChip("22:47", palette)
        CockpitButton(
            label = if (dayMode) "Dark mode" else "Day mode",
            enabled = true,
            selected = false,
            primary = false,
            palette = palette,
            minWidth = 154.dp,
            onClick = onThemeToggle,
            tag = "theme-toggle",
        )
    }
}

@Composable
private fun StatusChip(label: String, palette: CockpitPalette) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(palette.panel)
            .border(1.dp, palette.line, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = palette.text, fontSize = 16.sp, maxLines = 1)
    }
}

@Composable
private fun FunctionRail(palette: CockpitPalette) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf("EV", "PWR", "NAV", "CLM", "SUN").forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .size(72.dp, 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (index == 0) palette.green.copy(alpha = 0.18f) else palette.panel)
                    .border(
                        1.dp,
                        if (index == 0) palette.green else palette.line,
                        RoundedCornerShape(8.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (index == 0) palette.green else palette.muted, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun MainGameArea(
    state: SnakeGameState,
    storageIssue: Boolean,
    palette: CockpitPalette,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SafetyBanner(state, palette)
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.boardSurface)
                    .border(1.dp, palette.line, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                val boardSide = minOf(maxWidth, maxHeight)
                Box(
                    modifier = Modifier
                        .size(boardSide)
                        .aspectRatio(1f)
                        .testTag("snake-board")
                        .semantics {
                            contentDescription = "Snake board ${state.roundState.name}, score ${state.score}, best ${state.bestScore}"
                        },
                ) {
                    SnakeBoard(state, palette)
                    BoardOverlay(state, palette)
                }
            }
            LeaderboardPanel(
                scoreHistory = state.scoreHistory,
                storageIssue = storageIssue,
                palette = palette,
                modifier = Modifier
                    .width(276.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun SafetyBanner(state: SnakeGameState, palette: CockpitPalette) {
    val locked = state.roundState == RoundState.DrivingLocked
    val title = if (locked) "Driving. Game unavailable." else "Parked. Game controls available."
    val body = if (locked) {
        "Play is locked while the vehicle is moving. Shift to Park to enable the game."
    } else {
        "Touch, keyboard, and focused control input are accepted while the vehicle is stationary."
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panel)
            .border(1.dp, if (locked) palette.amber else palette.line, RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(if (locked) "LOCK" else "PARK", color = if (locked) palette.amber else palette.green, fontSize = 16.sp)
        Column {
            Text(title, color = palette.text, fontSize = 20.sp, maxLines = 1)
            Text(body, color = palette.muted, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SnakeBoard(state: SnakeGameState, palette: CockpitPalette) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / state.boardSize
            val gap = 1.5f
            val tileSize = (cell - gap).coerceAtLeast(1f)
            for (y in 0 until state.boardSize) {
                for (x in 0 until state.boardSize) {
                    drawRect(
                        color = palette.gridLine.copy(alpha = 0.18f),
                        topLeft = Offset(x * cell + gap / 2f, y * cell + gap / 2f),
                        size = Size(tileSize, tileSize),
                    )
                }
            }
            drawGameCell(state.energy, cell, palette.cyan)
            state.snake.forEachIndexed { index, point ->
                drawGameCell(point, cell, if (index == 0) palette.mint else palette.green)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGameCell(
    point: Point,
    cell: Float,
    color: Color,
) {
    val gap = 3f
    drawRect(
        color = color,
        topLeft = Offset(point.x * cell + gap / 2f, point.y * cell + gap / 2f),
        size = Size((cell - gap).coerceAtLeast(1f), (cell - gap).coerceAtLeast(1f)),
    )
}

@Composable
private fun BoardOverlay(state: SnakeGameState, palette: CockpitPalette) {
    val overlayText = when (state.roundState) {
        RoundState.Ready -> Triple("Parked ready", "Snake is ready", "Start a round when the vehicle remains in Park.")
        RoundState.Paused -> Triple("Paused", "Paused", "Continue, restart, or exit while the vehicle remains parked.")
        RoundState.GameOver -> Triple("Collision", "Game over", "Score ${state.score}. Best ${state.bestScore}.")
        RoundState.DrivingLocked -> Triple(
            "Driving lock",
            "Unavailable while driving",
            "The game cannot be started or resumed until Vehicle state returns to Parked.",
        )
        RoundState.Playing -> null
    }
    if (overlayText != null) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.panel.copy(alpha = 0.94f))
                    .border(1.dp, palette.line, RoundedCornerShape(8.dp))
                    .padding(26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(overlayText.first, color = palette.muted, fontSize = 16.sp)
                    Text(overlayText.second, color = palette.text, fontSize = 32.sp, textAlign = TextAlign.Center)
                    Text(overlayText.third, color = palette.muted, fontSize = 18.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardPanel(
    scoreHistory: List<ScoreEntry>,
    storageIssue: Boolean,
    palette: CockpitPalette,
    modifier: Modifier = Modifier,
) {
    Panel(palette, modifier = modifier.testTag("leaderboard")) {
        PanelHeader("Leaderboard", if (storageIssue) "Local storage limited" else "Local history", palette)
        if (scoreHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.canvas.copy(alpha = 0.38f))
                    .border(1.dp, palette.line, RoundedCornerShape(8.dp))
                    .testTag("leaderboard-empty"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No completed rounds yet",
                    color = palette.muted,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            scoreHistory.take(5).forEachIndexed { index, entry ->
                LeaderboardRow(index = index, entry = entry, palette = palette)
                if (index < minOf(scoreHistory.size, 5) - 1) {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        if (storageIssue) {
            Spacer(Modifier.height(10.dp))
            Text(
                "History was repaired or storage is unavailable.",
                color = palette.amber,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun LeaderboardRow(index: Int, entry: ScoreEntry, palette: CockpitPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.canvas.copy(alpha = 0.38f))
            .border(1.dp, palette.line, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp)
            .testTag("leaderboard-row-$index"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "#${index + 1}",
            color = if (index == 0) palette.green else palette.muted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(34.dp),
        )
        Text(
            entry.score.formatScore(),
            color = if (index == 0) palette.green else palette.text,
            fontSize = 24.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Text(
            entry.speedProfile.label,
            color = palette.muted,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ControlRail(
    state: SnakeGameState,
    palette: CockpitPalette,
    onVehicleState: (VehicleState) -> Unit,
    onSpeed: (SpeedProfile) -> Unit,
    onStart: () -> Unit,
    onPauseContinue: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(palette, modifier = Modifier.height(132.dp)) {
            PanelHeader("Vehicle state", "Demo simulator", palette)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitButton(
                    label = "Driving",
                    enabled = true,
                    selected = state.vehicleState == VehicleState.Driving,
                    primary = false,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = { onVehicleState(VehicleState.Driving) },
                    tag = "vehicle-driving",
                )
                CockpitButton(
                    label = "Parked",
                    enabled = true,
                    selected = state.vehicleState == VehicleState.Parked,
                    primary = false,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = { onVehicleState(VehicleState.Parked) },
                    tag = "vehicle-parked",
                )
            }
        }
        Panel(palette, modifier = Modifier.height(144.dp)) {
            PanelHeader("Score", state.roundState.label, palette)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ValueTile("Current", state.score.formatScore(), palette, Modifier.weight(1f), accent = true)
                ValueTile("Best", state.bestScore.formatScore(), palette, Modifier.weight(1f), accent = false)
                ValueTile("Speed", state.speedProfile.speedValue, palette, Modifier.weight(1f), accent = false)
            }
        }
        Panel(palette, modifier = Modifier.weight(1f)) {
            PanelHeader("Game controls", if (state.canChangeDirection) "Touch ready" else "Locked until play", palette)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SpeedProfile.entries.forEach { speed ->
                    CockpitButton(
                        label = speed.label,
                        enabled = state.canChangeSpeed,
                        selected = state.speedProfile == speed,
                        primary = false,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onSpeed(speed) },
                        tag = "speed-${speed.name.lowercase()}",
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitButton(
                    label = "Start",
                    enabled = state.canStart,
                    selected = false,
                    primary = true,
                    palette = palette,
                    minWidth = 72.dp,
                    onClick = onStart,
                    tag = "start",
                )
                CockpitButton(
                    label = if (state.roundState == RoundState.Paused) "Continue" else "Pause",
                    enabled = state.canPauseOrContinue,
                    selected = false,
                    primary = false,
                    palette = palette,
                    minWidth = 96.dp,
                    onClick = onPauseContinue,
                    tag = "pause-continue",
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitButton(
                    label = "Restart",
                    enabled = state.canRestart,
                    selected = false,
                    primary = false,
                    palette = palette,
                    minWidth = 86.dp,
                    onClick = onRestart,
                    tag = "restart",
                )
                CockpitButton(
                    label = "Exit",
                    enabled = state.canExit,
                    selected = false,
                    primary = false,
                    palette = palette,
                    minWidth = 72.dp,
                    onClick = onExit,
                    tag = "exit",
                )
            }
            Spacer(Modifier.height(18.dp))
            DirectionPad(state, palette, onDirection)
        }
    }
}

@Composable
private fun Panel(palette: CockpitPalette, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panel)
            .border(1.dp, palette.line, RoundedCornerShape(8.dp))
            .padding(14.dp),
        content = content,
    )
}

@Composable
private fun PanelHeader(title: String, meta: String, palette: CockpitPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = palette.text, fontSize = 24.sp, maxLines = 1)
        Text(meta, color = palette.muted, fontSize = 16.sp, maxLines = 1)
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ValueTile(label: String, value: String, palette: CockpitPalette, modifier: Modifier, accent: Boolean) {
    Column(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.canvas.copy(alpha = 0.44f))
            .border(1.dp, palette.line, RoundedCornerShape(8.dp))
            .padding(10.dp),
    ) {
        Text(label, color = palette.muted, fontSize = 15.sp, maxLines = 1)
        Text(
            value,
            color = if (accent) palette.green else palette.text,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}

@Composable
private fun DirectionPad(state: SnakeGameState, palette: CockpitPalette, onDirection: (Direction) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        DirectionButton(Direction.Up, "^", state, palette, onDirection, Modifier.align(Alignment.TopCenter))
        DirectionButton(Direction.Left, "<", state, palette, onDirection, Modifier.align(Alignment.CenterStart))
        DirectionButton(Direction.Right, ">", state, palette, onDirection, Modifier.align(Alignment.CenterEnd))
        DirectionButton(Direction.Down, "v", state, palette, onDirection, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DirectionButton(
    direction: Direction,
    label: String,
    state: SnakeGameState,
    palette: CockpitPalette,
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    CockpitButton(
        label = label,
        enabled = state.canChangeDirection,
        selected = state.nextDirection == direction && state.roundState == RoundState.Playing,
        primary = false,
        palette = palette,
        modifier = modifier.size(72.dp),
        onClick = { onDirection(direction) },
        tag = "direction-${direction.name.lowercase()}",
        contentDescription = "Move ${direction.name.lowercase()}",
        minHeight = 72.dp,
    )
}

@Composable
private fun CockpitButton(
    label: String,
    enabled: Boolean,
    selected: Boolean,
    primary: Boolean,
    palette: CockpitPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = 64.dp,
    minHeight: Dp = 64.dp,
    tag: String,
    contentDescription: String = label,
) {
    val borderColor = when {
        selected || primary -> palette.blue
        else -> palette.line
    }
    val background = when {
        !enabled -> palette.canvas.copy(alpha = 0.28f)
        selected -> palette.blue.copy(alpha = 0.20f)
        primary -> palette.blue.copy(alpha = 0.16f)
        else -> palette.canvas.copy(alpha = 0.45f)
    }
    Box(
        modifier = modifier
            .height(minHeight)
            .then(if (modifier == Modifier) Modifier.width(minWidth) else Modifier)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .focusable(enabled)
            .testTag(tag)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (enabled) palette.text else palette.muted,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

private val RoundState.label: String
    get() = when (this) {
        RoundState.Ready -> "Ready"
        RoundState.Playing -> "Playing"
        RoundState.Paused -> "Paused"
        RoundState.GameOver -> "Game over"
        RoundState.DrivingLocked -> "Locked"
    }

private fun Int.formatScore(): String = toString().padStart(3, '0')

data class CockpitPalette(
    val canvas: Color,
    val panel: Color,
    val boardSurface: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val gridLine: Color,
    val blue: Color,
    val green: Color,
    val mint: Color,
    val cyan: Color,
    val amber: Color,
) {
    companion object {
        fun dark(): CockpitPalette = CockpitPalette(
            canvas = Color(0xFF101418),
            panel = Color(0xFF20262D),
            boardSurface = Color(0xFF11181D),
            text = Color(0xFFF6F8F7),
            muted = Color(0xFF8A949E),
            line = Color(0xFF3A424B),
            gridLine = Color(0xFF3A424B),
            blue = Color(0xFF1C69D4),
            green = Color(0xFF24A148),
            mint = Color(0xFF8FDCC2),
            cyan = Color(0xFF00AEEF),
            amber = Color(0xFFF5A623),
        )

        fun day(): CockpitPalette = CockpitPalette(
            canvas = Color(0xFFF8FAF7),
            panel = Color(0xFFE7E2D6),
            boardSurface = Color(0xFFEFF2EF),
            text = Color(0xFF161A1D),
            muted = Color(0xFF5E6872),
            line = Color(0xFFB5B1A8),
            gridLine = Color(0xFFB5B1A8),
            blue = Color(0xFF1C69D4),
            green = Color(0xFF1C7F3A),
            mint = Color(0xFF4DB08A),
            cyan = Color(0xFF007DA8),
            amber = Color(0xFF9B6500),
        )
    }
}
