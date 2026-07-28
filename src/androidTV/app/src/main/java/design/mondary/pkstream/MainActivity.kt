package design.mondary.pkstream

import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.delay
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val API = "https://mondary.design/pk/stream/api.php"
private const val PROXY = "https://mondary.design/pk/stream/stremio/proxy.php?embed="

data class Content(
    val newsid: String,
    val title: String,
    val poster: String,
    val type: String = "film",
    val backdrop: String = "",
)
data class Home(val films: List<Content>, val series: List<Content>)
data class Stream(val title: String, val url: String)
data class Season(val season: Int, val newsid: String, val title: String)
data class Episode(val number: Int, val title: String, val synopsis: String, val poster: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PKStreamApp() }
    }
}

@Composable
private fun PKStreamApp() {
    var selected by remember { mutableStateOf<Content?>(null) }
    var playerStream by remember { mutableStateOf<Stream?>(null) }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = Color(0xFF070707), surface = Color(0xFF181818), primary = Color(0xFFF0C44E))) {
        when {
            playerStream != null -> PlayerScreen(playerStream!!, onBack = { playerStream = null })
            selected != null -> DetailsScreen(selected!!, onBack = { selected = null }, onPlay = { playerStream = it })
            else -> HomeScreen(onSelect = { selected = it })
        }
    }
}

@Composable
private fun HomeScreen(onSelect: (Content) -> Unit) {
    val home by produceState<Home?>(initialValue = null) { value = runCatching { Api.featured() }.getOrNull() }
    var query by remember { mutableStateOf("") }
    val firstCardFocus = remember { FocusRequester() }
    val search by produceState<List<Content>>(initialValue = emptyList(), query) {
        value = if (query.length < 3) emptyList() else runCatching { Api.search(query) }.getOrDefault(emptyList())
    }
    LaunchedEffect(home?.films) {
        if (home?.films?.isNotEmpty() == true) {
            try { firstCardFocus.requestFocus() } catch (_: Exception) {}
        }
    }
    Column(
        Modifier.fillMaxSize().background(Color(0xFF070707)).padding(vertical = 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.pk_stream_icon),
                contentDescription = "PK Stream",
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(16.dp))
            Text("PK ", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("STREAM", color = Color(0xFFF0C44E), fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            OutlinedTextField(query, { query = it }, label = { Text("Rechercher") }, singleLine = true, modifier = Modifier.width(440.dp))
        }
        Spacer(Modifier.height(26.dp))
        if (query.length >= 3) ContentRow("Résultats", search, onSelect, null)
        else if (home == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Chargement…", color = Color.LightGray) }
        else {
            ContentRow("Derniers films", home!!.films, onSelect, firstCardFocus)
            Spacer(Modifier.height(28.dp))
            ContentRow("Dernières séries", home!!.series, onSelect, null)
        }
    }
}

@Composable
private fun ContentRow(title: String, entries: List<Content>, onSelect: (Content) -> Unit, firstFocus: FocusRequester?) {
    Column {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 48.dp, vertical = 10.dp))
        LazyRow(
            modifier = Modifier.height(294.dp),
            contentPadding = PaddingValues(horizontal = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            itemsIndexed(entries, key = { _, it -> it.newsid }) { index, entry ->
                PosterCard(entry, onSelect, if (index == 0) firstFocus else null)
            }
        }
    }
}

@Composable
private fun PosterCard(content: Content, onSelect: (Content) -> Unit, focusRequester: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(172.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.06f else 1f)
            .border(if (focused) 4.dp else 0.dp, Color(0xFFF0C44E), RoundedCornerShape(10.dp))
            .focusable().clickable { onSelect(content) }
    ) {
        AsyncImage(content.poster, content.title, Modifier.fillMaxWidth().height(248.dp).clip(RoundedCornerShape(8.dp)))
        Text(content.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun DetailsScreen(content: Content, onBack: () -> Unit, onPlay: (Stream) -> Unit) {
    BackHandler(onBack = onBack)
    if (content.type == "tv") SeriesScreen(content, onBack, onPlay)
    else FilmScreen(content, onBack, onPlay)
}

@Composable
private fun FilmScreen(content: Content, onBack: () -> Unit, onPlay: (Stream) -> Unit) {
    val streams by produceState<List<Stream>>(initialValue = emptyList(), content.newsid) {
        value = runCatching { Api.streams(content.newsid, "film") }.getOrDefault(emptyList())
    }
    Row(Modifier.fillMaxSize().background(Color(0xFF070707)).padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(content.poster, content.title, Modifier.width(280.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.width(44.dp))
        Column(Modifier.fillMaxHeight().padding(vertical = 42.dp), verticalArrangement = Arrangement.Center) {
            Text(content.title, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(24.dp))
            Text("Sources disponibles", color = Color.LightGray, fontSize = 18.sp)
            Spacer(Modifier.height(18.dp))
            if (streams.isEmpty()) Text("Chargement des sources…", color = Color.Gray)
            streams.forEach { stream -> SourceButton(stream, onPlay) }
        }
    }
}

@Composable
private fun SeriesScreen(content: Content, onBack: () -> Unit, onPlay: (Stream) -> Unit) {
    var activeSeason by remember { mutableStateOf(1) }
    var activeSeasonNid by remember { mutableStateOf(content.newsid) }
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }

    val seasons by produceState<List<Season>>(emptyList(), content.newsid) {
        value = runCatching { Api.seasons(Api.baseTitle(content.title), content.newsid) }.getOrDefault(emptyList())
    }

    if (selectedEpisode != null) {
        EpisodeSourcesScreen(content.title, activeSeason, selectedEpisode!!, activeSeasonNid,
            onBack = { selectedEpisode = null }, onPlay = onPlay)
        return
    }

    val episodes by produceState<List<Episode>>(emptyList(), activeSeasonNid) {
        value = runCatching { Api.episodes(activeSeasonNid) }.getOrDefault(emptyList())
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF070707)).padding(48.dp).verticalScroll(rememberScrollState())) {
        Text(content.title, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(24.dp))

        if (seasons.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                seasons.forEach { s ->
                    SeasonTab(s, activeSeason == s.season) {
                        activeSeason = s.season
                        activeSeasonNid = s.newsid
                    }
                }
            }
        }

        if (episodes.isEmpty()) {
            Text("Chargement des épisodes…", color = Color.Gray, modifier = Modifier.padding(top = 40.dp))
        } else {
            episodes.forEach { ep -> EpisodeCard(ep, activeSeason) { selectedEpisode = it } }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SeasonTab(season: Season, isActive: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.clip(RoundedCornerShape(999.dp))
            .background(when { focused -> Color.White; isActive -> Color(0xFFF0C44E); else -> Color(0xFF242424) })
            .border(2.dp, if (focused || isActive) Color(0xFFF0C44E) else Color(0xFF444444), RoundedCornerShape(999.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text("Saison ${season.season}", color = if (focused || isActive) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EpisodeCard(episode: Episode, season: Int, onClick: (Episode) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color(0xFF2A2A2A) else Color(0xFF181818))
            .border(if (focused) 3.dp else 1.dp, if (focused) Color(0xFFF0C44E) else Color(0xFF333333), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable { onClick(episode) }
            .padding(12.dp)
    ) {
        if (episode.poster.isNotEmpty()) {
            AsyncImage(episode.poster, "", Modifier.width(160.dp).height(90.dp).clip(RoundedCornerShape(6.dp)))
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("S${season} E${episode.number}", color = Color(0xFFF0C44E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(episode.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (episode.synopsis.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(episode.synopsis, color = Color.Gray, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EpisodeSourcesScreen(seriesTitle: String, season: Int, episode: Episode, seasonNid: String, onBack: () -> Unit, onPlay: (Stream) -> Unit) {
    BackHandler(onBack = onBack)
    val streams by produceState<List<Stream>>(emptyList(), episode.number) {
        value = runCatching { Api.streams(seasonNid, "tv", season, episode.number) }.getOrDefault(emptyList())
    }
    Row(Modifier.fillMaxSize().background(Color(0xFF070707)).padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.fillMaxHeight().padding(vertical = 42.dp), verticalArrangement = Arrangement.Center) {
            Text(seriesTitle, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
            Text("S${season} E${episode.number} - ${episode.title}", color = Color(0xFFF0C44E), fontSize = 20.sp)
            Spacer(Modifier.height(24.dp))
            Text("Sources disponibles", color = Color.LightGray, fontSize = 18.sp)
            Spacer(Modifier.height(18.dp))
            if (streams.isEmpty()) Text("Chargement des sources…", color = Color.Gray)
            streams.forEach { stream -> SourceButton(stream, onPlay) }
        }
    }
}

@Composable
private fun SourceButton(stream: Stream, onPlay: (Stream) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier.padding(vertical = 6.dp).width(360.dp).clip(RoundedCornerShape(10.dp))
            .background(if (focused) Color.White else Color(0xFF242424))
            .border(if (focused) 3.dp else 1.dp, if (focused) Color(0xFFF0C44E) else Color(0xFF4A4A4A), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().clickable { onPlay(stream) }.padding(16.dp)
    ) {
        Text("▶ ${stream.title}", color = if (focused) Color.Black else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlayerScreen(stream: Stream, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val source = remember(stream.url) { PROXY + URLEncoder.encode(Base64.encodeToString(stream.url.toByteArray(), Base64.NO_WRAP), "UTF-8") }
    var playbackError by remember(source) { mutableStateOf<String?>(null) }
    var controlsVisible by remember(source) { mutableStateOf(true) }
    var lastKeyTime by remember(source) { mutableStateOf(System.currentTimeMillis()) }
    var speed by remember(source) { mutableStateOf(1.0f) }
    var speedBadgeTime by remember(source) { mutableStateOf(0L) }
    val player = remember(source) {
        val mediaSource = HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
            .createMediaSource(MediaItem.fromUri(source))
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }
    var position by remember(player) { mutableStateOf(0L) }
    LaunchedEffect(player) { while (true) { position = player.currentPosition; delay(500) } }
    val duration = player.duration.coerceAtLeast(0)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackError = "Lecture impossible : ${error.errorCodeName}"
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    LaunchedEffect(lastKeyTime) {
        delay(4000)
        controlsVisible = false
    }

    var speedBadgeVisible by remember(speedBadgeTime) { mutableStateOf(true) }
    LaunchedEffect(speedBadgeTime) {
        if (speedBadgeTime > 0) { speedBadgeVisible = true; delay(1500); speedBadgeVisible = false }
    }

    BackHandler(onBack = onBack)
    Box(
        Modifier.fillMaxSize().background(Color.Black).focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                lastKeyTime = System.currentTimeMillis()
                controlsVisible = true
                when (event.key) {
                    Key.DirectionCenter, Key.Enter -> { if (player.isPlaying) player.pause() else player.play(); true }
                    Key.DirectionRight -> { player.seekTo((player.currentPosition + 10_000).coerceAtMost(duration)); true }
                    Key.DirectionLeft -> { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); true }
                    Key.DirectionUp -> { speed = ((speed + 0.1f).coerceAtMost(3.0f) * 10).toInt() / 10f; player.playbackParameters = PlaybackParameters(speed); speedBadgeTime = System.currentTimeMillis(); true }
                    Key.DirectionDown -> { speed = ((speed - 0.1f).coerceAtLeast(0.5f) * 10).toInt() / 10f; player.playbackParameters = PlaybackParameters(speed); speedBadgeTime = System.currentTimeMillis(); true }
                    Key.MediaPlay -> { player.play(); true }
                    Key.MediaPause -> { player.pause(); true }
                    Key.MediaPlayPause -> { if (player.isPlaying) player.pause() else player.play(); true }
                    Key.MediaRewind -> { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); true }
                    Key.MediaFastForward -> { player.seekTo((player.currentPosition + 10_000).coerceAtMost(duration)); true }
                    else -> false
                }
            }
    ) {
        AndroidView(factory = { PlayerView(it).apply {
            this.player = player
            useController = false
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } }, modifier = Modifier.fillMaxSize())

        AnimatedVisibility(controlsVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Column(
                Modifier.background(Color(0xCC000000)).fillMaxWidth().padding(horizontal = 48.dp, vertical = 20.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(position), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (speed != 1.0f) "${"%.1f".format(speed)}x" else (if (player.isPlaying) "⏸ Lecture" else "▶ Pause"), color = Color(0xFFF0C44E), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(duration), color = Color.LightGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF444444))
                ) {
                    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Color(0xFFF0C44E)))
                }
                Spacer(Modifier.height(8.dp))
                Text("◀◀ -10s    ⬆⬇ Vitesse    OK: Lecture/Pause    ▶▶ +10s    Retour: Quitter", color = Color.Gray, fontSize = 13.sp)
            }
        }

        AnimatedVisibility(speedBadgeVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
            Box(Modifier.background(Color(0xCC000000), RoundedCornerShape(16.dp)).padding(horizontal = 40.dp, vertical = 24.dp)) {
                Text("${"%.1f".format(speed)}x", color = Color(0xFFF0C44E), fontSize = 56.sp, fontWeight = FontWeight.Black)
            }
        }

        playbackError?.let { Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(24.dp).background(Color(0xCC7A1E1E), RoundedCornerShape(8.dp)).padding(14.dp)) }
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

@Composable
private fun AsyncImage(url: String, description: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, url) {
        value = runCatching { withContext(Dispatchers.IO) { android.graphics.BitmapFactory.decodeStream(URL(url).openStream()) } }.getOrNull()
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), description, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color(0xFF202020)))
}

private object Api {
    fun baseTitle(t: String): String = t.replace(Regex("\\s*-\\s*Saison\\s*\\d+.*$", RegexOption.IGNORE_CASE), "").trim()

    suspend fun featured(): Home = JSONObject(get("?api=featured")).let {
        Home(it.array("films", "film"), it.array("series", "tv"))
    }

    suspend fun search(q: String): List<Content> =
        JSONObject(get("?api=search&q=${URLEncoder.encode(q, "UTF-8")}")).array("results", null)

    suspend fun seasons(title: String, id: String): List<Season> {
        val json = JSONObject(get("?api=seasons&title=${URLEncoder.encode(title, "UTF-8")}&id=$id"))
        return json.getJSONArray("seasons").let { arr ->
            List(arr.length()) { i ->
                arr.getJSONObject(i).let { Season(it.getInt("season"), it.getString("newsid"), it.getString("title")) }
            }
        }
    }

    suspend fun episodes(nid: String): List<Episode> {
        val json = JSONObject(get("?api=episodes&id=$nid"))
        return json.getJSONArray("episodes").let { arr ->
            List(arr.length()) { i ->
                arr.getJSONObject(i).let {
                    Episode(it.getInt("number"), it.getString("title"), it.optString("synopsis", ""), it.optString("poster", ""))
                }
            }
        }
    }

    suspend fun streams(id: String, type: String, season: Int = 1, episode: Int = 1): List<Stream> {
        val suffix = if (type == "tv") "&t=tv&s=$season&e=$episode" else "&t=film"
        return JSONObject(get("?api=streams&id=$id$suffix")).getJSONArray("streams").let { array ->
            List(array.length()) { index ->
                array.getJSONObject(index).let { Stream(it.getString("title"), it.getString("url")) }
            }
        }
    }

    private fun JSONObject.array(key: String, type: String?): List<Content> = getJSONArray(key).let { array ->
        List(array.length()) { index ->
            array.getJSONObject(index).let {
                val contentType = type ?: if (it.getString("title").contains("Saison", ignoreCase = true)) "tv" else "film"
                Content(it.getString("newsid"), it.getString("title"), it.getString("poster"), contentType, it.optString("backdrop"))
            }
        }
    }

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val connection = URL(API + path).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "PK Stream TV/1.0")
        connection.inputStream.bufferedReader().use { it.readText() }
    }
}
