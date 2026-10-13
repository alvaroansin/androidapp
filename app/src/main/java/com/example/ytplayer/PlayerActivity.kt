package com.example.ytplayer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import kotlin.concurrent.thread

class PlayerActivity : AppCompatActivity() {

    private var mediaController: MediaController? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)

        val videoId = intent.getStringExtra("videoId") ?: return
        val title = intent.getStringExtra("title") ?: ""

        // Conectar con el servicio (el audio sigue con la pantalla apagada)
        val sessionToken = SessionToken(this, android.content.ComponentName(this, PlayerService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                playerView.player = mediaController
                loadAndPlay(videoId, title)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.player_error, e.message), Toast.LENGTH_LONG).show()
            }
        }, androidx.media3.common.util.UnstableApi::class.java.let { Runnable { it } }.let { mainExecutor }) { }
    }

    private fun loadAndPlay(videoId: String, title: String) {
        thread {
            try {
                val streamUrl = PipedApi.getStreamUrl(videoId)
                runOnUiThread {
                    val mediaItem = MediaItem.Builder()
                        .setUri(streamUrl)
                        .setMediaId(videoId)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(title)
                                .build()
                        )
                        .build()
                    mediaController?.let {
                        it.setMediaItem(mediaItem)
                        it.prepare()
                        it.play()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.stream_error, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        // Solo liberamos el controlador; el servicio sigue reproduciendo en segundo plano
        mediaController?.release()
        mediaController = null
        playerView.player = null
        super.onDestroy()
    }
}
