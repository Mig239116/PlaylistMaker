package com.example.playlistmaker

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SearchActivity: AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private val baseUrl="https://itunes.apple.com/"
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val trackService = retrofit.create(TrackApi::class.java)
    private val tracks = ArrayList<Track>()
    private val historyTracks = ArrayList<Track>()
    private lateinit var searchAdapter : TrackAdapter
    private lateinit var historyAdapter : TrackAdapter

    private lateinit var searchHistory: SearchHistory

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyLayout: View
    private lateinit var clearHistoryButton: MaterialButton
    private lateinit var placeholderNothingFound: View
    private lateinit var placeholderNoConnection: View
    private lateinit var updateButton: MaterialButton

    private var lastFailedQuery = LAST_FAILED_QUERY

    private var searchText = SEARCH_TEXT_DEF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val sharedPreferences = getSharedPreferences(PLAYLIST_MAKER_PREFERENCES, MODE_PRIVATE)
        searchHistory = SearchHistory(sharedPreferences)

        val backButton = findViewById<ImageView>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }

        searchEditText = findViewById(R.id.search_edit_text)
        val clearButton = findViewById<ImageView>(R.id.clear_button)
        updateButton = findViewById<MaterialButton>(R.id.update_button)
        placeholderNothingFound = findViewById<LinearLayout>(R.id.placeholder_found_nothing)
        placeholderNoConnection = findViewById<LinearLayout>(R.id.placeholder_no_connection)
        recyclerView = findViewById(R.id.recycler_view)
        historyLayout = findViewById(R.id.history_layout)
        historyRecyclerView = findViewById(R.id.history_recycler_view)
        clearHistoryButton = findViewById(R.id.clear_history_button)

        searchAdapter = TrackAdapter(tracks) { track ->
            searchHistory.add(track)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = searchAdapter

        historyAdapter = TrackAdapter(historyTracks) { track ->
            searchHistory.add(track)
            refreshHistoryList()
        }
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = historyAdapter

        searchEditText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
            showHistoryIfPossible(hasFocus)
        }

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (searchText.isNotEmpty()) {
                    performSearch(searchText)
                }
                true
            } else {
                false
            }
        }

        clearHistoryButton.setOnClickListener {
            searchHistory.clear()
            historyTracks.clear()
            historyAdapter.notifyDataSetChanged()
            showState(State.CONTENT)
        }

        updateButton.setOnClickListener {
            if (lastFailedQuery.isNotEmpty()) {
                performSearch(lastFailedQuery)
            }
        }

        clearButton.setOnClickListener {
            searchEditText.setText("")
            tracks.clear()
            searchAdapter.notifyDataSetChanged()
            showState(State.CONTENT)
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(searchEditText.windowToken, 0)
            searchEditText.clearFocus()
            showHistoryIfPossible(false)
        }

        val searchTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchText = s?.toString() ?: SEARCH_TEXT_DEF
                clearButton.visibility = clearButtonVisibility(s)
                showHistoryIfPossible(searchEditText.hasFocus())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
        searchEditText.addTextChangedListener(searchTextWatcher)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(SEARCH_TEXT_KEY, searchText)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        searchText = savedInstanceState.getString(SEARCH_TEXT_KEY, SEARCH_TEXT_DEF)
        searchEditText.setText(searchText)
    }

    private fun showHistoryIfPossible(hasFocus : Boolean) {
        if (hasFocus && searchEditText.text.isEmpty()) {
            refreshHistoryList()
            if (historyTracks.isNotEmpty()) {
                showState(State.HISTORY)
            } else {
                showState(State.CONTENT)
            }
        } else if (searchEditText.text.isEmpty()) {
            showState(State.CONTENT)
        }
    }

    private fun refreshHistoryList() {
        historyTracks.clear()
        historyTracks.addAll(searchHistory.read())
        historyAdapter.notifyDataSetChanged()
    }

    private fun clearButtonVisibility(s: CharSequence?): Int {
        return if (s.isNullOrEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun performSearch(query : String) {
        trackService.search(query).enqueue(object : Callback<TracksResponse> {
            override fun onResponse(call : Call<TracksResponse>, response : Response<TracksResponse>) {
                if (response.code() == 200) {
                    tracks.clear()
                    val results = response.body()?.results
                    if (!results.isNullOrEmpty()) {
                        tracks.addAll(results)
                        searchAdapter.notifyDataSetChanged()
                        showState(State.CONTENT)
                    } else {
                        showState(State.EMPTY)
                    }
                } else {
                    lastFailedQuery = query
                    showState(State.ERROR)
                }
            }

            override fun onFailure(call: Call<TracksResponse>, t: Throwable) {
                lastFailedQuery = query
                showState(State.ERROR)
            }
        })
    }

    private fun showState(state : State) {
        when (state) {
            State.CONTENT -> {
                recyclerView.visibility = if (tracks.isNotEmpty()) View.VISIBLE else View.GONE
                historyLayout.visibility = View.GONE
                placeholderNothingFound.visibility = View.GONE
                placeholderNoConnection.visibility = View.GONE
            }
            State.HISTORY -> {
                recyclerView.visibility = View.GONE
                historyLayout.visibility = View.VISIBLE
                placeholderNothingFound.visibility = View.GONE
                placeholderNoConnection.visibility = View.GONE
            }
            State.EMPTY -> {
                recyclerView.visibility = View.GONE
                historyLayout.visibility = View.GONE
                placeholderNothingFound.visibility = View.VISIBLE
                placeholderNoConnection.visibility = View.GONE
            }
            State.ERROR -> {
                recyclerView.visibility = View.GONE
                historyLayout.visibility = View.GONE
                placeholderNothingFound.visibility = View.GONE
                placeholderNoConnection.visibility = View.VISIBLE
            }
        }
    }

    enum class State { CONTENT, HISTORY, EMPTY, ERROR }
    companion object {
        const val PLAYLIST_MAKER_PREFERENCES = "playlist_maker_preferences"
        private const val SEARCH_TEXT_KEY = "SEARCH_TEXT"
        private const val SEARCH_TEXT_DEF = ""
        private const val LAST_FAILED_QUERY = ""
    }
}
