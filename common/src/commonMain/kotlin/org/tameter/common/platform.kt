package org.tameter.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect fun getPlatformName(): String

@Composable
expect fun VerticalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
)

@Composable
expect fun HorizontalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier = Modifier,
)

expect interface ScrollbarAdapter

@Composable
expect fun rememberScrollbarAdapter(scrollState: ScrollState): ScrollbarAdapter

@Composable
expect fun rememberScrollbarAdapter(lazyListState: LazyListState): ScrollbarAdapter