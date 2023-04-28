package com.parmclee.orientmapplaces.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.orientmapplaces.R


@Composable
fun BoxScope.SaveAreaView(nameText: String,
                          onNameChanged: (String) -> Unit,
                          onSave: () -> Unit,
                          onClose: () -> Unit,
                          canSave: Boolean,
                          needExplanation: Boolean) {
    Column(Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
        .align(BottomCenter)) {
        if (needExplanation) {
            Text(text = stringResource(id = R.string.area_point_explain),
                Modifier.fillMaxWidth().padding(12.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(30.dp))
                    .padding(10.dp),
                color = Color.Gray,
                textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth()) {
            BaseFloatingButton(label = stringResource(id = R.string.save),
                onClick = onSave,
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 4.dp),
                enabled = canSave)
            BaseFloatingButton(label = stringResource(id = R.string.cancel),
                onClick = onClose,
                Modifier
                    .weight(1f)
                    .padding(start = 4.dp, end = 16.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        FloatingEditText(nameText, onNameChanged,
            hintText = stringResource(id = R.string.map_name))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingEditText(nameText: String,
                     onNameChanged: (String) -> Unit,
                     hintText: String) {
    Surface(Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp),
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(6.dp)) {
        OutlinedTextField(value = nameText,
            onValueChange = onNameChanged,
            Modifier
                .fillMaxWidth()
                .padding(10.dp, 6.dp, 10.dp, 10.dp),
            label = {
                Text(text = hintText)
            }, keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences))
    }
}

@Composable
fun BaseFloatingButton(label: String,
                       onClick: () -> Unit,
                       modifier: Modifier = Modifier,
                       enabled: Boolean = true) {
    Button(onClick = onClick,
        shape = RoundedCornerShape(40.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(),
        modifier = modifier) {
        Text(text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(CenterVertically))
    }
}