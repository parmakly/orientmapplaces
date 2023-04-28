package com.parmclee.orientmapplaces.ui.screen

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import ru.orientmapplaces.R
import com.parmclee.orientmapplaces.ui.BaseImage


@Composable
fun PictureScreen(picture: String,
                  onClose: () -> Unit) {
    BackHandler {
        onClose()
    }
    val isFullscreen = rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    fun download() {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val name = "${picture.substringAfterLast("/")}.jpg"
        val request = DownloadManager
            .Request(Uri.parse(picture))
            .setTitle(name)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setMimeType("image/jpeg")
            .setAllowedOverRoaming(true)
        downloadManager.enqueue(request)
    }
    Box(Modifier.fillMaxSize()) {
        BaseImage(url = picture,
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
            isRound = false,
            scaleType = ContentScale.Fit,
            placeholder = R.drawable.wallpaper,
            zoomable = true,
            onZoomableClick = { isFullscreen.value = !isFullscreen.value })
        ActionButton(iconRes = R.drawable.arrow_back_48px, onClick = {
            onClose()
        })
        ActionButton(iconRes = R.drawable.download_24px, onClick = {
            download()
        }, modifier = Modifier.align(Alignment.TopEnd))
    }
}