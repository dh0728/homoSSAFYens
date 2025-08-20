package com.example.dive.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.example.dive.presentation.MainViewModel
import com.example.dive.presentation.theme.TextPrimary
import com.example.dive.presentation.theme.TextSecondary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked

enum class MarineActivityMode(val label: String) {
    OFF("모드 해제"),
    FISHING("낚시 모드"),
    BOATING("보트 모드"),
    DIVING("다이빙 모드"),
    GENERAL_MARINE("일반 해양 활동")
}

@Composable
fun RadioIcon(checked: Boolean) {
    if (checked) {
        Icon(Icons.Filled.RadioButtonChecked, contentDescription = null)
    } else {
        Icon(Icons.Filled.RadioButtonUnchecked, contentDescription = null)
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel = viewModel()) {
    val isMonitoringEnabled by viewModel.isMonitoringEnabled.collectAsState()
    val selectedMode by viewModel.selectedMarineActivityMode.collectAsState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text(
                text = "해양 활동 모드",
                style = MaterialTheme.typography.title1,
                color = TextPrimary
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        items(MarineActivityMode.values().size) { index ->
            val mode = MarineActivityMode.values()[index]
            ToggleChip(
                checked = selectedMode == mode,
                onCheckedChange = { viewModel.setSelectedMarineActivityMode(mode) },
                label = { Text(mode.label, color = TextPrimary) },
                toggleControl = { RadioIcon(checked = selectedMode == mode) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            ToggleChip(
                checked = isMonitoringEnabled,
                onCheckedChange = { viewModel.setMonitoringEnabled(it) },
                label = { Text("심박수 자동 감지", color = TextPrimary) },
                toggleControl = {
                    Switch(
                        checked = isMonitoringEnabled,
                        onCheckedChange = { viewModel.setMonitoringEnabled(it) }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text(
                text = "선택한 모드에 따라 심박수\n모니터링 기준이 조정됩니다",
                style = MaterialTheme.typography.caption1,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.requestDataRefresh() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("데이터 새로고침")
            }
        }
    }
}
