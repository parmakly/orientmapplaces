package com.parmclee.orientmapplaces.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.Annotation
import com.mapbox.maps.plugin.annotation.generated.OnCircleAnnotationDragListener
import ru.orientmapplaces.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun Menu(isExpanded: MutableState<Boolean>,
         viewModel: MainViewModel,
         onAddShape: () -> Unit,
         onSettings: () -> Unit,
         onLogIn: () -> Unit,
         onSearch: () -> Unit) {
    DropdownMenu(expanded = isExpanded.value,
        onDismissRequest = { isExpanded.value = false },
        Modifier.background(MaterialTheme.colorScheme.background)) {
        if (viewModel.isAuthorized()) {
            MenuItem(iconRes = R.drawable.format_shapes_24px,
                text = stringResource(id = R.string.draw_area),
                isExpanded,
                onAddShape)
            Divider(Modifier.padding(horizontal = 16.dp))
        }
        MenuItem(
            iconRes = if (viewModel.isSettingsChanged()) R.drawable.settings_suggest_24px else R.drawable.settings_24px,
            text = stringResource(id = R.string.settings), isExpanded, onSettings
        )
        Divider(Modifier.padding(horizontal = 16.dp))
        MenuItem(
            iconRes = R.drawable.search_24px,
            text = stringResource(id = R.string.search), isExpanded, onSearch
        )
        Divider(Modifier.padding(horizontal = 16.dp))
        MenuItem(
            iconRes = if (viewModel.isAuthorized()) R.drawable.logout_40px else R.drawable.login_40px,
            text = stringResource(id = if (viewModel.isAuthorized()) R.string.logout else R.string.log_in), isExpanded, onLogIn
        )
    }
}

@Composable
fun MenuItem(iconRes: Int,
             text: String,
             isExpanded: MutableState<Boolean>,
             onClick: () -> Unit) {
    Row(Modifier
        .fillMaxWidth()
        .clickable {
            isExpanded.value = false
            onClick()
        }
        .padding(16.dp, 8.dp, 16.dp, 8.dp)) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null,
            tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge
            .copy(MaterialTheme.colorScheme.onBackground))
    }
}

fun Any.toRequestBody(): RequestBody {
    val json = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()
        .toJson(this)
    return json.toRequestBody("application/json".toMediaType())
}

fun nextSaturdayDate(): Date {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
    return calendar.time
}

val userFriendlySdf by lazy { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
val userFriendlyTimeSdf by lazy { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

fun List<Point>.circuitPoints(): List<Point> {
    if (this.size < 4) return this
    return this.toMutableList() + this.first()
}

open class SimpleCircleAnnotationDragListener: OnCircleAnnotationDragListener {

    override fun onAnnotationDrag(annotation: Annotation<*>) {
    }

    override fun onAnnotationDragFinished(annotation: Annotation<*>) {
    }

    override fun onAnnotationDragStarted(annotation: Annotation<*>) {
    }
}

@Composable
fun ImagePicker(context: Context,
                successListener: (String) -> Unit,
                launch: MutableState<Boolean>) {
    fun copyImageToFile(uri: Uri) {
        val imgFile = getImageFile(context)
        copyUriToFile(context, uri, imgFile) {
            successListener(imgFile.path)
        }
    }
    val pickImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                copyImageToFile(uri)
            }
        }
    LaunchedEffect(launch.value) {
        if (launch.value) {
            pickImageLauncher.launch("image/*")
            launch.value = false
        }
    }
}

fun getImageFile(context: Context?): File {
    val dir = context?.cacheDir
    if (dir != null && !dir.exists()) dir.mkdirs()
    val sdf = SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.CANADA)
    val image = File(dir, "tmpImage_" + sdf.format(Date()) + ".jpg")
    if (!image.exists()) {
        try {
            image.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    return image
}

fun copyUriToFile(context: Context?, uri: Uri, file: File?, callback: () -> Unit) {
    MainScope().launch(Dispatchers.IO) {
        context?.contentResolver?.openInputStream(uri)?.use {
            copyInputStreamToFile(it, file)
            MainScope().launch(Dispatchers.Main) {
                callback()
            }
        }
    }
}

private fun copyInputStreamToFile(`in`: InputStream, file: File?) {
    var out: OutputStream? = null
    try {
        out = FileOutputStream(file)
        val buf = ByteArray(1024)
        var len: Int
        while (`in`.read(buf).also { len = it } > 0) {
            out.write(buf, 0, len)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            out?.close()
            `in`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

fun openNavigationApp(context: Context, point: Point) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${point.latitude()},${point.longitude()}"))
    context.startActivity(intent)
}

fun Context.sendTextThroughMessenger(text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND)
    sendIntent.type = "text/plain"
    sendIntent.putExtra(Intent.EXTRA_TEXT, text)
    try {
        startActivity(sendIntent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, getString(R.string.app_not_found), Toast.LENGTH_SHORT).show()
    }
}
