package com.luckyzyx.luckytool.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * 底栏目标与实际分页位置的协调器。
 * 切页任务可被后续点击或拖动打断，选中项随最终落定页面同步。
 */
@Stable
internal class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
    private val selection: MutableIntState,
    pagingEnabled: Boolean,
) {
    var selectedPage by selection
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var pagingEnabled by mutableStateOf(pagingEnabled)
    val settledPage: Int get() = if (pagingEnabled) pagerState.settledPage else selectedPage

    fun updatePagingMode(enabled: Boolean) {
        if (pagingEnabled == enabled) return
        navigationJob?.cancel()
        navigationJob = null
        isNavigating = false
        pagingEnabled = enabled
        if (enabled) pagerState.requestScrollToPage(selectedPage)
    }

    private var navigationJob: Job? = null

    fun animateToPage(target: Int) {
        if (target !in 0 until pagerState.pageCount || target == selectedPage) return
        navigationJob?.cancel()
        navigationJob = null
        selectedPage = target
        if (!pagingEnabled) {
            isNavigating = false
            return
        }
        isNavigating = true
        // 先登记任务再启动，确保立即完成或取消时也能正确清理自己的状态。
        val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
            val currentJob = coroutineContext.job
            try {
                pagerState.animateScrollToPage(target)
            } finally {
                if (navigationJob == currentJob) {
                    isNavigating = false
                    selectedPage = pagerState.currentPage
                }
            }
        }
        navigationJob = job
        job.start()
    }

    fun syncPage() {
        if (pagingEnabled && !isNavigating) selectedPage = pagerState.currentPage
    }
}

@Composable
internal fun rememberMainPagerState(pagerState: PagerState): MainPagerState {
    val scope = rememberCoroutineScope()
    val selection = rememberSaveable { mutableIntStateOf(pagerState.currentPage) }
    val pagingEnabled = LocalUiMode.current != UiMode.Coui
    val state = remember(pagerState, scope) { MainPagerState(pagerState, scope, selection, pagingEnabled) }
    SideEffect { state.updatePagingMode(pagingEnabled) }
    return state
}

/** 根页面返回只切回主页；二级栈或阻塞弹层出现时交还返回处理权。 */
@Composable
internal fun MainPagerBackHandler(state: MainPagerState, enabled: Boolean) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = enabled && state.selectedPage != MainTab.Home.ordinal,
        onBackCompleted = { state.animateToPage(MainTab.Home.ordinal) },
    )
}

/** 五个一级页面共用 HorizontalPager 默认拖动、嵌套滚动和落位行为。 */
@Composable
internal fun MainPager(
    state: MainPagerState,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    pageKey: (Int) -> Any = { it },
    content: @Composable (Int) -> Unit,
) {
    val pager = state.pagerState
    var contentReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // 首屏完成一帧后再保留其他页，避免首次绘制同时加载所有页面。
        withFrameNanos { }
        withFrameNanos { }
        contentReady = true
    }
    LaunchedEffect(pager.currentPage) { state.syncPage() }

    HorizontalPager(
        state = pager,
        modifier = modifier.fillMaxSize(),
        key = pageKey,
        beyondViewportPageCount = if (contentReady) (pager.pageCount - 1).coerceAtLeast(0) else 0,
        userScrollEnabled = gesturesEnabled,
    ) { page ->
        if (contentReady || page == pager.settledPage) content(page)
    }
}
