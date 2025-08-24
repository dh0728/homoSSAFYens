package com.example.dive.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File

object AppDataStores {
    @Volatile
    private var INSTANCE: DataStore<Preferences>? = null

    fun preferences(context: Context): DataStore<Preferences> {
        val appContext = context.applicationContext
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: PreferenceDataStoreFactory.create(
                produceFile = { File(appContext.filesDir, "wear_cache.preferences_pb") }
            ).also { INSTANCE = it }
        }
    }
}