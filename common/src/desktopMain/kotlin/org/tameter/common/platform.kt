package org.tameter.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual fun getPlatformName(): String {
    return "Desktop"
}

actual typealias ScrollbarAdapter = androidx.compose.foundation.v2.ScrollbarAdapter

@Composable
actual fun VerticalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier
) = androidx.compose.foundation.VerticalScrollbar(adapter, modifier)

@Composable
actual fun HorizontalScrollbar(
    adapter: ScrollbarAdapter,
    modifier: Modifier
) = androidx.compose.foundation.HorizontalScrollbar(adapter, modifier)

@Composable
actual fun rememberScrollbarAdapter(scrollState: ScrollState): ScrollbarAdapter =
    androidx.compose.foundation.rememberScrollbarAdapter(scrollState)

@Composable
actual fun rememberScrollbarAdapter(lazyListState: LazyListState): ScrollbarAdapter =
    androidx.compose.foundation.rememberScrollbarAdapter(lazyListState)