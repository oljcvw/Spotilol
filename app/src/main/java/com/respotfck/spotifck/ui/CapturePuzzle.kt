package com.respotfck.spotifck.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private const val N = 4
private const val SHUFFLE_MOVES = 400
private const val SECRET_TAPS = 7
private const val SECRET_WINDOW_MS = 4000L

private fun solvedBoard() = IntArray(N * N) { if (it == N * N - 1) 0 else it + 1 }

/** Random walk of legal moves from the solved state, so the board is always solvable. */
private fun shuffledBoard(): IntArray {
    val b = solvedBoard()
    var blank = N * N - 1
    var prev = -1
    repeat(SHUFFLE_MOVES) {
        val r = blank / N
        val c = blank % N
        val options = buildList {
            if (r > 0) add(blank - N)
            if (r < N - 1) add(blank + N)
            if (c > 0) add(blank - 1)
            if (c < N - 1) add(blank + 1)
        }.filter { it != prev }
        val next = options[Random.nextInt(options.size)]
        b[blank] = b[next]; b[next] = 0
        prev = blank; blank = next
    }
    return if (b.contentEquals(solvedBoard())) shuffledBoard() else b
}

/** Slides every tile between [index] and the blank (same row or column) one step toward the blank. */
private fun slide(board: IntArray, index: Int): IntArray? {
    val blank = board.indexOf(0)
    val (br, bc) = blank / N to blank % N
    val (r, c) = index / N to index % N
    if (index == blank || (r != br && c != bc)) return null
    val step = when {
        r == br -> if (c < bc) -1 else 1
        else -> if (r < br) -N else N
    }
    val b = board.copyOf()
    var hole = blank
    while (hole != index) {
        b[hole] = b[hole + step]
        hole += step
    }
    b[index] = 0
    return b
}

/**
 * 15-puzzle gate for turning off the screenshot block. Solving it (or the hidden shortcut:
 * tap the title [SECRET_TAPS] times quickly) calls [onSolved].
 */
@Composable
fun CapturePuzzle(onSolved: () -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    var board by remember { mutableStateOf(shuffledBoard()) }
    var moves by remember { mutableIntStateOf(0) }
    var titleTaps by remember { mutableIntStateOf(0) }
    var firstTapAt by remember { mutableLongStateOf(0L) }
    val solved = board.contentEquals(solvedBoard())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
            Text(
                "Unlock screen capture",
                color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - firstTapAt > SECRET_WINDOW_MS) { firstTapAt = now; titleTaps = 0 }
                    if (++titleTaps >= SECRET_TAPS) onSolved()
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Screenshots and screen recording are blocked to protect your account. " +
                    "Put the tiles in order 1–15 to allow them.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                for (r in 0 until N) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0 until N) {
                            val i = r * N + c
                            val v = board[i]
                            val inPlace = v != 0 && v == i + 1
                            val color by animateColorAsState(
                                when {
                                    v == 0 -> Color.Transparent
                                    inPlace -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                }, label = "tile"
                            )
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(color, RoundedCornerShape(12.dp))
                                    .clickable(enabled = v != 0 && !solved) {
                                        slide(board, i)?.let { board = it; moves++ }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (v != 0) Text("$v", color = if (inPlace) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(if (solved) "Solved in $moves moves" else "Moves: $moves", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            Row {
                TextButton(onClick = onCancel) { Text("Keep blocked", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (solved) TextButton(onClick = onSolved) { Text("Allow capture", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                else TextButton(onClick = { board = shuffledBoard(); moves = 0 }) { Text("Reshuffle", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}
