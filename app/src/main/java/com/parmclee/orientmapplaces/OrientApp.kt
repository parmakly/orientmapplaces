package com.parmclee.orientmapplaces

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.realm.Realm
import io.realm.RealmConfiguration
import ru.orientmapplaces.BuildConfig


class OrientApp: Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        Realm.init(this)
        Realm.setDefaultConfiguration(RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .build())
    }
}