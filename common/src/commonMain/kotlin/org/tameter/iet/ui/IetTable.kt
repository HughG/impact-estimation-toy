package org.tameter.iet.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.tameter.common.HorizontalScrollbar
import org.tameter.common.VerticalScrollbar
import org.tameter.common.rememberScrollbarAdapter
import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.model.bridge.RowType
import org.tameter.iet.model.bridge.TableReadModel
import org.tameter.iet.policy.NumberPolicy

@Composable
fun IetTable(modelBridge: ModelBridge, inlineTotals: Boolean = false) {
    val readModel by modelBridge.readModel.collectAsState()

    val horizontalScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(16.dp)) {
            HeaderRow(readModel, horizontalScrollState, modelBridge)
        }

        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            val performanceRows = readModel.rows.filter { it.type == RowType.Performance }
            val resourceRows = readModel.rows.filter { it.type == RowType.Resource }
            val footerRows = readModel.rows.filter { it.isPinnedFooter }

            if (inlineTotals) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Performance Group Area
                    Box(modifier = Modifier.weight(1f)) {
                        val perfScrollState = rememberLazyListState()
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(state = perfScrollState, modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(performanceRows) { index, row ->
                                        DataRow(row, horizontalScrollState, modelBridge = modelBridge)
                                        Divider()
                                    }
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(perfScrollState),
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                    }

                    // Performance Total (Fixed between groups)
                    val perfTotal = footerRows.find { it.id == "__perf_totals__" }
                    perfTotal?.let {
                        DataRow(it, horizontalScrollState, isTotal = true, modelBridge = modelBridge)
                        Divider(thickness = 2.dp)
                    }

                    // Resource Group Area
                    Box(modifier = Modifier.weight(1f)) {
                        val resScrollState = rememberLazyListState()
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(state = resScrollState, modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(resourceRows) { index, row ->
                                        DataRow(row, horizontalScrollState, modelBridge = modelBridge)
                                        Divider()
                                    }
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(resScrollState),
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                    }

                    // Resource Total (Fixed after Resource group)
                    val resTotal = footerRows.find { it.id == "__res_totals__" }
                    resTotal?.let {
                        DataRow(it, horizontalScrollState, isTotal = true, modelBridge = modelBridge)
                        Divider(thickness = 2.dp)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    val mainScrollState = rememberLazyListState()
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            LazyColumn(state = mainScrollState, modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(performanceRows) { index, row ->
                                    DataRow(row, horizontalScrollState, modelBridge = modelBridge)
                                    Divider()
                                }
                                itemsIndexed(resourceRows) { index, row ->
                                    DataRow(row, horizontalScrollState, modelBridge = modelBridge)
                                    Divider()
                                }
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(mainScrollState),
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }
        }

        // Pinned Footer Rows (Ratio always here, others here if not inline)
        Box(modifier = Modifier.padding(16.dp)) {
            Column {
                val footerRows = readModel.rows.filter { it.isPinnedFooter }
                val pinnedToShow = if (inlineTotals) {
                    footerRows.filter { it.id == "__ratio__" }
                } else {
                    footerRows
                }

                pinnedToShow.forEach { row ->
                    Divider(thickness = 2.dp)
                    DataRow(row, horizontalScrollState, isTotal = true, isPinned = true, modelBridge = modelBridge)
                }
                
                Row {
                    // Fixed corner spacer
                    Spacer(modifier = Modifier.width(200.dp))
                    
                    HorizontalScrollbar(
                        adapter = rememberScrollbarAdapter(horizontalScrollState),
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Spacer for vertical scrollbar
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    readModel: TableReadModel,
    scrollState: ScrollState,
    modelBridge: ModelBridge
) {
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
        Box(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
            Row {
                readModel.columns.forEachIndexed { index, column ->
                    var offsetX by remember { mutableStateOf(0f) }
                    Surface(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp)
                            .pointerInput(column.id) {
                                detectDragGestures(
                                    onDragStart = { offsetX = 0f },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                        val threshold = 120f // column width
                                        if (offsetX > threshold && index < readModel.columns.size - 1) {
                                            modelBridge.reorderColumns(index, index + 1)
                                            offsetX -= threshold
                                        } else if (offsetX < -threshold && index > 0) {
                                            modelBridge.reorderColumns(index, index - 1)
                                            offsetX += threshold
                                        }
                                    }
                                )
                            },
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
        // Spacer for vertical scrollbar
        Spacer(modifier = Modifier.width(12.dp))
    }
}

@Composable
private fun DataRow(
    row: org.tameter.iet.model.bridge.RowView,
    scrollState: ScrollState,
    isTotal: Boolean = false,
    isPinned: Boolean = false,
    modelBridge: ModelBridge
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Row Header (Requirement Name/ID)
        var offsetY by remember { mutableStateOf(0f) }
        Surface(
            modifier = Modifier
                .width(200.dp)
                .height(40.dp)
                .then(
                    if (!isTotal && !isPinned) {
                        Modifier.pointerInput(row.id) {
                            detectDragGestures(
                                onDragStart = { offsetY = 0f },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetY += dragAmount.y
                                    val threshold = 40f // row height
                                    
                                    val allRows = modelBridge.readModel.value.rows
                                    val normalRows = allRows.filter { !it.isPinnedFooter }
                                    val currentIndexInNormal = normalRows.indexOfFirst { it.id == row.id }
                                    
                                    if (currentIndexInNormal != -1) {
                                        if (offsetY > threshold && currentIndexInNormal < normalRows.size - 1) {
                                            modelBridge.reorderRows(currentIndexInNormal, currentIndexInNormal + 1)
                                            offsetY -= threshold
                                        } else if (offsetY < -threshold && currentIndexInNormal > 0) {
                                            modelBridge.reorderRows(currentIndexInNormal, currentIndexInNormal - 1)
                                            offsetY += threshold
                                        }
                                    }
                                }
                            )
                        }
                    } else Modifier
                ),
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
        Box(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
            Row {
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
        if (!isPinned) {
            // Spacer for vertical scrollbar
            Spacer(modifier = Modifier.width(12.dp))
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
