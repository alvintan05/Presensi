package com.pnj.presensi.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.pnj.presensi.entity.pegawai.Pegawai
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PresensiDataStore(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "DataStore")
        private val loginStatusKey = booleanPreferencesKey("LoginStatus")
        private val idPegawaiKey = intPreferencesKey("IdPegawai")
        private val nipKey = stringPreferencesKey("NIP")
        private val namaKey = stringPreferencesKey("Nama")
    }

    suspend fun saveSessionAndData(pegawai: Pegawai) {
        context.dataStore.edit { settings ->
            settings[loginStatusKey] = true
            settings[idPegawaiKey] = pegawai.idPegawai
            settings[nipKey] = pegawai.nip
            settings[namaKey] = pegawai.nama
        }
    }

    suspend fun getName(): String {
        val namaFlow: Flow<String> = context.dataStore.data.map { preferences ->
            preferences[namaKey] ?: ""
        }
        return namaFlow.first()
    }

    suspend fun isUserLoggedIn(): Boolean {
        val statusFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
            preferences[loginStatusKey] ?: false
        }
        return statusFlow.first()
    }

    suspend fun deleteLoginSession() {
        context.dataStore.edit {
            it.clear()
        }
    }
}