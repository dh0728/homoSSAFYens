package com.example.dive.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.wear.compose.material.*
import com.example.dive.sensors.casting.CastingAnalyzer
import com.example.dive.sensors.casting.CastingResult

@Composable
fun CastingScreen(navController: NavHostController) {
    var step by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<CastingResult?>(null) }
    val context = LocalContext.current
    val analyzer = remember { CastingAnalyzer(context) }

    when (step) {
        0 -> CastingReadyScreen(onStart = { step = 1 })
        1 -> {
            // 측정 화면 + 자동 종료 콜백
            LaunchedEffect(Unit) {
                analyzer.startCasting { castingResult ->
                    result = castingResult
                    step = 2
                }
            }
            CastingMeasureScreen()
        }
        2 -> CastingResultScreen(
            result = result,
            onRetry = { step = 0 },
            onExit = { navController.popBackStack() }
        )
    }
}

@Composable
fun CastingReadyScreen(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("준비하세요!", style = MaterialTheme.typography.title2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("워치를 고정한 채\n센서를 보정합니다", style = MaterialTheme.typography.body2, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text("백스윙 후 앞으로 부드럽게 휘두르세요\n릴리즈는 전방을 향해!", style = MaterialTheme.typography.body2, textAlign = TextAlign.Center, color = Color.Gray)
            Spacer(Modifier.height(20.dp))
            WideActionButton("측정 시작", onClick = onStart)
        }
    }
}

@Composable
fun CastingMeasureScreen() {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("휘두르는 중...", style = MaterialTheme.typography.title2)
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(8.dp))
            Text("릴리즈 순간 진동 후 자동 종료됩니다", style = MaterialTheme.typography.body2, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CastingResultScreen(result: CastingResult?, onRetry: () -> Unit, onExit: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("🎯 점수: ${result?.score ?: 0}점", style = MaterialTheme.typography.title2)
            Spacer(Modifier.height(8.dp))
            Text("속도: ${String.format("%.1f", result?.speed ?: 0.0)} m/s")
            Text("릴리즈: ${result?.releaseTiming ?: 0} ms")
            Text("안정성: ${String.format("%.1f", result?.stability ?: 0.0)}")
            Spacer(Modifier.height(8.dp))
            Text(result?.coaching ?: "피드백 없음", style = MaterialTheme.typography.body2, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Button(onClick = onRetry, modifier = Modifier.width(80.dp).height(40.dp)) { Text("다시") }
                Button(onClick = onExit, modifier = Modifier.width(80.dp).height(40.dp)) { Text("종료") }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun WideActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(0.6f).height(46.dp).padding(horizontal = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary, contentColor = Color.White)
    ) {
        Text(text, style = MaterialTheme.typography.button, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
