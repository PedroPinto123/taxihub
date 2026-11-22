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
import androidx.core.graphics.drawable.toBitmap
 
data class TaxiApp(val name: String, val packageName: String)
 
val taxiApps = listOf(
    TaxiApp("Yango", "com.yandex.yango"),
    TaxiApp("inDrive", "sinet.startup.inDriver"),
    TaxiApp("Heetch", "com.heetch"),
    TaxiApp("Coopetaxi", "com.tedmob.coopetaxi"),
    TaxiApp("Vambzar", "com.vambzar.app"),
    TaxiApp("Kubinga", "com.kubinga.passenger"),
    TaxiApp("OnTaxi", "ua.com.ontaxi.client"),
    TaxiApp("Yassir", "com.yatechnologies.yassir_rider"),
    TaxiApp("Move It", "com.moveit.app.customer")
)
 
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("TaxiHub", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Todos os teus táxis num só sítio")
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(taxiApps) { taxi ->
                TaxiCard(taxi, refresh)
            }
        }
    }
}
 
@Composable
fun TaxiCard(taxi: TaxiApp, refresh: Int) {
    val context = LocalContext.current
    val icon = remember(refresh) { loadIcon(context, taxi.packageName) }
    val installed = icon != null
 
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openTaxi(context, taxi) }
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
                Text(if (installed) "Instalada" else "Não instalada")
            }
            Text(
                if (installed) "Abrir" else "Instalar",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
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
