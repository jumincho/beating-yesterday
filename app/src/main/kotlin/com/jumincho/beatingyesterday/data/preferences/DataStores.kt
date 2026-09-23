package com.jumincho.beatingyesterday.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Holds the user profile. */
val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

/** Holds the state of a running or paused focus session. */
val Context.focusTimerDataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_timer")
