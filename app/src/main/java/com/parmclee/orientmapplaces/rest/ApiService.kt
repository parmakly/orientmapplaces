package com.parmclee.orientmapplaces.rest

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.parmclee.orientmapplaces.data.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import ru.orientmapplaces.BuildConfig
import java.util.concurrent.TimeUnit


interface ApiService {
    companion object {

        const val BASE_URL = "https://orientmapplaces.ru/"
        private const val API = "api/"

        private const val TIMEOUT_SECONDS = 30L

        private var mService: ApiService? = null

        fun getInstance(): ApiService {
            if (mService == null) {
                mService = createService()
            }
            return mService!!
        }

        private fun createService(): ApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL + API)
                .client(OkHttpClient.Builder()
                    .apply {
                        addInterceptor(createAuthInterceptor())
                        if (BuildConfig.DEBUG) {
                            addInterceptor(HttpLoggingInterceptor().apply {
                                setLevel(HttpLoggingInterceptor.Level.BODY)
                            })
                        }
                    }
                    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .build())
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .create()))
                .build().create(ApiService::class.java)
        }

        private fun createAuthInterceptor(): Interceptor {
            return Interceptor.invoke { chain ->
                val newUrl = chain
                    .request()
                    .url
                    .newBuilder()
                    .build()

                val builder = chain
                    .request()
                    .newBuilder()
                Preferences.getLogin()?.let {
                    builder.addHeader(Preferences.LOGIN, it)
                }
                Preferences.getPassword()?.let {
                    builder.addHeader(Preferences.PASSWORD, it)
                }
                val request = builder
                    .url(newUrl)
                    .build()
                chain.proceed(request)
            }
        }

        const val USER = "user"
        const val MAPS = "maps"
        const val MAP = "map"
        const val DELETE_COMPETITION = "deletecompetition.php"
        const val DELETE_MAP = "deletemap.php"
        const val ADD_COMPETITION = "addcompetition.php"
        const val ADD_MAP = "addmap.php"
        const val UPDATE_MAP = "updatemap.php"
        const val UPLOAD_MAP = "uploadmap.php"
    }

    @GET(USER)
    suspend fun getUser(@Header("login") login: String,
                        @Header ("password") password: String): Response<User>

    @GET(MAPS)
    suspend fun getMaps(): Response<List<MapInfo>>

    @GET(MAP)
    suspend fun getMap(@Query("id") id: Int): Response<MapInfo>

    @DELETE(DELETE_COMPETITION)
    suspend fun deleteCompetition(@Query("id") id: Int): Response<BaseResponse>

    @DELETE(DELETE_MAP)
    suspend fun deleteMap(@Query("id") id: Int): Response<BaseResponse>

    @POST(ADD_COMPETITION)
    suspend fun addCompetition(@Body body: RequestBody): Response<Competition>

    @POST(ADD_MAP)
    suspend fun addMap(@Body body: RequestBody): Response<MapInfo>

    @POST(UPDATE_MAP)
    suspend fun updateMap(@Body body: RequestBody): Response<MapInfo>

    @POST(UPLOAD_MAP)
    suspend fun uploadMap(@Query("id") mapId: Int,
                          @Body body: RequestBody): Response<MapInfo>
}