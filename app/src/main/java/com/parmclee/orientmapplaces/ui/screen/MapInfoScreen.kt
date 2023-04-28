package com.parmclee.orientmapplaces.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.parmclee.orientmapplaces.data.MapInfo
import ru.orientmapplaces.R
import com.parmclee.orientmapplaces.data.Competition
import com.parmclee.orientmapplaces.data.Preferences
import com.parmclee.orientmapplaces.rest.ApiService
import com.parmclee.orientmapplaces.ui.ImagePicker
import com.parmclee.orientmapplaces.ui.openNavigationApp
import com.parmclee.orientmapplaces.ui.sendTextThroughMessenger
import com.parmclee.orientmapplaces.ui.userFriendlySdf
import java.util.*


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MapInfoScreen(mapInfo: MapInfo?,
                  bottomSheetScaffoldState: BottomSheetScaffoldState,
                  onCompetitionDeleteClicked: (Competition) -> Unit,
                  onMapDeleteClicked: () -> Unit,
                  onMapEditClicked: () -> Unit,
                  onNewCompetition: (Date, String) -> Unit,
                  onUploadMap: (String) -> Unit,
                  onImageClicked: (String) -> Unit,
                  content: @Composable (it: PaddingValues) -> Unit) {
    val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
    val launchPickImage = remember { mutableStateOf(false) }
    val context = LocalContext.current
    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetContent =  {
            Column(Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(top = 8.dp)) {
                Box(modifier = Modifier
                    .align(CenterHorizontally)
                    .padding(bottom = 8.dp)
                    .size(30.dp, 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.LightGray))
                Row(Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                    verticalAlignment = CenterVertically) {
                    Text(text = mapInfo?.name ?: "",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge
                            .copy(MaterialTheme.colorScheme.onBackground))
                    mapInfo?.image?.let {
                        Icon(painter = painterResource(id = R.drawable.image_search_24px),
                            contentDescription = null,
                            Modifier
                                .clip(CircleShape)
                                .clickable { onImageClicked(it) }
                                .padding(8.dp),
                            tint = Color.Blue)
                    }
                    Icon(painter = painterResource(id = R.drawable.share_24px),
                        contentDescription = null,
                        Modifier
                            .clip(CircleShape)
                            .clickable {
                                mapInfo?.let {
                                    context.sendTextThroughMessenger(ApiService.BASE_URL + "?id=${mapInfo.id}")
                                }
                            }
                            .padding(8.dp),
                        tint = Color.Blue)
                    Icon(painter = painterResource(id = R.drawable.near_me_24px),
                        contentDescription = null,
                        Modifier
                            .clip(CircleShape)
                            .clickable {
                                mapInfo?.getBounds()?.center()?.let {
                                    openNavigationApp(context, it)
                                }
                            }
                            .padding(8.dp),
                        tint = Color.Blue)
                }
                if (Preferences.getUserId() == mapInfo?.user?.id) {
                    Row(Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())) {
                        OutlineButton(
                            modifier = Modifier
                                .padding(16.dp, 0.dp, 3.dp, 0.dp)
                                .alpha(0.7f),
                            drawableRes = R.drawable.add_photo_alternate_24px,
                            stringResource(id = R.string.add_image),
                        ) {
                            launchPickImage.value = true
                        }
                        OutlineButton(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .alpha(0.7f),
                            drawableRes = R.drawable.build_24px,
                            stringResource(id = R.string.edit),
                        ) {
                            onMapEditClicked()
                        }
                        OutlineButton(
                            modifier = Modifier
                                .padding(3.dp, 0.dp, 16.dp, 0.dp)
                                .alpha(0.7f),
                            drawableRes = R.drawable.delete_48px,
                            stringResource(id = R.string.delete_map),
                        ) {
                            onMapDeleteClicked()
                        }
                    }
                } else {
                    Divider(Modifier.padding(top = 16.dp))
                }
                Column(Modifier
                .verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(10.dp))
                    if (!mapInfo?.getFutureCompetitions().isNullOrEmpty()) {
                        Text(text = stringResource(id = R.string.future_competitions),
                            Modifier
                                .padding(vertical = 10.dp)
                                .align(CenterHorizontally),
                            style = MaterialTheme.typography.bodyMedium
                                .copy(MaterialTheme.colorScheme.onBackground))
                        mapInfo?.getFutureCompetitions()?.forEach { comp ->
                            CompetitionView(comp, onCompetitionDeleteClicked)
                        }
                    }
                    if (!mapInfo?.getPreviousCompetitions().isNullOrEmpty()) {
                        Text(text = stringResource(id = R.string.previous_competitions),
                            Modifier
                                .padding(vertical = 10.dp)
                                .align(CenterHorizontally),
                            style = MaterialTheme.typography.bodyMedium
                                .copy(MaterialTheme.colorScheme.onBackground))
                        mapInfo?.getPreviousCompetitions()?.forEach { comp ->
                            CompetitionView(comp, onCompetitionDeleteClicked)
                        }
                    }
                    if (mapInfo?.competitions.isNullOrEmpty()) {
                        CompetitionPlaceholder()
                    }
                    if (Preferences.getLogin() != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlineButton(Modifier.align(CenterHorizontally),
                            R.drawable.alarm_add_48px,
                            stringResource(id = R.string.add_competiton)) {
                            setShowDialog(true)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetBackgroundColor = MaterialTheme.colorScheme.background,
        sheetElevation = 5.dp,
        sheetPeekHeight = 0.dp
    ) {
        content(it)
    }
    AddCompetitionDialog(showDialog, setShowDialog, onNewCompetition)
    ImagePicker(context, successListener = onUploadMap, launch = launchPickImage)
}

@Composable
fun CompetitionView(comp: Competition,
                    onDeleteClicked: (Competition) -> Unit) {
    Card(Modifier
        .fillMaxWidth()
        .padding(16.dp, 4.dp, 16.dp),
        shape = RoundedCornerShape(6.dp),
        elevation = 2.dp) {
        Column(Modifier
            .fillMaxWidth()
            .padding(10.dp, 5.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(text = userFriendlySdf.format(comp.date),
                    Modifier
                        .padding(end = 12.dp)
                        .weight(1f),
                    style = MaterialTheme.typography.labelMedium.copy(Color.Gray))
                Text(text = comp.user?.getShortName() ?: "",
                    Modifier,
                    style = MaterialTheme.typography.labelSmall.copy(MaterialTheme.colorScheme.primary))
            }
            Row(Modifier.fillMaxWidth()) {
                Text(text = comp.name,
                    Modifier
                        .weight(1f)
                        .padding(top = 5.dp),
                    style = MaterialTheme.typography.labelMedium
                        .copy(MaterialTheme.colorScheme.onBackground))
                if (Preferences.getUserId() == comp.user?.id) {
                    Icon(painter = painterResource(id = R.drawable.delete_filled_48px), contentDescription = null,
                        Modifier
                            .clip(CircleShape)
                            .clickable { onDeleteClicked(comp) }
                            .padding(4.dp), tint = Color.Red)
                }
            }
        }
    }

}

@Composable
fun CompetitionPlaceholder() {
    Column(Modifier
        .fillMaxWidth()
        .padding(10.dp, 40.dp, 10.dp, 36.dp),
        horizontalAlignment = CenterHorizontally) {
        Icon(painter = painterResource(id = R.drawable.timer_off_48px), contentDescription = null,
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(id = R.string.no_comp_yet),
            style = MaterialTheme.typography.titleMedium
                .copy(MaterialTheme.colorScheme.onBackground))
    }
}