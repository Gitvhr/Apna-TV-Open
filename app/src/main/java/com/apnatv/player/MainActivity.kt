package com.apnatv.player

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.net.Uri
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors

data class Channel(val name: String, val url: String, val group: String = "Live TV")

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var playerView: PlayerView
    private lateinit var list: ListView
    private lateinit var status: TextView
    private lateinit var playlistInput: EditText
    private var player: ExoPlayer? = null
    private val channels = mutableListOf<Channel>()
    private val shownChannels = mutableListOf<Channel>()
    private val io = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("apna_tv", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 19, 26)
        window.navigationBarColor = Color.rgb(16, 19, 26)
        buildUi()
        val saved = prefs.getString("playlist_url", "") ?: ""
        playlistInput.setText(saved)
        if (saved.isNotBlank()) loadPlaylist(saved)
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 18, 24, 18)
            setBackgroundColor(Color.rgb(16, 19, 26))
        }
        val header = TextView(this).apply {
            text = "APNA TV   •   YOUR TV, YOUR PLAYLIST"
            textSize = 23f
            setTextColor(Color.rgb(66, 214, 164))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 18)
        }
        root.addView(header, LinearLayout.LayoutParams(-1, -2))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        playlistInput = EditText(this).apply {
            hint = "Paste your M3U playlist URL"
            setHintTextColor(Color.LTGRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            singleLine = true
            setPadding(14, 8, 14, 8)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(35, 40, 52)); cornerRadius = 12f
                setStroke(1, Color.rgb(70, 80, 98))
            }
        }
        row.addView(playlistInput, LinearLayout.LayoutParams(0, 58, 1f))
        val load = button("LOAD PLAYLIST")
        load.setOnClickListener {
            val value = playlistInput.text.toString().trim()
            if (value.isNotBlank()) {
                prefs.edit().putString("playlist_url", value).apply()
                loadPlaylist(value)
            } else toast("Enter an M3U playlist URL")
        }
        val loadParams = LinearLayout.LayoutParams(-2, 58); loadParams.leftMargin = 10
        row.addView(load, loadParams)
        root.addView(row)

        status = TextView(this).apply {
            text = "Add an M3U URL to load your channels."
            textSize = 14f; setTextColor(Color.LTGRAY); setPadding(8, 12, 8, 10)
        }
        root.addView(status)

        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(Color.BLACK)
            contentDescription = "Video player"
        }
        body.addView(playerView, LinearLayout.LayoutParams(0, 1, 2.1f))
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val search = EditText(this).apply {
            hint = "Search channels"
            setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY)
            setSingleLine(true); textSize = 15f
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.rgb(35, 40, 52)); cornerRadius = 10f
            }
            setPadding(12, 8, 12, 8)
        }
        right.addView(search, LinearLayout.LayoutParams(-1, 48))
        list = ListView(this).apply {
            divider = android.graphics.drawable.ColorDrawable(Color.rgb(48, 54, 67))
            dividerHeight = 1
            setBackgroundColor(Color.rgb(23, 27, 36))
        }
        right.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        body.addView(right, LinearLayout.LayoutParams(0, 1, 1f))
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        list.setOnItemClickListener { _, _, position, _ ->
            if (position in shownChannels.indices) play(shownChannels[position])
        }
        search.addTextChangedListener(SimpleTextWatcher { query ->
            showChannels(channels.filter { it.name.contains(query, true) || it.group.contains(query, true) })
        })
        setContentView(root)
    }

    private fun button(label: String): Button = Button(this).apply {
        text = label; textSize = 12f; isAllCaps = false
        setTextColor(Color.rgb(16, 19, 26))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(66, 214, 164)); cornerRadius = 12f
        }
        isFocusable = true
    }

    private fun loadPlaylist(address: String) {
        status.text = "Loading playlist…"
        io.execute {
            try {
                val url = URL(address)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.setRequestProperty("User-Agent", "ApnaTV/0.1 Android")
                conn.connect()
                if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
                val parsed = parseM3u(BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)))
                conn.disconnect()
                runOnUiThread {
                    channels.clear(); channels.addAll(parsed)
                    showChannels(channels)
                    status.text = if (channels.isEmpty()) "No channels found. Check the playlist URL/format." else "${channels.size} channels loaded • Select a channel to play"
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not load playlist: ${e.message ?: "check URL and connection"}" }
            }
        }
    }

    private fun parseM3u(reader: BufferedReader): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = "Unknown channel"
        var group = "Live TV"
        reader.useLines { lines ->
            lines.forEach { line ->
                val s = line.trim()
                if (s.startsWith("#EXTINF", true)) {
                    name = s.substringAfter(",", "Unknown channel").trim().ifBlank { "Unknown channel" }
                    group = Regex("""group-title=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                        .find(s)?.groupValues?.get(1) ?: "Live TV"
                } else if (s.isNotBlank() && !s.startsWith("#") &&
                    (s.startsWith("http://", true) || s.startsWith("https://", true))) {
                    result.add(Channel(name, s, group))
                    name = "Unknown channel"; group = "Live TV"
                }
            }
        }
        return result
    }

    private fun showChannels(items: List<Channel>) {
        shownChannels.clear(); shownChannels.addAll(items)
        list.adapter = object : BaseAdapter() {
            override fun getCount() = shownChannels.size
            override fun getItem(position: Int) = shownChannels[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val channel = getItem(position)
                return (convertView as? LinearLayout ?: LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(14, 10, 14, 10)
                    isFocusable = true; isFocusableInTouchMode = true
                }).apply {
                    removeAllViews()
                    addView(TextView(this@MainActivity).apply {
                        text = channel.name; textSize = 15f; setTextColor(Color.WHITE)
                    })
                    addView(TextView(this@MainActivity).apply {
                        text = channel.group; textSize = 11f; setTextColor(Color.rgb(66, 214, 164))
                    })
                }
            }
        }
    }

    private fun play(channel: Channel) {
        try {
            if (player == null) {
                player = ExoPlayer.Builder(this).build()
                playerView.player = player
            }
            status.text = "Playing: ${channel.name}"
            player?.setMediaItem(MediaItem.fromUri(Uri.parse(channel.url)))
            player?.prepare()
            player?.playWhenReady = true
        } catch (e: Exception) {
            status.text = "Playback error: ${e.message ?: "unsupported stream"}"
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        io.shutdown()
        super.onDestroy()
    }
}

class SimpleTextWatcher(private val onChange: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChange(s?.toString() ?: "") }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
