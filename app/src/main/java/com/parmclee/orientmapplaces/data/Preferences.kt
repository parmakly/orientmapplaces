package com.parmclee.orientmapplaces.data

import android.app.Activity
import android.content.Context
import java.lang.ref.WeakReference
import java.util.*


object Preferences {

    private const val SHARED_PREFERENCES_NAME = "SHARED_PREFERENCES_PRIVATE"
    const val LOGIN = "LOGIN"
    const val PASSWORD = "PASSWORD"
    private const val NAME = "NAME"
    private const val USER_ID = "USER_ID"
    private const val FILTER = "FILTER"
    private const val MAP_REQUEST_TIME = "MAP_REQUEST_TIME"
    private const val CENTERING = "CENTERING"
    private const val PREMIUM = "PREMIUM"

    private var contextReference: WeakReference<Context>? = null
    fun init(activity: Activity) {
        contextReference = WeakReference(activity.applicationContext)
    }

    fun saveUser(user: User) {
        contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(LOGIN, user.login)
            ?.putString(PASSWORD, user.password)
            ?.putInt(USER_ID, user.id)
            ?.putString(NAME, "${user.firstName} ${user.lastName}")
            ?.putBoolean(PREMIUM, user.premium == 1)
            ?.apply()
    }

    fun getLogin(): String? {
        return contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.getString(LOGIN, null)
    }

    fun getPassword(): String? {
        return contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.getString(PASSWORD, null)
    }

    fun getName(): String? {
        return contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.getString(NAME, null)
    }

    fun getUserId(): Int? {
        return contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.getInt(USER_ID, -1)
    }

    fun saveFilter(filter: MapDateFilter) {
        contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(FILTER, filter.name)
            ?.apply()
    }

    fun getFilter(): MapDateFilter? {
        return contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.getString(FILTER, null)?.let {
                MapDateFilter.valueOf(it)
            }
    }

    fun saveMapSync() {
        contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putLong(MAP_REQUEST_TIME, System.currentTimeMillis())
            ?.apply()
    }

    fun checkAndSavePremium(user: User) {
        if (user.id == getUserId()) {
            contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean(PREMIUM, user.premium == 1)
                ?.apply()
        }
    }

    fun isPremium(): Boolean {
        return contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.getBoolean(PREMIUM, false) == true
    }

    fun getMapSync(): Date? {
        return contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.getLong(MAP_REQUEST_TIME, 0)?.let {
                if (it != 0L) {
                    Date(it)
                } else null
            }
    }

    fun saveCentering(center: Boolean) {
        contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(CENTERING, center)
            ?.apply()
    }

    fun getCentering(): Boolean {
        return contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.getBoolean(CENTERING, false) ?: false
    }

    fun logout() {
        contextReference?.get()?.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(LOGIN)
            ?.remove(PASSWORD)
            ?.remove(NAME)
            ?.remove(USER_ID)
            ?.remove(FILTER)
            ?.remove(PREMIUM)
            ?.remove(CENTERING)
            ?.apply()
    }

}