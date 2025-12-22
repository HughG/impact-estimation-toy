package org.tameter.iet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.tameter.common.HorizontalScrollbar
import org.tameter.common.VerticalScrollbar
import org.tameter.common.rememberScrollbarAdapter
import org.tameter.iet.model.DesignIdea
import org.tameter.iet.model.Estimation
import org.tameter.iet.model.PerformanceRequirement
import org.tameter.iet.model.ResourceRequirement
import org.tameter.iet.model.bridge.ModelBridge
import org.tameter.iet.model.bridge.RowType
import org.tameter.iet.model.bridge.TableReadModel
import org.tameter.iet.policy.NumberPolicy
import kotlin.math.roundToInt

private const val ROW_HEADER_KEY = "__row_header__"

private class ColumnWidthState {
    val widths = mutableStateMapOf<String, Dp>()

    fun updateWidth(key: String, width: Dp) {
        val current = widths[key] ?: 0.dp
        if (width > current) {
            widths[key] = width
        }
    }
}

@Composable
fun IetTable(modelBridge: ModelBridge, inlineTotals: Boolean = false) {
    val readModel by modelBridge.readModel.collectAsState()
    val tableWidthState = remember { ColumnWidthState() }
    val density = LocalDensity.current

    val horizontalScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.padding(16.dp)) {
            HeaderRow(readModel, horizontalScrollState, modelBridge, tableWidthState)
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
                                        DataRow(row, horizontalScrollState, modelBridge = modelBridge, tableWidthState = tableWidthState)
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
                        DataRow(it, horizontalScrollState, isTotal = true, modelBridge = modelBridge, tableWidthState = tableWidthState)
                        Divider(thickness = 2.dp)
                    }

                    // Resource Group Area
                    Box(modifier = Modifier.weight(1f)) {
                        val resScrollState = rememberLazyListState()
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(state = resScrollState, modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(resourceRows, key = { _, row -> row.id }) { index, row ->
                                        DataRow(row, horizontalScrollState, modelBridge = modelBridge, tableWidthState = tableWidthState)
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
                        DataRow(it, horizontalScrollState, isTotal = true, modelBridge = modelBridge, tableWidthState = tableWidthState)
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
                                    DataRow(row, horizontalScrollState, modelBridge = modelBridge, tableWidthState = tableWidthState)
                                    Divider()
                                }
                                itemsIndexed(resourceRows, key = { _, row -> row.id }) { index, row ->
                                    DataRow(row, horizontalScrollState, modelBridge = modelBridge, tableWidthState = tableWidthState)
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
                    DataRow(row, horizontalScrollState, isTotal = true, isPinned = true, modelBridge = modelBridge, tableWidthState = tableWidthState)
                }
                
                Row {
                    // Fixed corner spacer
                    val rowHeaderWidth = tableWidthState.widths[ROW_HEADER_KEY] ?: 200.dp
                    Spacer(modifier = Modifier.width(rowHeaderWidth.coerceAtLeast(200.dp)))
                    
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
    modelBridge: ModelBridge,
    tableWidthState: ColumnWidthState
) {
    val density = LocalDensity.current
    Row(modifier = Modifier.fillMaxWidth()) {
        // Fixed corner
        val rowHeaderWidth = tableWidthState.widths[ROW_HEADER_KEY] ?: 200.dp
        Surface(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = 200.dp.coerceAtLeast(rowHeaderWidth))
                .height(40.dp)
                .onSizeChanged { size ->
                    tableWidthState.updateWidth(ROW_HEADER_KEY, with(density) { size.width.toDp() })
                },
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
                    val columnWidth = tableWidthState.widths[column.id] ?: 120.dp

                    Surface(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .widthIn(min = 120.dp.coerceAtLeast(columnWidth))
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
                                        val widthPx = with(density) { columnWidth.toPx() }
                                        val numMoved = (offsetX / widthPx).roundToInt()
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
                            }
                            .onSizeChanged { size ->
                                tableWidthState.updateWidth(column.id, with(density) { size.width.toDp() })
                            },
                        border = BorderStroke(0.5.dp, Color.LightGray),
                        color = MaterialTheme.colors.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            EditableField(
                                value = column.id,
                                onValueChange = { newId ->
                                    if (newId != column.id) {
                                        modelBridge.updateDesignIdea(column.id, DesignIdea(newId))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                placeholder = "Idea name",
                                textColor = MaterialTheme.colors.onPrimary
                            )
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
private fun EditableField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCaption: Boolean = false,
    placeholder: String = "",
    textColor: Color = Color.Unspecified
) {
    val focusManager = LocalFocusManager.current
    var textState by remember(value) { mutableStateOf(value) }

    val onCommit = {
        if (textState != value) {
            onValueChange(textState)
        }
    }

    BasicTextField(
        value = textState,
        onValueChange = { textState = it },
        modifier = modifier
            .onKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onKeyEvent false
                
                if (it.key == Key.Enter) {
                    onCommit()
                    focusManager.moveFocus(FocusDirection.Down)
                    true
                } else if (it.key == Key.DirectionUp) {
                    onCommit()
                    focusManager.moveFocus(FocusDirection.Up)
                    true
                } else if (it.key == Key.DirectionDown) {
                    onCommit()
                    focusManager.moveFocus(FocusDirection.Down)
                    true
                } else if (it.key == Key.Tab) {
                    onCommit()
                    if (it.isShiftPressed) {
                        focusManager.moveFocus(FocusDirection.Previous)
                    } else {
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { 
                if (!it.isFocused) {
                    onCommit()
                }
            },
        textStyle = if (isCaption) {
            MaterialTheme.typography.caption.copy(color = if (textColor != Color.Unspecified) textColor else Color.Gray, textAlign = TextAlign.Center)
        } else {
            MaterialTheme.typography.body1.copy(color = textColor, textAlign = TextAlign.Center, fontWeight = if (textColor != Color.Unspecified) FontWeight.Bold else FontWeight.Normal)
        },
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (textState.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = if (isCaption) MaterialTheme.typography.caption else MaterialTheme.typography.body1,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun DataRow(
    row: org.tameter.iet.model.bridge.RowView,
    scrollState: ScrollState,
    isTotal: Boolean = false,
    isPinned: Boolean = false,
    modelBridge: ModelBridge,
    tableWidthState: ColumnWidthState
) {
    val density = LocalDensity.current
    Row(modifier = Modifier.fillMaxWidth()) {
        // Row Header (Requirement Name/ID)
        var offsetY by remember(row.id) { mutableStateOf(0f) }
        var isDragging by remember(row.id) { mutableStateOf(false) }

        val rowHeaderWidth = tableWidthState.widths[ROW_HEADER_KEY] ?: 200.dp
        Surface(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .widthIn(min = 200.dp.coerceAtLeast(rowHeaderWidth))
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
                )
                .onSizeChanged { size ->
                    tableWidthState.updateWidth(ROW_HEADER_KEY, with(density) { size.width.toDp() })
                },
            color = if (isTotal) {
                if (isPinned) MaterialTheme.colors.secondaryVariant else MaterialTheme.colors.secondary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colors.surface
            },
            border = BorderStroke(0.5.dp, Color.LightGray)
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.Center) {
                if (isTotal || isPinned) {
                    Text(
                        text = formatRowName(row.id),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    EditableField(
                        value = row.id,
                        onValueChange = { newId ->
                            if (newId != row.id) {
                                val currentReq = modelBridge.readModel.value.rows.find { it.id == row.id }
                                val req = when (row.type) {
                                    RowType.Performance -> PerformanceRequirement(
                                        id = newId,
                                        unit = row.unit,
                                        current = row.performanceDetails?.current ?: 0.0,
                                        goal = row.performanceDetails?.goal ?: 0.0
                                    )
                                    RowType.Resource -> ResourceRequirement(
                                        id = newId,
                                        unit = row.unit,
                                        budget = row.resourceDetails?.budget ?: 0.0
                                    )
                                    else -> null
                                }
                                req?.let { modelBridge.updateRequirement(row.id, it) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (row.type) {
                            RowType.Performance -> {
                                EditableField(
                                    value = row.performanceDetails?.current?.toString() ?: "",
                                    onValueChange = { newValue ->
                                        newValue.toDoubleOrNull()?.let { d ->
                                            modelBridge.updateRequirement(row.id, PerformanceRequirement(
                                                id = row.id,
                                                unit = row.unit,
                                                current = d,
                                                goal = row.performanceDetails?.goal ?: 0.0
                                            ))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    isCaption = true
                                )
                                Text(" -> ", style = MaterialTheme.typography.caption, color = Color.Gray)
                                EditableField(
                                    value = row.performanceDetails?.goal?.toString() ?: "",
                                    onValueChange = { newValue ->
                                        newValue.toDoubleOrNull()?.let { d ->
                                            modelBridge.updateRequirement(row.id, PerformanceRequirement(
                                                id = row.id,
                                                unit = row.unit,
                                                current = row.performanceDetails?.current ?: 0.0,
                                                goal = d
                                            ))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    isCaption = true
                                )
                            }
                            RowType.Resource -> {
                                Text("<= ", style = MaterialTheme.typography.caption, color = Color.Gray)
                                EditableField(
                                    value = row.resourceDetails?.budget?.toString() ?: "",
                                    onValueChange = { newValue ->
                                        newValue.toDoubleOrNull()?.let { d ->
                                            modelBridge.updateRequirement(row.id, ResourceRequirement(
                                                id = row.id,
                                                unit = row.unit,
                                                budget = d
                                            ))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    isCaption = true
                                )
                            }
                            else -> {}
                        }
                        EditableField(
                            value = row.unit,
                            onValueChange = { newUnit ->
                                val req = when (row.type) {
                                    RowType.Performance -> PerformanceRequirement(
                                        id = row.id,
                                        unit = newUnit,
                                        current = row.performanceDetails?.current ?: 0.0,
                                        goal = row.performanceDetails?.goal ?: 0.0
                                    )
                                    RowType.Resource -> ResourceRequirement(
                                        id = row.id,
                                        unit = newUnit,
                                        budget = row.resourceDetails?.budget ?: 0.0
                                    )
                                    else -> null
                                }
                                req?.let { modelBridge.updateRequirement(row.id, it) }
                            },
                            modifier = Modifier.width(40.dp),
                            isCaption = true,
                            placeholder = "unit"
                        )
                    }
                }
            }
        }
        
        // Data Cells
        Box(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
            Row {
                row.cells.forEach { cell ->
                    val columnWidth = tableWidthState.widths[cell.columnId] ?: 120.dp
                    Surface(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .widthIn(min = 120.dp.coerceAtLeast(columnWidth))
                            .height(60.dp)
                            .onSizeChanged { size ->
                                tableWidthState.updateWidth(cell.columnId, with(density) { size.width.toDp() })
                            },
                        border = BorderStroke(0.5.dp, Color.LightGray),
                        color = if (isTotal && !isPinned) MaterialTheme.colors.secondary.copy(alpha = 0.05f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isTotal || isPinned) {
                                val impactText = cell.impactPercent?.let { 
                                    NumberPolicy.formatPercentage(it)
                                } ?: "N/A"
                                Text(
                                    text = impactText,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cell.impactPercent == null) Color.Red else Color.Unspecified
                                )
                            } else {
                                EditableField(
                                    value = cell.estimatedValue?.toString() ?: "",
                                    onValueChange = { newValue ->
                                        val d = newValue.toDoubleOrNull()
                                        if (d != null) {
                                            modelBridge.setEstimation(cell.rowId, cell.columnId, Estimation(d, cell.confidenceRange))
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = "val"
                                )
                                Text("±", style = MaterialTheme.typography.caption, color = Color.Gray)
                                EditableField(
                                    value = cell.confidenceRange?.toString() ?: "",
                                    onValueChange = { newValue ->
                                        val d = newValue.toDoubleOrNull()
                                        modelBridge.setEstimation(cell.rowId, cell.columnId, Estimation(cell.estimatedValue ?: 0.0, d))
                                    },
                                    modifier = Modifier.weight(1f),
                                    isCaption = true,
                                    placeholder = "conf"
                                )
                                
                                cell.impactPercent?.let { 
                                    Text(
                                        text = " (${NumberPolicy.formatPercentage(it)})",
                                        style = MaterialTheme.typography.caption,
                                        color = Color.Gray
                                    )
                                }
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
