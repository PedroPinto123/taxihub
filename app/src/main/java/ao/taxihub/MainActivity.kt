package ao.taxihub
 
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
 
data class TaxiApp(val name: String, val packageName: String)
 
val taxiApps = listOf(
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
}
 
class MainActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TaxiListScreen(refresh)
            }
        }
    }
 
    override fun onResume() {
        super.onResume()
        refresh++
    }
}
 
@Composable
fun TaxiListScreen(refresh: Int) {
    val context = LocalContext.current
    val prefs = remember { TaxiPrefs(context) }
    var changes by remember { mutableIntStateOf(0) }
 
    val installedCount = remember(refresh) {
        taxiApps.count { isInstalled(context, it.packageName) }
    }
    val sorted = remember(refresh, changes) {
        taxiApps.sortedByDescending { prefs.isFavorite(it.packageName) }
    }
 
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Táxi Ágil", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Todos os teus táxis num só sítio")
        Spacer(Modifier.height(12.dp))
        SummaryCard(installedCount, taxiApps.size)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(sorted, key = { it.packageName }) { taxi ->
                TaxiCard(
                    taxi = taxi,
                    refresh = refresh,
                    favorite = prefs.isFavorite(taxi.packageName),
                    opens = prefs.opens(taxi.packageName),
                    onToggleFavorite = {
                        prefs.setFavorite(taxi.packageName, !prefs.isFavorite(taxi.packageName))
                        changes++
                    },
                    onOpen = {
                        if (isInstalled(context, taxi.packageName)) {
                            prefs.addOpen(taxi.packageName)
                            changes++
                        }
                        openTaxi(context, taxi)
                    }
                )
            }
        }
    }
}
 
@Composable
fun SummaryCard(installed: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                installed.toString() + " de " + total.toString() + " apps instaladas",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (installed == total) "Tens todos os táxis à mão."
                else "Toca nas que faltam para as instalares."
            )
        }
    }
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
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        taxi.name.take(1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(taxi.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (!installed) "Não instalada"
                    else if (opens == 0) "Instalada"
                    else "Instalada · aberta " + opens.toString() + (if (opens == 1) " vez" else " vezes")
                )
            }
            Text(
                if (favorite) "★" else "☆",
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onToggleFavorite() }
                    .padding(8.dp)
            )
            Text(
                if (installed) "Abrir" else "Instalar",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
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
