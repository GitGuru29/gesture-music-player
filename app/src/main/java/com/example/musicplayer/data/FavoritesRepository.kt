package com.example.musicplayer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Simple favorites repository using SharedPreferences
 */
class FavoritesRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "favorites_prefs",
        Context.MODE_PRIVATE
    )
    
    fun getAllFavoriteIds(): Flow<Set<Long>> = callbackFlow {
        val sendCurrent: () -> Unit = {
            val ids = getFavoriteIdsSync()
            trySend(ids)
            Unit
        }
        
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            sendCurrent()
        }
        
        sendCurrent()
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    
    fun getFavoriteIdsSync(): Set<Long> {
        val stringSet = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }
    
    fun isFavorite(songId: Long): Boolean {
        return songId in getFavoriteIdsSync()
    }
    
    fun addFavorite(songId: Long) {
        val current = getFavoriteIdsSync().toMutableSet()
        current.add(songId)
        saveFavorites(current)
    }
    
    fun removeFavorite(songId: Long) {
        val current = getFavoriteIdsSync().toMutableSet()
        current.remove(songId)
        saveFavorites(current)
    }
    
    fun toggleFavorite(songId: Long) {
        if (isFavorite(songId)) {
            removeFavorite(songId)
        } else {
            addFavorite(songId)
        }
    }
    
    private fun saveFavorites(ids: Set<Long>) {
        prefs.edit()
            .putStringSet(KEY_FAVORITES, ids.map { it.toString() }.toSet())
            .apply()
    }
    
    companion object {
        private const val KEY_FAVORITES = "favorite_song_ids"
    }
}
