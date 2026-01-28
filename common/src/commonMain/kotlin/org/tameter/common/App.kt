package org.tameter.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.tameter.iet.model.*
import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.ui.IetTable

@Composable
fun App() {
    val modelBridge = remember { ModelBridge(ImpactEstimationTable()) }

    LaunchedEffect(Unit) {
        val table = ImpactEstimationTable().apply {
            // 3 Performance Requirements
            requirements.add(PerformanceRequirement("Perf 1", "%", 50.0, 100.0))
            requirements.add(PerformanceRequirement("Perf 2", "ms", 200.0, 100.0))
            requirements.add(PerformanceRequirement("Perf 3", "users", 1000.0, 5000.0))

            // 2 Resource Requirements
            requirements.add(ResourceRequirement("Res 1", "USD", 10000.0))
            requirements.add(ResourceRequirement("Res 2", "Hours", 500.0))

            // 3 Design Ideas
            ideas.add(DesignIdea("Idea A"))
            ideas.add(DesignIdea("Idea B"))
            ideas.add(DesignIdea("Idea C"))

            // Populate some dummy estimations
            // Row 0, Col 0: Perf 1, Idea A. Current=50, Goal=100. Let's say Idea A gives 75.
            setEstimation(0, 0, Estimation(75.0, confidenceRange = 5.0))
            // Row 3, Col 0: Res 1, Idea A. Budget=10000. Let's say Idea A costs 2000.
            setEstimation(3, 0, Estimation(2000.0, confidenceRange = 500.0))

            // Row 0, Col 1: Perf 1, Idea B. Let's say Idea B gives 100.
            setEstimation(0, 1, Estimation(100.0))
            // Row 3, Col 1: Res 1, Idea B. Let's say Idea B costs 5000.
            setEstimation(3, 1, Estimation(5000.0, confidenceRange = 1000.0))

            // Row 1, Col 2: Perf 2, Idea C. Current=200, Goal=100. Let's say Idea C gives 150.
            setEstimation(1, 2, Estimation(150.0, confidenceRange = 10.0))
            // Row 4, Col 2: Res 2, Idea C. Budget=500. Let's say Idea C costs 100.
            setEstimation(4, 2, Estimation(100.0))
        }
        modelBridge.updateModel(table)
    }

    var inlineTotals by remember { mutableStateOf(false) }

    MaterialTheme {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = inlineTotals,
                    onCheckedChange = { inlineTotals = it }
                )
                Text("Inline totals", modifier = Modifier.padding(start = 8.dp))
            }
            IetTable(modelBridge, inlineTotals = inlineTotals)
        }
    }
}
