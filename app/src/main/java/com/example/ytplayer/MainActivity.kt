package com.example.ytplayer

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val videos = mutableListOf<Video>()
    private lateinit var adapter: VideoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchInput = findViewById<EditText>(R.id.searchInput)
        val searchButton = findViewById<Button>(R.id.searchButton)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        adapter = VideoAdapter(videos) { video ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("videoId", video.id)
            intent.putExtra("title", video.title)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                searchVideos(query)
            }
        }
    }

    private fun searchVideos(query: String) {
        Toast.makeText(this, getString(R.string.searching), Toast.LENGTH_SHORT).show()
        thread {
            try {
                val results = PipedApi.search(query)
                runOnUiThread {
                    videos.clear()
                    videos.addAll(results)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.search_error, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
