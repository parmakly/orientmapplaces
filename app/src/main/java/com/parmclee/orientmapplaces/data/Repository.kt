package com.parmclee.orientmapplaces.data

import com.google.gson.Gson
import com.mapbox.geojson.Point
import com.parmclee.orientmapplaces.rest.ApiService
import com.parmclee.orientmapplaces.ui.toRequestBody
import io.realm.Realm
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class Repository {

    private val apiService = ApiService.getInstance()

    private val mCoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace() // not display internal errors
    }

    var errorFlow = MutableStateFlow<BaseResponse?>(null)
    val isLoading = MutableStateFlow(false)

    private fun onError(response: BaseResponse) {
        errorFlow.value = response
    }

    private fun <T> handleResponse(res: suspend () -> Response<T>,
                                   onError: (() -> Unit)? = null,
                                   showError: Boolean = true,
                                   onEmptySuccessResponse: () -> Unit = {},
                                   onSuccess: (T) -> Unit) {
        val scope = MainScope()
        scope.launch(Dispatchers.IO + mCoroutineExceptionHandler) {
            isLoading.value = true
            try {
                val response = res()
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        if (it is BaseResponse && response.code() >= 400) {
                            if (onError == null) onError(it)
                            else onError.invoke()
                        } else {
                            scope.launch { onSuccess(it) }
                        }
                    }
                    if (body == null) {
                        onEmptySuccessResponse()
                    }
                } else {
                    try {
                        if (showError) {
                            onError(Gson().fromJson(response.errorBody()?.string(), BaseResponse::class.java))
                        }
                        onError?.invoke()
                    } catch (e: Exception) {
                        onError?.invoke()
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                onError?.invoke()
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun getUser(login: String, password: String, onSuccess: (String) -> Unit) {
        handleResponse( { apiService.getUser(login, password) }) {
            Preferences.saveUser(it)
            onSuccess("${it.firstName} ${it.lastName}")
        }
    }

    fun getMaps(onMapsReceived: (List<MapInfo>) -> Unit) {
        val saved = Realm.getDefaultInstance().where(MapInfo::class.java).findAll()
        if (saved.isNotEmpty()) {
            onMapsReceived(saved)
        }
        handleResponse( { apiService.getMaps() }) {
            onMapsReceived(it)
            saveMaps(it)
        }
    }

    private fun saveMaps(maps: List<MapInfo>) {
        Realm.getDefaultInstance().executeTransactionAsync {
            it.delete(MapInfo::class.java)
            it.delete(User::class.java)
            it.delete(Competition::class.java)
            it.delete(LatLng::class.java)
            it.copyToRealmOrUpdate(maps)
        }
    }

    private fun saveMap(mapInfo: MapInfo) {
        Realm.getDefaultInstance().executeTransactionAsync {
            it.copyToRealmOrUpdate(mapInfo)
        }
        mapInfo.user?.let {
            Preferences.checkAndSavePremium(it)
        }
    }

    private fun deleteCompetition(id: Int) {
        Realm.getDefaultInstance().executeTransactionAsync {
            it.where(Competition::class.java)
                .equalTo("id", id)
                .findFirst()
                ?.deleteFromRealm()
        }
    }

    private fun deleteMap(id: Int) {
        Realm.getDefaultInstance().executeTransactionAsync {
            it.where(MapInfo::class.java)
                .equalTo("id", id)
                .findFirst()
                ?.deleteFromRealm()
        }
    }

    fun addMap(name: String, points: List<Point>, onAdded: (MapInfo) -> Unit) {
        handleResponse( { apiService.addMap(AddMapData.create(null, name, points).toRequestBody()) }) {
            onAdded(it)
            saveMap(it)
        }
    }

    fun updateMap(id: Int, name: String, points: List<Point>, onAdded: (MapInfo) -> Unit) {
        handleResponse( { apiService.updateMap(AddMapData.create(id, name, points).toRequestBody()) }) {
            onAdded(it)
            saveMap(it)
        }
    }

    fun addCompetition(mapId: Int, date: Date, name: String, onAdded: (Competition) -> Unit) {
        val data = AddCompetitionData(name, mapId, SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).format(date) + " 11:00:00")
        handleResponse( { apiService.addCompetition(data.toRequestBody()) }) {
            onAdded(it)
        }
    }

    fun deleteMap(id: Int, onSuccess: () -> Unit) {
        handleResponse( { apiService.deleteMap(id) }) {
            onSuccess()
            deleteMap(id)
        }
    }

    fun deleteCompetition(id: Int, onSuccess: () -> Unit) {
        handleResponse( { apiService.deleteCompetition(id) }) {
            onSuccess()
            deleteCompetition(id)
        }
    }

    fun getMap(id: Int, onSuccess: (MapInfo) -> Unit) {
        handleResponse( { apiService.getMap(id) }) {
            onSuccess(it)
            saveMap(it)
        }
    }

    fun uploadMap(mapId: Int, filePath: String, onSuccess: (MapInfo) -> Unit) {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "filename.jpg",
                File(filePath).asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()
        handleResponse( { apiService.uploadMap(mapId, body) }) {
            onSuccess(it)
            saveMap(it)
        }
    }
}