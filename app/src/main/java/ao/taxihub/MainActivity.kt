package ao.taxihub
 
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
 
data class TaxiApp(val name: String, val packageName: String)
 
val defaultTaxiApps: List<TaxiApp> = listOf(
    TaxiApp("Yango", "com.yandex.yango"),
    TaxiApp("inDrive", "sinet.startup.inDriver"),
    TaxiApp("Heetch", "com.heetch"),
    TaxiApp("UGO", "com.tedmob.coopetaxi"),
    TaxiApp("Vambazar", "com.vambzar.app"),
    TaxiApp("Kubinga", "com.kubinga.passenger")
)
 
class TaxiPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("taxiagil", Context.MODE_PRIVATE)
 
    fun isFavorite(pkg: String): Boolean = prefs.getBoolean("fav_" + pkg, false)
 
    fun setFavorite(pkg: String, value: Boolean) {
        prefs.edit { putBoolean("fav_" + pkg, value) }
    }
 
    fun opens(pkg: String): Int = prefs.getInt("opens_" + pkg, 0)
 
    fun addOpen(pkg: String) {
        prefs.edit { putInt("opens_" + pkg, opens(pkg) + 1) }
    }
 
    fun cachedJson(): String? = prefs.getString("taxis_json", null)
 
    fun saveJson(json: String) {
        prefs.edit { putString("taxis_json", json) }
    }
}
 
const val REMOTE_URL = "https://raw.githubusercontent.com/PedroPinto123/taxihub/main/taxis.json"
 
fun parseTaxis(json: String): List<TaxiApp> {
    val result = mutableListOf<TaxiApp>()
    val array = JSONObject(json).getJSONArray("taxis")
    for (i in 0 until array.length()) {
        val item = array.getJSONObject(i)
        val name = item.getString("nome")
        val pkg = item.getString("pacote")
        val active = item.optBoolean("ativo", true)
        if (active && name.isNotBlank() && pkg.isNotBlank()) {
            result.add(TaxiApp(name, pkg))
        }
    }
    return result
}
 
class MainActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
    private var taxis by mutableStateOf(defaultTaxiApps)
    private lateinit var prefs: TaxiPrefs
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TaxiPrefs(this)
        loadCachedList()
        fetchRemoteList()
        setContent {
            TaxiAgilTheme {
                TaxiAgilApp(refresh, taxis)
            }
        }
    }
 
    override fun onResume() {
        super.onResume()
        refresh++
    }
 
    private fun loadCachedList() {
        val cached = prefs.cachedJson() ?: return
        try {
            val parsed = parseTaxis(cached)
            if (parsed.isNotEmpty()) {
                taxis = parsed
            }
        } catch (e: Exception) {
            // lista guardada invalida: fica a lista de origem
        }
    }
 
    private fun fetchRemoteList() {
        Thread {
            try {
                val conn = URL(REMOTE_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseTaxis(text)
                if (parsed.isNotEmpty()) {
                    prefs.saveJson(text)
                    runOnUiThread { taxis = parsed }
                }
            } catch (e: Exception) {
                // sem internet ou ficheiro invalido: fica a lista que ja la estava
            }
        }.start()
    }
}
 
@Composable
fun TaxiAgilTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = Ink,
        onPrimary = Color.White,
        secondaryContainer = Raised,
        onSecondaryContainer = Ink,
        background = Bg,
        onBackground = Ink,
        surface = Color.White,
        onSurface = Ink
    )
    MaterialTheme(colorScheme = colors, content = content)
}
 
@Composable
fun TaxiAgilApp(refresh: Int, taxis: List<TaxiApp>) {
    val context = LocalContext.current
    val prefs = remember { TaxiPrefs(context) }
    var changes by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
 
    val missing = remember(refresh, taxis) {
        taxis.filter { !isInstalled(context, it.packageName) }.map { it.name }
    }
    val installedCount = taxis.size - missing.size
    val sorted = remember(refresh, changes, taxis) {
        taxis.sortedByDescending { prefs.isFavorite(it.packageName) }
    }
    val favorites = remember(refresh, changes, taxis) {
        taxis.filter { prefs.isFavorite(it.packageName) }
    }
    val onChanged: () -> Unit = { changes++ }
 
    Scaffold(
        containerColor = Bg,
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline)
                )
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Ink,
                        selectedTextColor = Ink,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted,
                        indicatorColor = Raised
                    )
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { AppIcon(LineIcon.Home, LocalContentColor.current) },
                        label = { Text("Início", fontSize = 12.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { AppIcon(LineIcon.Star, LocalContentColor.current, filled = tab == 1) },
                        label = { Text("Favoritos", fontSize = 12.sp) },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { AppIcon(LineIcon.More, LocalContentColor.current) },
                        label = { Text("Mais", fontSize = 12.sp) },
                        colors = itemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = tab,
            modifier = Modifier.padding(innerPadding),
            animationSpec = tween(durationMillis = 180, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
            label = "separadores"
        ) { current ->
            when (current) {
                0 -> HomeTab(installedCount, taxis.size, missing, sorted, prefs, refresh, onChanged)
                1 -> FavoritesTab(favorites, prefs, refresh, onChanged)
                else -> MoreTab()
            }
        }
    }
}
 
@Composable
fun HomeTab(
    installed: Int,
    total: Int,
    missing: List<String>,
    taxis: List<TaxiApp>,
    prefs: TaxiPrefs,
    refresh: Int,
    onChanged: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoofSign()
                Spacer(Modifier.width(12.dp))
                PageTitle("Táxi Ágil")
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { ZoneCard() }
        item { Spacer(Modifier.height(24.dp)) }
        item { SummaryBlock(installed, total, missing) }
        item { Spacer(Modifier.height(24.dp)) }
        item { TaxiGroup(taxis, prefs, refresh, onChanged) }
    }
}
 
@Composable
fun FavoritesTab(
    favorites: List<TaxiApp>,
    prefs: TaxiPrefs,
    refresh: Int,
    onChanged: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)
    ) {
        item { PageTitle("Favoritos") }
        item { Spacer(Modifier.height(24.dp)) }
        if (favorites.isEmpty()) {
            item { EmptyFavorites() }
        } else {
            item { TaxiGroup(favorites, prefs, refresh, onChanged) }
        }
    }
}
 
@Composable
fun EmptyFavorites() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(LineIcon.Star, Muted, iconSize = 32.dp)
        Spacer(Modifier.height(12.dp))
        Text("Sem favoritos", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(
            "Toca na estrela de um táxi no Início",
            fontSize = 14.sp,
            color = Secondary,
            textAlign = TextAlign.Center
        )
    }
}
 
@Composable
fun MoreTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)
    ) {
        item { PageTitle("Mais") }
        item { Spacer(Modifier.height(24.dp)) }
        item {
            GroupCard {
                InfoBlock(
                    "Sobre",
                    "A Táxi Ágil junta as apps de táxi de Angola num só ecrã. Toca num táxi para o abrir ou, se ainda não o tens, para o instalar."
                )
                RowDivider()
                InfoBlock(
                    "Dica",
                    "Toca na estrela para fixar os teus táxis preferidos. Eles sobem para o topo da lista e aparecem em Favoritos."
                )
                RowDivider()
                InfoBlock(
                    "Em breve",
                    "Conta pessoal e a opinião de outros utilizadores sobre qual app responde mais depressa na tua zona."
                )
                RowDivider()
                SuggestBlock()
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item { Text("Versão 1.4", fontSize = 12.sp, color = Muted) }
    }
}
 
@Composable
fun InfoBlock(title: String, text: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 14.sp, lineHeight = 20.sp, color = Secondary)
    }
}
 
@Composable
fun SummaryBlock(installed: Int, total: Int, missing: List<String>) {
    val fraction = if (total == 0) 0f else installed.toFloat() / total.toFloat()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            installed.toString() + " de " + total.toString() + " instaladas",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Ink
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Ink)
            )
        }
        if (missing.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                (if (missing.size == 1) "Falta: " else "Faltam: ") + missing.joinToString(", "),
                fontSize = 13.sp,
                color = Muted
            )
        }
    }
}
 
@Composable
fun TaxiGroup(taxis: List<TaxiApp>, prefs: TaxiPrefs, refresh: Int, onChanged: () -> Unit) {
    GroupCard {
        taxis.forEachIndexed { index, taxi ->
            if (index > 0) {
                RowDivider(startInset = 72.dp)
            }
            TaxiRow(taxi, prefs, refresh, onChanged)
        }
    }
}
 
@Composable
fun TaxiRow(taxi: TaxiApp, prefs: TaxiPrefs, refresh: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    TaxiItem(
        taxi = taxi,
        refresh = refresh,
        favorite = prefs.isFavorite(taxi.packageName),
        opens = prefs.opens(taxi.packageName),
        onToggleFavorite = {
            prefs.setFavorite(taxi.packageName, !prefs.isFavorite(taxi.packageName))
            onChanged()
        },
        onOpen = {
            if (isInstalled(context, taxi.packageName)) {
                prefs.addOpen(taxi.packageName)
                onChanged()
            }
            openTaxi(context, taxi)
        }
    )
}
 
@Composable
fun TaxiItem(
    taxi: TaxiApp,
    refresh: Int,
    favorite: Boolean,
    opens: Int,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val icon = remember(refresh) { loadIcon(context, taxi.packageName) }
    val installed = icon != null
    val meta = if (!installed) "Não instalada"
    else if (opens == 0) "Instalada"
    else if (opens == 1) "Aberta 1 vez"
    else "Aberta " + opens.toString() + " vezes"
 
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = taxi.name,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Raised),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    taxi.name.take(1),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Secondary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                taxi.name,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            )
            Text(meta, fontSize = 13.sp, lineHeight = 16.sp, color = Muted)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onToggleFavorite() },
            contentAlignment = Alignment.Center
        ) {
            AppIcon(
                LineIcon.Star,
                if (favorite) Ink else Muted,
                iconSize = 22.dp,
                filled = favorite
            )
        }
        Spacer(Modifier.width(4.dp))
        if (installed) {
            QuietButton("Abrir", Modifier.width(84.dp)) { onOpen() }
        } else {
            AccentButton("Instalar", Modifier.width(84.dp)) { onOpen() }
        }
    }
}
 
fun isInstalled(context: Context, pkg: String): Boolean {
    return try {
        context.packageManager.getApplicationInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
 
fun loadIcon(context: Context, pkg: String): ImageBitmap? {
    return try {
        context.packageManager.getApplicationIcon(pkg).toBitmap().asImageBitmap()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
 
fun openTaxi(context: Context, taxi: TaxiApp) {
    val launch = context.packageManager.getLaunchIntentForPackage(taxi.packageName)
    if (launch != null) {
        context.startActivity(launch)
    } else {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=" + taxi.packageName)
        )
        try {
            context.startActivity(market)
        } catch (e: Exception) {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + taxi.packageName)
            )
            context.startActivity(web)
        }
    }
}
