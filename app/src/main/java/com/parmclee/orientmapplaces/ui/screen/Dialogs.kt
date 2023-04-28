package com.parmclee.orientmapplaces.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.orientmapplaces.R
import com.parmclee.orientmapplaces.ui.nextSaturdayDate
import com.parmclee.orientmapplaces.ui.userFriendlySdf
import kotlinx.coroutines.android.awaitFrame
import java.util.*


@Composable
fun SimpleAlertDialog(
    showDialog: Boolean,
    setShowDialog: (Boolean) -> Unit,
    dialogTitle: String,
    dialogMessage: String,
    confirmText: String = stringResource(id = R.string.ok),
    cancelText: String = stringResource(id = R.string.cancel),
    negativeClickListener: () -> Unit = {},
    positiveClickListener: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { setShowDialog(false) },
            title = {
                if (dialogTitle.isNotEmpty()) {
                    Text(dialogTitle, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        setShowDialog(false)
                        positiveClickListener()
                    }
                ) {
                    Text(confirmText, style = MaterialTheme.typography.labelMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    setShowDialog(false)
                    negativeClickListener()
                }) {
                    Text(cancelText, style = MaterialTheme.typography.labelMedium)
                }
            },
            shape = RoundedCornerShape(4.dp),
            containerColor = MaterialTheme.colorScheme.background,
            text = {
                Text(dialogMessage, style = MaterialTheme.typography.labelLarge
                    .copy(MaterialTheme.colorScheme.onBackground))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginDialog(showDialog: Boolean,
                setShowDialog: (Boolean) -> Unit,
                onSaveClicked: (String, String) -> Unit) {
    val loginText = remember { mutableStateOf("") }
    val passwordText = remember { mutableStateOf("") }
    fun clearValues() {
        loginText.value = ""
        passwordText.value = ""
    }
    if (showDialog) {
        val focusRequester = remember { FocusRequester() }
        val keyboardHelper = LocalSoftwareKeyboardController.current
        Dialog(onDismissRequest = {
            setShowDialog(false)
            clearValues()
        }) {
            Column(Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                .padding(8.dp)) {
                OutlinedTextField(value = loginText.value,
                    onValueChange = { loginText.value = it },
                    Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth()
                        .padding(6.dp),
                    label = {
                        Text(text = stringResource(id = R.string.login), color = Color.Gray)
                    })
                OutlinedTextField(value = passwordText.value,
                    onValueChange = { passwordText.value = it },
                    Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    label = {
                        Text(text = stringResource(id = R.string.password), color = Color.Gray)
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation())
                Text(text = stringResource(id = R.string.log_in),
                    Modifier
                        .align(Alignment.End)
                        .padding(6.dp)
                        .clickable {
                            onSaveClicked(loginText.value, passwordText.value)
                            setShowDialog(false)
                            clearValues()
                        }
                        .padding(8.dp), style = MaterialTheme.typography.bodyLarge)
            }
            LaunchedEffect(Unit) {
                awaitFrame()
                focusRequester.requestFocus()
                keyboardHelper?.show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddCompetitionDialog(showDialog: Boolean,
                         setShowDialog: (Boolean) -> Unit,
                         onSaveClicked: (Date, String) -> Unit) {
    val competitionText = remember { mutableStateOf("") }
    val date = remember { mutableStateOf(nextSaturdayDate()) }
    val context = LocalContext.current
    fun clearValues() {
        competitionText.value = ""
        date.value = nextSaturdayDate()
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardHelper = LocalSoftwareKeyboardController.current
    if (showDialog) {
        Dialog(onDismissRequest = {
            setShowDialog(false)
            clearValues()
        }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                .padding(8.dp)) {
                OutlinedTextField(value = competitionText.value,
                    onValueChange = { competitionText.value = it },
                    Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth()
                        .padding(6.dp),
                    label = {
                        Text(text = stringResource(id = R.string.competition_name),
                            color = Color.Gray)
                    }, keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences))
                Row(Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance().apply { time = date.value }
                        DatePickerDialog(context, { view, year, month, dayOfMonth ->
                                val newCalendar = Calendar.getInstance()
                                newCalendar.set(year, month, dayOfMonth)
                                date.value = newCalendar.time
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(id = R.string.date_),
                        Modifier.weight(1f).padding(start = 10.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(MaterialTheme.colorScheme.primary))
                    Text(text = userFriendlySdf.format(date.value),
                        style = MaterialTheme.typography.bodyLarge)
                    Icon(painter = painterResource(id = R.drawable.chevron_right_48px),
                        contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Text(text = stringResource(id = R.string.save),
                    Modifier
                        .align(Alignment.End)
                        .padding(6.dp)
                        .alpha(if (competitionText.value.isEmpty()) 0.5f else 1f)
                        .clickable {
                            if (competitionText.value.isNotEmpty()) {
                                onSaveClicked(date.value, competitionText.value)
                                setShowDialog(false)
                                clearValues()
                            }
                        }
                        .padding(8.dp), style = MaterialTheme.typography.bodyLarge)
            }
            LaunchedEffect(Unit) {
                awaitFrame()
                awaitFrame()
                focusRequester.requestFocus()
                keyboardHelper?.show()
            }
        }
    }
}