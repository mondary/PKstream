package design.mondary.pkstream

import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
    val search by produceState<List<Content>>(initialValue = emptyList(), query) {
        value = if (query.length < 3) emptyList() else runCatching { Api.search(query) }.getOrDefault(emptyList())
    }
    Column(Modifier.fillMaxSize().background(Color(0xFF070707)).padding(vertical = 28.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("PK ", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("STREAM", color = Color(0xFFF0C44E), fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            OutlinedTextField(query, { query = it }, label = { Text("Rechercher") }, singleLine = true, modifier = Modifier.width(440.dp))
        }
        Spacer(Modifier.height(26.dp))
        if (query.length >= 3) ContentRow("Résultats", search, onSelect)
        else if (home == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Chargement…", color = Color.LightGray) }
        else {
            ContentRow("Derniers films", home!!.films, onSelect)
            Spacer(Modifier.height(28.dp))
            ContentRow("Dernières séries", home!!.series, onSelect)
        }
    }
}

@Composable
private fun ContentRow(title: String, entries: List<Content>, onSelect: (Content) -> Unit) {
    Column {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 48.dp, vertical = 10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 48.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(entries, key = { it.newsid }) { entry -> PosterCard(entry, onSelect) }
        }
    }
}

@Composable
private fun PosterCard(content: Content, onSelect: (Content) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(156.dp).onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.08f else 1f)
            .border(if (focused) 3.dp else 0.dp, Color(0xFFF0C44E), RoundedCornerShape(10.dp))
            .focusable().clickable { onSelect(content) }
    ) {
        AsyncImage(content.poster, content.title, Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)))
        Text(content.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun DetailsScreen(content: Content, onBack: () -> Unit, onPlay: (Stream) -> Unit) {
    val streams by produceState<List<Stream>>(initialValue = emptyList(), content.newsid) { value = runCatching { Api.streams(content.newsid, content.type) }.getOrDefault(emptyList()) }
    Row(Modifier.fillMaxSize().background(Color(0xFF070707)).padding(56.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(content.poster, content.title, Modifier.width(280.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.width(44.dp))
        Column(Modifier.fillMaxHeight().padding(vertical = 42.dp), verticalArrangement = Arrangement.Center) {
            Text(content.title, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text(if (content.type == "tv") "Sources - épisode 1" else "Sources disponibles", color = Color.LightGray, fontSize = 18.sp)
            Spacer(Modifier.height(18.dp))
            if (streams.isEmpty()) Text("Chargement des sources…", color = Color.Gray)
            streams.forEach { stream ->
                Button(onClick = { onPlay(stream) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0C44E), contentColor = Color.Black), modifier = Modifier.padding(vertical = 5.dp)) { Text("▶ ${stream.title}") }
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = onBack) { Text("Retour") }
        }
    }
}

@Composable
private fun PlayerScreen(stream: Stream, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val source = remember(stream.url) { PROXY + URLEncoder.encode(Base64.encodeToString(stream.url.toByteArray(), Base64.NO_WRAP), "UTF-8") }
    val player = remember(source) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(source)); prepare(); playWhenReady = true } }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { PlayerView(it).apply { this.player = player; layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) } }, modifier = Modifier.fillMaxSize())
        Button(onClick = onBack, modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)) { Text("Retour") }
    }
}

@Composable
private fun AsyncImage(url: String, description: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, url) { value = runCatching { withContext(Dispatchers.IO) { android.graphics.BitmapFactory.decodeStream(URL(url).openStream()) } }.getOrNull() }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), description, modifier, contentScale = ContentScale.Crop)
    else Box(modifier.background(Color(0xFF202020)))
}

private object Api {
    suspend fun featured(): Home = JSONObject(get("?api=featured")).let {
        Home(it.array("films", "film"), it.array("series", "tv"))
    }

    suspend fun search(q: String): List<Content> =
        JSONObject(get("?api=search&q=${URLEncoder.encode(q, "UTF-8")}")).array("results", null)

    suspend fun streams(id: String, type: String): List<Stream> {
        val suffix = if (type == "tv") "&t=tv&s=1&e=1" else "&t=film"
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
        (URL(API + path).openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 30000; setRequestProperty("User-Agent", "PK Stream TV/1.0"); inputStream.bufferedReader().use { return@withContext it.readText() } }
    }
}
