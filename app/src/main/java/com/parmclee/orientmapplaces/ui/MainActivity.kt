package com.parmclee.orientmapplaces.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.orientmapplaces.R
import com.parmclee.orientmapplaces.data.Preferences
import com.parmclee.orientmapplaces.ui.screen.MainScreen
import com.parmclee.orientmapplaces.ui.screen.SettingsScreen
import com.parmclee.orientmapplaces.ui.screen.SimpleAlertDialog
import com.parmclee.orientmapplaces.ui.theme.OrientMapPlacesTheme
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Preferences.init(this)
        locationPermissionHelper = LocationPermissionHelper(java.lang.ref.WeakReference(this))
        val appLinkIntent: Intent = intent
        val appLinkData: Uri? = appLinkIntent.data
        viewModel.changeInitialId(appLinkData?.getQueryParameter("id")?.toIntOrNull())
        intent.data = null
        setContent {
            OrientMapPlacesTheme {
                val errorFlow by viewModel.errorFlow.collectAsStateWithLifecycle()
                val (showDialog, setShowDialog) = remember { mutableStateOf(false) }
                val isSettingsShown = remember{ mutableStateOf(false) }
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    MainScreen(locationPermissionHelper, viewModel) {
                        isSettingsShown.value = true
                    }
                    if (isSettingsShown.value) {
                        SettingsScreen(viewModel) {
                            isSettingsShown.value = false
                        }
                    }
                    SimpleAlertDialog(showDialog,
                        setShowDialog,
                        dialogTitle = stringResource(id = R.string.error),
                        dialogMessage = errorFlow?.message ?: "") {
                        viewModel.clearError()
                    }
                    LaunchedEffect(errorFlow) {
                        if (errorFlow != null) {
                            setShowDialog(true)
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("super.onRequestPermissionsResult(requestCode, permissions, grantResults)",
        "androidx.activity.ComponentActivity"))
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
