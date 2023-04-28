package com.parmclee.orientmapplaces.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ru.orientmapplaces.R
import com.parmclee.orientmapplaces.data.MapInfo


@Composable
fun SearchView(modifier: Modifier,
               focusRequester: FocusRequester,
               searchString: String,
               onSearchChanged: (String) -> Unit) {
    Surface(modifier
        .fillMaxWidth()
        .padding(12.dp),
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(56.dp)) {
        TextField(value = searchString,
            onValueChange = { onSearchChanged(it) },
            Modifier.height(48.dp).focusRequester(focusRequester),
            leadingIcon = {
                Icon(painter = painterResource(id = R.drawable.search_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground)
            },
            colors = TextFieldDefaults.textFieldColors(
                cursorColor = MaterialTheme.colorScheme.primary,
                backgroundColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
    }
}

@Composable
fun SearchItems(maps: List<MapInfo>,
                onClick: (MapInfo) -> Unit) {
    if (maps.isNotEmpty()) {
        Surface(Modifier
            .fillMaxWidth()
            .padding(16.dp, 0.dp, 16.dp),
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(10.dp)) {
            Column(Modifier.fillMaxWidth()) {
                val list = if (maps.size > 10) maps.subList(0, 9) else maps
                list.forEachIndexed { index, mapInfo ->
                    SearchItem(text = mapInfo.name) {
                        onClick(mapInfo)
                    }
                    if (index != list.size - 1) {
                        Divider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchItem(text: String,
               onClick: () -> Unit) {
    Row(Modifier
        .fillMaxWidth()
        .clickable {
            onClick()
        }
        .padding(16.dp, 8.dp, 16.dp, 8.dp)) {
        Icon(painter = painterResource(id = R.drawable.image_24px),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge
            .copy(MaterialTheme.colorScheme.onBackground))
    }
}