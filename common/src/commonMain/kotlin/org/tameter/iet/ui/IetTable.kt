package org.tameter.iet.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.model.bridge.RowType
import org.tameter.iet.model.bridge.TableReadModel
import org.tameter.iet.policy.NumberPolicy

@Composable
fun IetTable(modelBridge: ModelBridge, inlineTotals: Boolean = false) {
    val readModel by modelBridge.readModel.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.weight(1f)) {
            val horizontalScrollState = rememberScrollState()
            
            Column {
                // Header Row
                HeaderRow(readModel, horizontalScrollState)
                
                // Data Rows
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn {
                        val performanceRows = readModel.rows.filter { it.type == RowType.Performance }
                        val resourceRows = readModel.rows.filter { it.type == RowType.Resource }
                        val footerRows = readModel.rows.filter { it.isPinnedFooter }
                        
                        // Performance Group
                        items(performanceRows) { row ->
                            DataRow(row, horizontalScrollState)
                            Divider()
                        }
                        
                        if (inlineTotals) {
                            val perfTotal = footerRows.find { it.id == "__perf_totals__" }
                            perfTotal?.let {
                                item {
                                    DataRow(it, horizontalScrollState, isTotal = true)
                                    Divider(thickness = 2.dp)
                                }
                            }
                        }

                        // Resource Group
                        items(resourceRows) { row ->
                            DataRow(row, horizontalScrollState)
                            Divider()
                        }
                        
                        if (inlineTotals) {
                            val resTotal = footerRows.find { it.id == "__res_totals__" }
                            resTotal?.let {
                                item {
                                    DataRow(it, horizontalScrollState, isTotal = true)
                                    Divider(thickness = 2.dp)
                                }
                            }
                        }
                    }
                }

                // Pinned Footer Rows
                Column {
                    val footerRows = readModel.rows.filter { it.isPinnedFooter }
                    val pinnedToShow = if (inlineTotals) {
                        footerRows.filter { it.id == "__ratio__" }
                    } else {
                        footerRows
                    }
                    
                    pinnedToShow.forEach { row ->
                        Divider(thickness = 2.dp)
                        DataRow(row, horizontalScrollState, isTotal = true, isPinned = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(readModel: TableReadModel, scrollState: ScrollState) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Fixed corner
        Surface(
            modifier = Modifier.width(200.dp).height(40.dp),
            color = MaterialTheme.colors.primary
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Requirements", fontWeight = FontWeight.Bold)
            }
        }
        
        // Scrollable headers
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            readModel.columns.forEach { column ->
                Surface(
                    modifier = Modifier.width(120.dp).height(40.dp),
                    border = BorderStroke(0.5.dp, Color.LightGray),
                    color = MaterialTheme.colors.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(column.id, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataRow(
    row: org.tameter.iet.model.bridge.RowView,
    scrollState: ScrollState,
    isTotal: Boolean = false,
    isPinned: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Row Header (Requirement Name/ID)
        Surface(
            modifier = Modifier.width(200.dp).height(40.dp),
            color = if (isTotal) {
                if (isPinned) MaterialTheme.colors.secondaryVariant else MaterialTheme.colors.secondary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colors.surface
            },
            border = BorderStroke(0.5.dp, Color.LightGray)
        ) {
            Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    text = formatRowName(row.id),
                    fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        
        // Data Cells
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            row.cells.forEach { cell ->
                Surface(
                    modifier = Modifier.width(120.dp).height(40.dp),
                    border = BorderStroke(0.5.dp, Color.LightGray),
                    color = if (isTotal && !isPinned) MaterialTheme.colors.secondary.copy(alpha = 0.05f) else Color.Transparent
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val text = cell.impactPercent?.let { 
                            NumberPolicy.formatPercentage(it)
                        } ?: "N/A"
                        Text(
                            text = text,
                            color = if (cell.impactPercent == null && !isTotal) Color.Red else Color.Unspecified
                        )
                    }
                }
            }
        }
    }
}

private fun formatRowName(id: String): String {
    return when (id) {
        "__perf_totals__" -> "Performance Total"
        "__res_totals__" -> "Resource Total"
        "__ratio__" -> "P/C Ratio"
        else -> id
    }
}
