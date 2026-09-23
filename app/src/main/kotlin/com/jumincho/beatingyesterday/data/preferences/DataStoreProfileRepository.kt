package com.jumincho.beatingyesterday.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jumincho.beatingyesterday.core.data.ProfileRepository
import com.jumincho.beatingyesterday.core.model.ActivityLevel
import com.jumincho.beatingyesterday.core.model.Sex
import com.jumincho.beatingyesterday.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** [ProfileRepository] backed by a Preferences DataStore. */
class DataStoreProfileRepository(private val dataStore: DataStore<Preferences>) : ProfileRepository {

    override val profile: Flow<UserProfile?> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toUserProfile() }

    override suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { it.putUserProfile(profile) }
    }
}

private val NAME = stringPreferencesKey("name")
private val SEX = stringPreferencesKey("sex")
private val BIRTH_YEAR = intPreferencesKey("birth_year")
private val HEIGHT_CM = doublePreferencesKey("height_cm")
private val WEIGHT_KG = doublePreferencesKey("weight_kg")
private val ACTIVITY_LEVEL = stringPreferencesKey("activity_level")

/** The stored profile, or `null` if none is stored or it is incomplete. */
internal fun Preferences.toUserProfile(): UserProfile? {
    val sex = Sex.entries.firstOrNull { it.name == this[SEX] }
    val activityLevel = ActivityLevel.entries.firstOrNull { it.name == this[ACTIVITY_LEVEL] }
    return UserProfile(
        name = this[NAME] ?: return null,
        sex = sex ?: return null,
        birthYear = this[BIRTH_YEAR] ?: return null,
        heightCm = this[HEIGHT_CM] ?: return null,
        weightKg = this[WEIGHT_KG] ?: return null,
        activityLevel = activityLevel ?: return null,
    )
}

/** Writes every field of [profile]. */
internal fun MutablePreferences.putUserProfile(profile: UserProfile) {
    this[NAME] = profile.name
    this[SEX] = profile.sex.name
    this[BIRTH_YEAR] = profile.birthYear
    this[HEIGHT_CM] = profile.heightCm
    this[WEIGHT_KG] = profile.weightKg
    this[ACTIVITY_LEVEL] = profile.activityLevel.name
}
