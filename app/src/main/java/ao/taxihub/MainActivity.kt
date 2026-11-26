package ao.taxihub
 
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 
val Yellow = Color(0xFFFFC107)
val Ink = Color(0xFF1A1A1A)
val Paper = Color(0xFFFFFBF0)
 
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
        if (name.isNotBlank() && pkg.isNotBlank()) {
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
        onPrimary = Yellow,
        primaryContainer = Yellow,
        onPrimaryContainer = Ink,
        secondaryContainer = Yellow,
        onSecondaryContainer = Ink,
        background = Paper,
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
 
    val installedCount = remember(refresh, taxis) {
        taxis.count { isInstalled(context, it.packageName) }
    }
    val sorted = remember(refresh, changes, taxis) {
        taxis.sortedByDescending { prefs.isFavorite(it.packageName) }
    }
    val favorites = remember(refresh, changes, taxis) {
        taxis.filter { prefs.isFavorite(it.packageName) }
    }
    val onChanged: () -> Unit = { changes++ }
 
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("\uD83D\uDE95", fontSize = 22.sp) },
                    label = { Text("Início") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("★", fontSize = 22.sp) },
                    label = { Text("Favoritos") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Text("•••", fontSize = 18.sp) },
                    label = { Text("Mais") }
                )
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = tab,
            modifier = Modifier.padding(innerPadding),
            label = "separadores"
        ) { current ->
            when (current) {
                0 -> HomeTab(installedCount, taxis.size, sorted, prefs, refresh, onChanged)
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
    taxis: List<TaxiApp>,
    prefs: TaxiPrefs,
    refresh: Int,
    onChanged: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header() }
        item { ZoneCard() }
        item { SummaryCard(installed, total) }
        items(taxis, key = { it.packageName }) { taxi ->
            TaxiRow(taxi, prefs, refresh, onChanged)
        }
    }
}
 
@Composable
fun FavoritesTab(
    favorites: List<TaxiApp>,
    prefs: TaxiPrefs,
    refresh: Int,
    onChanged: () -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ainda não tens favoritos", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "No separador Início, toca na estrela dos táxis que mais usas.",
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Os teus favoritos", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            items(favorites, key = { it.packageName }) { taxi ->
                TaxiRow(taxi, prefs, refresh, onChanged)
            }
        }
    }
}
 
@Composable
fun MoreTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header() }
        item {
            InfoCard(
                "Sobre",
                "O Táxi Ágil junta as apps de táxi de Angola num só sítio. Toca num táxi para o abrir, ou para o instalar se ainda não o tens."
            )
        }
        item {
            InfoCard(
                "Dica",
                "Toca na estrela para fixar os teus táxis preferidos. Eles sobem para o topo e aparecem no separador Favoritos."
            )
        }
        item {
            InfoCard(
                "Em breve",
                "Localização, conta pessoal e a opinião de outros utilizadores sobre qual app responde mais depressa na tua zona."
            )
        }
        item { Text("Versão 1.2", modifier = Modifier.padding(top = 8.dp)) }
    }
}
 
@Composable
fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink)
            .padding(20.dp)
    ) {
        Column {
            Text("Táxi Ágil", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Yellow)
            Text("Todos os teus táxis num só sítio", color = Color.White)
        }
    }
}
 
@Composable
fun SummaryCard(installed: Int, total: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Yellow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                installed.toString() + " de " + total.toString() + " apps instaladas",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else installed.toFloat() / total.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Ink,
                trackColor = Color(0x33000000)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (installed == total) "Tens todos os táxis à mão."
                else "Toca nas que faltam para as instalares.",
                color = Ink
            )
        }
    }
}
 
@Composable
fun InfoCard(title: String, text: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text)
        }
    }
}
 
@Composable
fun TaxiRow(taxi: TaxiApp, prefs: TaxiPrefs, refresh: Int, onChanged: () -> Unit) {
    val context = LocalContext.current
    TaxiCard(
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
fun TaxiCard(
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
 
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = taxi.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        taxi.name.take(1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(taxi.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (!installed) "Não instalada"
                    else if (opens == 0) "Instalada"
                    else "Aberta " + opens.toString() + (if (opens == 1) " vez" else " vezes"),
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            Text(
                if (favorite) "★" else "☆",
                fontSize = 28.sp,
                color = if (favorite) Color(0xFFF9A825) else Color(0xFF9E9E9E),
                modifier = Modifier
                    .clickable { onToggleFavorite() }
                    .padding(8.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (installed) Ink else Yellow)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    if (installed) "Abrir" else "Instalar",
                    color = if (installed) Yellow else Ink,
                    fontWeight = FontWeight.Bold
                )
            }
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
