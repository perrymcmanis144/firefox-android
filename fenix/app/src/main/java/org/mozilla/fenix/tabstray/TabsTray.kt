/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.TabEntry
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.compose.Divider
import org.mozilla.fenix.compose.annotation.LightDarkPreview
import org.mozilla.fenix.tabstray.ext.isNormalTab
import org.mozilla.fenix.tabstray.inactivetabs.InactiveTabsList
import org.mozilla.fenix.tabstray.syncedtabs.SyncedTabsList
import org.mozilla.fenix.tabstray.syncedtabs.SyncedTabsListItem
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.browser.storage.sync.Tab as SyncTab

/**
 * Top-level UI for displaying the Tabs Tray feature.
 *
 * @param appStore [AppStore] used to listen for changes to [AppState].
 * @param browserStore [BrowserStore] used to listen for changes to [BrowserState].
 * @param tabsTrayStore [TabsTrayStore] used to listen for changes to [TabsTrayState].
 * @param displayTabsInGrid Whether the normal and private tabs should be displayed in a grid.
 * @param onTabClose Invoked when the user clicks to close a tab.
 * @param onTabMediaClick Invoked when the user interacts with a tab's media controls.
 * @param onTabClick Invoked when the user clicks on a tab.
 * @param onTabMultiSelectClick Invoked when the user clicks on a tab while in multi-select mode.
 * @param onTabLongClick Invoked when the user long clicks a tab.
 * @param onInactiveTabsHeaderClick Invoked when the user clicks on the inactive tabs section header.
 * @param onDeleteAllInactiveTabsClick Invoked when the user clicks on the delete all inactive tabs button.
 * @param onInactiveTabsAutoCloseDialogShown Invoked when the inactive tabs auto close dialog
 * is presented to the user.
 * @param onInactiveTabAutoCloseDialogCloseButtonClick Invoked when the user clicks on the inactive
 * tab auto close dialog's dismiss button.
 * @param onEnableInactiveTabAutoCloseClick Invoked when the user clicks on the inactive tab auto
 * close dialog's enable button.
 * @param onInactiveTabClick Invoked when the user clicks on an inactive tab.
 * @param onInactiveTabClose Invoked when the user clicks on an inactive tab's close button.
 * @param onSyncedTabClick Invoked when the user clicks on a synced tab.
 */
@OptIn(ExperimentalPagerApi::class, ExperimentalComposeUiApi::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
fun TabsTray(
    appStore: AppStore,
    browserStore: BrowserStore,
    tabsTrayStore: TabsTrayStore,
    displayTabsInGrid: Boolean,
    shouldShowInactiveTabsAutoCloseDialog: (Int) -> Boolean,
    onTabPageClick: (Page) -> Unit,
    onTabClose: (TabSessionState) -> Unit,
    onTabMediaClick: (TabSessionState) -> Unit,
    onTabClick: (TabSessionState) -> Unit,
    onTabMultiSelectClick: (TabSessionState) -> Unit,
    onTabLongClick: (TabSessionState) -> Unit,
    onInactiveTabsHeaderClick: (Boolean) -> Unit,
    onDeleteAllInactiveTabsClick: () -> Unit,
    onInactiveTabsAutoCloseDialogShown: () -> Unit,
    onInactiveTabAutoCloseDialogCloseButtonClick: () -> Unit,
    onEnableInactiveTabAutoCloseClick: () -> Unit,
    onInactiveTabClick: (TabSessionState) -> Unit,
    onInactiveTabClose: (TabSessionState) -> Unit,
    onSyncedTabClick: (SyncTab) -> Unit,
) {
    val selectedTabId = browserStore
        .observeAsComposableState { state -> state.selectedTabId }.value
    val multiselectMode = tabsTrayStore
        .observeAsComposableState { state -> state.mode }.value ?: TabsTrayState.Mode.Normal
    val selectedPage = tabsTrayStore
        .observeAsComposableState { state -> state.selectedPage }.value ?: Page.NormalTabs
    val normalTabs = tabsTrayStore
        .observeAsComposableState { state -> state.normalTabs }.value ?: emptyList()
    val privateTabs = tabsTrayStore
        .observeAsComposableState { state -> state.privateTabs }.value ?: emptyList()
    val inactiveTabsExpanded = appStore
        .observeAsComposableState { state -> state.inactiveTabsExpanded }.value ?: false
    val inactiveTabs = tabsTrayStore
        .observeAsComposableState { state -> state.inactiveTabs }.value ?: emptyList()
    val pagerState = rememberPagerState(initialPage = selectedPage.ordinal)
    val isInMultiSelectMode = multiselectMode is TabsTrayState.Mode.Select

    val handleTabClick: ((TabSessionState) -> Unit) = { tab ->
        if (isInMultiSelectMode) {
            onTabMultiSelectClick(tab)
        } else {
            onTabClick(tab)
        }
    }

    LaunchedEffect(selectedPage) {
        pagerState.animateScrollToPage(selectedPage.ordinal)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(FirefoxTheme.colors.layer1),
    ) {
        Box(modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
            TabsTrayBanner(
                isInMultiSelectMode = isInMultiSelectMode,
                selectedPage = selectedPage,
                normalTabCount = normalTabs.size + inactiveTabs.size,
                onTabPageIndicatorClicked = onTabPageClick,
            )
        }

        Divider()

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                count = Page.values().size,
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                userScrollEnabled = false,
            ) { position ->
                when (Page.positionToPage(position)) {
                    Page.NormalTabs -> {
                        val showInactiveTabsAutoCloseDialog = shouldShowInactiveTabsAutoCloseDialog(inactiveTabs.size)
                        var showAutoCloseDialog by remember { mutableStateOf(showInactiveTabsAutoCloseDialog) }

                        val optionalInactiveTabsHeader: (@Composable () -> Unit)? = if (inactiveTabs.isEmpty()) {
                            null
                        } else {
                            {
                                InactiveTabsList(
                                    inactiveTabs = inactiveTabs,
                                    expanded = inactiveTabsExpanded,
                                    showAutoCloseDialog = showAutoCloseDialog,
                                    onHeaderClick = onInactiveTabsHeaderClick,
                                    onDeleteAllButtonClick = onDeleteAllInactiveTabsClick,
                                    onAutoCloseDismissClick = {
                                        onInactiveTabAutoCloseDialogCloseButtonClick()
                                        showAutoCloseDialog = !showAutoCloseDialog
                                    },
                                    onEnableAutoCloseClick = {
                                        onEnableInactiveTabAutoCloseClick()
                                        showAutoCloseDialog = !showAutoCloseDialog
                                    },
                                    onTabClick = onInactiveTabClick,
                                    onTabCloseClick = onInactiveTabClose,
                                )
                            }
                        }

                        if (showInactiveTabsAutoCloseDialog) {
                            onInactiveTabsAutoCloseDialogShown()
                        }

                        TabLayout(
                            tabs = normalTabs,
                            displayTabsInGrid = displayTabsInGrid,
                            selectedTabId = selectedTabId,
                            selectionMode = multiselectMode,
                            onTabClose = onTabClose,
                            onTabMediaClick = onTabMediaClick,
                            onTabClick = handleTabClick,
                            onTabLongClick = onTabLongClick,
                            header = optionalInactiveTabsHeader,
                        )
                    }
                    Page.PrivateTabs -> {
                        TabLayout(
                            tabs = privateTabs,
                            displayTabsInGrid = displayTabsInGrid,
                            selectedTabId = selectedTabId,
                            selectionMode = multiselectMode,
                            onTabClose = onTabClose,
                            onTabMediaClick = onTabMediaClick,
                            onTabClick = handleTabClick,
                            onTabLongClick = onTabLongClick,
                        )
                    }
                    Page.SyncedTabs -> {
                        val syncedTabs = tabsTrayStore
                            .observeAsComposableState { state -> state.syncedTabs }.value ?: emptyList()

                        SyncedTabsList(
                            syncedTabs = syncedTabs,
                            taskContinuityEnabled = true,
                            onTabClick = onSyncedTabClick,
                        )
                    }
                }
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun TabsTrayPreview() {
    val tabs = generateFakeTabsList()
    TabsTrayPreviewRoot(
        displayTabsInGrid = false,
        selectedTabId = tabs[0].id,
        normalTabs = tabs,
        privateTabs = generateFakeTabsList(
            tabCount = 7,
            isPrivate = true,
        ),
        syncedTabs = generateFakeSyncedTabsList(),
    )
}

@Suppress("MagicNumber")
@LightDarkPreview
@Composable
private fun TabsTrayMultiSelectPreview() {
    val tabs = generateFakeTabsList()
    TabsTrayPreviewRoot(
        selectedTabId = tabs[0].id,
        mode = TabsTrayState.Mode.Select(tabs.take(4).toSet()),
        normalTabs = tabs,
    )
}

@LightDarkPreview
@Composable
private fun TabsTrayInactiveTabsPreview() {
    TabsTrayPreviewRoot(
        normalTabs = generateFakeTabsList(tabCount = 3),
        inactiveTabs = generateFakeTabsList(),
        inactiveTabsExpanded = true,
        showInactiveTabsAutoCloseDialog = true,
    )
}

@LightDarkPreview
@Composable
private fun TabsTrayPrivateTabsPreview() {
    TabsTrayPreviewRoot(
        selectedPage = Page.PrivateTabs,
        privateTabs = generateFakeTabsList(isPrivate = true),
    )
}

@LightDarkPreview
@Composable
private fun TabsTraySyncedTabsPreview() {
    TabsTrayPreviewRoot(
        selectedPage = Page.SyncedTabs,
        syncedTabs = generateFakeSyncedTabsList(deviceCount = 3),
    )
}

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun TabsTrayPreviewRoot(
    displayTabsInGrid: Boolean = true,
    selectedPage: Page = Page.NormalTabs,
    selectedTabId: String? = null,
    mode: TabsTrayState.Mode = TabsTrayState.Mode.Normal,
    normalTabs: List<TabSessionState> = emptyList(),
    inactiveTabs: List<TabSessionState> = emptyList(),
    privateTabs: List<TabSessionState> = emptyList(),
    syncedTabs: List<SyncedTabsListItem> = emptyList(),
    inactiveTabsExpanded: Boolean = false,
    showInactiveTabsAutoCloseDialog: Boolean = false,
) {
    var selectedPageState by remember { mutableStateOf(selectedPage) }
    val normalTabsState = remember { normalTabs.toMutableStateList() }
    val inactiveTabsState = remember { inactiveTabs.toMutableStateList() }
    val privateTabsState = remember { privateTabs.toMutableStateList() }
    val syncedTabsState = remember { syncedTabs.toMutableStateList() }
    var inactiveTabsExpandedState by remember { mutableStateOf(inactiveTabsExpanded) }
    var showInactiveTabsAutoCloseDialogState by remember { mutableStateOf(showInactiveTabsAutoCloseDialog) }

    val appStore = AppStore(
        initialState = AppState(
            inactiveTabsExpanded = inactiveTabsExpandedState,
        ),
    )
    val browserStore = BrowserStore(
        initialState = BrowserState(
            tabs = normalTabs + privateTabs,
            selectedTabId = selectedTabId,
        ),
    )
    val tabsTrayStore = TabsTrayStore(
        initialState = TabsTrayState(
            selectedPage = selectedPageState,
            mode = mode,
            inactiveTabs = inactiveTabsState,
            normalTabs = normalTabsState,
            privateTabs = privateTabsState,
            syncedTabs = syncedTabsState,
        ),
    )

    FirefoxTheme {
        TabsTray(
            appStore = appStore,
            browserStore = browserStore,
            tabsTrayStore = tabsTrayStore,
            displayTabsInGrid = displayTabsInGrid,
            shouldShowInactiveTabsAutoCloseDialog = { true },
            onTabPageClick = { page ->
                selectedPageState = page
            },
            onTabClose = { tab ->
                if (tab.isNormalTab()) {
                    normalTabsState.remove(tab)
                } else {
                    privateTabsState.remove(tab)
                }
            },
            onTabMediaClick = {},
            onTabClick = {},
            onTabMultiSelectClick = { tab ->
                if (tabsTrayStore.state.mode.selectedTabs.contains(tab)) {
                    tabsTrayStore.dispatch(TabsTrayAction.RemoveSelectTab(tab))
                } else {
                    tabsTrayStore.dispatch(TabsTrayAction.AddSelectTab(tab))
                }
            },
            onTabLongClick = { tab ->
                tabsTrayStore.dispatch(TabsTrayAction.AddSelectTab(tab))
            },
            onInactiveTabsHeaderClick = {
                inactiveTabsExpandedState = !inactiveTabsExpandedState
            },
            onDeleteAllInactiveTabsClick = inactiveTabsState::clear,
            onInactiveTabsAutoCloseDialogShown = {},
            onInactiveTabAutoCloseDialogCloseButtonClick = {
                showInactiveTabsAutoCloseDialogState = !showInactiveTabsAutoCloseDialogState
            },
            onEnableInactiveTabAutoCloseClick = {
                showInactiveTabsAutoCloseDialogState = !showInactiveTabsAutoCloseDialogState
            },
            onInactiveTabClick = {},
            onInactiveTabClose = inactiveTabsState::remove,
            onSyncedTabClick = {},
        )
    }
}

private fun generateFakeTabsList(tabCount: Int = 10, isPrivate: Boolean = false): List<TabSessionState> =
    List(tabCount) { index ->
        TabSessionState(
            id = "tabId$index-$isPrivate",
            content = ContentState(
                url = "www.mozilla.com",
                private = isPrivate,
            ),
        )
    }

private fun generateFakeSyncedTabsList(deviceCount: Int = 1): List<SyncedTabsListItem> =
    List(deviceCount) { index ->
        SyncedTabsListItem.DeviceSection(
            displayName = "Device $index",
            tabs = listOf(
                generateFakeSyncedTab("Mozilla", "www.mozilla.org"),
                generateFakeSyncedTab("Google", "www.google.com"),
                generateFakeSyncedTab("", "www.google.com"),
            ),
        )
    }

private fun generateFakeSyncedTab(tabName: String, tabUrl: String): SyncedTabsListItem.Tab =
    SyncedTabsListItem.Tab(
        tabName.ifEmpty { tabUrl },
        tabUrl,
        SyncTab(
            history = listOf(TabEntry(tabName, tabUrl, null)),
            active = 0,
            lastUsed = 0L,
        ),
    )
