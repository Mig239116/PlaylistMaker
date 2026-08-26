package com.example.playlistmaker

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistory(private val sharedPref: SharedPreferences) {
    private val gson = Gson()

    fun read() : ArrayList<Track> {
        val json = sharedPref.getString(SEARCH_HISTORY_KEY, null) ?: return arrayListOf()
        val type = object : TypeToken<ArrayList<Track>>() {}.type
        return gson.fromJson(json, type) ?: arrayListOf()
    }

    fun add(track : Track) {
        val history = read()
        history.removeAll {it.trackId == track.trackId}
        history.add(0, track)
        if (history.size > MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }
        save(history)
    }

    fun clear() {
        sharedPref.edit()
            .remove(SEARCH_HISTORY_KEY)
            .apply()
    }

    private fun save(history : List<Track>) {
        val json = gson.toJson(history)
        sharedPref.edit()
            .putString(SEARCH_HISTORY_KEY, json)
            .apply()
    }

    companion object {
        const val SEARCH_HISTORY_KEY = "search_history_key"
        private const val MAX_HISTORY_SIZE = 10
    }
}