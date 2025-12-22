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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
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
                                    itemsIndexed(performanceRows, key = { _, row -> row.id }) { index, row ->
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
                                    itemsIndexed(resourceRows, key = { _, row -> row.id }) { index, row ->
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
                                itemsIndexed(performanceRows, key = { _, row -> row.id }) { index, row ->
                                    DataRow(row, horizontalScrollState, modelBridge = modelBridge)
                                    Divider()
                                }
                                itemsIndexed(resourceRows, key = { _, row -> row.id }) { index, row ->
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
                    var offsetX by remember(column.id) { mutableStateOf(0f) }
                    var isDragging by remember(column.id) { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp)
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationX = offsetX
                                shadowElevation = if (isDragging) 8f else 0f
                            }
                            .pointerInput(column.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        offsetX = 0f
                                        isDragging = true
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        val numMoved = (offsetX / 120f).roundToInt()
                                        if (numMoved != 0) {
                                            val targetIndex = (index + numMoved).coerceIn(0, readModel.columns.size - 1)
                                            if (targetIndex != index) {
                                                modelBridge.reorderColumns(index, targetIndex)
                                            }
                                        }
                                        offsetX = 0f
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        offsetX = 0f
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
        var offsetY by remember(row.id) { mutableStateOf(0f) }
        var isDragging by remember(row.id) { mutableStateOf(false) }

        Surface(
            modifier = Modifier
                .width(200.dp)
                .height(60.dp)
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                    translationY = offsetY
                    shadowElevation = if (isDragging) 8f else 0f
                }
                .then(
                    if (!isTotal && !isPinned) {
                        Modifier.pointerInput(row.id) {
                            detectDragGestures(
                                onDragStart = {
                                    offsetY = 0f
                                    isDragging = true
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetY += dragAmount.y
                                },
                                onDragEnd = {
                                    isDragging = false
                                    val threshold = 60f // row height
                                    val numMoved = (offsetY / threshold).roundToInt()
                                    
                                    if (numMoved != 0) {
                                        val allRows = modelBridge.readModel.value.rows
                                        val normalRows = allRows.filter { !it.isPinnedFooter }
                                        val currentIndexInNormal = normalRows.indexOfFirst { it.id == row.id }
                                        
                                        if (currentIndexInNormal != -1) {
                                            val targetIndex = (currentIndexInNormal + numMoved).coerceIn(0, normalRows.size - 1)
                                            if (targetIndex != currentIndexInNormal) {
                                                modelBridge.reorderRows(currentIndexInNormal, targetIndex)
                                            }
                                        }
                                    }
                                    offsetY = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    offsetY = 0f
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
            Column(modifier = Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.Center) {
                Text(
                    text = formatRowName(row.id),
                    fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
                if (!isTotal && !isPinned) {
                    val detailText = when {
                        row.performanceDetails != null -> {
                            "${row.performanceDetails.current} -> ${row.performanceDetails.goal} ${row.unit}"
                        }
                        row.resourceDetails != null -> {
                            "<= ${row.resourceDetails.budget} ${row.unit}"
                        }
                        else -> ""
                    }
                    if (detailText.isNotEmpty()) {
                        Text(
                            text = detailText,
                            style = MaterialTheme.typography.caption,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        
        // Data Cells
        Box(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
            Row {
                row.cells.forEach { cell ->
                    Surface(
                        modifier = Modifier.width(120.dp).height(60.dp),
                        border = BorderStroke(0.5.dp, Color.LightGray),
                        color = if (isTotal && !isPinned) MaterialTheme.colors.secondary.copy(alpha = 0.05f) else Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val impactText = cell.impactPercent?.let { 
                                NumberPolicy.formatPercentage(it)
                            } ?: "N/A"
                            val confidencePlusMinusPctText =
                                cell.confidencePlusMinusPct?.let { NumberPolicy.formatPercentage(it) } ?: "?"
                            Text(
                                text = "${impactText} ±${confidencePlusMinusPctText}",
                                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
                                color = if (cell.impactPercent == null && !isTotal) Color.Red else Color.Unspecified
                            )
                            if (!isTotal && !isPinned && cell.estimatedValue != null) {
                                val estText = if (cell.confidenceRange != null) {
                                    "${cell.estimatedValue} ±${cell.confidenceRange}"
                                } else {
                                    "${cell.estimatedValue} ±?"
                                }
                                Text(
                                    text = estText,
                                    style = MaterialTheme.typography.caption,
                                    color = Color.Gray
                                )
                            }
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
