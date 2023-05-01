package com.parmclee.orientmapplaces.ui.screen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.orientmapplaces.R
import com.parmclee.orientmapplaces.data.MapDateFilter
import com.parmclee.orientmapplaces.data.Preferences
import com.parmclee.orientmapplaces.ui.MainUiState
import com.parmclee.orientmapplaces.ui.MainViewModel
import com.parmclee.orientmapplaces.ui.theme.Gold


@Composable
fun SettingsScreen(viewModel: MainViewModel,
                   onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
    val backHelper = LocalOnBackPressedDispatcherOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isAuthorized = remember{ mutableStateOf(Preferences.getLogin() != null) }
    Column(Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        TopAppBar(title = {
            Text(stringResource(id = R.string.settings),
                color = MaterialTheme.colorScheme.onBackground)
        }, navigationIcon = {
            IconButton(onClick = {
                backHelper?.onBackPressedDispatcher?.onBackPressed()
            }) {
                Icon(painter = painterResource(id = R.drawable.arrow_back_48px), contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            }
        }, backgroundColor = MaterialTheme.colorScheme.background)
        Column(Modifier
            .verticalScroll(rememberScrollState())
            .padding(12.dp)) {
            SettingsContainer {
                ColorSettings(selectedFilter = settings.filter,
                    onSelect = {
                        viewModel.changeSettings(settings.copy(filter = it))
                    })
            }
            SettingsContainer {
                SyncSettings(state, isLoading) {
                    viewModel.getMaps()
                }
            }
            if (isAuthorized.value) {
                SettingsContainer {
                    AuthSettings(onLogout = {
                        viewModel.logout()
                        isAuthorized.value = false
                    })
                }
                SettingsContainer {
                    CenterMyMapsSettings(state, onClicked = {
                        viewModel.changeCentering(it)
                    })
                }
            }
        }
    }
}

@Composable
fun SyncSettings(uiState: MainUiState,
                 isLoading: Boolean,
                 onClicked: () -> Unit) {
    Column {
        SettingsTitle(text = stringResource(id = R.string.sync))
        Row(Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(id = R.string.last_sync, uiState.settings.getDateText() ?: ""),
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.width(10.dp))
            if (isLoading) {
                CircularProgressIndicator(Modifier
                    .padding(3.dp)
                    .size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary)
            } else {
                Icon(painter = painterResource(id = R.drawable.refresh_24px),
                    contentDescription = null,
                    Modifier
                        .clip(CircleShape)
                        .clickable { onClicked() },
                    tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun AuthSettings(onLogout: () -> Unit) {
    val (showLogoutDialog, setShowLogoutDialog) = remember { mutableStateOf(false) }
    val isPremium = Preferences.isPremium()
    Column {
        SettingsTitle(text = stringResource(id = R.string.user))
        Row(Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = if (isPremium) R.drawable.star_24px else R.drawable.person_24px),
                contentDescription = null,
                tint = if (isPremium) Gold else MaterialTheme.colorScheme.primary)
            Text(text = Preferences.getName() ?: "",
                Modifier
                    .padding(start = 4.dp)
                    .weight(1f),
                color = if (isPremium) Gold else MaterialTheme.colorScheme.onBackground)
            Row(Modifier
                .clip(RoundedCornerShape(40.dp))
                .clickable { setShowLogoutDialog(true) }
                .padding(10.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = R.drawable.logout_40px), contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(id = R.string.logout_from_profile),
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    SimpleAlertDialog(showLogoutDialog, setShowLogoutDialog,
        stringResource(id = R.string.logout),
        stringResource(id = R.string.logout_confirm),
        positiveClickListener = onLogout)
}

@Composable
fun CenterMyMapsSettings(uiState: MainUiState,
                         onClicked: (Boolean) -> Unit) {
    Column {
        SettingsTitle(text = stringResource(id = R.string.centering))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = uiState.settings.needCentering,
                onCheckedChange = { onClicked(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onBackground))
            Text(text = stringResource(id = R.string.centering_by_own_maps),
                Modifier.clickable { onClicked(!uiState.settings.needCentering) },
                color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
fun ColorSettings(selectedFilter: MapDateFilter,
                  onSelect: (MapDateFilter) -> Unit) {
    val height = 14.dp
    val gradientHorizontalPadding = 14.dp
    val datesHorizontalPadding = 12.dp
    Column(Modifier.fillMaxWidth()) {
        SettingsTitle(text = stringResource(id = R.string.filter_map_settings))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selectedFilter == MapDateFilter.ALL,
                onClick = { onSelect(MapDateFilter.ALL) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onBackground))
            Text(text = stringResource(id = R.string.all),
                Modifier.clickable { onSelect(MapDateFilter.ALL) },
                style = MaterialTheme.typography.bodyMedium
                    .copy(MaterialTheme.colorScheme.onBackground))
        }
        Row(modifier = Modifier
            .padding(gradientHorizontalPadding, 2.dp, gradientHorizontalPadding)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(Brush.horizontalGradient(
                0f to Color.Green,
                0.25f to Color.Green,
                0.375f to Color.Yellow,
                0.5f to Color.Red,
                0.5001f to Color.Green,
                1f to Color.Green
            )), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(1.dp))
            Box(modifier = Modifier
                .size(2.dp, height)
                .background(Color.Black))
            Box(modifier = Modifier
                .size(2.dp, height)
                .background(Color.Black))
            Box(modifier = Modifier.size(1.dp))
            Box(modifier = Modifier.size(1.dp))
        }
        Row(Modifier
            .fillMaxWidth()
            .padding(datesHorizontalPadding, 4.dp, datesHorizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween) {
            DateText(text = stringResource(id = R.string.before_c), TextAlign.Start)
            DateText(text = stringResource(id = R.string.year_ago))
            DateText(text = stringResource(id = R.string.now))
            DateText(text = "")
            DateText(text = stringResource(id = R.string.light_future), TextAlign.End)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selectedFilter == MapDateFilter.FUTURE,
                onClick = { onSelect(MapDateFilter.FUTURE) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onBackground))
            Text(text = stringResource(id = R.string.with_future_competitions),
                Modifier.clickable { onSelect(MapDateFilter.FUTURE) },
                style = MaterialTheme.typography.bodyMedium
                    .copy(MaterialTheme.colorScheme.onBackground))
        }
        Row(modifier = Modifier
            .padding(gradientHorizontalPadding, 2.dp, gradientHorizontalPadding)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(Brush.horizontalGradient(
                0f to Color.Red,
                0.25f to Color.Yellow,
                0.5f to Color.Green,
                1f to Color.Green
            )), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(1.dp))
            Box(modifier = Modifier.size(1.dp))
            Box(modifier = Modifier
                .size(2.dp, height)
                .background(Color.Black))
            Box(modifier = Modifier.size(1.dp))
            Box(modifier = Modifier.size(1.dp))
        }
        Row(Modifier
            .fillMaxWidth()
            .padding(datesHorizontalPadding, 4.dp, datesHorizontalPadding), horizontalArrangement = Arrangement.SpaceBetween) {
            DateText(text = stringResource(id = R.string.now))
            DateText(text = stringResource(id = R.string.year_after))
            DateText(text = stringResource(id = R.string.light_future), TextAlign.End)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selectedFilter == MapDateFilter.PAST,
                onClick = { onSelect(MapDateFilter.PAST) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onBackground))
            Text(text = stringResource(id = R.string.with_past_competitions),
                Modifier.clickable { onSelect(MapDateFilter.PAST) },
                style = MaterialTheme.typography.bodyMedium
                    .copy(MaterialTheme.colorScheme.onBackground))
        }
        Row(modifier = Modifier
            .padding(gradientHorizontalPadding, 2.dp, gradientHorizontalPadding)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(Brush.horizontalGradient(
                0f to Color.Green,
                0.5f to Color.Yellow,
                1f to Color.Red
            )), horizontalArrangement = Arrangement.SpaceBetween){}
        Row(Modifier
            .fillMaxWidth()
            .padding(datesHorizontalPadding, 4.dp, datesHorizontalPadding), horizontalArrangement = Arrangement.SpaceBetween) {
            DateText(text = stringResource(id = R.string.year_ago), TextAlign.Start)
            DateText(text = stringResource(id = R.string.now))
        }
    }

}

@Composable
fun DateText(text: String,
             align: TextAlign = TextAlign.Center) {
    Text(text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            textAlign = align,
            lineHeight = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        ))
}

@Composable
fun SettingsTitle(text: String) {
    Text(text = text,
        Modifier.padding(10.dp, 10.dp, 10.dp, 0.dp),
        style = MaterialTheme.typography.titleMedium
            .copy(MaterialTheme.colorScheme.onBackground))
}

@Composable
fun SettingsContainer(content: @Composable () -> Unit) {
    Column(Modifier
        .fillMaxWidth()) {
        content()
        Spacer(Modifier.height(7.dp))
        Divider(Modifier.padding(horizontal = 4.dp))
    }
}